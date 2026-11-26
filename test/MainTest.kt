package ch.softappeal.diff

import kotlin.io.path.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TOOL_DIR = TEST_DATA

private fun assert(expected: String) {
    val out = StringBuffer()
    out.script(TOOL_DIR)
    assertEquals("$expected\n".trimIndent(), "\n$out")
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
                    - `/d/a.txt`
        """

        assert(
            """
            $duplicates
            NODE
            - ``
                - `a.txt` `+eLqqkLZ/p5VipuO8b82bxkKrKqDutJkHuEG6QQQluQ=`
                - `d`
                    - `a.txt` `+eLqqkLZ/p5VipuO8b82bxkKrKqDutJkHuEG6QQQluQ=`
                    - `b.txt` `Z7F2cFtGIGYUIZ9HoFrueuaj7b6FC7viFMU2uYmupNI=`
            """
        )

        assert(
            """
        $duplicates
            DELTA
            - ``
                - `diff.ser` NewFile
            """
        )

        assert(
            """
            $duplicates
            DELTA
            - ``
                - `diff.ser` ChangedFile
            """
        )

        nodeFile.deleteExisting()
    }
}
