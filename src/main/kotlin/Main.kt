package ch.softappeal.but

import ch.softappeal.yass2.transport.*
import java.io.*
import java.nio.charset.*

private fun usage() {
    throw RuntimeException("""
        usage:
        'backup' directory archivePrefix
        'node' algorithm directory nodeFile
        'print' nodeFile
        'delta' oldNodeFile newNodeFile deltaFile
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

private fun writeFile(file: String, block: (print: (s: String) -> Unit) -> Unit) {
    FileWriter(file, StandardCharsets.UTF_8).use { block(it::write) }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) usage()
    val command = args[0]
    when {
        command == "backup" && args.size == 3 -> backup(args[1], args[2])
        command == "node" && args.size == 4 -> writeNode(args[3], create(args[1], args[2], ::print))
        command == "print" && args.size == 2 -> readNode(args[1]).dump(::print)
        command == "delta" && args.size == 4 -> writeFile(args[3]) { create(readNode(args[1]), readNode(args[2])).dump(it) }
        command == "workflow" && (args.size == 4 || args.size == 5) -> {
            val algorithm = args[1]
            val directory = args[2]
            val nodeFile = args[3]
            val newNode = create(algorithm, directory, ::print)
            if (File(nodeFile).exists()) {
                val oldNode = readNode(nodeFile)
                val delta = create(oldNode, newNode)
                println()
                delta.dump(::print)
                println()
                print("type <y> to overwrite nodeFile (else abort): ")
                val answer = readln()
                if ("y" != answer) return
            }
            println()
            writeNode(nodeFile, newNode)
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
