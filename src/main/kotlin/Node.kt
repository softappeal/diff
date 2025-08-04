package ch.softappeal.diff

import ch.softappeal.yass2.core.serialize.binary.ByteArrayBinaryEncoder
import ch.softappeal.yass2.core.serialize.binary.IntBinaryEncoder
import ch.softappeal.yass2.core.serialize.binary.StringBinaryEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.regex.Pattern
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readBytes
import kotlin.io.path.relativeTo

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

internal val BinaryEncoderObjects = listOf(StringBinaryEncoder::class, IntBinaryEncoder::class, ByteArrayBinaryEncoder::class)
internal val ConcreteAndEnumClasses = listOf(FileNode::class, DirectoryNode::class)
val NodeSerializer = BinarySerializer

fun createDirectoryNode(name: String, nodes: List<Node>) = DirectoryNode(name, nodes.sortedBy(Node::name))

fun Node.print(indent: Int = 0) {
    print("${"    ".repeat(indent)}`$name`")
    when (this) {
        is FileNode -> println(" $size ${digest.toHex()}")
        is DirectoryNode -> {
            println()
            nodes.forEach { it.print(indent + 1) }
        }
    }
}

@Suppress("SpellCheckingInspection") private val HexChars = "0123456789ABCDEF".toCharArray()
internal fun ByteArray.toHex(): String {
    val hexDigits = CharArray(2 * size)
    var i = 0
    for (b in this) {
        val byte = 0xFF and b.toInt()
        hexDigits[i++] = HexChars[byte ushr 4]
        hexDigits[i++] = HexChars[byte and 0x0F]
    }
    return hexDigits.concatToString()
}

internal fun String.concatPath(name: String) = "$this$DIR_SEP$name"

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
        println("Duplicates")
        duplicates.forEach { duplicate ->
            println("    ${duplicate.key}")
            duplicate.value.forEach { println("        `$it`") }
        }
    }
}

private val GoodName = Pattern.compile("[a-zA-Z0-9-_.]+")
fun goodName(name: String) = GoodName.matcher(name).matches()
private fun DirectoryNode.badPaths(): List<String> = buildList {
    fun Node.visit(path: String) {
        val nextPath = if (this == this@badPaths) "" else path.concatPath(name)
        if (nextPath.isNotEmpty() && !goodName(name)) add("$nextPath${if (this is FileNode) "" else "/"}")
        when (this) {
            is FileNode -> {}
            is DirectoryNode -> nodes.forEach { it.visit(nextPath) }
        }
    }
    visit("")
}

fun DirectoryNode.printBadPaths() {
    val badPaths = badPaths()
    if (badPaths.isEmpty()) {
        println("<no-bad-paths>")
    } else {
        println("BadPaths")
        badPaths.forEach { println("    `$it`") }
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

private fun Path.ignored(basePath: Path, isIgnored: (relativePath: String) -> Boolean): Boolean {
    val relativePath = relativeTo(basePath).pathString
    val ignored = isIgnored(relativePath)
    // println("${if (ignored) 'i' else ' '} '$relativePath${if (isDirectory()) "/" else ""}'")
    return ignored
}

fun Path.ignoredFile(basePath: Path) = ignored(basePath) { ".DS_Store" == name }
fun Path.ignoredDir(basePath: Path) = ignored(basePath) { relativePath -> ".git" == relativePath }

fun DirectoryNode.printFilesBySize() {
    val extToPathSizes = buildMap {
        paths { size }.forEach { pathSize ->
            val path = pathSize.path
            val lastDot = path.lastIndexOf('.')
            val ext = if (lastDot < 0 || lastDot == path.lastIndex) null else path.substring(lastDot)
            val pathSizes = getOrPut(ext) { mutableListOf() }
            pathSizes.add(pathSize)
        }
    }
    extToPathSizes.entries.sortedBy { it.key }.forEach { (ext, pathSizes) ->
        println(if (ext == null) "<no-ext>" else "`$ext`")
        pathSizes.sortedByDescending { it.info }.forEach { (path, size) -> println("    ${size / 1000} KB `$path`") }
    }
}

fun createDirectoryNode(digestAlgorithm: String, sourceDirectory: Path): DirectoryNode = runBlocking {
    CoroutineScope(Dispatchers.Default).async {
        fun directoryNode(directory: Path, name: String): DirectoryNode = createDirectoryNode(
            name,
            buildList {
                Files.newDirectoryStream(directory).forEach { path ->
                    require(!path.isSymbolicLink()) { "'$path' is a symbolic link" }
                    if (path.isRegularFile()) {
                        if (path.ignoredFile(sourceDirectory)) return@forEach
                        add(FileNode(path.name).apply {
                            launch {
                                val bytes = path.readBytes()
                                size = bytes.size
                                digest = MessageDigest.getInstance(digestAlgorithm).digest(bytes)
                            }
                        })
                    } else if (path.isDirectory()) {
                        if (path.ignoredDir(sourceDirectory)) return@forEach
                        add(directoryNode(path, path.name))
                    } else {
                        error("path '$path' has unexpected type")
                    }
                }
            },
        )
        directoryNode(sourceDirectory, "")
    }.await()
}
