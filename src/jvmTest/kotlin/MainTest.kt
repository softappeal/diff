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

        redirectStdIn(NEW_EVERYTHING.write()) {
            assertOutput("""
                - 3 `/Changed/b`
                - 2 `/Changed/a`
                - 2 `/Changed/e`
                - 1 `/Changed/s`
                - 0 `/DirToFile/moved/a`
                - 0 `/DirToFile/renamed/a`
                - 0 `/DirToFile/simple/a`
                - 0 `/FileToDir/moved/a/b`
                - 0 `/FileToDir/renamed/a/b`
                - 0 `/FileToDir/renamed/c`
                - 0 `/FileToDir/simple/a/b`
                - 0 `/MovedFrom/c/a/b`
                - 0 `/New/a/b/c`
                - 0 `/RenamedFrom/c/b`
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
