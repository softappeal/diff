package ch.softappeal.diff

import ch.softappeal.yass2.serialize.binary.*
import ch.softappeal.yass2.transport.*

const val DIR_SEP = '/'

sealed class Node(
    val name: String,
) {
    init {
        require(!name.contains(DIR_SEP)) { "node name '$name' must not contain '$DIR_SEP'" }
    }
}

class FileNode(
    name: String,
) : Node(name) {
    lateinit var digest: ByteArray

    constructor(name: String, digest: ByteArray) : this(name) {
        this.digest = digest
    }

    override fun toString() = "FileNode(name=`$name`,digest=${digest.toHex()})"
}

class DirectoryNode(
    name: String,
    nodes: List<Node>,
) : Node(name) {
    val nodes = nodes.sortedBy(Node::name)

    init {
        require(nodes.map { it.name }.toSet().size == nodes.size) {
            "DirectoryNode '$name' has duplicated nodes ${nodes.map { "'${it.name}'" }}"
        }
    }

    override fun toString() = "DirectoryNode(name=`$name`,nodes=${nodes.size})"
}

val NodeBaseEncoders = listOf(StringEncoder, ByteArrayEncoder)
val NodeConcreteClasses = listOf(FileNode::class, DirectoryNode::class)

fun ByteArray.readNode(): DirectoryNode {
    val reader = BytesReader(this)
    val node = NodeSerializer.read(reader) as DirectoryNode
    check(reader.isDrained)
    return node
}

fun DirectoryNode.write(): ByteArray {
    val writer = BytesWriter(100_000)
    NodeSerializer.write(writer, this)
    return writer.buffer.copyOf(writer.current)
}

fun Node.print(indent: Int = 0) {
    print("${"    ".repeat(indent)}- `$name`")
    when (this) {
        is FileNode -> println(" ${digest.toHex()}")
        is DirectoryNode -> {
            println()
            nodes.forEach { it.print(indent + 1) }
        }
    }
}

@Suppress("SpellCheckingInspection") private val HexChars = "0123456789ABCDEF".toCharArray()
fun ByteArray.toHex(): String {
    val hexDigits = CharArray(2 * size)
    var i = 0
    for (b in this) {
        val byte = 0xFF and b.toInt()
        hexDigits[i++] = HexChars[byte ushr 4]
        hexDigits[i++] = HexChars[byte and 0x0F]
    }
    return hexDigits.concatToString()
}

typealias DigestToPaths = Map<String, List<String>>

fun String.concatPath(name: String) = "$this$DIR_SEP$name"

fun DirectoryNode.calculateDigestToPaths(): DigestToPaths {
    val pathDigests = buildList {
        fun Node.visit(path: String) {
            val nextPath = if (name == "") "" else path.concatPath(name)
            when (this) {
                is FileNode -> add(Pair(nextPath, digest.toHex()))
                is DirectoryNode -> nodes.forEach { it.visit(nextPath) }
            }
        }
        visit("")
    }
    return pathDigests.groupBy({ it.second }, { it.first })
}

fun printDuplicates(digestToPaths: DigestToPaths) {
    val duplicates = digestToPaths.filter { it.value.size != 1 }
    if (duplicates.isEmpty()) {
        println("<no-duplicates>")
    } else {
        println("- Duplicates")
        duplicates.forEach { duplicate ->
            println("    - ${duplicate.key}")
            duplicate.value.forEach { println("        - `$it`") }
        }
    }
}

class NodeIterator(node: DirectoryNode) {
    private val iterator = node.nodes.iterator()
    private var node: Node? = null

    init {
        advance0()
    }

    fun done() = node == null

    fun current() = node!!

    private fun advance0() {
        node = if (iterator.hasNext()) iterator.next() else null
    }

    fun advance() {
        check(!done())
        advance0()
    }

    override fun toString() = "NodeIterator(${if (done()) "<done>" else current().toString()})"
}

const val MAC_DS_STORE = ".DS_Store"
