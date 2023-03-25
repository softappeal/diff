package ch.softappeal.diff

import ch.softappeal.yass2.transport.*
import java.io.*
import kotlin.system.*

@Suppress("PrivatePropertyName")
private val NodeSerializer = generatedBinarySerializer(NodeBaseEncoders)

fun ByteArray.readNode(): DirectoryNode {
    val reader = BytesReader(this)
    val node = NodeSerializer.read(reader) as DirectoryNode
    check(reader.isDrained)
    return node
}

private fun readNodeFromFile(file: String) = File(file).readBytes().readNode()
private fun readNodeFromStdIn() = System.`in`.readAllBytes().readNode()

private fun DirectoryNode.writeToStdOut() {
    val writer = BytesWriter(100_000)
    NodeSerializer.write(writer, this)
    System.out.writeBytes(writer.buffer.copyOf(writer.current))
}

val USAGE = """
    usage:
        'create' algorithm directory > nodeFile
        'print' < nodeFile
        'duplicates' < nodeFile
        'diff' oldNodeFile < newNodeFile
""".trimIndent()

fun main(vararg args: String) {
    fun throwInvalidArgs(): Nothing = throw IllegalArgumentException(USAGE)
    if (args.isEmpty()) throwInvalidArgs()
    val command = args[0]
    when {
        command == "create" && args.size == 3 -> createDirectoryNode(args[1], args[2]).writeToStdOut()
        command == "print" && args.size == 1 -> readNodeFromStdIn().print()
        command == "duplicates" && args.size == 1 -> printDuplicates(readNodeFromStdIn().calculateDigestToPaths())
        command == "diff" && args.size == 2 -> createDirectoryDelta(readNodeFromFile(args[1]), readNodeFromStdIn()).print()
        else -> throwInvalidArgs()
    }
}

fun testMain(vararg args: String) = main(*args)
