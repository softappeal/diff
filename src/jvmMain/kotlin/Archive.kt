package ch.softappeal.diff

import java.nio.file.*
import java.nio.file.attribute.*
import java.util.zip.*
import kotlin.io.path.*

private const val DIR_SEP_STRING = DIR_SEP.toString()

fun createZipFile(sourceDirectory: Path, zipFile: Path) {
    ZipOutputStream(zipFile.outputStream()).use { output ->
        Files.walkFileTree(sourceDirectory, object : SimpleFileVisitor<Path>() {
            fun Path.putEntry(suffix: String = "") = output.putNextEntry(ZipEntry( // 'replace' is needed if we are on Windows
                sourceDirectory.relativize(this).toString().replace('\\', '/') + suffix
            ))

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (MAC_DS_STORE == file.name) return FileVisitResult.CONTINUE
                file.putEntry()
                Files.copy(file, output)
                return FileVisitResult.CONTINUE
            }

            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                dir.putEntry(DIR_SEP_STRING)
                return FileVisitResult.CONTINUE
            }
        })
    }
}
