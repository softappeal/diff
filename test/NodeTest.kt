package ch.softappeal.diff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NodeBuilder {
    val nodes = mutableListOf<Node>()

    fun file(name: String, digest: Byte) {
        nodes.add(FileNode(name, byteArrayOf(digest)))
    }

    fun dir(name: String, block: NodeBuilder.() -> Unit = {}) {
        val builder = NodeBuilder()
        builder.block()
        nodes.add(DirNode(name, builder.nodes))
    }
}

fun root(block: NodeBuilder.() -> Unit): DirNode {
    val builder = NodeBuilder()
    builder.block()
    return DirNode("", builder.nodes)
}

fun toString(print: Appendable.() -> Unit) = StringBuffer().apply { print() }.toString()

class NodeTest {
    @Test
    fun illegalNodeName() {
        assertEquals(
            "node name 'a/b' must not contain '/'",
            assertFailsWith<IllegalArgumentException> { DirNode("a/b", listOf()) }.message
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
            toString {
                printDuplicates(
                    root {
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
                )
            },
        )
    }

    @Test
    fun createDirNode() {
        // System.out.printNodes((DirNode("/Users/guru/OneDrive/data/minor", "MD5")))
        assertEquals(
            """
                - ``
                    - `a.txt` `z80ghJXVZe9m59/5+Ydk2g==`
                    - `diff`
                        - `a.txt` `z80ghJXVZe9m59/5+Ydk2g==`
                        - `b.txt` `xMpCOKC5I4INzFCab3WEmw==`

            """.trimIndent(),
            toString { printNodes(DirNode("testResources", "MD5")) },
        )
    }
}
