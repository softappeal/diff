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
                "/"
                    "a" Deleted
                    "b" Deleted
                    "d" New
                    "e" New
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

    /*
        @Test
        fun addNodeFileToDir1() {
            assertEquals(
                """
                    "/"
                        "a/" FileToDir
                            "d" New
                            "c/" New
                                "d2" New
                                "y" MovedFrom "/x"
                                "q" MovedFrom "/a"
                """
            ) {
                dump(
                    it,
                    DirectoryNode("", listOf(
                        FileNode("a", byteArrayOf(1)),
                        FileNode("x", byteArrayOf(99)),
                    )),
                    DirectoryNode("", listOf(
                        DirectoryNode("a", listOf(
                            FileNode("d", byteArrayOf(2)),
                            DirectoryNode("c", listOf(
                                FileNode("d2", byteArrayOf(3)),
                                FileNode("y", byteArrayOf(99)),
                                FileNode("q", byteArrayOf(1)),
                            )),
                        )),
                    )),
                )
            }
        }
    */
    @Test
    fun addNodeFileToDir2() {
        assertEquals(
            """
                "/"
                    "x/"
                        "a/" FileToDir
                            "b" MovedFrom "/x/a"
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    DirectoryNode("x", listOf(
                        FileNode("a", byteArrayOf(1)),
                    )),
                )),
                DirectoryNode("", listOf(
                    DirectoryNode("x", listOf(
                        DirectoryNode("a", listOf(
                            FileNode("b", byteArrayOf(1)),
                        )),
                    )),
                )),
            )
        }
    }

    @Test
    fun addNodeFileToDir3() {
        assertEquals(
            """
                "/"
                    "a/" FileToDir
                        "b" New
                        "d" New
                    "c" Deleted
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    FileNode("c", byteArrayOf(1)),
                )),
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("b", byteArrayOf(1)),
                        FileNode("d", byteArrayOf(1)),
                    )),
                )),
            )
        }
    }

    @Test
    fun addNodeFileToDir4() {
        assertEquals(
            """
                "/"
                    "a/" FileToDir
                        "c" MovedFrom "/b/c"
                    "b/" Deleted
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("b", listOf(
                        FileNode("c", byteArrayOf(2)),
                    )),
                )),
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("c", byteArrayOf(2)),
                    )),
                )),
            )
        }
    }

    @Test
    fun addNodeFileToDir5() {
        assertEquals(
            """
                "/"
                    "a/" FileToDir
                        "c" MovedFrom "/x/b/c"
                    "x/" Deleted
                        "b/" Deleted
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("x", listOf(
                        DirectoryNode("b", listOf(
                            FileNode("c", byteArrayOf(2)),
                        )),
                    )),
                )),
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("c", byteArrayOf(2)),
                    )),
                )),
            )
        }
    }

    @Test
    fun addNodeDirToFile1() {
        assertEquals(
            """
                "/"
                    "a" DirToFile
                        "d" Deleted
                        "e/" Deleted
                            "f" Deleted
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("d", byteArrayOf(1)),
                        DirectoryNode("e", listOf(
                            FileNode("f", byteArrayOf(2)),
                        )),
                    )),
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(3)),
                )),
            )
        }
    }

    @Test
    fun addNodeDirToFile2() {
        assertEquals(
            """
                "/"
                    "a" DirToFile MovedFrom "/a/b"
                        "c" Deleted
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("b", byteArrayOf(1)),
                        FileNode("c", byteArrayOf(2)),
                    )),
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                )),
            )
        }
    }

    @Test
    fun addNodeDirToFile3() {
        assertEquals(
            """
                "/"
                    "a" DirToFile
                        "b" Deleted
                        "c" Deleted
                        "x" Deleted
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("b", byteArrayOf(1)),
                        FileNode("c", byteArrayOf(2)),
                        FileNode("x", byteArrayOf(1)),
                    )),
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                )),
            )
        }
    }

    @Test
    fun addNodeDirToFile4() {
        assertEquals(
            """
                "/"
                    "a" DirToFile
                        "b" Deleted
                        "c" Deleted
                    "x" New
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("b", byteArrayOf(1)),
                        FileNode("c", byteArrayOf(2)),
                    )),
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    FileNode("x", byteArrayOf(1)),
                )),
            )
        }
    }

    @Test
    fun addNodeDirToFile5() {
        assertEquals(
            """
                "/"
                    "a" DirToFile
                        "b" Deleted
                        "c" Deleted
                        "y" Deleted
                    "x" New
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("b", byteArrayOf(1)),
                        FileNode("c", byteArrayOf(2)),
                        FileNode("y", byteArrayOf(1)),
                    )),
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    FileNode("x", byteArrayOf(1)),
                )),
            )
        }
    }

    @Test
    fun addNodeDirToFile6() {
        assertEquals(
            """
                "/"
                    "a" DirToFile RenamedFrom "b"
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf()),
                    FileNode("b", byteArrayOf(1)),
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                )),
            )
        }
    }

    @Test
    fun addNodeDirToFile7() {
        assertEquals(
            """
                "/"
                    "a" DirToFile RenamedFrom "c"
                    "d/" New
                        "b" MovedFrom "/a/b"
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("b", byteArrayOf(1)),
                    )),
                    FileNode("c", byteArrayOf(2)),
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(2)),
                    DirectoryNode("d", listOf(
                        FileNode("b", byteArrayOf(1)),
                    )),
                )),
            )
        }
    }

    @Test
    fun addNodeDirToFile8() {
        assertEquals(
            """
                "/"
                    "a" DirToFile
                    "c" Deleted
                    "d/" New
                        "b" MovedFrom "/a/b"
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("b", byteArrayOf(1)),
                    )),
                    FileNode("c", byteArrayOf(2)),
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(3)),
                    DirectoryNode("d", listOf(
                        FileNode("b", byteArrayOf(1)),
                    )),
                )),
            )
        }
    }

    @Test
    fun addNodeDirToFile9() {
        assertEquals(
            """
                "/"
                    "a" DirToFile RenamedFrom "c"
                        "b" Deleted
                    "d/" New
                        "b" New
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("b", byteArrayOf(1)),
                    )),
                    FileNode("c", byteArrayOf(2)),
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(2)),
                    DirectoryNode("d", listOf(
                        FileNode("b", byteArrayOf(3)),
                    )),
                )),
            )
        }
    }

    @Test
    fun addNodeDirToFile10() {
        assertEquals(
            """
                "/"
                    "a" DirToFile
                        "b" Deleted
                    "c" Deleted
                    "d/" New
                        "b" New
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("b", byteArrayOf(1)),
                    )),
                    FileNode("c", byteArrayOf(2)),
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(4)),
                    DirectoryNode("d", listOf(
                        FileNode("b", byteArrayOf(3)),
                    )),
                )),
            )
        }
    }

    @Test
    fun addDeltaCreated() {
        assertEquals(
            """
                "/"
                    "a" New
                    "d/" New
                        "d" New
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf()),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("d", listOf(
                        FileNode("d", byteArrayOf(2)),
                    )),
                )),
            )
        }
    }

    @Test
    fun addDeltaDeleted() {
        assertEquals(
            """
                "/"
                    "a" Deleted
                    "d/" Deleted
                        "d" Deleted
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("d", listOf(
                        FileNode("d", byteArrayOf(2)),
                    )),
                )),
                DirectoryNode("", listOf()),
            )
        }
    }

    @Test
    fun pruneEqualDirectories1() {
        assertEquals(
            """
                "/"
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("d", listOf(
                        FileNode("d", byteArrayOf(2)),
                    )),
                )),
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("d", listOf(
                        FileNode("d", byteArrayOf(2)),
                    )),
                )),
            )
        }
    }

    @Test
    fun pruneEqualDirectories2() {
        assertEquals(
            """
                "/"
                    "c/" Deleted
                    "x/"
                        "z" Deleted
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
                            FileNode("x", byteArrayOf(3)),
                        )),
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
                            FileNode("x", byteArrayOf(3)),
                        )),
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
                "/"
                    "a" Changed
                    "b" Changed
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
    fun renamed1() {
        assertEquals(
            """
                "/"
                    "c" RenamedFrom "a"
                    "d" RenamedFrom "b"
            """
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
    fun renamed2() {
        assertEquals(
            """
                "/"
                    "a" Deleted
                    "c" New
                    "d" New
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                )),
                DirectoryNode("", listOf(
                    FileNode("c", byteArrayOf(1)),
                    FileNode("d", byteArrayOf(1)),
                )),
            )
        }
    }

    @Test
    fun renamed3() {
        assertEquals(
            """
                "/"
                    "a" Deleted
                    "b" Deleted
                    "c" New
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    FileNode("b", byteArrayOf(1)),
                )),
                DirectoryNode("", listOf(
                    FileNode("c", byteArrayOf(1)),
                )),
            )
        }
    }

    @Test
    fun moved1() {
        assertEquals(
            """
                "/"
                    "c/" Deleted
                    "d/" New
                        "q" MovedFrom "/c/f"
                    "x" RenamedFrom "a"
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    FileNode("a", byteArrayOf(1)),
                    DirectoryNode("c", listOf(
                        FileNode("f", byteArrayOf(2)),
                    )),
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

    @Test
    fun moved2() {
        assertEquals(
            """
                "/"
                    "a/"
                        "s" MovedFrom "/r"
                        "y" RenamedFrom "x"
                    "c" MovedFrom "/a/b"
                    "i/"
                        "ii/"
                            "iii/"
                                "iiii/"
                                    "j" MovedFrom "/i/j"
                                    "n" RenamedFrom "m"
                        "jjjj" MovedFrom "/i/ii/iii/iiii/jjjj"
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("b", byteArrayOf(1)),
                        FileNode("x", byteArrayOf(2)),
                    )),
                    DirectoryNode("i", listOf(
                        DirectoryNode("ii", listOf(
                            DirectoryNode("iii", listOf(
                                DirectoryNode("iiii", listOf(
                                    FileNode("jjjj", byteArrayOf(10)),
                                    FileNode("m", byteArrayOf(99)),
                                )),
                                FileNode("jjj", byteArrayOf(11)),
                            )),
                            FileNode("jj", byteArrayOf(12)),
                        )),
                        FileNode("j", byteArrayOf(13)),
                    )),
                    FileNode("r", byteArrayOf(3)),
                )),
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        FileNode("s", byteArrayOf(3)),
                        FileNode("y", byteArrayOf(2)),
                    )),
                    FileNode("c", byteArrayOf(1)),
                    DirectoryNode("i", listOf(
                        DirectoryNode("ii", listOf(
                            DirectoryNode("iii", listOf(
                                DirectoryNode("iiii", listOf(
                                    FileNode("j", byteArrayOf(13)),
                                    FileNode("n", byteArrayOf(99)),
                                )),
                                FileNode("jjj", byteArrayOf(11)),
                            )),
                            FileNode("jj", byteArrayOf(12)),
                        )),
                        FileNode("jjjj", byteArrayOf(10)),
                    )),
                )),
            )
        }
    }

    @Test
    fun moved5() {
        assertEquals(
            """
                "/"
                    "a0/" New
                        "b2" MovedFrom "/a1/b2"
                    "a1/" Deleted
                    "a4" Changed
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    DirectoryNode("a1", listOf(
                        FileNode("b2", byteArrayOf(109)),
                    )),
                    FileNode("a4", byteArrayOf(114)),
                )),
                DirectoryNode("", listOf(
                    DirectoryNode("a0", listOf(
                        FileNode("b2", byteArrayOf(109)),
                    )),
                    FileNode("a4", byteArrayOf(14)),
                )),
            )
        }
    }

    @Test
    fun emptyDir() {
        assertEquals(
            """
                "/"
                    "a/" Deleted
                        "b/" Deleted
                    "x/" New
                        "b/" New
                        "c" MovedFrom "/a/c"
            """
        ) {
            dump(
                it,
                DirectoryNode("", listOf(
                    DirectoryNode("a", listOf(
                        DirectoryNode("b", listOf()),
                        FileNode("c", byteArrayOf(114)),
                    )),
                )),
                DirectoryNode("", listOf(
                    DirectoryNode("x", listOf(
                        DirectoryNode("b", listOf()),
                        FileNode("c", byteArrayOf(114)),
                    )),
                )),
            )
        }
    }
}
