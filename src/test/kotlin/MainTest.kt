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
            assertEquals("""
                - ``
                    - `a.txt` CFCD208495D565EF66E7DFF9F98764DA
                    - `b`
                        - `d.txt` CFCD208495D565EF66E7DFF9F98764DA
                        - `e`
                            - `g.txt` C4CA4238A0B923820DCC509A6F75849B
                        - `f.txt` CFCD208495D565EF66E7DFF9F98764DA
                    - `c.txt` CFCD208495D565EF66E7DFF9F98764DA
            """) { main("printNode") }
        }

        redirectStdIn(oldNode) {
            assertEquals("""
                - Duplicates
                    - CFCD208495D565EF66E7DFF9F98764DA
                        - `/a.txt`
                        - `/b/d.txt`
                        - `/b/f.txt`
                        - `/c.txt`
            """) { main("printDuplicates") }
        }

        redirectStdIn(newNode) {
            assertEquals("""
                <no-duplicates>
            """) { main("printDuplicates") }
        }

        val oldNodeFile = Path("build/oldNode.yass")
        oldNodeFile.writeBytes(oldNode)
        redirectStdIn(newNode) {
            assertEquals("""
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

        assertEquals("""

            DONE
   
        """) { script(toolDirectory, ALGORITHM, false, null) }

        redirectStdIn("\n".toByteArray()) {
            assertEquals("""
                
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
                ABORTED
                
            """) { script(toolDirectory, ALGORITHM, true, null) }
        }

        redirectStdIn("y\n".toByteArray()) {
            assertEquals("""
                
                - `/`
                    - `b/`
                        - `node.yass` New
                
                type <y> to accept changes (else abort): 
                DONE
                
            """) { script(toolDirectory, ALGORITHM, false, null) }
        }

        redirectStdIn("\n".toByteArray()) {
            assertEquals("""
                
                - `/`
                    - `b/`
                        - `node.yass` Changed
                
                type <y> to accept changes (else abort): 
                ABORTED
                
            """) { script(toolDirectory, ALGORITHM, false, null) }
        }

        redirectStdIn("y\n".toByteArray()) {
            script(toolDirectory, ALGORITHM, false, "build/backup_")
        }

        toolDirectory.nodeFile().deleteExisting()
    }
}
