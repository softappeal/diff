package ch.softappeal.diff

import kotlin.test.Test
import kotlin.test.assertEquals

private fun assertDiff(old: DirNode, new: DirNode, expected: String) {
    assertEquals(
        (expected + "\n").trimIndent(),
        toString { printDelta(DirDelta(old, new)) },
    )
}

private val root = root {
    dir("dir") {
        file("dir-file", 1)
    }
    file("file", 0)
}

class DeltaTest {
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
                - `dir` Deleted
                    - `dir-file` Deleted
                - `file` Deleted
        """,
    )

    @Test
    fun diff_New() = assertDiff(
        root {
        },
        root,
        """
            - ``
                - `dir` New
                    - `dir-file` New
                - `file` New
        """,
    )

    @Test
    fun diff_ToDir() = assertDiff(
        root {
            file("dir", 10)
        },
        root,
        """
            - ``
                - `dir` ToDir
                    - `dir-file` New
                - `file` New
        """,
    )

    @Test
    fun diff_ToFile() = assertDiff(
        root,
        root {
            file("dir", 10)
        },
        """
            - ``
                - `dir` ToFile
                    - `dir-file` Deleted
                - `file` Deleted
        """,
    )

    @Test
    fun diff_Changed() = assertDiff(
        root {
            file("file", 1)
        },
        root {
            file("file", 2)
        },
        """
            - ``
                - `file` Changed
        """,
    )

    @Test
    fun moveFiles_Changed() = assertDiff(
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
                - `changed` Changed < `/dir/deleted`
        """,
    )

    @Test
    fun moveFiles_New() = assertDiff(
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
                - `new` New < `/a1` `/a2`
                - `x1` New < `/a1` `/a2`
                - `x2` New < `/a1` `/a2`
        """,
    )

    @Test
    fun moveFiles_ToDir() = assertDiff(
        root {
            file("dir", 1)
        },
        root,
        """
            - ``
                - `dir` ToDir
                    - `dir-file` New < `/dir`
                - `file` New
        """,
    )

    @Test
    fun moveFiles_ToFile() = assertDiff(
        root,
        root {
            file("dir", 1)
        },
        """
            - ``
                - `dir` ToFile < `/dir/dir-file`
                - `file` Deleted
        """,
    )

    @Test
    fun tryPruneKept() = assertDiff(
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
                    - `dir2-file` Deleted
                - `newDir` New
        """,
    )
}
