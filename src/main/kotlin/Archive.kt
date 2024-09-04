package ch.softappeal.diff

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.name
import kotlin.io.path.outputStream

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
