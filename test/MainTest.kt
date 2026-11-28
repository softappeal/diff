package ch.softappeal.diff

import kotlin.io.path.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals

val DUPLICATES = root {
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

class MainTest {
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
            toString { printDuplicates(DUPLICATES) },
        )
    }

    @Test
    fun script() {
        fun assertScript(expected: String) {
            val out = StringBuffer()
            out.script(TEST_DATA)
            assertEquals("$expected\n".trimIndent(), "\n$out")
        }

        val nodeFile = Path("$TEST_DATA/$NODE_FILE")
        nodeFile.deleteIfExists()

        val duplicates = """
            diff null

            - DUPLICATES
                - `+eLqqkLZ/p5VipuO8b82bxkKrKqDutJkHuEG6QQQluQ=`
                    - `/a.txt`
                    - `/d/a.txt`
        """

        assertScript(
            """
            $duplicates
            NODE
            - `` NewDir
                - `a.txt` NewFile `+eLqqkLZ/p5VipuO8b82bxkKrKqDutJkHuEG6QQQluQ=`
                - `d` NewDir
                    - `a.txt` NewFile `+eLqqkLZ/p5VipuO8b82bxkKrKqDutJkHuEG6QQQluQ=`
                    - `b.txt` NewFile `Z7F2cFtGIGYUIZ9HoFrueuaj7b6FC7viFMU2uYmupNI=`
            """
        )

        assertScript(
            """
        $duplicates
            DIFF
            - ``
            """
        )

        assertScript(
            """
            $duplicates
            DIFF
            - ``
            """
        )

        nodeFile.deleteExisting()
    }
}
