package ch.softappeal.diff

import ch.softappeal.diff.DeltaState.ChangedFile
import ch.softappeal.diff.DeltaState.DeletedDir
import ch.softappeal.diff.DeltaState.DeletedFile
import ch.softappeal.diff.DeltaState.DirToFile
import ch.softappeal.diff.DeltaState.FileToDir
import ch.softappeal.diff.DeltaState.KeptDir
import ch.softappeal.diff.DeltaState.NewDir
import ch.softappeal.diff.DeltaState.NewFile

private const val DEBUG = false

enum class DeltaState {
    NewFile, DeletedFile, ChangedFile, // FileDelta
    NewDir, DeletedDir, KeptDir, FileToDir, DirToFile, // DirDelta
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
        check(state == NewFile || state == DeletedFile || state == ChangedFile)
    }
}

class DirDelta(
    parent: DirDelta?,
    name: String,
    state: DeltaState,
    digest: ByteArray?,
) : Delta(parent, name, state, digest?.toDigest()) {
    init {
        check(state == NewDir || state == DeletedDir || state == KeptDir || state == FileToDir || state == DirToFile)
        check(digest == null || state == FileToDir || state == DirToFile)
    }

    val children = mutableListOf<Delta>()

    fun addChild(child: Delta) {
        children.add(child)
    }
}

private fun DirDelta.diff(oldDir: DirNode, newDir: DirNode) {
    fun DirDelta.addChild(node: Node, new: Boolean) {
        addChild(
            when (node) {
                is FileNode -> FileDelta(this, node.name, if (new) NewFile else DeletedFile, node.digest)
                is DirNode -> DirDelta(this, node.name, if (new) NewDir else DeletedDir, null).apply {
                    node.children.forEach { addChild(it, new) }
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
                        is FileNode -> if (!old.digest.contentEquals(new.digest)) addChild(
                            FileDelta(this, new.name, ChangedFile, new.digest)
                        )
                        is DirNode -> DirDelta(this, new.name, FileToDir, old.digest).apply {
                            new.children.forEach { addChild(it, true) }
                            this@diff.addChild(this)
                        }
                    }
                    is DirNode -> when (new!!) {
                        is FileNode -> DirDelta(this, old.name, DirToFile, new.digest).apply {
                            old.children.forEach { addChild(it, false) }
                            this@diff.addChild(this)
                        }
                        is DirNode -> addChild(
                            DirDelta(this, new.name, KeptDir, null).apply {
                                diff(old, new)
                            }
                        )
                    }
                }
                old = oldIter.nextOrNull()
                new = newIter.nextOrNull()
            }
            compareTo < 0 -> {
                addChild(old!!, false)
                old = oldIter.nextOrNull()
            }
            else -> {
                addChild(new!!, true)
                new = newIter.nextOrNull()
            }
        }
    }
}

private fun DirDelta.moveFiles() {
    val digestToDeltas = buildMap {
        fun Delta.visit() {
            if (digest != null) getOrPut(digest) { mutableListOf() }.add(this)
            if (this is DirDelta) children.forEach { it.visit() }
        }
        visit()
    }
    digestToDeltas.values.forEach { deltas ->
        if (DEBUG) println("deltas: $deltas")
        val newFiles = deltas.filter { it.state == NewFile || it.state == DirToFile || it.state == ChangedFile }
        if (newFiles.isEmpty()) return@forEach
        val deletedFiles = deltas.filter { it.state == DeletedFile || it.state == FileToDir }
        if (deletedFiles.isEmpty()) return@forEach
        newFiles.forEach { newFile ->
            newFile.movedFrom = deletedFiles
            deletedFiles.forEach { deletedFile ->
                if (deletedFile.state == FileToDir) return@forEach
                deletedFile.siblings.remove(deletedFile) // deletedFile could only be in siblings once
            }
        }
    }
}

private fun DirDelta.tryPruneKeptDir(): Boolean {
    children.removeAll { it is DirDelta && it.tryPruneKeptDir() }
    return children.isEmpty() && state == KeptDir
}

fun DirDelta(old: DirNode, new: DirNode) = DirDelta(null, "", KeptDir, null).apply {
    diff(old, new)
    if (DEBUG) print("diff:\n${printDelta()}")
    tryPruneKeptDir()
    moveFiles()
    if (DEBUG) print("moveFiles:\n${printDelta()}")
    tryPruneKeptDir()
}

fun Appendable.printDelta(delta: Delta, indent: Int = 0, debug: Boolean = false) {
    with(delta) {
        append("    ".repeat(indent))
        append("- `$name`")
        if (debug || state != KeptDir) append(" $state")
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
