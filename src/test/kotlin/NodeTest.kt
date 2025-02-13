package ch.softappeal.diff

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NodeBuilder {
    val nodes = mutableListOf<Node>()
    fun file(name: String, size: Int, digest: Byte) {
        nodes.add(FileNode(name, size, byteArrayOf(digest)))
    }

    fun file(name: String, digest: Byte) = file(name, 0, digest)
    fun dir(name: String, block: NodeBuilder.() -> Unit = {}) {
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

fun assertOutput(expected: String, printBlock: () -> Unit) {
    val charset = Charsets.UTF_8
    val bytes = ByteArrayOutputStream()
    val out = System.out
    System.setOut(PrintStream(bytes, true, charset))
    try {
        printBlock()
    } finally {
        System.setOut(out)
    }
    assertEquals((expected + "\n").trimIndent(), bytes.toString(charset))
}

val TEST_DIR: Path = Path("src/test/resources/test")
val TEST2_DIR: Path = TEST_DIR.resolveSibling("test2")

class NodeTest {
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
        assertOutput("""
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
        assertOutput("""
            {01=[/a1, /f/ff/a2, /f/ff/a3], 02=[/b1, /f/ff/b2], 03=[/c], 04=[/f/ff/c]}
        """) { println(digestToPaths.toString()) }
        assertOutput("""
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

    @Test
    fun createDirectoryNode() {
        assertOutput("""
            - ``
                - `a.txt` 1 CFCD208495D565EF66E7DFF9F98764DA
                - `b`
                    - `d.txt` 1 CFCD208495D565EF66E7DFF9F98764DA
                    - `e`
                        - `g.txt` 1 C4CA4238A0B923820DCC509A6F75849B
                    - `f.txt` 1 CFCD208495D565EF66E7DFF9F98764DA
                - `c.txt` 1 CFCD208495D565EF66E7DFF9F98764DA
        """) { createDirectoryNode(ALGORITHM, TEST_DIR).print() }
    }

    @Test
    fun goodName() {
        assertFalse(goodName(""))
        assertFalse(goodName("+"))
        assertTrue(goodName(" "))
        assertTrue(goodName("."))
        assertTrue(goodName("-"))
        assertTrue(goodName("_"))
        @Suppress("SpellCheckingInspection")
        assertTrue(goodName("..  --__aazzAAZZ0099"))
    }

    @Test
    fun printBadPaths() {
        assertOutput("""
            <no-bad-paths>
        """) {
            root {
            }.printBadPaths()
        }
        assertOutput("""
            - BadPaths
                - '/bad*dir/'
                - '/bad*dir/bad*file'
                - '/bad*file'
                - '/good-dir/bad*file'
                - '/good-dir/bad*file 2'
        """) {
            root {
                file("bad*file", 1)
                file("good-file", 1)
                dir("good-dir") {
                    file("bad*file 2", 1)
                    file("bad*file", 1)
                    file("good-file", 1)
                }
                dir("bad*dir") {
                    file("bad*file", 1)
                    file("good-file", 1)
                }
            }.printBadPaths()
        }
    }
}
