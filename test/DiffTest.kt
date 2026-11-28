package ch.softappeal.diff

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class DiffTest {
    fun assertDiff(old: DirNode, new: DirNode, expected: String) {
        assertEquals(
            "${expected.trimIndent()}\n",
            toString { printNode(createDiff(old, new)) },
        )
    }

    @Test
    fun movedSubTrees() = assertDiff(
        root {
            file("deletedFile", 3)
            dir("dir") {
                dir("toBeMoved") {
                    file("a", 1)
                    dir("dir") {
                        file("b", 2)
                    }
                }
            }
            dir("toBeMoved") {
                file("a", 1)
                dir("dir") {
                    file("b", 2)
                }
            }
        },
        root {
            dir("renamed") {
                file("a", 1)
                dir("dir") {
                    file("b", 2)
                }
            }
            dir("newDir")
        },
        """
            - ``
                - `renamed` NewDir < `/dir/toBeMoved` `/toBeMoved`
        """,
    )

    @Test
    fun movedFiles() = assertDiff(
        root {
            file("file1", 3)
            file("file2", 3)
            dir("dir") {
                file("file3", 4)
            }
        },
        root {
            file("file5", 3)
            file("file6", 4)
        },
        """
            - ``
                - `file5` NewFile < `/file1` `/file2`
                - `file6` NewFile < `/dir/file3`
        """,
    )

    @Test
    fun unchangedNodesAreNotReported() = assertDiff(
        root {
            file("a", 1)
            dir("d") {
                file("b", 2)
            }
        },
        root {
            file("a", 1)
            dir("d") {
                file("b", 2)
            }
        },
        """
            - ``
        """,
    )

    @Test
    fun copyIsReportedAsMove() = assertDiff(
        root {
            file("x", 5)
            dir("d") {
                file("b", 2)
            }
        },
        root {
            file("x", 5)
            file("y", 5)
            dir("d") {
                file("b", 2)
            }
            dir("d2") {
                file("b", 2)
            }
        },
        """
            - ``
                - `d2` NewDir < `/d`
                - `y` NewFile < `/x`
        """,
    )

    @Test
    fun movedNodesInsideDirectoryThatStays() = assertDiff(
        root {
            dir("keep") {
                file("f", 7)
                dir("sub") {
                    file("g", 8)
                }
            }
        },
        root {
            dir("keep") {
                file("f2", 7)
                dir("sub2") {
                    file("g", 8)
                }
            }
        },
        """
            - ``
                - `keep` NewDir
                    - `f2` NewFile < `/keep/f`
                    - `sub2` NewDir < `/keep/sub`
        """,
    )

    @Test
    fun emptyDirectoriesAreIgnored() = assertDiff(
        root {
            file("a", 1)
            dir("d")
        },
        root {
            file("a", 1)
            dir("e")
        },
        """
            - ``
        """,
    )

    @Test
    fun digestsDifferingInLaterByteAreDifferent() = assertDiff(
        root {
            file("f", 2)
        },
        root {
            file("g", 3)
            file("h", 2)
        },
        """
            - ``
                - `h` NewFile < `/f`
        """,
    )

    @Test
    fun fileBecomesDirectory() = assertDiff(
        root {
            file("x", 1)
        },
        root {
            dir("x") {
                file("a", 1)
            }
        },
        """
            - ``
                - `x` NewDir
                    - `a` NewFile < `/x`
        """,
    )

    @Test
    fun directoryBecomesFile() = assertDiff(
        root {
            dir("x") {
                file("a", 1)
            }
        },
        root {
            file("x", 1)
        },
        """
            - ``
                - `x` NewFile < `/x/a`
        """,
    )

    @Test
    fun directoryReplacesFileAndMatchesMovedDirectory() = assertDiff(
        root {
            file("x", 1)
            dir("old") {
                file("a", 1)
            }
        },
        root {
            dir("x") {
                file("a", 1)
            }
        },
        """
            - ``
                - `x` NewDir < `/old`
        """,
    )

    @Test
    fun fileModifiedInPlaceIsNotReported() = assertDiff(
        root {
            file("a", 1)
        },
        root {
            file("a", 3)
        },
        """
            - ``
        """,
    )

    @Test
    fun directoryModifiedInPlaceIsNotReported() = assertDiff(
        root {
            dir("d") {
                file("a", 1)
            }
        },
        root {
            dir("d") {
                file("a", 1)
                file("b", 2)
            }
        },
        """
            - ``
        """,
    )

    @Test
    fun identicalDirectoriesThatStayAreNotReported() = assertDiff(
        root {
            dir("d") {
                file("b", 2)
            }
            dir("e") {
                file("b", 2)
            }
        },
        root {
            dir("d") {
                file("b", 2)
            }
            dir("e") {
                file("b", 2)
            }
        },
        """
            - ``
        """,
    )

    @Test
    fun swappedFilesAreReported() = assertDiff(
        root {
            file("a", 1)
            file("b", 2)
        },
        root {
            file("a", 2)
            file("b", 1)
        },
        """
            - ``
                - `a` NewFile < `/b`
                - `b` NewFile < `/a`
        """,
    )

    @Test
    fun fileMovedOutOfDirectory() = assertDiff(
        root {
            dir("d") {
                file("a", 1)
                file("b", 2)
            }
        },
        root {
            file("a", 1)
            dir("d") {
                file("b", 2)
            }
        },
        """
            - ``
                - `a` NewFile < `/d/a`
        """,
    )

    @Test
    fun directoryMovedToDifferentParent() = assertDiff(
        root {
            dir("p") {
                dir("q") {
                    dir("dir") {
                        file("f", 1)
                    }
                }
            }
        },
        root {
            dir("r") {
                dir("s") {
                    dir("moved") {
                        file("f", 1)
                    }
                }
            }
        },
        """
            - ``
                - `r` NewDir
                    - `s` NewDir
                        - `moved` NewDir < `/p/q/dir`
        """,
    )

    @Test
    fun emptyDirectoryInsideMovedDirectoryIsPartOfItsContent() = assertDiff(
        root {
            dir("d") {
                file("a", 1)
                dir("e")
            }
        },
        root {
            dir("d2") {
                file("a", 1)
                dir("e")
            }
        },
        """
            - ``
                - `d2` NewDir < `/d`
        """,
    )

    @Test
    fun missingEmptyDirectoryPreventsDirectoryMatch() = assertDiff(
        root {
            dir("d") {
                file("a", 1)
                dir("e")
            }
        },
        root {
            dir("d2") {
                file("a", 1)
            }
        },
        """
            - ``
                - `d2` NewDir
                    - `a` NewFile < `/d/a`
        """,
    )

    @Test
    fun emptyOldTree() = assertDiff(
        root {},
        root {
            file("a", 1)
            dir("d") {
                file("b", 2)
            }
        },
        """
            - ``
        """,
    )

    @Test
    fun emptyNewTree() = assertDiff(
        root {
            file("a", 1)
            dir("d") {
                file("b", 2)
            }
        },
        root {},
        """
            - ``
        """,
    )

    @Test
    fun sourcePathsAreInPreorderAcrossFilesAndSubdirectories() = assertDiff(
        root {
            file("a", 1)
            dir("b") {
                file("x", 1)
            }
            file("c", 1)
        },
        root {
            file("n", 1)
        },
        """
            - ``
                - `n` NewFile < `/a` `/b/x` `/c`
        """,
    )

    @Test
    fun fileAndDirectoryWithSameNameAreNotConfused() = assertDiff(
        root {
            dir("p") {
                file("x", 1)
            }
        },
        root {
            dir("r") {
                dir("x") {
                    file("y", 2)
                }
            }
        },
        """
            - ``
        """,
    )
}

class DeltaTest {
    fun assertDiff(old: DirNode, new: DirNode, expected: String) {
        assertEquals(
            "${expected.trimIndent()}\n",
            toString { printNode(createDiffOld(old, new)) },
        )
    }

    val root = root {
        dir("dir") {
            file("dir-file", 1)
        }
        file("file", 0)
    }

    @Ignore
    @Test
    fun diff_NoChanges() = assertDiff(
        root,
        root,
        """
            - ``
        """,
    )

    @Test
    fun diff_Deleted() = assertDiff(
        root,
        root {
        },
        """
            - ``
                - `dir` DeletedDir
                    - `dir-file` DeletedFile
                - `file` DeletedFile
        """,
    )

    @Test
    fun diff_New() = assertDiff(
        root {
        },
        root,
        """
            - ``
                - `dir` NewDir
                    - `dir-file` NewFile
                - `file` NewFile
        """,
    )

    @Test
    fun diff_FileToDir() = assertDiff(
        root {
            file("dir", 10)
        },
        root,
        """
            - ``
                - `dir` FileToDir
                    - `dir-file` NewFile
                - `file` NewFile
        """,
    )

    @Test
    fun diff_DirToFile() = assertDiff(
        root,
        root {
            file("dir", 10)
        },
        """
            - ``
                - `dir` DirToFile
                    - `dir-file` DeletedFile
                - `file` DeletedFile
        """,
    )

    @Test
    fun diff_ChangedFile() = assertDiff(
        root {
            file("file", 1)
        },
        root {
            file("file", 2)
        },
        """
            - ``
                - `file` ChangedFile
        """,
    )

    @Ignore
    @Test
    fun moveFiles_ChangedFile() = assertDiff(
        root {
            file("changed", 1)
            dir("dir") {
                file("deleted", 2)
            }
        },
        root {
            file("changed", 2)
            dir("dir")
        },
        """
            - ``
                - `changed` ChangedFile < `/dir/deleted`
        """,
    )

    @Ignore
    @Test
    fun moveFiles_NewFile() = assertDiff(
        root {
            file("a1", 1)
            file("a2", 1)
        },
        root {
            file("x1", 1)
            file("x2", 1)
            file("new", 1)
        },
        """
            - ``
                - `new` NewFile < `/a1` `/a2`
                - `x1` NewFile < `/a1` `/a2`
                - `x2` NewFile < `/a1` `/a2`
        """,
    )

    @Ignore
    @Test
    fun moveFiles_FileToDir() = assertDiff(
        root {
            file("dir", 1)
        },
        root,
        """
            - ``
                - `dir` FileToDir
                    - `dir-file` NewFile < `/dir`
                - `file` NewFile
        """,
    )

    @Ignore
    @Test
    fun moveFiles_DirToFile() = assertDiff(
        root,
        root {
            file("dir", 1)
        },
        """
            - ``
                - `dir` DirToFile < `/dir/dir-file`
                - `file` DeletedFile
        """,
    )

    @Ignore
    @Test
    fun tryPruneKeptDir() = assertDiff(
        root {
            dir("dir1") {
                file("dir1-file", 1)
            }
            dir("dir2") {
                file("dir2-file", 2)
            }
        },
        root {
            dir("dir1") {
                file("dir1-file", 1)
            }
            dir("dir2")
            dir("newDir")
        },
        """
            - ``
                - `dir2`
                    - `dir2-file` DeletedFile
                - `newDir` NewDir
        """,
    )

    @Ignore
    @Test
    fun moveDirectories() = assertDiff(
        root {
            dir("old") {
                file("file1", 1)
                dir("dir1") {
                    file("file2", 2)
                }
                dir("dir2")
            }
        },
        root {
            dir("new") {
                file("file1", 1)
                dir("dir1") {
                    file("file2", 2)
                }
                dir("dir2")
            }
        },
        """
            - ``
                - `new` NewDir
                    - `dir1` NewDir
                        - `file2` NewFile < `/old/dir1/file2`
                    - `dir2` NewDir
                    - `file1` NewFile < `/old/file1`
                - `old` DeletedDir
                    - `dir1` DeletedDir
                    - `dir2` DeletedDir
        """,
    )
}
