package ch.softappeal.diff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

const val TEST_DATA = "test/data"

class NodeBuilder {
    val children = mutableListOf<Node>()

    fun file(name: String, digest: Byte) {
        children.add(FileNode(name, FileState.NewFile, Digest(byteArrayOf(digest))))
    }

    fun dir(name: String, block: NodeBuilder.() -> Unit = {}) {
        val builder = NodeBuilder()
        builder.block()
        children.add(DirNode(name, DirState.NewDir, builder.children.sortedBy(Node::name)))
    }
}

fun root(block: NodeBuilder.() -> Unit): DirNode {
    val builder = NodeBuilder()
    builder.block()
    return DirNode("", DirState.NewDir, builder.children.sortedBy(Node::name))
}

fun toString(print: Appendable.() -> Unit) = StringBuffer().apply { print() }.toString()

class NodeTest {
    @Test
    fun illegalNodeName() {
        assertEquals(
            "node name 'a/b' must not contain '/'",
            assertFailsWith<IllegalArgumentException> { FileNode("a/b", FileState.NewFile, Digest(byteArrayOf())) }.message
        )
        assertEquals(
            "node name 'a`b' must not contain '`'",
            assertFailsWith<IllegalArgumentException> { DirNode("a`b", DirState.NewDir, listOf()) }.message
        )
    }

    @Test
    fun createDirNodeFromFileSystem() {
        assertEquals(
            """
                - `` NewDir
                    - `a.txt` NewFile `z80ghJXVZe9m59/5+Ydk2g==`
                    - `d` NewDir
                        - `a.txt` NewFile `z80ghJXVZe9m59/5+Ydk2g==`
                        - `b.txt` NewFile `xMpCOKC5I4INzFCab3WEmw==`

            """.trimIndent(),
            toString { printNode(createDirNodeFromFileSystem(TEST_DATA, "MD5")) },
        )
    }
}
