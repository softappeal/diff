package ch.softappeal.but

import ch.softappeal.yass2.serialize.binary.*
import kotlinx.coroutines.*
import java.io.*
import java.security.*

sealed class Node(
    val name: String,
)

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
    val nodes: List<Node>,
) : Node(name)

val NodeBaseEncoders = listOf(StringEncoder, ByteArrayEncoder)
val NodeConcreteClasses = listOf(FileNode::class, DirectoryNode::class)

fun ByteArray.toHex() = joinToString("") { "%02X".format(it) }

private typealias DigestToPaths = Map<String, List<String>>

fun DirectoryNode.calculateDigestToPaths(): DigestToPaths {
    val pathDigests = mutableListOf<Pair<String, String>>()
    fun Node.visit(path: String) {
        val nextPath = if (name == "") "" else "$path/$name"
        when (this) {
            is FileNode -> pathDigests.add(Pair(nextPath, digest.toHex()))
            is DirectoryNode -> nodes.forEach { it.visit(nextPath) }
        }
    }
    visit("")
    return pathDigests.groupBy({ it.second }, { it.first })
}

const val DsStore = ".DS_Store"

private fun createDirectoryNode(digestAlgorithm: String, directory: String): DirectoryNode {
    fun CoroutineScope.fileNode(file: File) = FileNode(file.name).apply {
        launch { digest = MessageDigest.getInstance(digestAlgorithm).digest(file.readBytes()) }
    }

    fun CoroutineScope.directoryNode(directory: File, name: String): DirectoryNode = DirectoryNode(
        name,
        mutableListOf<Node>().apply {
            (directory.listFiles() ?: throw IOException(directory.toString())).forEach { file ->
                if (file.isFile) {
                    if (DsStore != file.name) add(fileNode(file))
                } else {
                    add(directoryNode(file, file.name))
                }
            }
        }.sortedBy { it.name }
    )

    lateinit var node: DirectoryNode
    runBlocking {
        CoroutineScope(Dispatchers.Default).launch { node = directoryNode(File(directory), "") }.join()
    }
    return node
}

private fun printDuplicates(digestToPaths: DigestToPaths, print: (s: String) -> Unit) {
    val duplicates = digestToPaths.values.filter { it.size != 1 }
    if (duplicates.isEmpty()) {
        print("<no duplicates>\n")
    } else {
        print("duplicates:\n")
        duplicates.forEach { print("    ${it.map { name -> "\"$name\"" }}\n") }
    }
}

data class DirectoryNodeDigestToPaths(
    val directoryNode: DirectoryNode,
    val digestToPaths: DigestToPaths = directoryNode.calculateDigestToPaths(),
)

fun create(digestAlgorithm: String, directory: String, print: (s: String) -> Unit): DirectoryNodeDigestToPaths {
    val directoryNodeDigestToPaths = DirectoryNodeDigestToPaths(createDirectoryNode(digestAlgorithm, directory))
    printDuplicates(directoryNodeDigestToPaths.digestToPaths, print)
    return directoryNodeDigestToPaths
}
