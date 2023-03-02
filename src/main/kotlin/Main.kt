package ch.softappeal.but

import ch.softappeal.yass2.transport.*
import java.io.*
import java.nio.charset.*

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
    check(args.size == 3 || args.size == 4) { "usage: algorithm directory nodeFile [ archivePrefix ]" }
    println()
    val algorithm = args[0]
    val directory = args[1]
    val nodeFile = args[2]
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
    if (args.size == 4) {
        val archivePrefix = args[3]
        backup(directory, archivePrefix)
        println("archive '$archivePrefix' written")
    }
    println()
    print("press <return> ")
    readln()
}
