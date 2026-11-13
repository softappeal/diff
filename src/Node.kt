package ch.softappeal.diff

import ch.softappeal.yass2.serialize.ConcreteAndEnumClasses
import ch.softappeal.yass2.serialize.binary.BinaryEncoders
import ch.softappeal.yass2.serialize.binary.ByteArrayBinaryEncoder
import ch.softappeal.yass2.serialize.binary.StringBinaryEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.name
import kotlin.io.path.readBytes

private const val DIR_SEP = '/'

private fun Node.checkName() {
    require(!name.contains(DIR_SEP)) { "node name '$name' must not contain '$DIR_SEP'" }
    require(!name.contains('`')) { "node name '$name' must not contain '`'" }
}

sealed class Node {
    abstract val name: String
}

class FileNode(override val name: String, var digest: ByteArray) : Node() {
    init {
        checkName()
    }
}

class DirectoryNode(override val name: String, nodes: List<Node>) : Node() {
    val nodes = nodes.sortedBy(Node::name)

    init {
        checkName()
    }
}

@BinaryEncoders(StringBinaryEncoder::class, ByteArrayBinaryEncoder::class)
@ConcreteAndEnumClasses(FileNode::class, DirectoryNode::class)
val NodeSerializer = BinarySerializer()

typealias Path = String

fun DirectoryNode(root: Path, digestAlgorithm: String) = runBlocking(Dispatchers.Default) {
    fun directoryNode(current: Path, name: String): DirectoryNode = DirectoryNode(
        name,
        buildList {
            Files.newDirectoryStream(Path(current)).forEach { path ->
                require(!path.isSymbolicLink()) { "path '$path' is a symbolic link" }
                when {
                    path.isRegularFile() -> add(
                        FileNode(path.name, byteArrayOf()).apply {
                            launch { digest = MessageDigest.getInstance(digestAlgorithm).digest(path.readBytes()) }
                        }
                    )
                    path.isDirectory() -> add(
                        directoryNode(path.toString(), path.name)
                    )
                    else -> error("path '$path' has unexpected type")
                }
            }
        },
    )
    directoryNode(root, "")
}

typealias Digest = String

fun ByteArray.toDigest(): Digest = Base64.encode(this)

fun Appendable.printNodes(node: Node, indent: Int = 0) {
    append("${"    ".repeat(indent)}- `${node.name}`")
    when (node) {
        is FileNode -> appendLine(" `${node.digest.toDigest()}`")
        is DirectoryNode -> {
            appendLine()
            node.nodes.forEach { printNodes(it, indent + 1) }
        }
    }
}

fun Path.Path(name: String): Path = "$this$DIR_SEP$name"

typealias DigestToPaths = Map<Digest, List<Path>>

fun DirectoryNode.DigestToPaths(): DigestToPaths = buildMap<Digest, MutableList<Path>> {
    fun Node.visit(current: Path) {
        val next = if (name.isEmpty()) "" else current.Path(name)
        when (this) {
            is FileNode -> getOrPut(digest.toDigest()) { mutableListOf() }.add(next)
            is DirectoryNode -> nodes.forEach { it.visit(next) }
        }
    }
    visit("")
}

fun Appendable.printDuplicates(digestToPaths: DigestToPaths) {
    val duplicates = digestToPaths.filter { it.value.size != 1 }
    if (duplicates.isEmpty()) return
    appendLine("- DUPLICATES")
    duplicates.forEach { [digest, paths] ->
        appendLine("    - `$digest`")
        paths.forEach { appendLine("        - `$it`") }
    }
    appendLine()
}

class NodeDesc(
    val node: DirectoryNode,
    val digestToPaths: DigestToPaths = node.DigestToPaths(),
)
