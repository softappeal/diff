package ch.softappeal.diff

enum class DeltaState { Kept, New, Deleted, Changed, FileToDir, DirToFile, MovedFrom }

sealed class Delta(
    val parent: DirDelta?,
    val name: String,
    val state: DeltaState,
    val digest: Digest?,
) {
    var movedFrom: List<Delta>? = null

    fun remove() {
        check(parent!!.deltas.remove(this))
    }
}

private class FileDelta(
    parent: DirDelta,
    name: String,
    state: DeltaState,
    digest: Digest?,
) : Delta(parent, name, state, digest) {
    init {
        check(state != DeltaState.Kept && state != DeltaState.FileToDir && state != DeltaState.DirToFile)
        check(digest != null || state == DeltaState.Changed)
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
        check(state != DeltaState.Changed && state != DeltaState.MovedFrom)
        check(digest == null || state == DeltaState.DirToFile || state == DeltaState.FileToDir)
    }

    fun add(delta: Delta) {
        deltas.add(delta)
    }
}

private fun DirDelta.diff(oldDir: DirNode, newDir: DirNode) {
    val oldIter = oldDir.nodes.iterator()
    val newIter = newDir.nodes.iterator()
    fun Iterator<Node>.nextOrNull() = if (hasNext()) next() else null
    var old = oldIter.nextOrNull()
    var new = newIter.nextOrNull()
    while (true) {
        fun DirDelta.addNode(node: Node, state: DeltaState) {
            check(state == DeltaState.New || state == DeltaState.Deleted)
            add(
                when (node) {
                    is FileNode -> FileDelta(this, node.name, state, node.digest.toDigest())
                    is DirNode -> DirDelta(this, node.name, state).apply {
                        node.nodes.forEach { addNode(it, state) }
                    }
                }
            )
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
                        is FileNode -> if (!old.digest.contentEquals(new.digest)) {
                            add(FileDelta(this, new.name, DeltaState.New, new.digest.toDigest()))
                            add(FileDelta(this, old.name, DeltaState.Deleted, old.digest.toDigest()))
                        }
                        is DirNode -> DirDelta(this, new.name, DeltaState.FileToDir, old.digest.toDigest()).apply {
                            new.nodes.forEach { addNode(it, DeltaState.New) }
                            this@diff.add(this)
                        }
                    }
                    is DirNode -> when (new!!) {
                        is FileNode -> DirDelta(this, old.name, DeltaState.DirToFile, new.digest.toDigest()).apply {
                            old.nodes.forEach { addNode(it, DeltaState.Deleted) }
                            this@diff.add(this)
                        }
                        is DirNode -> add(
                            DirDelta(this, new.name, DeltaState.Kept).apply {
                                diff(old, new)
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

private fun DirDelta.moveFiles() {
    val digestToDeltas = buildMap {
        fun Delta.visit() {
            if (digest != null) getOrPut(digest) { mutableListOf() }.add(this)
            if (this is DirDelta) deltas.forEach { it.visit() }
        }
        visit()
    }
    digestToDeltas.values.forEach { deltas -> // TODO: implement multi moves and dirs
        if (deltas.size != 2) return@forEach
        val new = deltas.firstOrNull { it.state == DeltaState.New } ?: return@forEach
        val old = deltas.firstOrNull { it.state == DeltaState.Deleted } ?: return@forEach
        if (new is DirDelta) return@forEach
        if (old is DirDelta) return@forEach
        new.remove()
        old.remove()
        val file = FileDelta(new.parent!!, new.name, DeltaState.MovedFrom, new.digest!!)
        file.movedFrom = listOf(old)
        new.parent.add(file)
    }
}

private fun DirDelta.tryPruneKept(): Boolean {
    deltas.removeAll { it is DirDelta && it.tryPruneKept() }
    return deltas.isEmpty() && state == DeltaState.Kept
}

private fun Delta.mergeToChangedFiles() {
    when (this) {
        is DirDelta -> deltas.toList().forEach { it.mergeToChangedFiles() }
        is FileDelta -> {
            val children = parent!!.deltas
            while (true) {
                val new = children.firstOrNull { it is FileDelta && it.state == DeltaState.New && it.name == name } ?: break
                val deleted = children.firstOrNull { it is FileDelta && it.state == DeltaState.Deleted && it.name == name } ?: break
                new.remove()
                deleted.remove()
                children.add(FileDelta(parent, name, DeltaState.Changed, null))
            }
        }
    }
}

private const val DEBUG = true // TODO: change to false

fun DirDelta(old: DirNode, new: DirNode) = DirDelta(null, "", DeltaState.Kept).apply {
    diff(old, new)
    if (DEBUG) {
        println("diff")
        printDeltas()
    }
    moveFiles()
    if (DEBUG) {
        println("moveFiles")
        printDeltas()
    }
    // TODO: moves of dirs
    tryPruneKept() // is safe to be before mergeToChangedFiles
    mergeToChangedFiles()
}

fun Appendable.printDeltas(delta: Delta, indent: Int = 0, withDigest: Boolean = false) {
    fun Delta.Path(): Path = parent?.Path()?.Path(name) ?: ""
    with(delta) {
        append("    ".repeat(indent))
        append("- `$name${if (this is DirDelta && state != DeltaState.DirToFile) "/" else ""}`")
        if (state != DeltaState.Kept) append(" $state")
        if (state == DeltaState.MovedFrom) movedFrom!!.forEach { append(" `${it.Path()}`") }
        if (withDigest && digest != null) append(" `$digest`")
        appendLine()
        if (this is DirDelta) deltas.sortedBy { it.name }.forEach { printDeltas(it, indent + 1, withDigest) }
    }
}

private fun Delta.printDeltas() {
    System.out.printDeltas(this, withDigest = true)
}
