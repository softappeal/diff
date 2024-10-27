package ch.softappeal.diff

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.test.Test

class SerializeTest {
    @Test
    fun test() {
        assertOutput("""
            - ``
                - `a.txt` 2000 00
                - `c` 4000 01
                - `dir`
                    - `dir2`
                        - `x.txt` 10000000 00
                    - `x.txt` 10000 02
        """) {
            val bytes = ByteArrayOutputStream()
            DataOutputStream(bytes).writeNode(root {
                file("a.txt", 2000, 0)
                file("c", 4000, 1)
                dir("dir") {
                    file("x.txt", 10000, 2)
                    dir("dir2") {
                        file("x.txt", 10000000, 0)
                    }
                }
            })
            DataInputStream(ByteArrayInputStream(bytes.toByteArray())).readNode().print()
        }
    }
}
