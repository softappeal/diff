package ch.softappeal.but

enum class DeltaState { Same, New, Deleted, Changed, FileToDir, DirToFile }

sealed class Delta(
    val name: String,
    val state: DeltaState,
    val digest: String?,
) {
    var fileMovedFrom: String? = null
}

class FileDelta(
    name: String,
    state: DeltaState,
    digest: String?,
) : Delta(name, state, digest)

class DirectoryDelta(
    name: String,
    state: DeltaState,
    digest: String?,
) : Delta(name, state, digest) {
    val deltas: MutableList<Delta> = mutableListOf()
}

fun create(oldDirectoryNode: DirectoryNode, newDirectoryNodeDigestToPaths: DirectoryNodeDigestToPaths): DirectoryDelta {
    val (newDirectoryNode, newDigestToPaths) = newDirectoryNodeDigestToPaths
    val oldDigestToPaths = oldDirectoryNode.calculateDigestToPaths()
    val deletedDigestToPath = mutableMapOf<String, String>()

    fun updateDeletedDigestToPath(name: String, digest: String, path: String): Boolean {
        val paths = newDigestToPaths[digest]
        if (paths != null && paths.size == 1 && oldDigestToPaths[digest]!!.size == 1) {
            deletedDigestToPath[digest] = "$path/$name"
            return true
        }
        return false
    }

    fun DirectoryDelta.addDelta(node: Node, state: DeltaState, path: String) {
        when (node) {
            is FileNode -> {
                val digest = node.digest.toHex()
                if (state == DeltaState.Deleted) {
                    if (updateDeletedDigestToPath(node.name, digest, path)) return
                }
                deltas.add(FileDelta(node.name, state, digest))
            }
            is DirectoryNode -> deltas.add(DirectoryDelta(node.name, state, null).apply {
                val newPath = "$path/$name"
                node.nodes.forEach { addDelta(it, state, newPath) }
            })
        }
    }

    fun DirectoryDelta.addNodeTypeChanged(node: DirectoryNode, state: DeltaState, nestedState: DeltaState, path: String, digest: String?) {
        deltas.add(DirectoryDelta(node.name, state, digest).apply {
            val newPath = "$path/$name"
            node.nodes.forEach { addDelta(it, nestedState, newPath) }
        })
    }

    return DirectoryDelta("/", DeltaState.Same, null).apply {
        fun DirectoryDelta.diff(oldDirectoryNode: DirectoryNode, newDirectoryNode: DirectoryNode, path: String) {
            class DeltaIterator(node: DirectoryNode) {
                private val iterator = node.nodes.iterator()
                private var node: Node? = null

                init {
                    advance()
                }

                @Suppress("BooleanMethodIsAlwaysInverted") fun done() = node == null
                fun node() = node!!
                fun advance() {
                    node = if (iterator.hasNext()) iterator.next() else null
                }
            }

            val old = DeltaIterator(oldDirectoryNode)
            val new = DeltaIterator(newDirectoryNode)
            while (true) {
                val compareTo = when {
                    old.done() -> if (new.done()) return else 1
                    new.done() -> -1
                    else -> old.node().name.compareTo(new.node().name)
                }
                when {
                    compareTo == 0 -> {
                        when (val oldNode = old.node()) {
                            is FileNode -> when (val newNode = new.node()) {
                                is FileNode -> if (!oldNode.digest.contentEquals(newNode.digest)) {
                                    deltas.add(FileDelta(newNode.name, DeltaState.Changed, null))
                                }
                                is DirectoryNode -> {
                                    updateDeletedDigestToPath(oldNode.name, oldNode.digest.toHex(), path)
                                    addNodeTypeChanged(newNode, DeltaState.FileToDir, DeltaState.New, "path is never used if nestedState is New", null)
                                }
                            }
                            is DirectoryNode -> when (val newNode = new.node()) {
                                is FileNode -> addNodeTypeChanged(oldNode, DeltaState.DirToFile, DeltaState.Deleted, path, newNode.digest.toHex())
                                is DirectoryNode -> deltas.add(DirectoryDelta(newNode.name, DeltaState.Same, null).apply {
                                    diff(oldNode, newNode, "$path/${newNode.name}")
                                })
                            }
                        }
                        old.advance()
                        new.advance()
                    }
                    compareTo < 0 -> {
                        addDelta(old.node(), DeltaState.Deleted, path)
                        old.advance()
                    }
                    else -> {
                        addDelta(new.node(), DeltaState.New, "path is never used if state is New")
                        new.advance()
                    }
                }
            }
        }
        diff(oldDirectoryNode, newDirectoryNode, "")

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
            fileMovedFrom = deletedDigestToPath[digest]
            return when (this) {
                is FileDelta -> {}
                is DirectoryDelta -> deltas.forEach { it.fixupMovedFrom() }
            }
        }
        fixupMovedFrom()
    }
}

fun Delta.dump(print: (s: String) -> Unit, indent: Int = 0, path: String = "") {
    print("    ".repeat(indent))
    print("\"$name\"")
    val newPath = if (name == "/") "" else "$path/$name"
    fun moved(): String {
        val from = fileMovedFrom!!
        return " <- \"${if (from.substringBeforeLast("/") == newPath.substringBeforeLast("/")) from.substringAfterLast("/") else from}\""
    }
    when (this) {
        is FileDelta -> print("${if (fileMovedFrom == null) " $state" else moved()}\n")
        is DirectoryDelta -> {
            print("${if (state == DeltaState.Same) "" else " $state"}${if (fileMovedFrom == null) "" else moved()}\n")
            deltas.forEach { it.dump(print, indent + 1, newPath) }
        }
    }
}
