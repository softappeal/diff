package ch.softappeal.diff

import kotlinx.coroutines.*
import java.nio.file.*
import java.security.*
import kotlin.io.path.*

fun createDirectoryNode(digestAlgorithm: String, sourceDirectory: Path): DirectoryNode = runBlocking {
    CoroutineScope(Dispatchers.Default).async {
        fun directoryNode(directory: Path, name: String): DirectoryNode = DirectoryNode(
            name,
            buildList {
                Files.newDirectoryStream(directory).forEach { path ->
                    require(!path.isSymbolicLink()) { "'$path' is a symbolic link" }
                    if (path.isRegularFile()) {
                        if (MAC_DS_STORE == path.name) return@forEach
                        add(FileNode(path.name).apply {
                            launch { digest = MessageDigest.getInstance(digestAlgorithm).digest(path.readBytes()) }
                        })
                    } else {
                        add(directoryNode(path, path.name))
                    }
                }
            }
        )
        directoryNode(sourceDirectory, "")
    }.await()
}
