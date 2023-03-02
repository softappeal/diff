package ch.softappeal.but

import kotlin.test.*

fun assertEquals(expected: String, block: (print: (s: String) -> Unit) -> Unit) {
    val s = StringBuilder()
    block { s.append(it) }
    assertEquals((expected + "\n").trimIndent(), s.toString())
}

private fun Node.dump(print: (s: String) -> Unit, indent: Int = 0) {
    print("    ".repeat(indent))
    print("\"$name\"")
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
    fun test() {
        assertEquals(
            """
                <no duplicates>
                "/"
                    "a.txt" 0CC175B9C0F1B6A831C399E269772661
                    "dir"
                        "d.txt" 8277E0910D750195B448797616E091AD
            """
        ) {
            create("MD5", "src/test/resources/test", it).directoryNode.dump(it)
        }
    }

    @Test
    fun test2() {
        assertEquals(
            """
                duplicates:
                    ["/a.txt", "/dir/d.txt"]
                "/"
                    "a.txt" 8277E0910D750195B448797616E091AD
                    "dir"
                        "d.txt" 8277E0910D750195B448797616E091AD
            """
        ) {
            create("MD5", "src/test/resources/test2", it).directoryNode.dump(it)
        }
    }
}
