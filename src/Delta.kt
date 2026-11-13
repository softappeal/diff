package ch.softappeal.diff

enum class DeltaState { Same, New, Deleted, Changed, FileToDir, DirToFile }
enum class FromState { MovedFrom, RenamedFrom }

sealed class Delta(
    val parent: DirectoryDelta?,
    val name: String,
    val state: DeltaState,
    val digest: Digest?,
) {
    var fromState: FromState? = null
    var fromDelta: Delta? = null
}

private class FileDelta(
    parent: DirectoryDelta,
    name: String,
    state: DeltaState,
    digest: Digest?,
) : Delta(parent, name, state, digest) {
    init {
        check(state == DeltaState.New || state == DeltaState.Deleted || state == DeltaState.Changed)
        check(digest != null || state == DeltaState.Changed)
    }
}

class DirectoryDelta(
    parent: DirectoryDelta?,
    name: String,
    state: DeltaState,
    digest: Digest?,
) : Delta(parent, name, state, digest) {
    val deltas = mutableListOf<Delta>()

    init {
        check(state != DeltaState.Changed)
        check(digest == null || state == DeltaState.DirToFile)
    }
}

fun DirectoryDelta(oldNodeDesc: NodeDesc, newNodeDesc: NodeDesc) = DirectoryDelta(null, "", DeltaState.Same, null).apply {
    val movedDigestToDelta = mutableMapOf<Digest, Delta>()

    fun DirectoryDelta.compare(oldNode: DirectoryNode, newNode: DirectoryNode) {
        val oldIter = oldNode.nodes.iterator()
        val newIter = newNode.nodes.iterator()
        fun Iterator<Node>.nextOrNull() = if (hasNext()) next() else null
        var old = oldIter.nextOrNull()
        var new = newIter.nextOrNull()
        while (true) {
            fun Delta.tryAddMoved(old: FileNode): Boolean {
                val oldDigest = old.digest.toDigest()
                val oldPaths = oldNodeDesc.digestToPaths[oldDigest]
                check(oldPaths != null)
                val newPaths = newNodeDesc.digestToPaths[oldDigest]
                if (newPaths == null || newPaths.size != 1 || oldPaths.size != 1) return false
                check(movedDigestToDelta.put(oldDigest, this) == null)
                return true
            }

            fun DirectoryDelta.addNode(node: Node, state: DeltaState) {
                check(state == DeltaState.New || state == DeltaState.Deleted)
                when (node) {
                    is FileNode -> {
                        val delta = FileDelta(this, node.name, state, node.digest.toDigest())
                        if (state == DeltaState.Deleted && delta.tryAddMoved(node)) return
                        deltas.add(delta)
                    }
                    is DirectoryNode -> deltas.add(
                        DirectoryDelta(this, node.name, state, null).apply {
                            node.nodes.forEach { addNode(it, state) }
                        }
                    )
                }
            }

            val compareTo = when {
                old == null -> if (new == null) return else 1
                new == null -> -1
                else -> old.name.compareTo(new.name)
            }
            when {
                compareTo == 0 -> {
                    when (old!!) {
                        is FileNode -> when (new!!) {
                            is FileNode -> if (!old.digest.contentEquals(new.digest)) deltas.add(
                                FileDelta(this, old.name, DeltaState.Changed, null)
                            )
                            is DirectoryNode -> DirectoryDelta(this, new.name, DeltaState.FileToDir, null).apply {
                                new.nodes.forEach { addNode(it, DeltaState.New) }
                                this@compare.deltas.add(this)
                                tryAddMoved(old)
                            }
                        }
                        is DirectoryNode -> when (new!!) {
                            is FileNode -> DirectoryDelta(this, old.name, DeltaState.DirToFile, new.digest.toDigest()).apply {
                                old.nodes.forEach { addNode(it, DeltaState.Deleted) }
                                this@compare.deltas.add(this)
                            }
                            is DirectoryNode -> deltas.add(
                                DirectoryDelta(this, new.name, DeltaState.Same, null).apply {
                                    compare(old, new)
                                }
                            )
                        }
                    }
                    old = oldIter.nextOrNull()
                    new = newIter.nextOrNull()
                }
                compareTo < 0 -> {
                    addNode(old!!, DeltaState.Deleted)
                    old = oldIter.nextOrNull()
                }
                else -> {
                    addNode(new!!, DeltaState.New)
                    new = newIter.nextOrNull()
                }
            }
        }
    }
    compare(oldNodeDesc.node, newNodeDesc.node)

    fun Delta.setFrom(moved: Delta) {
        check(state == DeltaState.New || state == DeltaState.DirToFile)
        fromState = if (moved.parent != parent) FromState.MovedFrom else {
            check(moved.name != name)
            FromState.RenamedFrom
        }
        fromDelta = moved
    }

    fun Delta.fixFrom() {
        val moved = movedDigestToDelta[digest]
        if (moved != null) setFrom(moved)
        if (this is DirectoryDelta) deltas.forEach { it.fixFrom() }
    }
    fixFrom()

    fun Delta.fixMovedDirectories() {
        if (this !is DirectoryDelta) return
        deltas.toList().forEach { it.fixMovedDirectories() }
        if (state != DeltaState.New) return
        val movedDeltas = deltas.filter { it.fromState == FromState.MovedFrom }
        check(movedDeltas.all { it.state == DeltaState.New })
        check(movedDeltas.all { it.fromDelta!!.state == DeltaState.Deleted })
        val movedParents = movedDeltas.map { it.fromDelta!!.parent!! }
        if (movedParents.distinct().size != 1) return
        if (movedDeltas.any { it.fromDelta!!.name != it.name /* renamed */ }) return
        val movedParent = movedParents.first()
        if (movedParent.state != DeltaState.Deleted) return
        val unmovedDeltas = deltas - movedDeltas.toSet()
        if (unmovedDeltas.size != movedParent.deltas.size) return
        for (unmoved in unmovedDeltas) { // have unmovedDeltas matching empty directories?
            fun Delta.hasDeltas() = !(this is DirectoryDelta && deltas.isEmpty())
            if (unmoved.hasDeltas()) return
            val moved = movedParent.deltas.find { it.name == unmoved.name } ?: return
            check(moved.state == DeltaState.Deleted)
            if (moved.hasDeltas()) return
        }
        check(movedParent.parent!!.deltas.remove(movedParent))
        setFrom(movedParent)
        check(deltas.isNotEmpty())
        deltas.clear()
    }
    fixMovedDirectories()

    fun DirectoryDelta.tryRemoveObsoleteDirectories(): Boolean {
        deltas.removeAll { it is DirectoryDelta && it.tryRemoveObsoleteDirectories() }
        return deltas.isEmpty() && state == DeltaState.Same
    }
    tryRemoveObsoleteDirectories()
}

fun Appendable.printDeltas(delta: Delta, indent: Int = 0) {
    fun Delta.Path(): Path = parent?.Path()?.Path(name) ?: ""
    fun Delta.info(): String {
        val from = if (fromState == null) "" else {
            " $fromState `${if (fromState == FromState.MovedFrom) fromDelta!!.Path() else fromDelta!!.name}`"
        }
        return if (state == DeltaState.New && fromState != null) from else {
            if (state == DeltaState.Same) "" else " $state$from"
        }
    }
    appendLine("${"    ".repeat(indent)}- `${delta.name}`${delta.info()}")
    if (delta is DirectoryDelta) delta.deltas.forEach { printDeltas(it, indent + 1) }
}
