package ch.softappeal.diff

import kotlin.test.*

class NodeBuilder {
    val nodes = mutableListOf<Node>()
    fun file(name: String, size: Int, digest: Byte) {
        nodes.add(FileNode(name, size, byteArrayOf(digest)))
    }

    fun file(name: String, digest: Byte) = file(name, 0, digest)
    fun dir(name: String, block: NodeBuilder.() -> Unit) {
        val builder = NodeBuilder()
        builder.block()
        nodes.add(createDirectoryNode(name, builder.nodes))
    }
}

fun root(block: NodeBuilder.() -> Unit): DirectoryNode {
    val builder = NodeBuilder()
    builder.block()
    return createDirectoryNode("", builder.nodes)
}

const val ALGORITHM = "MD5"

abstract class NodeTest {
    protected abstract fun assertEquals(expected: String, printBlock: () -> Unit)

    @Test
    fun toHex() {
        assertEquals("00", byteArrayOf(0).toHex())
        assertEquals("01", byteArrayOf(1).toHex())
        assertEquals("02", byteArrayOf(2).toHex())
        assertEquals("03", byteArrayOf(3).toHex())
        assertEquals("04", byteArrayOf(4).toHex())
        assertEquals("05", byteArrayOf(5).toHex())
        assertEquals("06", byteArrayOf(6).toHex())
        assertEquals("07", byteArrayOf(7).toHex())
        assertEquals("08", byteArrayOf(8).toHex())
        assertEquals("09", byteArrayOf(9).toHex())
        assertEquals("0A", byteArrayOf(10).toHex())
        assertEquals("0B", byteArrayOf(11).toHex())
        assertEquals("0C", byteArrayOf(12).toHex())
        assertEquals("0D", byteArrayOf(13).toHex())
        assertEquals("0E", byteArrayOf(14).toHex())
        assertEquals("0F", byteArrayOf(15).toHex())
        assertEquals("7F", byteArrayOf(127).toHex())
        assertEquals("FF", byteArrayOf(-1).toHex())
        assertEquals("00FF", byteArrayOf(0, -1).toHex())
    }

    @Test
    fun illegalNodeName() {
        assertEquals(
            "node name 'a/b' must not contain '/'",
            assertFailsWith<IllegalArgumentException> { createDirectoryNode("a/b", listOf()) }.message
        )
    }

    @Test
    fun duplicatedNodeNames() {
        assertEquals(
            "DirectoryNode 'd' has duplicated nodes ['a', 'a', 'b']",
            assertFailsWith<IllegalArgumentException> {
                root {
                    dir("d") {
                        file("a", 0)
                        file("a", 0)
                        file("b", 0)
                    }
                }
            }.message
        )
    }

    @Test
    fun notSortedNodes() {
        assertEquals(
            "nodes [FileNode(name=`b`,size=0,digest=00), FileNode(name=`a`,size=0,digest=00)] must be sorted",
            assertFailsWith<IllegalArgumentException> {
                DirectoryNode("", listOf(
                    FileNode("b", 0, byteArrayOf(0)),
                    FileNode("a", 0, byteArrayOf(0)),
                ))
            }.message
        )
    }

    @Test
    fun noDuplicates() {
        assertEquals("""
            <no-duplicates>
        """) {
            printDuplicates(root {
                file("a", 0)
                file("b", 1)
            }.calculateDigestToPaths())
        }
    }

    @Test
    fun duplicates() {
        val digestToPaths = root {
            file("a1", 1)
            dir("f") {
                dir("ff") {
                    file("a2", 1)
                    file("b2", 2)
                    file("a3", 1)
                    file("c", 4)
                }
            }
            file("b1", 2)
            file("c", 3)
        }.calculateDigestToPaths()
        assertEquals("""
            {01=[/a1, /f/ff/a2, /f/ff/a3], 02=[/b1, /f/ff/b2], 03=[/c], 04=[/f/ff/c]}
        """) { println(digestToPaths.toString()) }
        assertEquals("""
            - Duplicates
                - 01
                    - `/a1`
                    - `/f/ff/a2`
                    - `/f/ff/a3`
                - 02
                    - `/b1`
                    - `/f/ff/b2`
        """) { printDuplicates(digestToPaths) }
    }

    @Test
    fun nodeIterator() {
        assertTrue(NodeIterator(createDirectoryNode("", listOf())).done())
        val fileNode1 = FileNode("a", 0, byteArrayOf())
        val fileNode2 = FileNode("b", 0, byteArrayOf())
        val iterator = NodeIterator(createDirectoryNode("", listOf(fileNode1, fileNode2)))
        assertFalse(iterator.done())
        assertSame(fileNode1, iterator.current())
        iterator.advance()
        assertFalse(iterator.done())
        assertSame(fileNode2, iterator.current())
        iterator.advance()
        assertTrue(iterator.done())
        assertFailsWith<NullPointerException> { iterator.current() }
        assertFailsWith<IllegalStateException> { iterator.advance() }
        assertTrue(iterator.done())
    }
}
