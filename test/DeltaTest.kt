package ch.softappeal.diff

import kotlin.test.Test
import kotlin.test.assertEquals

private fun assertDiff(old: DirNode, new: DirNode, expected: String) {
    assertEquals(
        (expected + "\n").trimIndent(),
        toString { printDeltas(DirDelta(old, new)) },
    )
}

class DeltaTest {
    @Test
    fun diff() {
        assertDiff(
            root {
                file("file", 20)
                file("deletedFile", 10)
                file("same")
                dir("dir") {
                    file("same")
                }
                file("changed")
                dir("deleted") {
                    dir("deleted") {
                        file("deleted")
                    }
                }
                file("FileToDir")
                dir("DirToFile") {
                    dir("simple") {
                        file("b")
                    }
                }
            },
            root {
                file("file", 21)
                file("newFile", 11)
                file("same")
                dir("dir") {
                    file("same")
                }
                file("changed", 0)
                dir("new") {
                    dir("new") {
                        file("new")
                    }
                }
                dir("FileToDir") {
                    dir("simple") {
                        file("b")
                    }
                }
                file("DirToFile")
            },
            """
                - `/`
                    - `DirToFile` DirToFile
                        - `simple/` Deleted
                            - `b` Deleted
                    - `FileToDir/` FileToDir
                        - `simple/` New
                            - `b` New
                    - `changed` Changed
                    - `deleted/` Deleted
                        - `deleted/` Deleted
                            - `deleted` Deleted
                    - `deletedFile` Deleted
                    - `file` Changed
                    - `new/` New
                        - `new/` New
                            - `new` New
                    - `newFile` New
            """,
        )
    }

    @Test
    fun tryPruneKept() {
        assertDiff(
            root {
                file("sameFile1")
                dir("sameDir") {
                    dir("sameDir") {
                        file("sameFile2")
                    }
                }
                dir("deleted") {
                    file("deleted")
                }
                dir("nested") {
                    file("moved", 2)
                }
            },
            root {
                file("sameFile1")
                dir("sameDir") {
                    dir("sameDir") {
                        file("sameFile2")
                    }
                }
                dir("deleted")
                dir("new")
                dir("nested")
                file("moved", 3)
            },
            """
                - `/`
                    - `deleted/`
                        - `deleted` Deleted
                    - `moved` New
                    - `nested/`
                        - `moved` Deleted
                    - `new/` New
            """
        )
    }

    @Test
    fun moveFiles() {
        assertDiff(
            root {
                file("a")
                file("changed", 1)
                dir("dir") {
                    file("x", 20)
                }
                file("z", 40)
            },
            root {
                file("b")
                file("c", 40)
                file("changed", 2)
                file("new1", 10)
                file("new2", 10)
                file("new3", 10)
                file("y", 20)
            },
            """
                - `/`
                    - `b` MovedFrom `/a`
                    - `c` MovedFrom `/z`
                    - `changed` Changed
                    - `dir/` Deleted
                    - `new1` New
                    - `new2` New
                    - `new3` New
                    - `y` MovedFrom `/dir/x`
            """,
        )
    }
}
