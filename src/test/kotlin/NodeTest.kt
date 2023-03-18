package ch.softappeal.but

import java.io.*
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

fun assertEquals(expected: String, block: (print: (s: String) -> Unit) -> Unit) {
    val s = StringBuilder()
    block { s.append(it) }
    assertEquals((expected + "\n").trimIndent(), s.toString())
}

private fun Node.dump(print: (s: String) -> Unit, indent: Int = 0) {
    print("    ".repeat(indent))
    print("'$name'")
    when (this) {
        is FileNode -> print(" ${digest.toHex()}\n")
        is DirectoryNode -> {
            print("\n")
            nodes.forEach { it.dump(print, indent + 1) }
        }
    }
}

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
    fun dump() {
        assertEquals("""
            ''
                'c' FD
                'f'
                    'empty'
                    'ff'
                        'x' FE
                'q' FF
        """) {
            root {
                file("q", -1)
                dir("f") {
                    dir("ff") {
                        file("x", -2)
                    }
                    dir("empty") {}
                }
                file("c", -3)
            }.dump(it)
        }
    }

    @Test
    fun thisIsNotADirectory() {
        assertEquals(
            "'this-is-not-a-directory' is not a directory",
            assertFailsWith<IOException> { createDirectoryNode("MD5", "this-is-not-a-directory") }.message
        )
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun createDirectoryNode() {
        assertEquals("""
            ''
                'a.txt' CFCD208495D565EF66E7DFF9F98764DA
                'b'
                    'd.txt' CFCD208495D565EF66E7DFF9F98764DA
                    'e'
                        'g.txt' C4CA4238A0B923820DCC509A6F75849B
                    'f.txt' CFCD208495D565EF66E7DFF9F98764DA
                'c.txt' CFCD208495D565EF66E7DFF9F98764DA
        """) {
            createDirectoryNode("MD5", "src/test/resources/test").dump(it)
        }
    }

    @Test
    fun noDuplicates() {
        assertEquals("""
            <no duplicates>
        """) {
            printDuplicates(root {
                file("a", 0)
                file("b", 1)
            }.calculateDigestToPaths(), it)
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
         """) {
            it(digestToPaths.toString() + '\n')
        }
        assertEquals("""
            duplicates:
                ['/a1', '/f/ff/a2', '/f/ff/a3']
                ['/b1', '/f/ff/b2']
        """) {
            printDuplicates(digestToPaths, it)
        }
    }

    @Ignore
    @Test
    fun big() {
        createDirectoryNode("MD5", "/Users/guru/Library/CloudStorage/OneDrive-Personal/data")
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
