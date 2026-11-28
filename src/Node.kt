package ch.softappeal.diff

import kotlin.io.encoding.Base64

class Digest(val bytes: ByteArray) {
    override fun hashCode() = bytes.contentHashCode()
    override fun equals(other: Any?) = other is Digest && bytes.contentEquals(other.bytes)
    override fun toString() = Base64.encode(bytes)
}

private const val DIR_SEP = '/'

typealias Path = String

fun Path.Path(name: String): Path = "$this$DIR_SEP$name"

private fun Node.checkName() {
    require(!name.contains(DIR_SEP)) { "node name '$name' must not contain '$DIR_SEP'" }
    require(!name.contains('`')) { "node name '$name' must not contain '`'" }
}

sealed class Node {
    abstract val name: String
    var movedPaths: List<Path>? = null
}

enum class FileState { NewFile, DeletedFile, ChangedFile }

class FileNode(override val name: String, val state: FileState, var digest: Digest?) : Node() {
    init {
        checkName()
    }

    override fun toString() = "`${Path()}`:$state"
}

enum class DirState { NewDir, DeletedDir, KeptDir, FileToDir, DirToFile }

class DirNode(override val name: String, val state: DirState, val children: List<Node>) : Node() {
    init {
        checkName()
        check(children.isSortedBy(Node::name))
    }

    override fun toString() = "`${Path()}`:$state"
}

fun Appendable.printNode(node: Node, diff: Boolean = true, indent: Int = 0) {
    fun movedPaths() {
        if (node.movedPaths != null) {
            append(" <")
            node.movedPaths!!.forEach { append(" `$it`") }
        }
    }
    append("${"    ".repeat(indent)}- `${node.name}`")
    when (node) {
        is FileNode -> {
            if (diff) append(" ${node.state}")
            movedPaths()
            if (node.digest != null) appendLine(" `${node.digest}`")
        }
        is DirNode -> {
            if (diff && node.state != DirState.KeptDir) append(" ${node.state}")
            movedPaths()
            appendLine()
            node.children.forEach { printNode(it, diff, indent + 1) }
        }
    }
}
