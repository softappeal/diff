package ch.softappeal.diff

class JvmDeltaTest : DeltaTest() {
    override fun assertEquals(old: DirectoryNode, new: DirectoryNode, expected: String) {
        assertOutput(expected) { createDirectoryDelta(NodeDigestToPaths(old), NodeDigestToPaths(new)).print() }
    }
}
