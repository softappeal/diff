package ch.softappeal.but

enum class DeltaState { Equal, Created, Deleted, Differ, FileToDir, DirToFile }

sealed class Delta(
    val name: String,
    val state: DeltaState,
)

class FileDelta(
    name: String,
    state: DeltaState,
) : Delta(name, state)

class DirectoryDelta(
    name: String,
    state: DeltaState,
) : Delta(name, state) {
    val deltas: MutableList<Delta> = mutableListOf()
}

fun create(oldDirectoryNode: DirectoryNode, newDirectoryNode: DirectoryNode): DirectoryDelta =
    DirectoryDelta(".", DeltaState.Equal).apply {
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

            fun DirectoryDelta.addDelta(node: Node, state: DeltaState) {
                deltas.add(when (node) {
                    is FileNode -> FileDelta(node.name, state)
                    is DirectoryNode -> DirectoryDelta(node.name, state).apply {
                        node.nodes.forEach { addDelta(it, state) }
                    }
                })
            }

            fun DirectoryDelta.addNodeTypeChanged(node: DirectoryNode, state: DeltaState, nestedState: DeltaState) {
                deltas.add(DirectoryDelta(node.name, state).apply {
                    node.nodes.forEach { addDelta(it, nestedState) }
                })
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
                                    deltas.add(FileDelta(newNode.name, DeltaState.Differ))
                                }
                                is DirectoryNode -> addNodeTypeChanged(newNode, DeltaState.FileToDir, DeltaState.Created)
                            }
                            is DirectoryNode -> when (val newNode = new.node()) {
                                is FileNode -> addNodeTypeChanged(oldNode, DeltaState.DirToFile, DeltaState.Deleted)
                                is DirectoryNode -> deltas.add(DirectoryDelta(newNode.name, DeltaState.Equal).apply {
                                    diff(oldNode, newNode)
                                })
                            }
                        }
                        old.advance()
                        new.advance()
                    }
                    compareTo < 0 -> {
                        addDelta(old.node(), DeltaState.Deleted)
                        old.advance()
                    }
                    else -> {
                        addDelta(new.node(), DeltaState.Created)
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
            return deltas.isEmpty() && state == DeltaState.Equal
        }
        pruneEqualDirectories()
    }

fun Delta.dump(print: (s: String) -> Unit, indent: Int = 0) {
    print("    ".repeat(indent))
    print("\"$name\" ")
    when (this) {
        is FileDelta -> print("file $state\n")
        is DirectoryDelta -> {
            print("${if (state == DeltaState.DirToFile) "file" else "dir"} $state\n")
            deltas.forEach { it.dump(print, indent + 1) }
        }
    }
}
