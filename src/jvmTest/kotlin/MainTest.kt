package ch.softappeal.diff

import java.io.*
import kotlin.io.path.*
import kotlin.test.*

private fun redirectStdOut(block: () -> Unit): ByteArray {
    val bytes = ByteArrayOutputStream()
    val out = System.out
    System.setOut(PrintStream(bytes, true))
    try {
        block()
    } finally {
        System.setOut(out)
    }
    return bytes.toByteArray()
}

private fun redirectStdIn(bytes: ByteArray, block: () -> Unit) {
    val `in` = System.`in`
    System.setIn(ByteArrayInputStream(bytes))
    try {
        block()
    } finally {
        System.setIn(`in`)
    }
}

class MainTest {
    @Test
    fun wrongUsage() {
        fun test(vararg args: String) = assertEquals(USAGE, assertFailsWith<IllegalArgumentException> { testMain(*args) }.message)
        test()
        test("xxx")
        test("createNode", "x", "x", "x")
        test("printNode", "x", "x", "x")
        test("printDuplicates", "x", "x", "x")
        test("diff", "x", "x", "x")
        test("script")
        test("script", "x", "x", "x")
        test("scriptNoDuplicates")
        test("scriptNoDuplicates", "x", "x", "x")
    }

    @Test
    fun test() {
        val oldNode = redirectStdOut { main("createNode", ALGORITHM, TEST_DIR.toString()) }
        val newNode = redirectStdOut { main("createNode", ALGORITHM, TEST2_DIR.toString()) }

        redirectStdIn(oldNode) {
            assertOutput("""
                - ``
                    - `a.txt` 1 CFCD208495D565EF66E7DFF9F98764DA
                    - `b`
                        - `d.txt` 1 CFCD208495D565EF66E7DFF9F98764DA
                        - `e`
                            - `g.txt` 1 C4CA4238A0B923820DCC509A6F75849B
                        - `f.txt` 1 CFCD208495D565EF66E7DFF9F98764DA
                    - `c.txt` 1 CFCD208495D565EF66E7DFF9F98764DA
            """) { main("printNode") }
        }

        redirectStdIn(oldNode) {
            assertOutput("""
                - Duplicates
                    - CFCD208495D565EF66E7DFF9F98764DA
                        - `/a.txt`
                        - `/b/d.txt`
                        - `/b/f.txt`
                        - `/c.txt`
            """) { main("printDuplicates") }
        }

        redirectStdIn(root {
            file(".", 0, 0)
            file(".txt", 1000, 0)
            file("a.txt", 2000, 0)
            file("b.txt", 3000, 0)
            file("c", 4000, 0)
            file("d.", 5000, 0)
            file("e.pdf", 6000, 0)
            file("f.pdf", 7000, 0)
            file("g.a", 8000, 0)
            file("h.A", 9000, 0)
            dir("dir") {
                file("x.txt", 10000, 0)
            }
        }.write()) {
            assertOutput("""
                - <no-ext>
                    - 5 KB `/d.`
                    - 4 KB `/c`
                    - 0 KB `/.`
                - `.A`
                    - 9 KB `/h.A`
                - `.a`
                    - 8 KB `/g.a`
                - `.pdf`
                    - 7 KB `/f.pdf`
                    - 6 KB `/e.pdf`
                - `.txt`
                    - 10 KB `/dir/x.txt`
                    - 3 KB `/b.txt`
                    - 2 KB `/a.txt`
                    - 1 KB `/.txt`
            """) { main("printSizes") }
        }

        redirectStdIn(newNode) {
            assertOutput("""
                <no-duplicates>
            """) { main("printDuplicates") }
        }

        val oldNodeFile = Path("build/oldNode.yass")
        oldNodeFile.writeBytes(oldNode)
        redirectStdIn(newNode) {
            assertOutput("""
                - `/`
                    - `b/` Deleted
                        - `d.txt` Deleted
                        - `e/` Deleted
                            - `g.txt` Deleted
                        - `f.txt` Deleted
                    - `c.txt` Deleted
            """) { main("diff", oldNodeFile.toString()) }
        }
    }

    @Test
    fun scriptTest() {
        val toolDirectory = TEST_DIR.resolve("b")

        assertOutput("""

            - Duplicates
                - CFCD208495D565EF66E7DFF9F98764DA
                    - `/a.txt`
                    - `/b/d.txt`
                    - `/b/f.txt`
                    - `/c.txt`
            
            DONE
   
        """) { script(toolDirectory, ALGORITHM, null) }

        redirectStdIn("y\n".toByteArray()) {
            assertOutput("""
                
                - Duplicates
                    - CFCD208495D565EF66E7DFF9F98764DA
                        - `/a.txt`
                        - `/b/d.txt`
                        - `/b/f.txt`
                        - `/c.txt`
                
                - `/`
                    - `b/`
                        - `node.yass` New
                
                type <y> to accept changes (else abort): 
                DONE
                
            """) { script(toolDirectory, ALGORITHM, null) }
        }

        redirectStdIn("\n".toByteArray()) {
            assertOutput("""
                
                - Duplicates
                    - CFCD208495D565EF66E7DFF9F98764DA
                        - `/a.txt`
                        - `/b/d.txt`
                        - `/b/f.txt`
                        - `/c.txt`
                
                - `/`
                    - `b/`
                        - `node.yass` Bigger
                
                type <y> to accept changes (else abort): 
                ABORTED
                
            """) { script(toolDirectory, ALGORITHM, null) }
        }

        redirectStdIn("y\n".toByteArray()) {
            script(toolDirectory, ALGORITHM, "build/backup_")
        }

        toolDirectory.nodeFile().deleteExisting()
    }

    @Ignore
    @Test
    fun testExternalDir() {
        val dir = Path("/Users/guru/Library/CloudStorage/OneDrive-Personal/data/major")
        val oldNode = dir.resolve("diff/node.yass").readNode()
        val newNode = createDirectoryNode(ALGORITHM, dir)
        val newNodeDigestToPaths = NodeDigestToPaths(newNode)
        printDuplicates(newNodeDigestToPaths.digestToPaths)
        createDirectoryDelta(NodeDigestToPaths(oldNode), newNodeDigestToPaths).print()
    }
}
