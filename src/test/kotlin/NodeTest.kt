package ch.softappeal.diff

import java.io.*
import java.nio.file.*
import kotlin.io.path.*
import kotlin.test.*

class NodeBuilder {
    val nodes = mutableListOf<Node>()

    fun file(name: String, digest: Byte) {
        nodes.add(FileNode(name, byteArrayOf(digest)))
    }

    fun dir(name: String, block: NodeBuilder.() -> Unit) {
        val builder = NodeBuilder()
        builder.block()
        nodes.add(DirectoryNode(name, builder.nodes))
    }
}

fun root(block: NodeBuilder.() -> Unit): DirectoryNode {
    val builder = NodeBuilder()
    builder.block()
    return DirectoryNode("", builder.nodes)
}

fun assertEquals(expected: String, block: () -> Unit) {
    val charset = Charsets.UTF_8
    val bytes = ByteArrayOutputStream()
    val out = System.out
    System.setOut(PrintStream(bytes, true, charset))
    try {
        block()
    } finally {
        System.setOut(out)
    }
    assertEquals((expected + "\n").trimIndent(), bytes.toString(charset))
}

const val ALGORITHM = "MD5"
val TEST_DIR: Path = Path("src/test/resources/test")
val TEST2_DIR: Path = TEST_DIR.resolveSibling("test2")

class NodeTest {
    @Test
    fun toHex() {
        assertEquals("00", byteArrayOf(0).toHex())
        assertEquals("09", byteArrayOf(9).toHex())
        assertEquals("0A", byteArrayOf(10).toHex())
        assertEquals("0F", byteArrayOf(15).toHex())
        assertEquals("7F", byteArrayOf(127).toHex())
        assertEquals("FF", byteArrayOf(-1).toHex())
        assertEquals("00FF", byteArrayOf(0, -1).toHex())
    }

    @Test
    fun illegalNodeName() {
        assertEquals(
            "node name 'a/b' must not contain '/'",
            assertFailsWith<IllegalArgumentException> { DirectoryNode("a/b", listOf()) }.message
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
    fun createDirectoryNode() {
        assertEquals("""
            - ``
                - `a.txt` CFCD208495D565EF66E7DFF9F98764DA
                - `b`
                    - `d.txt` CFCD208495D565EF66E7DFF9F98764DA
                    - `e`
                        - `g.txt` C4CA4238A0B923820DCC509A6F75849B
                    - `f.txt` CFCD208495D565EF66E7DFF9F98764DA
                - `c.txt` CFCD208495D565EF66E7DFF9F98764DA
        """) { createDirectoryNode(ALGORITHM, TEST_DIR).print() }
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
        assertTrue(NodeIterator(DirectoryNode("", listOf())).done())
        val fileNode1 = FileNode("a", byteArrayOf())
        val fileNode2 = FileNode("b", byteArrayOf())
        val iterator = NodeIterator(DirectoryNode("", listOf(fileNode1, fileNode2)))
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
