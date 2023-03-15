package ch.softappeal.but

enum class DeltaState { Same, New, Deleted, Changed, FileToDir, DirToFile }
enum class FromState { MovedFrom, RenamedFrom }

sealed class Delta(
    parent: DirectoryDelta?,
    val name: String,
    val state: DeltaState,
    val digest: String?,
) {
    val path: String = if (parent == null) "" else nextPath(parent.path, name)
    var fromState: FromState? = null
    var fromPath: String? = null
}

class FileDelta(
    parent: DirectoryDelta,
    name: String,
    state: DeltaState,
    digest: String?,
) : Delta(parent, name, state, digest)

class DirectoryDelta(
    val parent: DirectoryDelta?,
    name: String,
    state: DeltaState,
    digest: String?,
) : Delta(parent, name, state, digest) {
    val deltas = mutableListOf<Delta>()
}

fun DirectoryDelta.getPath(path: String): DirectoryDelta {
    if (path.isEmpty()) return this
    val secondDirSep = path.indexOf(DIR_SEP, 1)
    val name = path.substring(1, if (secondDirSep < 0) path.length else secondDirSep)
    val child = deltas.find { it.name == name } as DirectoryDelta
    return child.getPath(path.substring(name.length + 1))
}

data class NodeDigestToPaths(
    val node: DirectoryNode,
    val digestToPaths: DigestToPaths = node.calculateDigestToPaths(),
)

fun Delta.isEmptyDirectory() = this is DirectoryDelta && deltas.isEmpty()

fun createDirectoryDelta(oldNodeDigestToPaths: NodeDigestToPaths, newNodeDigestToPaths: NodeDigestToPaths): DirectoryDelta =
    DirectoryDelta(null, "", DeltaState.Same, null).apply {
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
                        fun addNodeTypeChanged(node: DirectoryNode, state: DeltaState, nestedState: DeltaState, digest: String?) {
                            deltas.add(DirectoryDelta(this, node.name, state, digest).apply {
                                node.nodes.forEach { addNode(it, nestedState) }
                            })
                        }
                        when (val old = oldIter.current()) {
                            is FileNode -> when (val new = newIter.current()) {
                                is FileNode -> if (!old.digest.contentEquals(new.digest)) {
                                    deltas.add(FileDelta(this, new.name, DeltaState.Changed, null))
                                }
                                is DirectoryNode -> {
                                    updateDeletedDigestToPath(old)
                                    addNodeTypeChanged(new, DeltaState.FileToDir, DeltaState.New, null)
                                }
                            }
                            is DirectoryNode -> when (val new = newIter.current()) {
                                is FileNode -> addNodeTypeChanged(old, DeltaState.DirToFile, DeltaState.Deleted, new.digest.toHex())
                                is DirectoryNode -> deltas.add(DirectoryDelta(this, new.name, DeltaState.Same, null).apply {
                                    compare(old, new)
                                })
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

        fun Delta.setFrom(from: String) {
            check(state == DeltaState.New || state == DeltaState.DirToFile)
            if (from.substringBeforeLast(DIR_SEP) == path.substringBeforeLast(DIR_SEP)) {
                fromState = FromState.RenamedFrom
                fromPath = from.substringAfterLast(DIR_SEP)
                check(fromPath != name)
            } else {
                fromState = FromState.MovedFrom
                fromPath = from
            }
        }

        fun Delta.fixupFrom() {
            val path = deletedDigestToPath[digest]
            if (path != null) setFrom(path)
            when (this) {
                is FileDelta -> {}
                is DirectoryDelta -> deltas.forEach { it.fixupFrom() }
            }
        }
        fixupFrom()

        val root = this
        fun Delta.mergeFrom() {
            when (this) {
                is FileDelta -> {}
                is DirectoryDelta -> {
                    deltas.toList().forEach { it.mergeFrom() }
                    if (state != DeltaState.New) return
                    val movedDeltas = deltas.filter { it.fromState == FromState.MovedFrom }
                    val movedPaths = movedDeltas.map { it.fromPath!!.substringBeforeLast(DIR_SEP) }
                    if (movedPaths.distinct().size != 1) return
                    val movedPath = movedPaths.first()
                    val movedDir = root.getPath(movedPath)
                    check((movedDir == root && movedDir.state == DeltaState.Same) || (movedDir != root && movedDir.state == DeltaState.Deleted))
                    val unmovedDeltas = deltas - movedDeltas.toSet()
                    val movedDirDeltas = movedDir.deltas
                    if (unmovedDeltas.size != movedDirDeltas.size) return
                    for (unmovedDelta in unmovedDeltas) {
                        check(unmovedDelta.state == DeltaState.New)
                        if (!unmovedDelta.isEmptyDirectory()) return
                        val movedDelta = movedDirDeltas.find { it.name == unmovedDelta.name } ?: return
                        check(movedDelta.state == DeltaState.Deleted)
                        if (!movedDelta.isEmptyDirectory()) return
                    }
                    check(movedDir.parent!!.deltas.remove(movedDir))
                    setFrom(movedPath)
                    deltas.clear()
                }
            }
        }
        mergeFrom()
    }

fun Delta.dump(print: (s: String) -> Unit, indent: Int = 0) {
    val dirSep = if (this is DirectoryDelta && state != DeltaState.DirToFile) "$DIR_SEP" else ""
    val from = " $fromState '$fromPath'"
    val info = if (state == DeltaState.New && fromState != null) {
        from
    } else {
        "${if (state == DeltaState.Same) "" else " $state"}${if (fromState == null) "" else from}"
    }
    print("${"    ".repeat(indent)}'$name$dirSep'$info\n")
    when (this) {
        is FileDelta -> {}
        is DirectoryDelta -> deltas.forEach { it.dump(print, indent + 1) }
    }
}
