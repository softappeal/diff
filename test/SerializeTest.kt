package ch.softappeal.diff

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SerializeTest {
    @Test
    fun serializer() {
        assertEquals(
            toString { printNode(DUPLICATES) },
            toString { printNode(DUPLICATES.toByteArray().fromByteArray(1)) },
        )
    }

    @Test
    fun varInt() {
        fun check(value: Int, vararg bytes: Int) {
            val writer = Writer(1000)
            writer.writeVarInt(value)
            val byteArray = writer.toByteArray()
            assertEquals(bytes.map { it.toByte() }, byteArray.toList())
            val reader = Reader(byteArray)
            assertEquals(value, reader.readVarInt().apply { check(reader.isDrained) })
        }
        check(0, 0)
        check(1, 1)
        check(2, 2)
        check(0x7F, 0x7F)
        check(0x80, 0x80, 0x01)
        check(0x3F_FF, 0xFF, 0x7F)
        check(0x40_00, 0x80, 0x80, 0x01)
        check(-1, 0xFF, 0xFF, 0xFF, 0xFF, 0x0F)
        check(-2, 0xFE, 0xFF, 0xFF, 0xFF, 0x0F)
    }

    @Test
    fun streams() {
        with(Writer(1)) {
            assertEquals(1, byteArray.size)
            writeByte(13)
            assertEquals(1, byteArray.size)
            writeByte(31)
            assertEquals(1000, byteArray.size)
            assertContentEquals(byteArrayOf(13, 31), toByteArray())
            writeByteArray(ByteArray(998))
            assertEquals(1000, byteArray.size)
            writeByte(99)
            assertEquals(2000, byteArray.size)
            assertEquals(13, byteArray[0])
            assertEquals(31, byteArray[1])
            assertEquals(99, byteArray[1000])
        }
        with(Writer(2)) {
            writeByteArray(byteArrayOf(1))
            assertEquals(2, byteArray.size)
            writeByteArray(byteArrayOf(2, 3))
            assertEquals(1003, byteArray.size)
            writeByteArray(ByteArray(1000))
            writeByteArray(byteArrayOf(4))
            assertEquals(2006, byteArray.size)
            assertEquals(1, byteArray[0])
            assertEquals(2, byteArray[1])
            assertEquals(3, byteArray[2])
            assertEquals(4, byteArray[1003])
        }
        val byteArray: ByteArray
        with(Writer(1000)) {
            writeByte(13)
            writeByteArray(byteArrayOf(1, 2, 3))
            writeByte(31)
            byteArray = toByteArray()
        }
        assertContentEquals(byteArrayOf(13, 1, 2, 3, 31), byteArray)
        with(Reader(byteArray)) {
            assertFalse(isDrained)
            assertEquals(13, readByte())
            assertContentEquals(byteArrayOf(1, 2, 3), readByteArray(3))
            assertEquals(31, readByte())
            assertTrue(isDrained)
            assertFails { readByte() }
            assertFails { readByteArray(1) }
        }
    }
}
