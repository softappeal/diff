package ch.softappeal.diff

enum class DeltaState { Same, New, Deleted, Changed, FileToDir, DirToFile }
enum class FromState { MovedFrom, RenamedFrom }

sealed class Delta(
    val parent: DirectoryDelta?,
    val name: String,
    val state: DeltaState,
    val digest: String?,
) {
    var fromState: FromState? = null
    var from: Delta? = null
    fun getPath(): String = parent?.getPath()?.concatPath(name) ?: ""
}

class FileDelta(
    parent: DirectoryDelta,
    name: String,
    state: DeltaState,
    digest: String?,
) : Delta(parent, name, state, digest)

class DirectoryDelta(
    parent: DirectoryDelta?,
    name: String,
    state: DeltaState,
    digest: String?,
) : Delta(parent, name, state, digest) {
    val deltas = mutableListOf<Delta>()
}

data class NodeDigestToPaths(
    val node: DirectoryNode,
    val digestToPaths: DigestToPaths = node.calculateDigestToPaths(),
)

fun createDirectoryDelta(oldNodeDigestToPaths: NodeDigestToPaths, newNodeDigestToPaths: NodeDigestToPaths) =
    DirectoryDelta(null, "", DeltaState.Same, null).apply {
        val (oldNode, oldDigestToPaths) = oldNodeDigestToPaths
        val (newNode, newDigestToPaths) = newNodeDigestToPaths
        val deletedDigestToDelta = mutableMapOf<String, Delta>()

        fun DirectoryDelta.compare(oldDirectoryNode: DirectoryNode, newDirectoryNode: DirectoryNode) {
            val oldIter = NodeIterator(oldDirectoryNode)
            val newIter = NodeIterator(newDirectoryNode)
            while (true) {
                fun Delta.updateDeletedDigestToDelta(node: FileNode): Boolean {
                    val digest = node.digest.toHex()
                    val newPaths = newDigestToPaths[digest]
                    if (newPaths == null || newPaths.size != 1 || oldDigestToPaths[digest]!!.size != 1) return false
                    check(deletedDigestToDelta.put(digest, this) == null)
                    return true
                }

                fun DirectoryDelta.addNode(node: Node, state: DeltaState) {
                    when (node) {
                        is FileNode -> {
                            val file = FileDelta(this, node.name, state, node.digest.toHex())
                            if (state == DeltaState.Deleted && file.updateDeletedDigestToDelta(node)) return
                            deltas.add(file)
                        }
                        is DirectoryNode -> deltas.add(DirectoryDelta(this, node.name, state, null).apply {
                            node.nodes.forEach { addNode(it, state) }
                        })
                    }
                }

                val compareTo = when {
                    oldIter.done() -> if (newIter.done()) return else 1
                    newIter.done() -> -1
                    else -> oldIter.current().name.compareTo(newIter.current().name)
                }
                when {
                    compareTo == 0 -> {
                        fun addNodeTypeChanged(node: DirectoryNode, state: DeltaState, nestedState: DeltaState, digest: String?) =
                            DirectoryDelta(this, node.name, state, digest).apply {
                                node.nodes.forEach { addNode(it, nestedState) }
                                this@compare.deltas.add(this)
                            }
                        when (val old = oldIter.current()) {
                            is FileNode -> when (val new = newIter.current()) {
                                is FileNode -> if (!old.digest.contentEquals(new.digest)) deltas.add(FileDelta(this, new.name, DeltaState.Changed, null))
                                is DirectoryNode -> addNodeTypeChanged(new, DeltaState.FileToDir, DeltaState.New, null).updateDeletedDigestToDelta(old)
                            }
                            is DirectoryNode -> when (val new = newIter.current()) {
                                is FileNode -> addNodeTypeChanged(old, DeltaState.DirToFile, DeltaState.Deleted, new.digest.toHex())
                                is DirectoryNode -> deltas.add(DirectoryDelta(this, new.name, DeltaState.Same, null).apply { compare(old, new) })
                            }
                        }
                        oldIter.advance()
                        newIter.advance()
                    }
                    compareTo < 0 -> {
                        addNode(oldIter.current(), DeltaState.Deleted)
                        oldIter.advance()
                    }
                    else -> {
                        addNode(newIter.current(), DeltaState.New)
                        newIter.advance()
                    }
                }
            }
        }
        compare(oldNode, newNode)

        fun Delta.setFrom(fromDelta: Delta) {
            check(state == DeltaState.New || state == DeltaState.DirToFile)
            if (fromDelta.parent == parent) {
                fromState = FromState.RenamedFrom
                check(fromDelta.name != name)
            } else {
                fromState = FromState.MovedFrom
            }
            from = fromDelta
        }

        fun Delta.fixupFrom() {
            val delta = deletedDigestToDelta[digest]
            if (delta != null) setFrom(delta)
            when (this) {
                is FileDelta -> {}
                is DirectoryDelta -> deltas.forEach { it.fixupFrom() }
            }
        }
        fixupFrom()

        val root = this
        @Suppress("SpellCheckingInspection") fun Delta.mergeMovedDirectory() {
            when (this) {
                is FileDelta -> {}
                is DirectoryDelta -> {
                    deltas.toList().forEach { it.mergeMovedDirectory() }
                    if (state != DeltaState.New) return
                    val moveds = deltas.filter { it.fromState == FromState.MovedFrom }
                    if (moveds.any { it.from!!.name != it.name }) return // detects renamed moveds
                    val movedsParent = moveds.map { it.from!!.parent }
                    if (movedsParent.distinct().size != 1) return
                    val movedParent = movedsParent.first()!!
                    check((movedParent == root && movedParent.state == DeltaState.Same) || (movedParent != root && movedParent.state == DeltaState.Deleted))
                    fun areUnmovedsMergeable(): Boolean {
                        val unmoveds = deltas - moveds.toSet()
                        if (unmoveds.size != movedParent.deltas.size) return false
                        fun haveUnmovedsMatchingEmptyDirectories(): Boolean {
                            for (unmoved in unmoveds) {
                                fun Delta.isEmptyDirectory() = this is DirectoryDelta && deltas.isEmpty()
                                check(unmoved.state == DeltaState.New)
                                if (!unmoved.isEmptyDirectory()) return false
                                val moved = movedParent.deltas.find { it.name == unmoved.name } ?: return false
                                check(moved.state == DeltaState.Deleted)
                                if (!moved.isEmptyDirectory()) return false
                            }
                            return true
                        }
                        return haveUnmovedsMatchingEmptyDirectories()
                    }
                    if (!areUnmovedsMergeable()) return
                    check(movedParent.parent!!.deltas.remove(movedParent))
                    setFrom(movedParent)
                    deltas.clear()
                }
            }
        }
        mergeMovedDirectory()

        fun DirectoryDelta.pruneEqualDirectory(): Boolean {
            deltas.removeIf { delta ->
                when (delta) {
                    is FileDelta -> false
                    is DirectoryDelta -> delta.pruneEqualDirectory()
                }
            }
            return deltas.isEmpty() && state == DeltaState.Same
        }
        pruneEqualDirectory()
    }

fun Delta.dump(print: (s: String) -> Unit, indent: Int = 0) {
    val dirSep = if (this is DirectoryDelta && state != DeltaState.DirToFile) "$DIR_SEP" else ""
    val from = if (fromState == null) "" else " $fromState `${if (fromState == FromState.MovedFrom) from!!.getPath() else from!!.name}`"
    val info = if (state == DeltaState.New && fromState != null) from else "${if (state == DeltaState.Same) "" else " $state"}${if (fromState == null) "" else from}"
    print("${"    ".repeat(indent)}- `$name$dirSep`$info\n")
    when (this) {
        is FileDelta -> {}
        is DirectoryDelta -> deltas.forEach { it.dump(print, indent + 1) }
    }
}
