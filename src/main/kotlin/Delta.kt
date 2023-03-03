package ch.softappeal.but

enum class DeltaState { Same, New, Deleted, Changed, FileToDir, DirToFile }
enum class MovedState { MovedFrom, RenamedFrom }

sealed class Delta(
    parent: String,
    val name: String,
    val state: DeltaState,
    val digest: String?,
) {
    val path = if (name == "") "" else "$parent/$name"
    var movedFrom: String? = null
    var movedState: MovedState? = null
}

class FileDelta(
    parent: String,
    name: String,
    state: DeltaState,
    digest: String?,
) : Delta(parent, name, state, digest)

class DirectoryDelta(
    parent: String,
    name: String,
    state: DeltaState,
    digest: String?,
) : Delta(parent, name, state, digest) {
    val deltas: MutableList<Delta> = mutableListOf()
}

fun DirectoryDelta.find(path: String): DirectoryDelta {
    if (path.isEmpty()) return this
    val secondSlash = path.indexOf('/', 1)
    val name = path.substring(1, if (secondSlash < 0) path.length else secondSlash)
    val child = deltas.find { it.name == name }!! as DirectoryDelta
    return child.find(path.substring(name.length + 1))
}

fun create(oldDirectoryNode: DirectoryNode, newDirectoryNodeDigestToPaths: DirectoryNodeDigestToPaths): DirectoryDelta {
    val (newDirectoryNode, newDigestToPaths) = newDirectoryNodeDigestToPaths
    val oldDigestToPaths = oldDirectoryNode.calculateDigestToPaths()
    val deletedDigestToPath = mutableMapOf<String, String>()

    fun FileNode.updateDeletedDigestToPath(path: String): Boolean {
        val hexDigest = digest.toHex()
        val paths = newDigestToPaths[hexDigest]
        if (paths != null && paths.size == 1 && oldDigestToPaths[hexDigest]!!.size == 1) {
            deletedDigestToPath[hexDigest] = "$path/$name"
            return true
        }
        return false
    }

    fun DirectoryDelta.addDelta(node: Node, state: DeltaState) {
        when (node) {
            is FileNode -> {
                if (state == DeltaState.Deleted) {
                    if (node.updateDeletedDigestToPath(path)) return
                }
                deltas.add(FileDelta(path, node.name, state, node.digest.toHex()))
            }
            is DirectoryNode -> deltas.add(DirectoryDelta(path, node.name, state, null).apply {
                node.nodes.forEach { addDelta(it, state) }
            })
        }
    }

    fun DirectoryDelta.addNodeTypeChanged(node: DirectoryNode, state: DeltaState, nestedState: DeltaState, digest: String?) {
        deltas.add(DirectoryDelta(path, node.name, state, digest).apply {
            node.nodes.forEach { addDelta(it, nestedState) }
        })
    }

    return DirectoryDelta("", "", DeltaState.Same, null).apply {
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
                                    deltas.add(FileDelta(path, newNode.name, DeltaState.Changed, null))
                                }
                                is DirectoryNode -> {
                                    oldNode.updateDeletedDigestToPath(path)
                                    addNodeTypeChanged(newNode, DeltaState.FileToDir, DeltaState.New, null)
                                }
                            }
                            is DirectoryNode -> when (val newNode = new.node()) {
                                is FileNode -> addNodeTypeChanged(oldNode, DeltaState.DirToFile, DeltaState.Deleted, newNode.digest.toHex())
                                is DirectoryNode -> deltas.add(DirectoryDelta(path, newNode.name, DeltaState.Same, null).apply {
                                    diff(oldNode, newNode, "$path/${newNode.name}")
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
                        addDelta(new.node(), DeltaState.New)
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

        fun Delta.setMoved(from: String) = if (from.substringBeforeLast('/') == path.substringBeforeLast('/')) {
            movedState = MovedState.RenamedFrom
            movedFrom = from.substringAfterLast('/')
        } else {
            movedState = MovedState.MovedFrom
            movedFrom = from
        }

        fun Delta.fixupMovedFrom() {
            val from = deletedDigestToPath[digest]
            if (from != null) setMoved(from)
            when (this) {
                is FileDelta -> {}
                is DirectoryDelta -> deltas.forEach { it.fixupMovedFrom() }
            }
        }
        fixupMovedFrom()

        val root = this
        fun Delta.removeMoved() {
            when (this) {
                is FileDelta -> {}
                is DirectoryDelta -> {
                    deltas.toList().forEach { it.removeMoved() }
                    if (deltas.all { it.movedState == MovedState.MovedFrom }) {
                        val paths = deltas.map { it.movedFrom!!.substringBeforeLast('/') }
                        if (paths.distinct().size == 1) {
                            if (deltas.any { it.movedFrom!!.substringAfterLast('/') != it.name }) return // are there renamed nodes?
                            val from = paths.first()
                            val parent = root.find(from.substringBeforeLast('/'))
                            val child = parent.find("/${from.substringAfterLast('/')}")
                            if (child.state == DeltaState.DirToFile) return
                            parent.deltas.remove(child)
                            setMoved(from)
                            deltas.clear()
                        }
                    }
                }
            }
        }
        removeMoved()
    }
}

fun Delta.dump(print: (s: String) -> Unit, indent: Int = 0) {
    val moved = " $movedState \"$movedFrom\""
    val status = if (state == DeltaState.Same) "" else " $state"
    val info = if (state == DeltaState.New && movedState != null) moved else "$status${if (movedState == null) "" else moved}"
    val dirSuffix = if (this is DirectoryDelta && state != DeltaState.DirToFile) "/" else ""
    print("${"    ".repeat(indent)}\"$name$dirSuffix\"$info\n")
    when (this) {
        is FileDelta -> {}
        is DirectoryDelta -> deltas.forEach { it.dump(print, indent + 1) }
    }
}
