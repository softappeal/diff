package ch.softappeal.diff

import java.io.DataInput
import java.io.DataOutput

private fun DataOutput.writeSize(value: Int) = writeInt(value)
private fun DataInput.readSize(): Int = readInt()

private fun DataOutput.writeDigest(value: ByteArray) {
    writeShort(value.size)
    write(value)
}

private fun DataInput.readDigest(): ByteArray {
    val size = readShort().toInt()
    return ByteArray(size).apply { readFully(this) }
}

private fun DataOutput.writeName(value: String) = writeUTF(value)
private fun DataInput.readName(): String = readUTF()

private fun DataOutput.writeNodes(value: List<Node>) {
    writeShort(value.size)
    value.forEach { writeNode(it) }
}

private fun DataInput.readNodes(): List<Node> {
    val size = readShort().toInt()
    return List(size) { readNode() }
}

private const val FILE_NODE: Byte = 0
private const val DIRECTORY_NODE: Byte = 1

fun DataOutput.writeNode(value: Node) = when (value) {
    is FileNode -> {
        writeByte(FILE_NODE.toInt())
        writeName(value.name)
        writeSize(value.size)
        writeDigest(value.digest)
    }
    is DirectoryNode -> {
        writeByte(DIRECTORY_NODE.toInt())
        writeName(value.name)
        writeNodes(value.nodes)
    }
}

fun DataInput.readNode(): Node = when (val type = readByte()) {
    FILE_NODE -> FileNode(
        name = readName(),
        size = readSize(),
        digest = readDigest(),
    )
    DIRECTORY_NODE -> DirectoryNode(
        name = readName(),
        nodes = readNodes(),
    )
    else -> error("unknown node type $type")
}
