package ch.softappeal.diff

import java.nio.file.*
import java.util.zip.*
import kotlin.io.path.*
import kotlin.test.*

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
        assertEquals("""
            - ``
                - `a.txt` CFCD208495D565EF66E7DFF9F98764DA
                - `b`
                    - `d.txt` CFCD208495D565EF66E7DFF9F98764DA
                    - `e`
                        - `g.txt` C4CA4238A0B923820DCC509A6F75849B
                    - `f.txt` CFCD208495D565EF66E7DFF9F98764DA
                - `c.txt` CFCD208495D565EF66E7DFF9F98764DA
        """) { createDirectoryNode(ALGORITHM, archiveDir).print() }
    }
}
