package ch.softappeal.but

import kotlin.system.*
import kotlin.test.*

fun assertEquals(expected: String, block: (print: (s: String) -> Unit) -> Unit) {
    val s = StringBuilder()
    block { s.append(it) }
    assertEquals((expected + "\n").trimIndent(), s.toString())
}

class NodeTest {
    @Test
    fun test() {
        assertEquals(
            """
                - / test
                    - a.txt 0cc175b9c0f1b6a831c399e269772661
                    - / dir
                        - d.txt 8277e0910d750195b448797616e091ad
            """
        ) {
            create("MD5", "src/test/resources/test").dump(it)
        }
    }

    @Ignore
    @Test
    fun test2() {
        println(measureTimeMillis {
            create("MD5", "C://Users/guru/OneDrive/data")
        })
    }
}
