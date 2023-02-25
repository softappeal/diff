package ch.softappeal.but

import kotlin.test.*

class DeltaTest {
    @Test
    fun addNodeChangedToDir() {
        assertEquals(
            """
                . Equal
                    a ChangedToDir
                        d Created
                        d2 Created
                            d2 Created
            """
        ) {
            create(
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1))
                )),
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("d", byteArrayOf(1)),
                        DirectoryNode("d2", listOf(
                            FileNode("d2", byteArrayOf(1))
                        ))
                    ))
                )),
            ).dump(it)
        }
    }

    @Test
    fun addNodeChangedToFile() {
        assertEquals(
            """
                . Equal
                    a ChangedToFile
                        d Deleted
                        d2 Deleted
                            d2 Deleted
            """
        ) {
            create(
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("d", byteArrayOf(1)),
                        DirectoryNode("d2", listOf(
                            FileNode("d2", byteArrayOf(1))
                        ))
                    ))
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1))
                )),
            ).dump(it)
        }
    }

    @Test
    fun addDeltaCreated() {
        assertEquals(
            """
                . Equal
                    a Created
                    d Created
                        d Created
            """
        ) {
            create(
                DirectoryNode("", listOf()),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("d", listOf(
                        FileNode("d", byteArrayOf(1))
                    ))
                )),
            ).dump(it)
        }
    }

    @Test
    fun addDeltaDeleted() {
        assertEquals(
            """
                . Equal
                    a Deleted
                    d Deleted
                        d Deleted
            """
        ) {
            create(
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("d", listOf(
                        FileNode("d", byteArrayOf(1))
                    ))
                )),
                DirectoryNode("", listOf()),
            ).dump(it)
        }
    }

    @Test
    fun pruneEqualDirectories1() {
        assertEquals(
            """
                . Equal
            """
        ) {
            create(
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("d", listOf(
                        FileNode("d", byteArrayOf(1))
                    ))
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("d", listOf(
                        FileNode("d", byteArrayOf(1))
                    ))
                )),
            ).dump(it)
        }
    }

    @Test
    fun pruneEqualDirectories2() {
        assertEquals(
            """
                . Equal
                    a Differ
            """
        ) {
            create(
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("d", listOf(
                        FileNode("d", byteArrayOf(1))
                    ))
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(2)),
                    DirectoryNode("d", listOf(
                        FileNode("d", byteArrayOf(1))
                    ))
                )),
            ).dump(it)
        }
    }

    @Test
    fun pruneEqualDirectories3() {
        assertEquals(
            """
                . Equal
                    a ChangedToDir
            """
        ) {
            create(
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1))
                )),
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf())
                )),
            ).dump(it)
        }
    }
}
