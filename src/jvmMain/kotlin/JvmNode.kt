package ch.softappeal.diff

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.name
import kotlin.io.path.readBytes

fun createDirectoryNode(digestAlgorithm: String, sourceDirectory: Path): DirectoryNode = runBlocking {
    CoroutineScope(Dispatchers.Default).async {
        fun directoryNode(directory: Path, name: String): DirectoryNode = createDirectoryNode(
            name,
            buildList {
                Files.newDirectoryStream(directory).forEach { path ->
                    require(!path.isSymbolicLink()) { "'$path' is a symbolic link" }
                    if (path.isRegularFile()) {
                        if (MAC_DS_STORE == path.name) return@forEach
                        add(FileNode(path.name).apply {
                            launch {
                                val bytes = path.readBytes()
                                size = bytes.size
                                digest = MessageDigest.getInstance(digestAlgorithm).digest(bytes)
                            }
                        })
                    } else {
                        add(directoryNode(path, path.name))
                    }
                }
            },
        )
        directoryNode(sourceDirectory, "")
    }.await()
}
