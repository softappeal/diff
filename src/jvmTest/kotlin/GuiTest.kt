package ch.softappeal.diff

fun main() {
    val delta = createDirectoryDelta(NodeDigestToPaths(OLD_EVERYTHING), NodeDigestToPaths(NEW_EVERYTHING))
    gui(delta)
}
