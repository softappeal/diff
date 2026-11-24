package ch.softappeal.diff

import ch.softappeal.diff.DeltaState.Changed
import ch.softappeal.diff.DeltaState.Deleted
import ch.softappeal.diff.DeltaState.Kept
import ch.softappeal.diff.DeltaState.New
import ch.softappeal.diff.DeltaState.ToDir
import ch.softappeal.diff.DeltaState.ToFile

private const val DEBUG = false

enum class DeltaState {
    New, Deleted, // Delta
    Changed, // FileDelta
    Kept, ToDir, ToFile, // DirDelta
}

private fun Delta.Path(): Path = parent?.Path()?.Path(name) ?: ""

sealed class Delta(
    val parent: DirDelta?,
    val name: String,
    val state: DeltaState,
    val digest: Digest?,
) {
    var movedFrom: List<Delta>? = null

    val siblings get() = parent!!.children

    override fun toString() = "`${Path()}`:$state"
}

private class FileDelta(
    parent: DirDelta,
    name: String,
    state: DeltaState,
    digest: ByteArray,
) : Delta(parent, name, state, digest.toDigest()) {
    init {
        check(state == New || state == Deleted || state == Changed)
    }
}

class DirDelta(
    parent: DirDelta?,
    name: String,
    state: DeltaState,
    digest: ByteArray?,
) : Delta(parent, name, state, digest?.toDigest()) {
    init {
        check(state != Changed)
        check(digest == null || state == ToFile || state == ToDir)
    }

    val children = mutableListOf<Delta>()

    fun addChild(child: Delta) {
        children.add(child)
    }
}

private fun diff(dirDelta: DirDelta, oldDir: DirNode, newDir: DirNode) {
    fun DirDelta.addChild(node: Node, state: DeltaState) {
        addChild(
            when (node) {
                is FileNode -> FileDelta(this, node.name, state, node.digest)
                is DirNode -> DirDelta(this, node.name, state, null).apply {
                    node.children.forEach { addChild(it, state) }
                }
            }
        )
    }

    fun Iterator<Node>.nextOrNull() = if (hasNext()) next() else null

    val oldIter = oldDir.children.iterator()
    val newIter = newDir.children.iterator()
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
                        is FileNode -> if (!old.digest.contentEquals(new.digest)) dirDelta.addChild(
                            FileDelta(dirDelta, new.name, Changed, new.digest)
                        )
                        is DirNode -> DirDelta(dirDelta, new.name, ToDir, old.digest).apply {
                            new.children.forEach { addChild(it, New) }
                            dirDelta.addChild(this)
                        }
                    }
                    is DirNode -> when (new!!) {
                        is FileNode -> DirDelta(dirDelta, old.name, ToFile, new.digest).apply {
                            old.children.forEach { addChild(it, Deleted) }
                            dirDelta.addChild(this)
                        }
                        is DirNode -> dirDelta.addChild(
                            DirDelta(dirDelta, new.name, Kept, null).apply {
                                diff(this, old, new)
                            }
                        )
                    }
                }
                old = oldIter.nextOrNull()
                new = newIter.nextOrNull()
            }
            compareTo < 0 -> {
                dirDelta.addChild(old!!, Deleted)
                old = oldIter.nextOrNull()
            }
            else -> {
                dirDelta.addChild(new!!, New)
                new = newIter.nextOrNull()
            }
        }
    }
}

private fun moveFiles(root: DirDelta) {
    val digestToDeltas = buildMap {
        fun Delta.visit() {
            if (digest != null) getOrPut(digest) { mutableListOf() }.add(this)
            if (this is DirDelta) children.forEach { it.visit() }
        }
        root.visit()
    }
    digestToDeltas.values.forEach { deltas ->
        if (DEBUG) println("deltas: $deltas")
        val newList = deltas.filter { it.state == New || it.state == ToFile || it.state == Changed }
        if (newList.isEmpty()) return@forEach
        val deletedList = deltas.filter { it.state == Deleted || it.state == ToDir }
        if (deletedList.isEmpty()) return@forEach
        newList.forEach { new ->
            new.movedFrom = deletedList
            deletedList.forEach { deleted ->
                if (deleted.state == ToDir) return@forEach
                deleted.siblings.remove(deleted) // deleted could only be in siblings once
            }
        }
    }
}

private fun DirDelta.tryPruneKept(): Boolean {
    children.removeAll { it is DirDelta && it.tryPruneKept() }
    return children.isEmpty() && state == Kept
}

fun DirDelta(old: DirNode, new: DirNode): DirDelta {
    val root = DirDelta(null, "", Kept, null)
    diff(root, old, new)
    if (DEBUG) println("diff:\n${root.printDelta()}")
    root.tryPruneKept()
    moveFiles(root)
    if (DEBUG) println("moveFiles:\n${root.printDelta()}")
    root.tryPruneKept()
    return root
}

fun Appendable.printDelta(delta: Delta, indent: Int = 0, debug: Boolean = false) {
    with(delta) {
        append("    ".repeat(indent))
        append("- `$name`")
        if (debug || state != Kept) append(" $state")
        if (movedFrom != null) {
            append(" <")
            movedFrom!!.forEach { append(" `${it.Path()}`") }
        }
        if (debug && digest != null) append(" `$digest`")
        appendLine()
        if (this is DirDelta) children.forEach { printDelta(it, indent + 1, debug) }
    }
}

private fun Delta.printDelta() = StringBuffer().apply { printDelta(this@printDelta, 1, true) }
