package ch.softappeal.diff

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.name
import kotlin.io.path.readBytes

fun createDirNodeFromFileSystem(root: Path, digestAlgorithm: String) = runBlocking(Dispatchers.Default) {
    fun createDirNode(current: Path, name: String): DirNode = DirNode(
        name,
        DirState.NewDir,
        buildList {
            Files.newDirectoryStream(Path(current)).forEach { path ->
                require(!path.isSymbolicLink()) { "path '$path' is a symbolic link" }
                when {
                    path.isRegularFile() -> add(
                        FileNode(path.name, FileState.NewFile, Digest(byteArrayOf())).apply {
                            launch { digest = Digest(MessageDigest.getInstance(digestAlgorithm).digest(path.readBytes())) }
                        }
                    )
                    path.isDirectory() -> add(
                        createDirNode(path.toString(), path.name)
                    )
                    else -> error("path '$path' has unexpected type")
                }
            }
        }.sortedBy(Node::name),
    )
    createDirNode(root, "")
}
