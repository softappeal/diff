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

    fun DirectoryDelta.addDelta(node: Node, state: DeltaState, path: String) {
        when (node) {
            is FileNode -> {
                val digest = node.digest.toHex()
                if (state == DeltaState.Deleted) {
                    val paths = newDigestToPaths[digest]
                    if (paths != null && paths.size == 1 && oldDigestToPaths[digest]!!.size == 1) {
                        deletedDigestToPath[digest] = "$path/${node.name}"
                        return
                    }
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
        fun DirectoryDelta.diff(oldDirectoryNode: DirectoryNode, newDirectoryNode: DirectoryNode) {
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
                                is DirectoryNode -> addNodeTypeChanged(newNode, DeltaState.FileToDir, DeltaState.New, "", null)
                            }
                            is DirectoryNode -> when (val newNode = new.node()) {
                                is FileNode -> addNodeTypeChanged(oldNode, DeltaState.DirToFile, DeltaState.Deleted, "", newNode.digest.toHex())
                                is DirectoryNode -> deltas.add(DirectoryDelta(newNode.name, DeltaState.Same, null).apply {
                                    diff(oldNode, newNode)
                                })
                            }
                        }
                        old.advance()
                        new.advance()
                    }
                    compareTo < 0 -> {
                        addDelta(old.node(), DeltaState.Deleted, "")
                        old.advance()
                    }
                    else -> {
                        addDelta(new.node(), DeltaState.New, "")
                        new.advance()
                    }
                }
            }
        }
        diff(oldDirectoryNode, newDirectoryNode)

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

fun Delta.dump(print: (s: String) -> Unit, indent: Int = 0, path: String? = null) {
    print("    ".repeat(indent))
    print("\"$name\"")
    val newPath = if (name == "/") "" else "$path/$name"
    fun moved(): String {
        val from = fileMovedFrom!!
        val slash = from.lastIndexOf("/")
        return " <- \"${if (from.substring(0, slash) == newPath.substring(0, newPath.lastIndexOf("/"))) from.substring(slash + 1) else from}\""
    }
    when (this) {
        is FileDelta -> print("${if (fileMovedFrom == null) " $state" else moved()}\n")
        is DirectoryDelta -> {
            print("${if (state == DeltaState.Same) "" else " $state"}${if (fileMovedFrom == null) "" else moved()}\n")
            deltas.forEach { it.dump(print, indent + 1, newPath) }
        }
    }
}
