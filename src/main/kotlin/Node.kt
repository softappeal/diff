package ch.softappeal.diff

import ch.softappeal.yass2.serialize.binary.*
import kotlinx.coroutines.*
import java.nio.file.*
import java.security.*
import kotlin.io.path.*

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
}

class DirectoryNode(
    name: String,
    nodes: List<Node>,
) : Node(name) {
    val nodes = nodes.sortedBy { it.name }

    init {
        require(nodes.map { it.name }.toSet().size == nodes.size) {
            "DirectoryNode '$name' has duplicated nodes ${nodes.map { "'${it.name}'" }}"
        }
    }
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

val NodeBaseEncoders = listOf(StringEncoder, ByteArrayEncoder)
val NodeConcreteClasses = listOf(FileNode::class, DirectoryNode::class)

const val MAC_DS_STORE = ".DS_Store"

fun createDirectoryNode(digestAlgorithm: String, sourceDirectory: Path): DirectoryNode = runBlocking {
    CoroutineScope(Dispatchers.Default).async {
        fun directoryNode(directory: Path, name: String): DirectoryNode = DirectoryNode(
            name,
            buildList {
                Files.newDirectoryStream(directory).forEach { path ->
                    require(!path.isSymbolicLink()) { "'$path' is a symbolic link" }
                    if (path.isRegularFile()) {
                        if (MAC_DS_STORE == path.name) return@forEach
                        add(FileNode(path.name).apply {
                            launch { digest = MessageDigest.getInstance(digestAlgorithm).digest(path.readBytes()) }
                        })
                    } else {
                        add(directoryNode(path, path.name))
                    }
                }
            }
        )
        directoryNode(sourceDirectory, "")
    }.await()
}

fun ByteArray.toHex() = joinToString("") { "%02X".format(it) }

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
}
