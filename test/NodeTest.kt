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
        nodes.add(DirectoryNode(name, builder.nodes))
    }
}

fun root(block: NodeBuilder.() -> Unit): DirectoryNode {
    val builder = NodeBuilder()
    builder.block()
    return DirectoryNode("", builder.nodes)
}

fun toString(print: Appendable.() -> Unit) = StringBuffer().apply { print() }.toString()

class NodeTest {
    @Test
    fun illegalNodeName() {
        assertEquals(
            "node name 'a/b' must not contain '/'",
            assertFailsWith<IllegalArgumentException> { DirectoryNode("a/b", listOf()) }.message
        )
        assertEquals(
            "node name 'a`b' must not contain '`'",
            assertFailsWith<IllegalArgumentException> { DirectoryNode("a`b", listOf()) }.message
        )
    }

    @Test
    fun noDuplicates() {
        assertEquals(
            "",
            toString {
                printDuplicates(root {
                    file("a", 0)
                    file("b", 1)
                }.DigestToPaths())
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
                        - `/f/ff/a2`
                        - `/f/ff/a3`
                    - `Ag==`
                        - `/b1`
                        - `/f/ff/b2`


            """.trimIndent(),
            toString {
                printDuplicates(
                    root {
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
                    }.DigestToPaths()
                )
            },
        )
    }

    @Test
    fun createDirectoryNode() {
        // System.out.printNodes((DirectoryNode("/Users/guru/OneDrive/data/minor", "MD5")))
        assertEquals(
            """
                - ``
                    - `a.txt` `z80ghJXVZe9m59/5+Ydk2g==`
                    - `diff`
                        - `a.txt` `z80ghJXVZe9m59/5+Ydk2g==`
                        - `b.txt` `xMpCOKC5I4INzFCab3WEmw==`

            """.trimIndent(),
            toString { printNodes(DirectoryNode("testResources", "MD5")) },
        )
    }
}
