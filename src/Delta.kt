package ch.softappeal.diff

private const val DEBUG = false

enum class DeltaState {
    New, Deleted, // Delta
    FileChanged, // FileDelta
    DirKept, FileToDir, DirToFile, // DirDelta
}

private fun Delta.Path(): Path = parent?.Path()?.Path(name) ?: ""

sealed class Delta(
    val parent: DirDelta?,
    val name: String,
    val state: DeltaState,
    val digest: Digest?,
) {
    var movedFrom: List<Delta>? = null

    override fun toString() = "`${Path()}`:$state"

    val siblings get() = parent!!.deltas

    fun remove() {
        check(siblings.remove(this))
    }
}

private class FileDelta(
    parent: DirDelta,
    name: String,
    state: DeltaState,
    digest: Digest?,
) : Delta(parent, name, state, digest) {
    init {
        check(state == DeltaState.New || state == DeltaState.Deleted || state == DeltaState.FileChanged)
        check(digest != null || state == DeltaState.FileChanged)
    }
}

class DirDelta(
    parent: DirDelta?,
    name: String,
    state: DeltaState,
    digest: Digest? = null,
) : Delta(parent, name, state, digest) {
    val deltas = mutableListOf<Delta>()

    init {
        check(state != DeltaState.FileChanged)
        check(digest == null || state == DeltaState.DirToFile || state == DeltaState.FileToDir)
    }

    fun add(delta: Delta) {
        deltas.add(delta)
    }
}

private fun diff(root: DirDelta, oldDir: DirNode, newDir: DirNode) {
    fun DirDelta.addNode(node: Node, state: DeltaState) {
        add(
            when (node) {
                is FileNode -> FileDelta(this, node.name, state, node.digest.toDigest())
                is DirNode -> DirDelta(this, node.name, state).apply {
                    node.nodes.forEach { addNode(it, state) }
                }
            }
        )
    }

    fun Iterator<Node>.nextOrNull() = if (hasNext()) next() else null

    val oldIter = oldDir.nodes.iterator()
    val newIter = newDir.nodes.iterator()
    var old = oldIter.nextOrNull()
    var new = newIter.nextOrNull()
    while (true) {
        val compareTo = when {
            old == null -> if (new == null) return else 1
            new == null -> -1
            else -> old.name.compareTo(new.name)
        }
        when {
            compareTo == 0 -> {
                when (old!!) {
                    is FileNode -> when (new!!) {
                        is FileNode -> if (!old.digest.contentEquals(new.digest)) root.add(
                            FileDelta(root, new.name, DeltaState.FileChanged, null)
                        )
                        is DirNode -> DirDelta(root, new.name, DeltaState.FileToDir, old.digest.toDigest()).apply {
                            new.nodes.forEach { addNode(it, DeltaState.New) }
                            root.add(this)
                        }
                    }
                    is DirNode -> when (new!!) {
                        is FileNode -> DirDelta(root, old.name, DeltaState.DirToFile, new.digest.toDigest()).apply {
                            old.nodes.forEach { addNode(it, DeltaState.Deleted) }
                            root.add(this)
                        }
                        is DirNode -> root.add(
                            DirDelta(root, new.name, DeltaState.DirKept).apply {
                                diff(this, old, new)
                            }
                        )
                    }
                }
                old = oldIter.nextOrNull()
                new = newIter.nextOrNull()
            }
            compareTo < 0 -> {
                root.addNode(old!!, DeltaState.Deleted)
                old = oldIter.nextOrNull()
            }
            else -> {
                root.addNode(new!!, DeltaState.New)
                new = newIter.nextOrNull()
            }
        }
    }
}

private fun moveFiles(root: DirDelta) {
    val digestToDeltas: Map<Digest, List<Delta>> = buildMap<Digest, MutableList<Delta>> {
        fun Delta.visit() {
            if (digest != null) getOrPut(digest) { mutableListOf() }.add(this)
            if (this is DirDelta) deltas.forEach { it.visit() }
        }
        root.visit()
    }
    digestToDeltas.values.forEach { deltas ->
        val newList = deltas.filter { it.state == DeltaState.New || it.state == DeltaState.DirToFile }
        if (newList.isEmpty()) return@forEach
        val deletedList = deltas.filter { it.state == DeltaState.Deleted || it.state == DeltaState.FileToDir }
        if (deletedList.isEmpty()) return@forEach
        newList.forEach { new ->
            new.movedFrom = deletedList
            deletedList.forEach { deleted ->
                if (deleted.state == DeltaState.FileToDir) return@forEach
                if (deleted.siblings.contains(deleted)) deleted.remove() else {
                    if (DEBUG) println("already removed: $deleted")
                }
            }
        }
    }
}

private fun DirDelta.tryPruneKeptDirs(): Boolean {
    deltas.removeAll { it is DirDelta && it.tryPruneKeptDirs() }
    return deltas.isEmpty() && state == DeltaState.DirKept
}

fun DirDelta(old: DirNode, new: DirNode): DirDelta {
    val root = DirDelta(null, "", DeltaState.DirKept)
    diff(root, old, new)
    if (DEBUG) println("diff: ${root.printDeltas()}")
    root.tryPruneKeptDirs()
    // TODO: moveDirectories(root)
    moveFiles(root)
    root.tryPruneKeptDirs()
    return root
}

fun Appendable.printDeltas(delta: Delta, indent: Int = 0, withDigest: Boolean = false) {
    with(delta) {
        append("    ".repeat(indent))
        append("- `$name`")
        if (state != DeltaState.DirKept) append(" $state")
        if (movedFrom != null) {
            append(" movedFrom:")
            movedFrom!!.forEach { append(" `${it.Path()}`") }
        }
        if (withDigest && digest != null) append(" `$digest`")
        appendLine()
        if (this is DirDelta) deltas.sortedBy { it.name }.forEach { printDeltas(it, indent + 1, withDigest) }
    }
}

private fun Delta.printDeltas() = StringBuffer().apply { printDeltas(this@printDeltas, withDigest = true) }
