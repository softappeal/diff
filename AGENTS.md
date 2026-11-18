# Overview

- This is a tool for tracking changes in a directory tree.
- It is written in the programming language `Kotlin`.
- It uses the `Gradle` build system.
- Keep changes local; do not commit them.

# How to build project

- Use `./gradlew build` to build the project.
- Use `./gradlew test` to run all tests.

# Coding guidelines

- Use idiomatic `Kotlin` code.
- The production code is in directory `src`.
- The corresponding test code is in directory `test`.
- `testResources` contains test fixtures.
- Build warnings are treated as errors.
- `KSP` generates serialization code.
- `test/MainTest.kt` writes and deletes `testResources/diff/node.yass`.

# How the code works

- `FileNode` and `DirectoryNode` in `src/Node.kt` represent files (with `SHA3-256` digests) and directories in the tracked tree.
  Symbolic links are not supported.
- `DirectoryDelta` in `src/Delta.kt` compares two tree snapshots.
  `FileDelta` is an implementation detail; changes are represented by file and directory deltas.
  These changes are calculated by comparing the current directory tree with the previous one
  with the function `fun DirectoryDelta(oldNodeDesc: NodeDesc, newNodeDesc: NodeDesc)` in the file `src/Delta.kt`.
  Duplicate file contents are reported; moves and renames are inferred only when the digest is unique in both snapshots.
  The function `fun Appendable.printDeltas(delta: Delta, indent: Int = 0)` in the file `src/Delta.kt`
  prints changes of the directory tree to be tracked in a human-readable format.
- The function `fun main()` in `src/Main.kt` is the entry point.
  It scans the parent of the working directory (including the tool directory) and
  stores the snapshot as `node.yass` in the working directory.
  Replacing an existing snapshot requires an exact `y` response.
- See [readme.md](readme.md) for instructions on running the packaged tool.
