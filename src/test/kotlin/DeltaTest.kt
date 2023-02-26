package ch.softappeal.but

import kotlin.test.*

private fun dump(print: (s: String) -> Unit, oldDirectoryNode: DirectoryNode, newDirectoryNode: DirectoryNode) {
    create(oldDirectoryNode, DirectoryNodeDigestToPaths(newDirectoryNode)).dump(print)
}

class DeltaTest {
    @Test
    fun diff() {
        assertEquals(
            """
                "." dir Equal
                    "a" file Deleted
                    "b" file Deleted
                    "d" file Created
                    "e" file Created
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    FileNode("b", byteArrayOf(2)),
                    FileNode("c", byteArrayOf(3)),
                )),
                DirectoryNode("", listOf(
                    FileNode("c", byteArrayOf(3)),
                    FileNode("d", byteArrayOf(4)),
                    FileNode("e", byteArrayOf(5)),
                )),
            )
        }
    }

    @Test
    fun addNodeFileToDir() {
        assertEquals(
            """
                "." dir Equal
                    "a" dir FileToDir
                        "d" file Created
                        "c" dir Created
                            "d2" file Created
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1))
                )),
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("d", byteArrayOf(2)),
                        DirectoryNode("c", listOf(
                            FileNode("d2", byteArrayOf(3))
                        ))
                    ))
                )),
            )
        }
    }

    @Test
    fun addNodeDirToFile() {
        assertEquals(
            """
                "." dir Equal
                    "a" file DirToFile
                        "d" file Deleted
                        "e" dir Deleted
                            "f" file Deleted
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("d", byteArrayOf(1)),
                        DirectoryNode("e", listOf(
                            FileNode("f", byteArrayOf(2))
                        ))
                    ))
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1))
                )),
            )
        }
    }

    @Test
    fun addDeltaCreated() {
        assertEquals(
            """
                "." dir Equal
                    "a" file Created
                    "d" dir Created
                        "d" file Created
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf()),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("d", listOf(
                        FileNode("d", byteArrayOf(2))
                    ))
                )),
            )
        }
    }

    @Test
    fun addDeltaDeleted() {
        assertEquals(
            """
                "." dir Equal
                    "a" file Deleted
                    "d" dir Deleted
                        "d" file Deleted
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("d", listOf(
                        FileNode("d", byteArrayOf(2))
                    ))
                )),
                DirectoryNode("", listOf()),
            )
        }
    }

    @Test
    fun pruneEqualDirectories1() {
        assertEquals(
            """
                "." dir Equal
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("d", listOf(
                        FileNode("d", byteArrayOf(2))
                    ))
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("d", listOf(
                        FileNode("d", byteArrayOf(2))
                    ))
                )),
            )
        }
    }

    @Test
    fun pruneEqualDirectories2() {
        assertEquals(
            """
                "." dir Equal
                    "c" dir Deleted
                    "x" dir Equal
                        "z" file Deleted
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("c", listOf()),
                    DirectoryNode("g", listOf(
                        FileNode("d", byteArrayOf(2)),
                        DirectoryNode("w", listOf(
                            FileNode("x", byteArrayOf(3))
                        ))
                    )),
                    DirectoryNode("x", listOf(
                        FileNode("z", byteArrayOf(4)),
                    )),
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("g", listOf(
                        FileNode("d", byteArrayOf(2)),
                        DirectoryNode("w", listOf(
                            FileNode("x", byteArrayOf(3))
                        ))
                    )),
                    DirectoryNode("x", listOf()),
                )),
            )
        }
    }

    @Test
    fun differ() {
        assertEquals(
            """
                "." dir Equal
                    "a" file Differ
                    "b" file Differ
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    FileNode("b", byteArrayOf(10)),
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(2)),
                    FileNode("b", byteArrayOf(20)),
                )),
            )
        }
    }

    @Test
    fun renamed() {
        assertEquals(
            """
                "." dir Equal
                    "a" file Deleted
                    "b" file Deleted
                    "c" file Created
                    "d" file Created
            """
            /*
                "." dir Equal
                    "c" file MovedFrom "./a"
                    "d" file MovedFrom "./b"
             */
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    FileNode("b", byteArrayOf(2)),
                )),
                DirectoryNode("", listOf(
                    FileNode("c", byteArrayOf(1)),
                    FileNode("d", byteArrayOf(2)),
                )),
            )
        }
    }

    @Test
    fun moved() {
        assertEquals(
            """
                "." dir Equal
                    "a" file Deleted
                    "c" dir Deleted
                        "f" file Deleted
                    "d" dir Created
                        "q" file Created
                    "x" file Created
            """
            /*
                "." dir Equal
                    "c" dir Deleted
                    "d" dir Created
                        "q" file MovedFrom "./c/f"
                    "x" file MovedFrom "./a"
             */
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("c", listOf(
                        FileNode("f", byteArrayOf(2)),
                    ))
                )),
                DirectoryNode("", listOf(
                    DirectoryNode("d", listOf(
                        FileNode("q", byteArrayOf(2)),
                    )),
                    FileNode("x", byteArrayOf(1)),
                )),
            )
        }
    }
}
