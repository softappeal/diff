package ch.softappeal.diff

import kotlin.test.Test

val OLD_EVERYTHING = root {
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
        file("e", 2, 60)
        file("a", 2, 60)
        file("s", 2, 60)
        file("b", 2, 60)
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
}

val NEW_EVERYTHING = root {
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
        file("e", 2, 60)
        file("a", 2, 61)
        file("s", 1, 60)
        file("b", 3, 60)
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
}

private fun assertEquals(old: DirectoryNode, new: DirectoryNode, expected: String) {
    assertOutput(expected) { createDirectoryDelta(NodeDigestToPaths(old), NodeDigestToPaths(new)).print() }
}

class DeltaTest {
    @Test
    fun everything() {
        assertEquals(
            OLD_EVERYTHING,
            NEW_EVERYTHING,
            """
                `/`
                    `Changed/`
                        `a` Changed
                        `b` Bigger
                        `s` Smaller
                    `Deleted/`
                        `a/` Deleted
                            `b/` Deleted
                                `c` Deleted
                    `DirToFile/`
                        `moved/`
                            `a` DirToFile MovedFrom `/DirToFile/moved/a/b`
                        `renamed/`
                            `a` DirToFile RenamedFrom `c`
                                `b` Deleted
                        `simple/`
                            `a` DirToFile
                                `b` Deleted
                    `FileToDir/`
                        `moved/`
                            `a/` FileToDir
                                `b` MovedFrom `/FileToDir/moved/a`
                        `renamed/`
                            `a/` FileToDir
                                `b` New
                            `c` RenamedFrom `a`
                        `simple/`
                            `a/` FileToDir
                                `b` New
                    `MovedFrom/`
                        `c/` New
                            `a/` MovedFrom `/MovedFrom/a`
                    `New/`
                        `a/` New
                            `b/` New
                                `c` New
                    `RenamedFrom/`
                        `c/` RenamedFrom `a`
            """
        )
    }

    @Test
    fun compareFlatEmpty() {
        assertEquals(
            root {
            },
            root {
            },
            """
                `/`
            """
        )
    }

    @Test
    fun compareFlatDeleted() {
        assertEquals(
            root {
                file("a", 0)
            },
            root {
            },
            """
                `/`
                    `a` Deleted
            """
        )
    }

    @Test
    fun compareFlatNew() {
        assertEquals(
            root {
            },
            root {
                file("a", 0)
            },
            """
                `/`
                    `a` New
            """
        )
    }

    @Test
    fun compareFlatSame() {
        assertEquals(
            root {
                file("a", 0)
            },
            root {
                file("a", 0)
            },
            """
                `/`
            """
        )
    }

    @Test
    fun compareFlatChanged() {
        assertEquals(
            root {
                file("a", 0)
            },
            root {
                file("a", 1)
            },
            """
                `/`
                    `a` Changed
            """
        )
    }

    @Test
    fun compareFlatMany() {
        assertEquals(
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
                `/`
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
    fun compareDirs() {
        assertEquals(
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
                `/`
                    `a` Changed
                    `d1/`
                        `d2/`
                            `d3/`
                                `c` New
                        `d3/`
                            `b` Deleted
                            `d3/` New
                                `a` New
                                `b` New
            """
        )
    }

    @Test
    fun addNodeDeleted() {
        assertEquals(
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
                `/`
                    `a` Deleted
                    `b/` Deleted
                        `c` Deleted
                        `d/` Deleted
                            `e` Deleted
            """
        )
    }

    @Test
    fun addNodeNew() {
        assertEquals(
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
                `/`
                    `a` New
                    `b/` New
                        `c` New
                        `d/` New
                            `e` New
            """
        )
    }

    @Test
    fun addNodeTypeChangedFileToDir() {
        assertEquals(
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
                `/`
                    `a/` FileToDir
                        `b` New
                        `c/` New
                            `d` New
            """
        )
    }

    @Test
    fun addNodeTypeChangedFileToDirRenamed() {
        assertEquals(
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
                `/`
                    `a/` FileToDir
                        `b` New
                    `b` RenamedFrom `a`
            """
        )
    }

    @Test
    fun addNodeTypeChangedFileToDirMoved() {
        assertEquals(
            root {
                file("a", 1)
            },
            root {
                dir("a") {
                    file("b", 1)
                }
            },
            """
                `/`
                    `a/` FileToDir
                        `b` MovedFrom `/a`
            """
        )
    }

    @Test
    fun addNodeTypeChangedDirToFile() {
        assertEquals(
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
                `/`
                    `a` DirToFile
                        `b` Deleted
                        `c/` Deleted
                            `d` Deleted
            """
        )
    }

    @Test
    fun addNodeTypeChangedDirToFileRenamed() {
        assertEquals(
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
                `/`
                    `a` DirToFile RenamedFrom `x`
                        `b` Deleted
                        `c/` Deleted
                            `d` Deleted
            """
        )
    }

    @Test
    fun addNodeTypeChangedDirToFileMoved() {
        assertEquals(
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
                `/`
                    `a` DirToFile MovedFrom `/a/b`
                        `x` Deleted
            """
        )
    }

    @Test
    fun pruneEqualDirectory() {
        assertEquals(
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
                `/`
                    `d2/`
                        `e` Deleted
                    `d3/` New
            """
        )
    }

    @Test
    fun renamed() {
        assertEquals(
            root {
                file("a", 1)
            },
            root {
                file("e", 1)
            },
            """
                `/`
                    `e` RenamedFrom `a`
            """
        )
    }

    @Test
    fun movedDown() {
        assertEquals(
            root {
                file("e", 1)
            },
            root {
                dir("d") {
                    file("e", 1)
                }
            },
            """
                `/`
                    `d/` New
                        `e` MovedFrom `/e`
            """
        )
    }

    @Test
    fun movedUp1() {
        assertEquals(
            root {
                dir("d") {
                    file("e", 1)
                }
            },
            root {
                file("e", 1)
            },
            """
                `/`
                    `d/` Deleted
                    `e` MovedFrom `/d/e`
            """
        )
    }

    @Test
    fun movedUp2() {
        assertEquals(
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
                `/`
                    `d/` MovedFrom `/z/d`
                    `z/` Deleted
            """
        )
    }

    @Test
    fun renamedOldDuplicated() {
        assertEquals(
            root {
                file("a", 1)
                file("b", 1)
            },
            root {
                file("e", 1)
            },
            """
                `/`
                    `a` Deleted
                    `b` Deleted
                    `e` New
            """
        )
    }

    @Test
    fun renamedNewDuplicated() {
        assertEquals(
            root {
                file("a", 1)
            },
            root {
                file("e", 1)
                file("f", 1)
            },
            """
                `/`
                    `a` Deleted
                    `e` New
                    `f` New
            """
        )
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun manyRenamedAndMoved() {
        assertEquals(
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
                `/`
                    `a/`
                        `s` MovedFrom `/r`
                        `y` RenamedFrom `x`
                    `c` MovedFrom `/a/b`
                    `i/`
                        `ii/`
                            `iii/`
                                `iiii/`
                                    `j` MovedFrom `/i/j`
                                    `n` RenamedFrom `m`
                        `jjjj` MovedFrom `/i/ii/iii/iiii/jjjj`
            """
        )
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun manyRenamedAndMovedDir() {
        assertEquals(
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
                `/`
                    `i/`
                        `ii/`
                            `a/` MovedFrom `/a`
                            `iii2/` RenamedFrom `iii`
            """
        )
    }

    @Test
    fun renamedDir() {
        assertEquals(
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
                `/`
                    `y/` RenamedFrom `x`
            """
        )
    }

    @Test
    fun movedDir() {
        assertEquals(
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
                `/`
                    `z/` New
                        `x/` MovedFrom `/x`
            """
        )
    }

    @Test
    fun mergeMovedDirectoryOk() {
        assertEquals(
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
                `/`
                    `t/` RenamedFrom `u`
            """
        )
    }

    @Test
    fun mergeMovedDirectoryWrongSize() {
        assertEquals(
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
                `/`
                    `t/` New
                        `v/` New
                            `d1/` New
                            `d2/` New
                            `same` MovedFrom `/u/v/same`
                    `u/` Deleted
                        `v/` Deleted
                            `d1/` Deleted
            """
        )
    }

    @Test
    fun mergeMovedDirectoryWrongName() {
        assertEquals(
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
                `/`
                    `t/` New
                        `v/` New
                            `d1/` New
                            `d3/` New
                            `same` MovedFrom `/u/v/same`
                    `u/` Deleted
                        `v/` Deleted
                            `d1/` Deleted
                            `d2/` Deleted
            """
        )
    }

    @Test
    fun mergeMovedDirectoryFile() {
        assertEquals(
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
                `/`
                    `t/` New
                        `v/` New
                            `d1/` New
                            `d2` New
                            `same` MovedFrom `/u/v/same`
                    `u/` Deleted
                        `v/` Deleted
                            `d1/` Deleted
                            `d2/` Deleted
            """
        )
    }

    @Test
    fun mergeMovedDirectoryNotEmptyDirectory() {
        assertEquals(
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
                `/`
                    `t/` New
                        `v/` New
                            `d1/` New
                            `d2/` New
                            `same` MovedFrom `/u/v/same`
                    `u/` Deleted
                        `v/` Deleted
                            `d1/` Deleted
                            `d2/` Deleted
                                `x` Deleted
            """
        )
    }

    @Test
    fun mergeMovedDirectoryRenamed() {
        assertEquals(
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
                `/`
                    `t/` New
                        `u/` New
                            `same` MovedFrom `/u/same`
                            `y` MovedFrom `/u/x`
                    `u/` Deleted
            """
        )
    }

    @Test
    fun mergeMovedDirectoryMovedParent() {
        assertEquals(
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
                `/`
                    `v/`
                        `c` Deleted
                    `w/` New
                        `b` MovedFrom `/v/b`
                        `c` New
            """
        )
    }

    @Test
    fun pruneEqualDirectoryAfterMergeMovedDirectory() {
        assertEquals(
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
                `/`
                    `x/` MovedFrom `/u/v`
            """
        )
    }

    @Test
    fun smallerBigger() {
        assertEquals(
            root {
                file("a", 2, 1)
                file("b", 2, 1)
                file("c", 2, 1)
            },
            root {
                file("a", 1, 1)
                file("b", 3, 1)
                file("c", 2, 1)
            },
            """
                `/`
                    `a` Smaller
                    `b` Bigger
            """
        )
    }
}
