@file:Suppress("SpellCheckingInspection")

package ch.softappeal.diff

import kotlin.test.Test
import kotlin.test.assertEquals

private fun assertDiff(old: DirectoryNode, new: DirectoryNode, expected: String) {
    assertEquals(
        (expected.split("\n").joinToString("\n") { it.replaceFirst(Regex("\\S"), "- $0") } + "\n").trimIndent(),
        toString { printDeltas(DirectoryDelta(NodeDesc(old), NodeDesc(new))) },
    )
}

class DeltaTest {
    @Test
    fun doNotConsolidateMovedDirectoryAndDropUnmatchedEmptySourceDirectory() {
        assertDiff(
            root {
                dir("source") {
                    file("f", 1)
                    dir("empty")
                }
            },
            root {
                dir("destination") {
                    file("f", 1)
                }
            },
            """
                ``
                    `destination` New
                        `f` MovedFrom `/source/f`
                    `source` Deleted
                        `empty` Deleted
            """
        )
    }

    @Test
    fun doNotConsolidateMovedDirectoriesWithDifferentSourcesOrContents2() {
        assertDiff(
            root {
                dir("source") {
                    file("f", 1)
                }
            },
            root {
                dir("destination") {
                    file("f", 1)
                }
                dir("source")
            },
            """
                ``
                    `destination` New
                        `f` MovedFrom `/source/f`
            """
        )
    }

    @Test
    fun everything() {
        assertDiff(
            root {
                dir("FileToDir") {
                    dir("simple") {
                        file("a", 0)
                    }
                    dir("renamed") {
                        file("a", 10)
                    }
                    dir("moved") {
                        file("a", 20)
                    }
                }
                dir("DirToFile") {
                    dir("simple") {
                        dir("a") {
                            file("b", 30)
                        }
                    }
                    dir("renamed") {
                        dir("a") {
                            file("b", 40)
                        }
                        file("c", 41)
                    }
                    dir("moved") {
                        dir("a") {
                            file("b", 50)
                        }
                    }
                }
                dir("Changed") {
                    file("e", 60)
                    file("a", 60)
                    file("s", 60)
                    file("b", 60)
                }
                dir("Deleted") {
                    dir("a") {
                        dir("b") {
                            file("c", 70)
                        }
                    }
                }
                dir("New") {
                }
                dir("RenamedFrom") {
                    dir("a") {
                        file("b", 90)
                    }
                }
                dir("MovedFrom") {
                    dir("a") {
                        file("b", 100)
                    }
                }
            },
            root {
                dir("FileToDir") {
                    dir("simple") {
                        dir("a") {
                            file("b", 1)
                        }
                    }
                    dir("renamed") {
                        dir("a") {
                            file("b", 11)
                        }
                        file("c", 10)
                    }
                    dir("moved") {
                        dir("a") {
                            file("b", 20)
                        }
                    }
                }
                dir("DirToFile") {
                    dir("simple") {
                        file("a", 31)
                    }
                    dir("renamed") {
                        file("a", 41)
                    }
                    dir("moved") {
                        file("a", 50)
                    }
                }
                dir("Changed") {
                    file("e", 60)
                    file("a", 61)
                    file("s", 60)
                    file("b", 60)
                }
                dir("Deleted") {
                }
                dir("New") {
                    dir("a") {
                        dir("b") {
                            file("c", 80)
                        }
                    }
                }
                dir("RenamedFrom") {
                    dir("c") {
                        file("b", 90)
                    }
                }
                dir("MovedFrom") {
                    dir("c") {
                        dir("a") {
                            file("b", 100)
                        }
                    }
                }
            },
            """
                ``
                    `Changed`
                        `a` Changed
                    `Deleted`
                        `a` Deleted
                            `b` Deleted
                                `c` Deleted
                    `DirToFile`
                        `moved`
                            `a` DirToFile MovedFrom `/DirToFile/moved/a/b`
                        `renamed`
                            `a` DirToFile RenamedFrom `c`
                                `b` Deleted
                        `simple`
                            `a` DirToFile
                                `b` Deleted
                    `FileToDir`
                        `moved`
                            `a` FileToDir
                                `b` MovedFrom `/FileToDir/moved/a`
                        `renamed`
                            `a` FileToDir
                                `b` New
                            `c` RenamedFrom `a`
                        `simple`
                            `a` FileToDir
                                `b` New
                    `MovedFrom`
                        `c` New
                            `a` MovedFrom `/MovedFrom/a`
                    `New`
                        `a` New
                            `b` New
                                `c` New
                    `RenamedFrom`
                        `c` RenamedFrom `a`
            """
        )
    }

    @Test
    fun compareDirs() {
        assertDiff(
            root {
                file("a", 0)
                dir("d1") {
                    file("a", 1)
                    file("q", 2)
                    dir("d3") {
                        file("a", 3)
                        file("b", 4)
                    }
                    dir("d2") {
                        dir("d3") {
                            file("a", 5)
                            file("b", 6)
                        }
                        dir("e")
                        file("a", 7)
                    }
                }
            },
            root {
                file("a", 100)
                dir("d1") {
                    file("a", 1)
                    file("q", 2)
                    dir("d3") {
                        file("a", 3)
                        dir("d3") {
                            file("a", 101)
                            file("b", 102)
                        }
                    }
                    dir("d2") {
                        dir("d3") {
                            file("a", 5)
                            file("b", 6)
                            file("c", 103)
                        }
                        dir("e")
                        file("a", 7)
                    }
                }
            },
            """
                ``
                    `a` Changed
                    `d1`
                        `d2`
                            `d3`
                                `c` New
                        `d3`
                            `b` Deleted
                            `d3` New
                                `a` New
                                `b` New
            """
        )
    }

    @Test
    fun compareFlatEmpty() {
        assertDiff(
            root {
            },
            root {
            },
            """
                ``
            """
        )
    }

    @Test
    fun compareFlatDeleted() {
        assertDiff(
            root {
                file("a", 0)
            },
            root {
            },
            """
                ``
                    `a` Deleted
            """
        )
    }

    @Test
    fun compareFlatNew() {
        assertDiff(
            root {
            },
            root {
                file("a", 0)
            },
            """
                ``
                    `a` New
            """
        )
    }

    @Test
    fun compareFlatSame() {
        assertDiff(
            root {
                file("a", 0)
            },
            root {
                file("a", 0)
            },
            """
                ``
            """
        )
    }

    @Test
    fun compareFlatChanged() {
        assertDiff(
            root {
                file("a", 0)
            },
            root {
                file("a", 1)
            },
            """
                ``
                    `a` Changed
            """
        )
    }

    @Test
    fun compareFlatMany() {
        assertDiff(
            root {
                file("a", 1)
                file("d", 2)
                file("e", 3)
                file("g", 4)
            },
            root {
                file("b", 5)
                file("c", 6)
                file("f", 7)
            },
            """
                ``
                    `a` Deleted
                    `b` New
                    `c` New
                    `d` Deleted
                    `e` Deleted
                    `f` New
                    `g` Deleted
            """
        )
    }

    @Test
    fun addNodeDeleted() {
        assertDiff(
            root {
                file("a", 1)
                dir("b") {
                    file("c", 2)
                    dir("d") {
                        file("e", 3)
                    }
                }
            },
            root {
            },
            """
                ``
                    `a` Deleted
                    `b` Deleted
                        `c` Deleted
                        `d` Deleted
                            `e` Deleted
            """
        )
    }

    @Test
    fun addNodeNew() {
        assertDiff(
            root {
            },
            root {
                file("a", 1)
                dir("b") {
                    file("c", 2)
                    dir("d") {
                        file("e", 3)
                    }
                }
            },
            """
                ``
                    `a` New
                    `b` New
                        `c` New
                        `d` New
                            `e` New
            """
        )
    }

    @Test
    fun addNodeTypeChangedFileToDir() {
        assertDiff(
            root {
                file("a", 1)
            },
            root {
                dir("a") {
                    file("b", 2)
                    dir("c") {
                        file("d", 3)
                    }
                }
            },
            """
                ``
                    `a` FileToDir
                        `b` New
                        `c` New
                            `d` New
            """
        )
    }

    @Test
    fun addNodeTypeChangedFileToDirRenamed() {
        assertDiff(
            root {
                file("a", 1)
            },
            root {
                dir("a") {
                    file("b", 2)
                }
                file("b", 1)
            },
            """
                ``
                    `a` FileToDir
                        `b` New
                    `b` RenamedFrom `a`
            """
        )
    }

    @Test
    fun addNodeTypeChangedFileToDirMoved() {
        assertDiff(
            root {
                file("a", 1)
            },
            root {
                dir("a") {
                    file("b", 1)
                }
            },
            """
                ``
                    `a` FileToDir
                        `b` MovedFrom `/a`
            """
        )
    }

    @Test
    fun addNodeTypeChangedDirToFile() {
        assertDiff(
            root {
                dir("a") {
                    file("b", 1)
                    dir("c") {
                        file("d", 2)
                    }
                }
            },
            root {
                file("a", 3)
            },
            """
                ``
                    `a` DirToFile
                        `b` Deleted
                        `c` Deleted
                            `d` Deleted
            """
        )
    }

    @Test
    fun addNodeTypeChangedDirToFileRenamed() {
        assertDiff(
            root {
                dir("a") {
                    file("b", 1)
                    dir("c") {
                        file("d", 2)
                    }
                }
                file("x", 9)
            },
            root {
                file("a", 9)
            },
            """
                ``
                    `a` DirToFile RenamedFrom `x`
                        `b` Deleted
                        `c` Deleted
                            `d` Deleted
            """
        )
    }

    @Test
    fun addNodeTypeChangedDirToFileMoved() {
        assertDiff(
            root {
                dir("a") {
                    file("b", 1)
                    file("x", 9)
                }
            },
            root {
                file("a", 1)
            },
            """
                ``
                    `a` DirToFile MovedFrom `/a/b`
                        `x` Deleted
            """
        )
    }

    @Test
    fun pruneEqualDirectory() {
        assertDiff(
            root {
                file("a", 1)
                dir("d") {
                    file("e", 2)
                }
                dir("d2") {
                    file("e", 3)
                }
            },
            root {
                file("a", 1)
                dir("d") {
                    file("e", 2)
                }
                dir("d2")
                dir("d3")
            },
            """
                ``
                    `d2`
                        `e` Deleted
                    `d3` New
            """
        )
    }

    @Test
    fun renamed() {
        assertDiff(
            root {
                file("a", 1)
            },
            root {
                file("e", 1)
            },
            """
                ``
                    `e` RenamedFrom `a`
            """
        )
    }

    @Test
    fun movedDown() {
        assertDiff(
            root {
                file("e", 1)
            },
            root {
                dir("d") {
                    file("e", 1)
                }
            },
            """
                ``
                    `d` New
                        `e` MovedFrom `/e`
            """
        )
    }

    @Test
    fun movedUp1() {
        assertDiff(
            root {
                dir("d") {
                    file("e", 1)
                }
            },
            root {
                file("e", 1)
            },
            """
                ``
                    `d` Deleted
                    `e` MovedFrom `/d/e`
            """
        )
    }

    @Test
    fun movedUp2() {
        assertDiff(
            root {
                dir("z") {
                    dir("d") {
                        file("e", 1)
                    }
                }
            },
            root {
                dir("d") {
                    file("e", 1)
                }
            },
            """
                ``
                    `d` MovedFrom `/z/d`
                    `z` Deleted
            """
        )
    }

    @Test
    fun renamedOldDuplicated() {
        assertDiff(
            root {
                file("a", 1)
                file("b", 1)
            },
            root {
                file("e", 1)
            },
            """
                ``
                    `a` Deleted
                    `b` Deleted
                    `e` New
            """
        )
    }

    @Test
    fun renamedNewDuplicated() {
        assertDiff(
            root {
                file("a", 1)
            },
            root {
                file("e", 1)
                file("f", 1)
            },
            """
                ``
                    `a` Deleted
                    `e` New
                    `f` New
            """
        )
    }

    @Test
    fun manyRenamedAndMoved() {
        assertDiff(
            root {
                dir("a") {
                    file("b", 1)
                    file("x", 2)
                }
                dir("i") {
                    dir("ii") {
                        dir("iii") {
                            dir("iiii") {
                                file("jjjj", 10)
                                file("m", 99)
                            }
                            file("jjj", 11)
                        }
                        file("jj", 12)
                    }
                    file("j", 13)
                }
                file("r", 3)
            },
            root {
                dir("a") {
                    file("s", 3)
                    file("y", 2)
                }
                file("c", 1)
                dir("i") {
                    dir("ii") {
                        dir("iii") {
                            dir("iiii") {
                                file("j", 13)
                                file("n", 99)
                            }
                            file("jjj", 11)
                        }
                        file("jj", 12)
                    }
                    file("jjjj", 10)
                }
            },
            """
                ``
                    `a`
                        `s` MovedFrom `/r`
                        `y` RenamedFrom `x`
                    `c` MovedFrom `/a/b`
                    `i`
                        `ii`
                            `iii`
                                `iiii`
                                    `j` MovedFrom `/i/j`
                                    `n` RenamedFrom `m`
                        `jjjj` MovedFrom `/i/ii/iii/iiii/jjjj`
            """
        )
    }

    @Test
    fun manyRenamedAndMovedDir() {
        assertDiff(
            root {
                dir("a") {
                    file("b", 1)
                    file("x", 2)
                    dir("a") {
                        file("q", 17)
                    }
                }
                dir("i") {
                    dir("ii") {
                        dir("iii") {
                            dir("iiii") {
                                file("jjjj", 10)
                                file("m", 99)
                            }
                            file("jjj", 11)
                        }
                        file("jj", 12)
                    }
                    file("j", 13)
                }
                file("r", 3)
            },
            root {
                dir("i") {
                    dir("ii") {
                        dir("iii2") {
                            dir("iiii") {
                                file("jjjj", 10)
                                file("m", 99)
                            }
                            file("jjj", 11)
                        }
                        dir("a") {
                            file("b", 1)
                            file("x", 2)
                            dir("a") {
                                file("q", 17)
                            }
                        }
                        file("jj", 12)
                    }
                    file("j", 13)
                }
                file("r", 3)
            },
            """
                ``
                    `i`
                        `ii`
                            `a` MovedFrom `/a`
                            `iii2` RenamedFrom `iii`
            """
        )
    }

    @Test
    fun renamedDir() {
        assertDiff(
            root {
                dir("x") {
                    file("a", 1)
                    dir("b") {
                        file("c", 2)
                        dir("d")
                    }
                }
            },
            root {
                dir("y") {
                    file("a", 1)
                    dir("b") {
                        file("c", 2)
                        dir("d")
                    }
                }
            },
            """
                ``
                    `y` RenamedFrom `x`
            """
        )
    }

    @Test
    fun movedDir() {
        assertDiff(
            root {
                dir("x") {
                    file("a", 1)
                    dir("b") {
                        file("c", 2)
                        dir("d")
                        dir("d1")
                    }
                }
            },
            root {
                dir("z") {
                    dir("x") {
                        file("a", 1)
                        dir("b") {
                            file("c", 2)
                            dir("d")
                            dir("d1")
                        }
                    }
                }
            },
            """
                ``
                    `z` New
                        `x` MovedFrom `/x`
            """
        )
    }

    @Test
    fun mergeMovedDirectoryOk() {
        assertDiff(
            root {
                dir("u") {
                    dir("v") {
                        file("same", 123)
                        dir("d1")
                        dir("d2")
                    }
                }
            },
            root {
                dir("t") {
                    dir("v") {
                        file("same", 123)
                        dir("d1")
                        dir("d2")
                    }
                }
            },
            """
                ``
                    `t` RenamedFrom `u`
            """
        )
    }

    @Test
    fun mergeMovedDirectoryWrongSize() {
        assertDiff(
            root {
                dir("u") {
                    dir("v") {
                        file("same", 123)
                        dir("d1")
                    }
                }
            },
            root {
                dir("t") {
                    dir("v") {
                        file("same", 123)
                        dir("d1")
                        dir("d2")
                    }
                }
            },
            """
                ``
                    `t` New
                        `v` New
                            `d1` New
                            `d2` New
                            `same` MovedFrom `/u/v/same`
                    `u` Deleted
                        `v` Deleted
                            `d1` Deleted
            """
        )
    }

    @Test
    fun mergeMovedDirectoryWrongName() {
        assertDiff(
            root {
                dir("u") {
                    dir("v") {
                        file("same", 123)
                        dir("d1")
                        dir("d2")
                    }
                }
            },
            root {
                dir("t") {
                    dir("v") {
                        file("same", 123)
                        dir("d1")
                        dir("d3")
                    }
                }
            },
            """
                ``
                    `t` New
                        `v` New
                            `d1` New
                            `d3` New
                            `same` MovedFrom `/u/v/same`
                    `u` Deleted
                        `v` Deleted
                            `d1` Deleted
                            `d2` Deleted
            """
        )
    }

    @Test
    fun mergeMovedDirectoryFile() {
        assertDiff(
            root {
                dir("u") {
                    dir("v") {
                        file("same", 123)
                        dir("d1")
                        dir("d2")
                    }
                }
            },
            root {
                dir("t") {
                    dir("v") {
                        file("same", 123)
                        dir("d1")
                        file("d2", 1)
                    }
                }
            },
            """
                ``
                    `t` New
                        `v` New
                            `d1` New
                            `d2` New
                            `same` MovedFrom `/u/v/same`
                    `u` Deleted
                        `v` Deleted
                            `d1` Deleted
                            `d2` Deleted
            """
        )
    }

    @Test
    fun mergeMovedDirectoryNotEmptyDirectory() {
        assertDiff(
            root {
                dir("u") {
                    dir("v") {
                        file("same", 123)
                        dir("d1")
                        dir("d2") {
                            file("x", 1)
                        }
                    }
                }
            },
            root {
                dir("t") {
                    dir("v") {
                        file("same", 123)
                        dir("d1")
                        dir("d2")
                    }
                }
            },
            """
                ``
                    `t` New
                        `v` New
                            `d1` New
                            `d2` New
                            `same` MovedFrom `/u/v/same`
                    `u` Deleted
                        `v` Deleted
                            `d1` Deleted
                            `d2` Deleted
                                `x` Deleted
            """
        )
    }

    @Test
    fun mergeMovedDirectoryRenamed() {
        assertDiff(
            root {
                dir("u") {
                    file("same", 123)
                    file("x", 1)
                }
            },
            root {
                dir("t") {
                    dir("u") {
                        file("same", 123)
                        file("y", 1)
                    }
                }
            },
            """
                ``
                    `t` New
                        `u` New
                            `same` MovedFrom `/u/same`
                            `y` MovedFrom `/u/x`
                    `u` Deleted
            """
        )
    }

    @Test
    fun mergeMovedDirectoryMovedParent() {
        assertDiff(
            root {
                dir("v") {
                    file("b", 2)
                    file("c", 3)
                    file("z", 100)
                }
            },
            root {
                dir("v") {
                    file("z", 100)
                }
                dir("w") {
                    file("b", 2)
                    file("c", 30)
                }
            },
            """
                ``
                    `v`
                        `c` Deleted
                    `w` New
                        `b` MovedFrom `/v/b`
                        `c` New
            """
        )
    }

    @Test
    fun pruneEqualDirectoryAfterMergeMovedDirectory() {
        assertDiff(
            root {
                dir("u") {
                    dir("v") {
                        file("a", 1)
                    }
                    file("b", 2)
                }
            },
            root {
                dir("u") {
                    file("b", 2)
                }
                dir("x") {
                    file("a", 1)
                }
            },
            """
                ``
                    `x` MovedFrom `/u/v`
            """
        )
    }

    @Test
    fun compareNodeStates() {
        assertDiff(
            root {
                file("as-dir", 5)
                dir("as-file") { file("gone", 4) }
                file("changed", 1)
                file("removed", 3)
                file("same", 2)
            },
            root {
                dir("as-dir") { file("child", 8) }
                file("as-file", 7)
                file("added", 6)
                file("changed", 10)
                file("same", 2)
            },
            """
                ``
                    `added` New
                    `as-dir` FileToDir
                        `child` New
                    `as-file` DirToFile
                        `gone` Deleted
                    `changed` Changed
                    `removed` Deleted
            """
        )
    }

    @Test
    fun detectMovesAndAvoidAmbiguousDigests() {
        assertDiff(
            root { file("old", 1) },
            root { file("new", 1) },
            """
                ``
                    `new` RenamedFrom `old`
            """
        )
        assertDiff(
            root {
                dir("a") { file("f", 1) }
                dir("b")
            },
            root {
                dir("a")
                dir("b") { file("f", 1) }
            },
            """
                ``
                    `b`
                        `f` MovedFrom `/a/f`
            """
        )
        assertDiff(
            root {
                file("a", 1)
                file("b", 1)
            },
            root { file("c", 1) },
            """
                ``
                    `a` Deleted
                    `b` Deleted
                    `c` New
            """
        )
        assertDiff(
            root { file("a", 1) },
            root {
                file("b", 1)
                file("c", 1)
            },
            """
                ``
                    `a` Deleted
                    `b` New
                    `c` New
            """
        )
    }

    @Test
    fun fileDirectoryTransitionsCanMatchMoves() {
        assertDiff(
            root { file("a", 1) },
            root { dir("a") { file("child", 1) } },
            """
                ``
                    `a` FileToDir
                        `child` MovedFrom `/a`
            """
        )
        assertDiff(
            root {
                dir("a")
                file("old", 1)
            },
            root { file("a", 1) },
            """
                ``
                    `a` DirToFile RenamedFrom `old`
            """
        )
    }

    @Test
    fun consolidateMovedDirectoryWithEmptySubdirectory() {
        assertDiff(
            root {
                dir("source") {
                    file("f", 1)
                    dir("empty")
                }
            },
            root {
                dir("destination") {
                    file("f", 1)
                    dir("empty")
                }
            },
            """
                ``
                    `destination` RenamedFrom `source`
            """
        )
    }

    @Test
    fun doNotConsolidateMovedDirectoryWhenUnmovedChildrenCountDiffers() {
        assertDiff(
            root { dir("source") { file("f", 1) } },
            root {
                dir("destination") {
                    file("f", 1)
                    file("extra", 2)
                }
            },
            """
                ``
                    `destination` New
                        `extra` New
                        `f` MovedFrom `/source/f`
                    `source` Deleted
            """
        )
    }

    @Test
    fun doNotConsolidateMovedDirectoryWithNonemptyUnmovedChild() {
        assertDiff(
            root {
                dir("source") {
                    file("f", 1)
                    dir("nested")
                }
            },
            root {
                dir("destination") {
                    file("f", 1)
                    dir("nested") { file("extra", 2) }
                }
            },
            """
                ``
                    `destination` New
                        `f` MovedFrom `/source/f`
                        `nested` New
                            `extra` New
                    `source` Deleted
                        `nested` Deleted
            """
        )
    }

    @Test
    fun doNotConsolidateMovedDirectoriesWithDifferentSourcesOrContents() {
        assertDiff(
            root { dir("source") { file("f", 1) } },
            root {
                dir("destination") { file("f", 1) }
                dir("source")
            },
            """
                ``
                    `destination` New
                        `f` MovedFrom `/source/f`
            """
        )
        assertDiff(
            root {
                dir("source1") { file("a", 1) }
                dir("source2") { file("b", 2) }
            },
            root {
                dir("destination") {
                    file("a", 1)
                    file("b", 2)
                }
            },
            """
                ``
                    `destination` New
                        `a` MovedFrom `/source1/a`
                        `b` MovedFrom `/source2/b`
                    `source1` Deleted
                    `source2` Deleted
            """
        )
        assertDiff(
            root { dir("source") { file("old", 1) } },
            root { dir("destination") { file("new", 1) } },
            """
                ``
                    `destination` New
                        `new` MovedFrom `/source/old`
                    `source` Deleted
            """
        )
        assertDiff(
            root {
                dir("source") {
                    file("f", 1)
                    file("extra", 2)
                }
            },
            root {
                dir("destination") {
                    file("f", 1)
                    file("extra", 3)
                }
            },
            """
                ``
                    `destination` New
                        `extra` New
                        `f` MovedFrom `/source/f`
                    `source` Deleted
                        `extra` Deleted
            """
        )
        assertDiff(
            root {
                dir("source") {
                    file("f", 1)
                    dir("empty-old")
                }
            },
            root {
                dir("destination") {
                    file("f", 1)
                    dir("empty-new")
                }
            },
            """
                ``
                    `destination` New
                        `empty-new` New
                        `f` MovedFrom `/source/f`
                    `source` Deleted
                        `empty-old` Deleted
            """
        )
        assertDiff(
            root {
                dir("source") {
                    file("f", 1)
                    dir("not-empty") { file("gone", 2) }
                }
            },
            root {
                dir("destination") {
                    file("f", 1)
                    dir("not-empty")
                }
            },
            """
                ``
                    `destination` New
                        `f` MovedFrom `/source/f`
                        `not-empty` New
                    `source` Deleted
                        `not-empty` Deleted
                            `gone` Deleted
            """
        )
    }

    @Test
    fun obsoleteDirectories() {
        assertDiff(
            root {
                dir("empty")
                dir("kept") { file("f", 1) }
            },
            root {
                dir("empty")
                dir("kept") { file("f", 1) }
                dir("new-empty")
            },
            """
                ``
                    `new-empty` New
            """
        )
    }
}
