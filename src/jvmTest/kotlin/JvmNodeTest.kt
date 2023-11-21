package ch.softappeal.diff

import java.io.*
import java.nio.file.*
import kotlin.io.path.*
import kotlin.test.*

fun assertOutput(expected: String, printBlock: () -> Unit) {
    val charset = Charsets.UTF_8
    val bytes = ByteArrayOutputStream()
    val out = System.out
    System.setOut(PrintStream(bytes, true, charset))
    try {
        printBlock()
    } finally {
        System.setOut(out)
    }
    assertEquals((expected + "\n").trimIndent(), bytes.toString(charset))
}

val TEST_DIR: Path = Path("src/commonTest/resources/test")
val TEST2_DIR: Path = TEST_DIR.resolveSibling("test2")

class JvmNodeTest : NodeTest() {
    @Suppress("SameParameterValue")
    override fun assertEquals(expected: String, printBlock: () -> Unit) = assertOutput(expected, printBlock)

    @Test
    fun createDirectoryNode() {
        assertEquals("""
            - ``
                - `a.txt` 1 CFCD208495D565EF66E7DFF9F98764DA
                - `b`
                    - `d.txt` 1 CFCD208495D565EF66E7DFF9F98764DA
                    - `e`
                        - `g.txt` 1 C4CA4238A0B923820DCC509A6F75849B
                    - `f.txt` 1 CFCD208495D565EF66E7DFF9F98764DA
                - `c.txt` 1 CFCD208495D565EF66E7DFF9F98764DA
        """) { createDirectoryNode(ALGORITHM, TEST_DIR).print() }
    }
}
