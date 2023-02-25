package ch.softappeal.but

import kotlin.test.*

class BackupTest {
    @Test
    fun test() {
        backup("src/test/resources/test", "build/backup_")
    }
}
