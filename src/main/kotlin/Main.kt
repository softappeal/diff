package ch.softappeal.but

import ch.softappeal.yass2.transport.*
import java.io.*
import java.nio.charset.*

private fun usage() {
    throw RuntimeException("""
        usage:
        'print' nodeFile
        'workflow' algorithm directory nodeFile [ archivePrefix ]
    """.trimIndent())
}

@Suppress("PrivatePropertyName")
private val NodeSerializer = generatedBinarySerializer(NodeBaseEncoders)

private fun readNode(file: String): DirectoryNode {
    val reader = BytesReader(File(file).readBytes())
    val node = NodeSerializer.read(reader) as DirectoryNode
    check(reader.isDrained)
    return node
}

private fun writeNode(file: String, node: Node) {
    val writer = BytesWriter(100_000)
    NodeSerializer.write(writer, node)
    File(file).writeBytes(writer.buffer.copyOf(writer.current))
}

fun main(args: Array<String>) {
    if (args.isEmpty()) usage()
    val command = args[0]
    when {
        command == "print" && args.size == 2 -> readNode(args[1]).dump(::print)
        command == "workflow" && (args.size == 4 || args.size == 5) -> {
            val algorithm = args[1]
            val directory = args[2]
            val nodeFile = args[3]
            val directoryNodeDigestToPaths = create(algorithm, directory, ::print)
            if (File(nodeFile).exists()) {
                val oldNode = readNode(nodeFile)
                val directoryDelta = create(oldNode, directoryNodeDigestToPaths)
                println()
                directoryDelta.dump(::print)
                println()
                print("type <y> to overwrite nodeFile (else abort): ")
                val answer = readln()
                if ("y" != answer) return
            }
            println()
            writeNode(nodeFile, directoryNodeDigestToPaths.directoryNode)
            println("nodeFile '$nodeFile' written")
            if (args.size == 5) {
                val archivePrefix = args[4]
                backup(directory, archivePrefix)
                println("archive '$archivePrefix' written")
            }
            println()
            print("press <return> ")
            readln()
        }
        else -> usage()
    }
}
