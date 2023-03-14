package ch.softappeal.but

enum class DeltaState { Same, New, Deleted, Changed, FileToDir, DirToFile, MovedFrom, RenamedFrom }

sealed class Delta(
    parent: DirectoryDelta?,
    val name: String,
    var state: DeltaState,
) {
    val path: String = if (parent == null) "" else nextPath(parent.path, name)
}

class FileDelta(
    parent: DirectoryDelta,
    name: String,
    state: DeltaState,
    val digest: String?,
) : Delta(parent, name, state) {
    var fromPath: String? = null
}

class DirectoryDelta(
    parent: DirectoryDelta?,
    name: String,
    state: DeltaState,
) : Delta(parent, name, state) {
    val deltas = mutableListOf<Delta>()
}

data class NodeDigestToPaths(
    val node: DirectoryNode,
    val digestToPaths: DigestToPaths = node.calculateDigestToPaths(),
)

fun createDirectoryDelta(oldNodeDigestToPaths: NodeDigestToPaths, newNodeDigestToPaths: NodeDigestToPaths): DirectoryDelta =
    DirectoryDelta(null, "", DeltaState.Same).apply {
        val (oldNode, oldDigestToPaths) = oldNodeDigestToPaths
        val (newNode, newDigestToPaths) = newNodeDigestToPaths
        val deletedDigestToPath = mutableMapOf<String, String>()

        fun DirectoryDelta.compare(oldDirectoryNode: DirectoryNode, newDirectoryNode: DirectoryNode) {
            val oldIter = NodeIterator(oldDirectoryNode)
            val newIter = NodeIterator(newDirectoryNode)
            while (true) {
                fun DirectoryDelta.updateDeletedDigestToPath(node: FileNode): Boolean {
                    val digest = node.digest.toHex()
                    val newPaths = newDigestToPaths[digest]
                    if (newPaths == null || newPaths.size != 1 || oldDigestToPaths[digest]!!.size != 1) return false
                    check(deletedDigestToPath.put(digest, nextPath(path, node.name)) == null)
                    return true
                }

                fun DirectoryDelta.addNode(node: Node, state: DeltaState) {
                    when (node) {
                        is FileNode -> {
                            if (state == DeltaState.Deleted && updateDeletedDigestToPath(node)) return
                            deltas.add(FileDelta(this, node.name, state, node.digest.toHex()))
                        }
                        is DirectoryNode -> deltas.add(DirectoryDelta(this, node.name, state).apply {
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
                        fun addNodeTypeChanged(node: DirectoryNode, state: DeltaState, nestedState: DeltaState) {
                            deltas.add(DirectoryDelta(this, node.name, state).apply {
                                node.nodes.forEach { addNode(it, nestedState) }
                            })
                        }
                        when (val old = oldIter.current()) {
                            is FileNode -> when (val new = newIter.current()) {
                                is FileNode -> if (!old.digest.contentEquals(new.digest)) deltas.add(FileDelta(this, new.name, DeltaState.Changed, null))
                                is DirectoryNode -> {
                                    updateDeletedDigestToPath(old)
                                    addNodeTypeChanged(new, DeltaState.FileToDir, DeltaState.New)
                                }
                            }
                            is DirectoryNode -> when (val new = newIter.current()) {
                                is FileNode -> addNodeTypeChanged(old, DeltaState.DirToFile, DeltaState.Deleted)
                                is DirectoryNode -> deltas.add(DirectoryDelta(this, new.name, DeltaState.Same).apply { compare(old, new) })
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

        fun DirectoryDelta.pruneEqualDirectories(): Boolean {
            deltas.removeIf { delta ->
                when (delta) {
                    is FileDelta -> false
                    is DirectoryDelta -> delta.pruneEqualDirectories()
                }
            }
            return deltas.isEmpty() && state == DeltaState.Same
        }
        pruneEqualDirectories()

        fun Delta.fixupMovedFrom() {
            when (this) {
                is FileDelta -> {
                    val from = deletedDigestToPath[digest]
                    if (from != null) {
                        if (from.substringBeforeLast(DIR_SEP) == path.substringBeforeLast(DIR_SEP)) {
                            state = DeltaState.RenamedFrom
                            fromPath = from.substringAfterLast(DIR_SEP)
                        } else {
                            state = DeltaState.MovedFrom
                            fromPath = from
                        }
                    }
                }
                is DirectoryDelta -> deltas.forEach { it.fixupMovedFrom() }
            }
        }
        fixupMovedFrom()
    }

fun Delta.dump(print: (s: String) -> Unit, indent: Int = 0) {
    print("${"    ".repeat(indent)}'$name")
    when (this) {
        is FileDelta -> print("' $state${if (fromPath == null) "" else " '$fromPath'"}\n")
        is DirectoryDelta -> {
            print("${if (state != DeltaState.DirToFile) "$DIR_SEP" else ""}'${if (state == DeltaState.Same) "" else " $state"}\n")
            deltas.forEach { it.dump(print, indent + 1) }
        }
    }
}
