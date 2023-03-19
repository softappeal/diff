package ch.softappeal.but

import ch.softappeal.yass2.transport.*
import java.io.*

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

fun main(
    algorithm: String, directory: String, nodeFile: String,
    print: (s: String) -> Unit, doOverwrite: () -> Boolean,
) {
    val newNodeDigestToPaths = NodeDigestToPaths(createDirectoryNode(algorithm, directory))
    printDuplicates(newNodeDigestToPaths.digestToPaths, print)
    if (File(nodeFile).exists()) {
        print("\n")
        val oldNode = readNode(nodeFile)
        createDirectoryDelta(NodeDigestToPaths(oldNode), newNodeDigestToPaths).dump(print)
        print("\n")
        print("type <y> to overwrite nodeFile (else abort): ")
        if (!doOverwrite()) return
    }
    print("\n")
    writeNode(nodeFile, newNodeDigestToPaths.node)
    print("nodeFile '$nodeFile' written\n")
}

fun main(vararg args: String) {
    require(args.size == 3) { "usage: algorithm directory nodeFile" }
    main(args[0], args[1], args[2], ::print) {
        val answer = readln()
        "y" == answer
    }
}
