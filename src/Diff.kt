package ch.softappeal.diff

/*

Each directory gets a structural signature made of its children's names, the file data, and the nested directory signatures.
The directory's own name is left out, so a rename counts as a move.
Signatures are interned to ints, so comparing two subtrees is constant time.

The old tree is indexed twice. One index maps each signature to all old paths with that content, in tree order.
The other maps each old path to its signature.

The new tree is walked top-down. For each non-empty directory:
- If the old tree has the same signature at the same path, it is unchanged. It is skipped, and so is everything under it.
- Otherwise, if the signature exists elsewhere in the old tree, it is a moved directory.
  It is emitted with movedPaths set to those old paths and no children.
- Otherwise, the walk descends into it.
  The directory is kept in the result only if something below it moved, and its movedPaths stays null.

nodes that change type are handled:
The old tree is indexed with separate maps for files (fileDigestByPath) and directories (dirSignatureByPath).
If /x was a file in the old tree and is a directory in the new one, the "unchanged" lookup for directories finds nothing at /x.
So the new directory is not skipped as unchanged, and the walk either matches it as a moved directory or descends into it.
The same holds the other way round for a directory that became a file.
A parent directory's signature includes the isDirectory flag for each child,
so a type change inside a directory makes its signature different, as it should.

 */

/**
 * Compares the trees [old] and [new] and returns a tree that describes the moved nodes of [new].
 *
 * The result has the name of [new]. It contains the moved nodes, which have [Node.movedPaths] set,
 * and their ancestor directories, which have it set to null. Everything else is left out. A moved
 * directory is returned without children.
 *
 * Paths are absolute and separated by "/", for example "/dir/toBeMoved". The root has the path ""
 * and is never reported as moved. The paths of one node are in preorder, with children sorted by name.
 *
 * Details:
 * - Files: "identical content" means the same digest.
 * - Directories: "identical content" means the same children (names, digests, nested structure).
 * - The node's own name is ignored, so a rename is a move.
 * - A node that exists unchanged at the same path in [old] is not a move, and nothing inside it is reported.
 * - A node whose type changed (file to directory or vice versa) is new at its path, so it is never "unchanged".
 *   It can still be reported as a move if its content matches nodes of the same type elsewhere in [old].
 * - Empty directories are ignored (they would otherwise match every other empty directory).
 * - Only the topmost moved node is reported, not nodes inside a moved directory.
 * - A copy is reported like a move, even if the original still exists unchanged.
 */
fun createDiff(old: DirNode, new: DirNode): DirNode {
    // Interns digests and directory signatures as small ints so comparing them is O(1).
    val digestIds = mutableMapOf<Digest, Int>()
    fun digestId(file: FileNode): Int = digestIds.getOrPut(file.digest!!) { digestIds.size }

    // Signature of a child: (name, isDirectory, digest id or directory signature id).
    // The isDirectory flag keeps a file and a directory with the same name from ever being equal.
    val signatures = mutableMapOf<List<Triple<String, Boolean, Int>>, Int>()
    val signatureByDir = mutableMapOf<DirNode, Int>()

    fun signatureOf(dir: DirNode): Int = signatureByDir.getOrPut(dir) {
        val key = dir.children.map { child ->
            when (child) {
                is FileNode -> Triple(child.name, false, digestId(child))
                is DirNode -> Triple(child.name, true, signatureOf(child))
            }
        }
        signatures.getOrPut(key) { signatures.size }
    }

    // Index the old tree: content -> paths (preorder, i.e. sorted), and path -> content.
    val dirPathsBySignature = mutableMapOf<Int, MutableList<String>>()
    val dirSignatureByPath = mutableMapOf<String, Int>()
    val filePathsByDigest = mutableMapOf<Int, MutableList<String>>()
    val fileDigestByPath = mutableMapOf<String, Int>()

    fun index(dir: DirNode, path: String) {
        for (child in dir.children) {
            val childPath = "$path/${child.name}"
            when (child) {
                is FileNode -> {
                    val digest = digestId(child)
                    filePathsByDigest.getOrPut(digest) { mutableListOf() }.add(childPath)
                    fileDigestByPath[childPath] = digest
                }
                is DirNode -> {
                    if (child.children.isNotEmpty()) {
                        val signature = signatureOf(child)
                        dirPathsBySignature.getOrPut(signature) { mutableListOf() }.add(childPath)
                        dirSignatureByPath[childPath] = signature
                    }
                    index(child, childPath)
                }
            }
        }
    }
    index(old, "")

    // Walk the new tree top-down; stop descending at the first moved (or unchanged) directory.
    fun walk(dir: DirNode, path: String): List<Node> = dir.children.mapNotNull { child ->
        val childPath = "$path/${child.name}"
        when (child) {
            is FileNode -> {
                val digest = digestId(child)
                if (fileDigestByPath[childPath] == digest) return@mapNotNull null // unchanged
                val sources = filePathsByDigest[digest] ?: return@mapNotNull null
                FileNode(child.name, FileState.NewFile, null).also { it.movedPaths = sources.toList() }
            }
            is DirNode -> {
                if (child.children.isEmpty()) return@mapNotNull null
                val signature = signatureOf(child)
                if (dirSignatureByPath[childPath] == signature) return@mapNotNull null // unchanged

                val sources = dirPathsBySignature[signature]
                if (sources != null) {
                    DirNode(child.name, DirState.KeptDir, emptyList()).also { it.movedPaths = sources.toList() }
                } else {
                    val moved = walk(child, childPath)
                    if (moved.isEmpty()) null else DirNode(child.name, DirState.NewDir, moved)
                }
            }
        }
    }

    return DirNode("", DirState.KeptDir, walk(new, ""))
}
