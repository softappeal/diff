package ch.softappeal.diff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

const val TEST_RESOURCES = "testResources"

val DUPLICATES = root {
    file("a1", 1)
    dir("dir") {
        file("a2", 1)
        file("b2", 2)
        file("a3", 1)
        file("c", 4)
    }
    file("b1", 2)
    file("c", 3)
}

class NodeBuilder {
    val children = mutableListOf<Node>()

    fun file(name: String, digest: Byte) {
        children.add(FileNode(name, byteArrayOf(digest)))
    }

    fun dir(name: String, block: NodeBuilder.() -> Unit = {}) {
        val builder = NodeBuilder()
        builder.block()
        children.add(DirNode(name, builder.children.sortedBy(Node::name)))
    }
}

fun root(block: NodeBuilder.() -> Unit): DirNode {
    val builder = NodeBuilder()
    builder.block()
    return DirNode("", builder.children.sortedBy(Node::name))
}

fun toString(print: Appendable.() -> Unit) = StringBuffer().apply { print() }.toString()

class NodeTest {
    @Test
    fun illegalNodeName() {
        assertEquals(
            "node name 'a/b' must not contain '/'",
            assertFailsWith<IllegalArgumentException> { FileNode("a/b", byteArrayOf()) }.message
        )
        assertEquals(
            "node name 'a`b' must not contain '`'",
            assertFailsWith<IllegalArgumentException> { DirNode("a`b", listOf()) }.message
        )
    }

    @Test
    fun noDuplicates() {
        assertEquals(
            "",
            toString {
                printDuplicates(
                    root {
                        file("a", 0)
                        file("b", 1)
                    }
                )
            },
        )
    }

    @Test
    fun duplicates() {
        assertEquals(
            """
                - DUPLICATES
                    - `AQ==`
                        - `/a1`
                        - `/dir/a2`
                        - `/dir/a3`
                    - `Ag==`
                        - `/b1`
                        - `/dir/b2`


            """.trimIndent(),
            toString { printDuplicates(DUPLICATES) },
        )
    }

    @Test
    fun createDirNode() {
        assertEquals(
            """
                - ``
                    - `a.txt` `z80ghJXVZe9m59/5+Ydk2g==`
                    - `diff`
                        - `a.txt` `z80ghJXVZe9m59/5+Ydk2g==`
                        - `b.txt` `xMpCOKC5I4INzFCab3WEmw==`

            """.trimIndent(),
            toString { printNode(DirNode(TEST_RESOURCES, "MD5")) },
        )
    }
}
