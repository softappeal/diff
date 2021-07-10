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

const val DsStore = ".DS_Store"

fun create(digestAlgorithm: String, directory: String): DirectoryNode {
    fun CoroutineScope.fileNode(file: File) = FileNode(file.name).apply {
        launch { digest = MessageDigest.getInstance(digestAlgorithm).digest(file.readBytes()) }
    }

    fun CoroutineScope.directoryNode(directory: File): DirectoryNode = DirectoryNode(
        directory.name,
        mutableListOf<Node>().apply {
            (directory.listFiles() ?: throw IOException(directory.toString())).forEach { file ->
                if (file.isFile) {
                    if (DsStore != file.name) add(fileNode(file))
                } else {
                    add(directoryNode(file))
                }
            }
        }.sortedBy { it.name }
    )

    lateinit var node: DirectoryNode
    runBlocking {
        CoroutineScope(Dispatchers.Default).launch { node = directoryNode(File(directory)) }.join()
    }
    return node
}

fun Node.dump(print: (s: String) -> Unit, indent: Int = 0) {
    fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    print("    ".repeat(indent))
    print("- $name")
    when (this) {
        is FileNode -> print(" `${digest.toHex()}`\n")
        is DirectoryNode -> {
            print("\n")
            nodes.forEach { it.dump(print, indent + 1) }
        }
    }
}
