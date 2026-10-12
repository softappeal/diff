package ch.softappeal.diff

import kotlin.test.Test

class GuiTest {
    @Test
    fun test() {
        val delta = createDirectoryDelta(NodeDigestToPaths(OLD_EVERYTHING), NodeDigestToPaths(NEW_EVERYTHING))
        gui(delta)
        Thread.sleep(1000)
    }
}
