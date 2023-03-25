package ch.softappeal.diff

import java.io.*
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
        test("create", "x", "x", "x")
        test("print", "x", "x", "x")
        test("duplicates", "x", "x", "x")
        test("diff", "x", "x", "x")
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun test() {
        val algorithm = "MD5"
        val oldDir = "src/test/resources/test"
        val newDir = "src/test/resources/test2"
        val oldNode = redirectStdOut { main("create", algorithm, oldDir) }
        val newNode = redirectStdOut { main("create", algorithm, newDir) }

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
            """) { main("print") }
        }

        redirectStdIn(oldNode) {
            assertEquals("""
                - Duplicates
                    - CFCD208495D565EF66E7DFF9F98764DA
                        - `/a.txt`
                        - `/b/d.txt`
                        - `/b/f.txt`
                        - `/c.txt`
            """) { main("duplicates") }
        }

        redirectStdIn(newNode) {
            assertEquals("""
                <no-duplicates>
            """) { main("duplicates") }
        }

        val oldNodeFile = "build/oldNode.yass"
        File(oldNodeFile).writeBytes(oldNode)
        redirectStdIn(newNode) {
            assertEquals("""
                - `/`
                    - `b/` Deleted
                        - `d.txt` Deleted
                        - `e/` Deleted
                            - `g.txt` Deleted
                        - `f.txt` Deleted
                    - `c.txt` Deleted
            """) { main("diff", oldNodeFile) }
        }
    }
}
