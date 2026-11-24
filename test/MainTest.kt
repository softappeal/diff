package ch.softappeal.diff

import java.io.ByteArrayInputStream
import kotlin.io.path.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TOOL_DIR = "$TEST_RESOURCES/diff"

private fun assert(expected: String, input: Boolean) {
    val stdIn = ByteArrayInputStream("${if (input) "y" else "n"}\n".toByteArray())
    val stdOut = StringBuffer()
    script(stdOut, stdIn, TOOL_DIR)
    assertEquals((expected + "\n").trimIndent(), stdOut.toString())
}

class MainTest {
    @Test
    fun test() {
        val nodeFile = Path("$TOOL_DIR/$NODE_FILE")

        nodeFile.deleteIfExists()

        val duplicates = """
            diff null

            - DUPLICATES
                - `+eLqqkLZ/p5VipuO8b82bxkKrKqDutJkHuEG6QQQluQ=`
                    - `/a.txt`
                    - `/diff/a.txt`
        """

        assert(
            """
            $duplicates
            NODE
            - ``
                - `a.txt` `+eLqqkLZ/p5VipuO8b82bxkKrKqDutJkHuEG6QQQluQ=`
                - `diff`
                    - `a.txt` `+eLqqkLZ/p5VipuO8b82bxkKrKqDutJkHuEG6QQQluQ=`
                    - `b.txt` `Z7F2cFtGIGYUIZ9HoFrueuaj7b6FC7viFMU2uYmupNI=`

            DONE

            """,
            true,
        )

        fun call(input: Boolean) {
            assert(
                """
            $duplicates
            DELTA
            - ``
                - `diff`
                    - `diff.ser` New

            type <y> to accept changes (else abort): 
            ${if (input) "DONE" else "ABORTED"}

            """,
                input,
            )
        }
        call(false)
        call(true)

        assert(
            """
            $duplicates
            DELTA
            - ``
                - `diff`
                    - `diff.ser` Changed

            type <y> to accept changes (else abort): 
            DONE

            """,
            true,
        )

        nodeFile.deleteExisting()
    }
}
