package ch.softappeal.but

import java.io.*
import java.nio.file.*
import java.nio.file.attribute.*
import java.text.*
import java.util.*
import java.util.zip.*

fun backup(directory: String, archivePrefix: String) {
    val folder = File(directory).toPath()
    val zipFile = archivePrefix + SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(Date()) + ".zip"
    ZipOutputStream(FileOutputStream(zipFile)).use { out ->
        Files.walkFileTree(folder, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                out.putNextEntry(ZipEntry(folder.relativize(file).toString()))
                Files.copy(file, out)
                out.closeEntry()
                return FileVisitResult.CONTINUE
            }

            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                out.putNextEntry(ZipEntry(folder.relativize(dir).toString() + "/"))
                out.closeEntry()
                return FileVisitResult.CONTINUE
            }
        })
    }
}
