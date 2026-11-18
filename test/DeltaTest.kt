package ch.softappeal.diff

import kotlin.test.Test
import kotlin.test.assertEquals

private fun assertDiff(old: DirNode, new: DirNode, expected: String) {
    assertEquals(
        (expected + "\n").trimIndent(),
        toString { printDeltas(DirDelta(old, new)) },
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
    fun diff_FileToDir() = assertDiff(
        root {
            file("dir", 10)
        },
        root,
        """
            - ``
                - `dir` FileToDir
                    - `dir-file` New
                - `file` New
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
                    - `dir-file` Deleted
                - `file` Deleted
        """,
    )

    @Test
    fun diff_FileChanged() = assertDiff(
        root {
            file("file", 0)
        },
        root {
            file("file", 1)
        },
        """
            - ``
                - `file` FileChanged
        """,
    )

    @Test
    fun tryPruneKeptDirs() = assertDiff(
        root {
            dir("dir") {
                file("dir-file", 1)
            }
            dir("dir2") {
                file("dir2-file", 2)
            }
        },
        root {
            dir("dir") {
                file("dir-file", 1)
            }
            dir("dir2")
            dir("empty")
        },
        """
            - ``
                - `dir2`
                    - `dir2-file` Deleted
                - `empty` New
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
        },
        """
            - ``
                - `x1` New movedFrom: `/a1` `/a2`
                - `x2` New movedFrom: `/a1` `/a2`
        """,
    )

    @Test
    fun moveFiles_DirToFile() = assertDiff(
        root,
        root {
            file("dir", 1)
        },
        """
            - ``
                - `dir` DirToFile movedFrom: `/dir/dir-file`
                - `file` Deleted
        """,
    )

    @Test
    fun moveFiles_FileToDir() = assertDiff(
        root {
            file("dir", 1)
        },
        root,
        """
            - ``
                - `dir` FileToDir
                    - `dir-file` New movedFrom: `/dir`
                - `file` New
        """,
    )
}
