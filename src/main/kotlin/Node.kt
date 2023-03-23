package ch.softappeal.diff

import ch.softappeal.yass2.serialize.binary.*
import kotlinx.coroutines.*
import java.io.*
import java.security.*

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

val NodeBaseEncoders = listOf(StringEncoder, ByteArrayEncoder)
val NodeConcreteClasses = listOf(FileNode::class, DirectoryNode::class)

private const val MAC_DS_STORE = ".DS_Store"

fun createDirectoryNode(digestAlgorithm: String, directory: String): DirectoryNode = runBlocking {
    CoroutineScope(Dispatchers.Default).async {
        fun fileNode(file: File) = FileNode(file.name).apply {
            launch { digest = MessageDigest.getInstance(digestAlgorithm).digest(file.readBytes()) }
        }

        fun directoryNode(directory: File, name: String): DirectoryNode = DirectoryNode(
            name,
            buildList {
                (directory.listFiles() ?: throw IOException("'$directory' is not a directory")).forEach { file ->
                    if (file.isFile) {
                        if (MAC_DS_STORE != file.name) add(fileNode(file))
                    } else {
                        add(directoryNode(file, file.name))
                    }
                }
            }
        )
        directoryNode(File(directory), "")
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

fun printDuplicates(digestToPaths: DigestToPaths, print: (s: String) -> Unit) {
    val duplicates = digestToPaths.filter { it.value.size != 1 }
    if (duplicates.isNotEmpty()) {
        print("\n")
        print("- Duplicates\n")
        duplicates.forEach { duplicate ->
            print("    - ${duplicate.key}\n")
            duplicate.value.forEach { print("        - `$it`\n") }
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
