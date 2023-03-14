package ch.softappeal.but

import kotlin.test.*

private fun assertEquals(old: DirectoryNode, new: DirectoryNode, expected: String) {
    assertEquals(expected) {
        createDirectoryDelta(NodeDigestToPaths(old), NodeDigestToPaths(new)).dump(it)
    }
}

class DeltaTest {
    @Test
    fun compareFlatEmpty() {
        assertEquals(
            root {
            },
            root {
            },
            """
                '/'
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
                '/'
                    'a' Deleted
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
                '/'
                    'a' New
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
                '/'
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
                '/'
                    'a' Changed
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
                '/'
                    'a' Deleted
                    'b' New
                    'c' New
                    'd' Deleted
                    'e' Deleted
                    'f' New
                    'g' Deleted
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
                        dir("e") {}
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
                        dir("e") {}
                        file("a", 7)
                    }
                }
            },
            """
                '/'
                    'a' Changed
                    'd1/'
                        'd2/'
                            'd3/'
                                'c' New
                        'd3/'
                            'b' Deleted
                            'd3/' New
                                'a' New
                                'b' New
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
                '/'
                    'a' Deleted
                    'b/' Deleted
                        'c' Deleted
                        'd/' Deleted
                            'e' Deleted
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
                '/'
                    'a' New
                    'b/' New
                        'c' New
                        'd/' New
                            'e' New
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
                '/'
                    'a/' FileToDir
                        'b' New
                        'c/' New
                            'd' New
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
                '/'
                    'a/' FileToDir
                        'b' New
                    'b' RenamedFrom 'a'
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
                '/'
                    'a/' FileToDir
                        'b' MovedFrom '/a'
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
                '/'
                    'a' DirToFile
                        'b' Deleted
                        'c/' Deleted
                            'd' Deleted
            """
        )
    }

    @Test
    fun pruneEqualDirectories() {
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
                dir("d2") {}
                dir("d3") {}
            },
            """
                '/'
                    'd2/'
                        'e' Deleted
                    'd3/' New
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
                '/'
                    'e' RenamedFrom 'a'
            """
        )
    }

    @Test
    fun moved() {
        assertEquals(
            root {
                file("a", 1)
            },
            root {
                dir("d") {
                    file("e", 1)
                }
            },
            """
                '/'
                    'd/' New
                        'e' MovedFrom '/a'
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
                '/'
                    'a' Deleted
                    'b' Deleted
                    'e' New
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
                '/'
                    'a' Deleted
                    'e' New
                    'f' New
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
                '/'
                    'a/'
                        's' MovedFrom '/r'
                        'y' RenamedFrom 'x'
                    'c' MovedFrom '/a/b'
                    'i/'
                        'ii/'
                            'iii/'
                                'iiii/'
                                    'j' MovedFrom '/i/j'
                                    'n' RenamedFrom 'm'
                        'jjjj' MovedFrom '/i/ii/iii/iiii/jjjj'
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
                        dir("d") {}
                    }
                }
            },
            root {
                dir("y") {
                    file("a", 1)
                    dir("b") {
                        file("c", 2)
                        dir("d") {}
                    }
                }
            },
            """
                '/'
                    'x/' Deleted
                        'b/' Deleted
                            'd/' Deleted
                    'y/' New
                        'a' MovedFrom '/x/a'
                        'b/' New
                            'c' MovedFrom '/x/b/c'
                            'd/' New
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
                        dir("d") {}
                    }
                }
            },
            root {
                dir("z") {
                    dir("x") {
                        file("a", 1)
                        dir("b") {
                            file("c", 2)
                            dir("d") {}
                        }
                    }
                }
            },
            """
                '/'
                    'x/' Deleted
                        'b/' Deleted
                            'd/' Deleted
                    'z/' New
                        'x/' New
                            'a' MovedFrom '/x/a'
                            'b/' New
                                'c' MovedFrom '/x/b/c'
                                'd/' New
            """
        )
    }
}
