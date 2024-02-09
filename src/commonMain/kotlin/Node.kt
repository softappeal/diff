@file:GenerateBinarySerializer(
    [StringEncoder::class, IntEncoder::class, ByteArrayEncoder::class],
    [FileNode::class, DirectoryNode::class],
    [],
    false
)

package ch.softappeal.diff

import ch.softappeal.yass2.serialize.binary.ByteArrayEncoder
import ch.softappeal.yass2.serialize.binary.GenerateBinarySerializer
import ch.softappeal.yass2.serialize.binary.IntEncoder
import ch.softappeal.yass2.serialize.binary.StringEncoder
import ch.softappeal.yass2.transport.BytesReader
import ch.softappeal.yass2.transport.BytesWriter

const val DIR_SEP = '/'

private fun Node.checkName() {
    require(!name.contains(DIR_SEP)) { "node name '$name' must not contain '$DIR_SEP'" }
}

sealed class Node(open val name: String)

class FileNode(override val name: String) : Node(name) {
    var size: Int = 0
    lateinit var digest: ByteArray

    constructor(name: String, size: Int, digest: ByteArray) : this(name) {
        this.size = size
        this.digest = digest
    }

    init {
        checkName()
    }

    override fun toString() = "FileNode(name=`$name`,size=$size,digest=${digest.toHex()})"
}

class DirectoryNode(override val name: String, val nodes: List<Node>) : Node(name) {
    init {
        checkName()
        fun isSorted(): Boolean {
            for (n in 0 until nodes.size - 1) {
                if (nodes[n].name > nodes[n + 1].name) return false
            }
            return true
        }
        require(isSorted()) { "nodes $nodes must be sorted" }
        require(nodes.map { it.name }.toSet().size == nodes.size) {
            "DirectoryNode '$name' has duplicated nodes ${nodes.map { "'${it.name}'" }}"
        }
    }

    override fun toString() = "DirectoryNode(name=`$name`,nodes=${nodes.size})"
}

fun createDirectoryNode(name: String, nodes: List<Node>) = DirectoryNode(name, nodes.sortedBy(Node::name))

private val NodeSerializer = createSerializer()

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
        is FileNode -> println(" $size ${digest.toHex()}")
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

fun String.concatPath(name: String) = "$this$DIR_SEP$name"

private data class PathInfo<I>(val path: String, val info: I)

private fun <I> DirectoryNode.paths(info: FileNode.() -> I): Collection<PathInfo<I>> = buildList {
    fun Node.visit(path: String) {
        val nextPath = if (name == "") "" else path.concatPath(name)
        when (this) {
            is FileNode -> add(PathInfo(nextPath, info()))
            is DirectoryNode -> nodes.forEach { it.visit(nextPath) }
        }
    }
    visit("")
}

typealias DigestToPaths = Map<String, List<String>>

fun DirectoryNode.calculateDigestToPaths(): DigestToPaths = paths { digest.toHex() }.groupBy({ it.info }, { it.path })

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

fun DirectoryNode.printFilesBySize() {
    val extToPathSizes = buildMap<String?, MutableList<PathInfo<Int>>> {
        paths { size }.forEach { pathSize ->
            val path = pathSize.path
            val lastDot = path.lastIndexOf('.')
            val ext = if (lastDot < 0 || lastDot == path.lastIndex) null else path.substring(lastDot)
            val pathSizes = getOrPut(ext) { mutableListOf() }
            pathSizes.add(pathSize)
        }
    }
    extToPathSizes.entries.sortedBy { it.key }.forEach { (ext, pathSizes) ->
        println("- ${if (ext == null) "<no-ext>" else "`$ext`"}")
        pathSizes.sortedByDescending { it.info }.forEach { (path, size) -> println("    - ${size / 1000} KB `$path`") }
    }
}
