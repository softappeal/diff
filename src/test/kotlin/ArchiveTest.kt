package ch.softappeal.diff

import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.writeBytes
import kotlin.test.Test

private fun extractZipFile(zipFile: Path, targetDirectory: Path) {
    targetDirectory.toFile().deleteRecursively()
    targetDirectory.createDirectories()
    ZipInputStream(zipFile.inputStream()).use { input ->
        var entry = input.nextEntry
        while (entry != null) {
            val path = targetDirectory.resolve(entry.name)
            if (entry.isDirectory) path.createDirectories() else path.writeBytes(input.readAllBytes())
            entry = input.nextEntry
        }
    }
}

class ArchiveTest {
    @Test
    fun test() {
        val archiveZip = Path("build/archive-test.zip")
        val archiveDir = Path("build/archive-test")
        createZipFile(TEST_DIR, archiveZip)
        extractZipFile(archiveZip, archiveDir)
        assertOutput("""
            ``
                `a.txt` 1 CFCD208495D565EF66E7DFF9F98764DA
                `b`
                    `d.txt` 1 CFCD208495D565EF66E7DFF9F98764DA
                    `e`
                        `g.txt` 1 C4CA4238A0B923820DCC509A6F75849B
                    `f.txt` 1 CFCD208495D565EF66E7DFF9F98764DA
                `c.txt` 1 CFCD208495D565EF66E7DFF9F98764DA
        """) { createDirectoryNode(ALGORITHM, archiveDir).print() }
    }
}
