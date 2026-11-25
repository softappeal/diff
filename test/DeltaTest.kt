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
