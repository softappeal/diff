package ch.softappeal.diff

private const val FILE_NODE: Byte = 0
private const val DIR_NODE: Byte = 1

fun DirNode.toByteArray(): ByteArray {
    val writer = Writer(100_000)
    fun Node.write() {
        val name = name.encodeToByteArray(throwOnInvalidSequence = true)
        writer.writeVarInt(name.size)
        writer.writeByteArray(name)
        when (this) {
            is FileNode -> {
                writer.writeByte(FILE_NODE)
                writer.writeByteArray(digest)
            }
            is DirNode -> {
                writer.writeByte(DIR_NODE)
                writer.writeVarInt(children.size)
                children.forEach { it.write() }
            }
        }
    }
    write()
    return writer.toByteArray()
}

fun ByteArray.fromByteArray(digestLength: Int): DirNode {
    fun Reader.read(): Node {
        val name = readByteArray(readVarInt()).decodeToString(throwOnInvalidSequence = true)
        return when (readByte()) {
            FILE_NODE -> FileNode(name, readByteArray(digestLength))
            DIR_NODE -> DirNode(name, List(readVarInt()) { read() })
            else -> error("")
        }
    }

    val reader = Reader(this)
    return reader.read().apply { check(reader.isDrained) } as DirNode
}

// copied from https://github.com/softappeal/yass2

class Writer(initialCapacity: Int) {
    var byteArray = ByteArray(initialCapacity)
    private var current = 0

    fun writeByte(byte: Byte) {
        if (current >= byteArray.size) byteArray = byteArray.copyOf(maxOf(1000, 2 * byteArray.size))
        byteArray[current++] = byte
    }

    fun writeByteArray(byteArray: ByteArray) {
        val newCurrent = current + byteArray.size
        if (newCurrent > this.byteArray.size) {
            this.byteArray = this.byteArray.copyOf(maxOf(newCurrent + 1000, 2 * this.byteArray.size))
        }
        byteArray.copyInto(this.byteArray, current)
        current = newCurrent
    }

    fun toByteArray() = byteArray.copyOf(current)
}

class Reader(private val byteArray: ByteArray) {
    private var current = 0

    val isDrained get() = current >= byteArray.size

    fun readByte(): Byte {
        require(current < byteArray.size)
        return byteArray[current++]
    }

    fun readByteArray(length: Int): ByteArray {
        val newCurrent = current + length
        require(newCurrent <= byteArray.size)
        return ByteArray(length).apply {
            byteArray.copyInto(this, 0, current, newCurrent)
            current = newCurrent
        }
    }
}

private const val INT_INV_7F = 0x7F.inv()

fun Writer.writeVarInt(value: Int) {
    var v = value
    while (true) {
        if (v and INT_INV_7F == 0) {
            writeByte(v.toByte())
            return
        }
        writeByte((v and 0x7F or 0x80).toByte())
        v = v ushr 7
    }
}

fun Reader.readVarInt(): Int {
    var shift = 0
    var value = 0
    while (true) {
        val b = readByte().toInt()
        value = value or (b and 0x7F shl shift)
        if (b and 0x80 == 0) return value
        shift += 7
    }
}
