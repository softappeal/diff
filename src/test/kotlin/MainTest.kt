package ch.softappeal.but

import java.io.*
import kotlin.test.*

class MainTest {
    @Test
    fun main() {
        val node = "build/node.yass"
        File(node).delete()
        assertEquals("""
            duplicates:
                ['/a.txt', '/b/d.txt', '/b/f.txt', '/c.txt']

            nodeFile 'build/node.yass' written
        """) {
            main("MD5", "src/test/resources/test", node, null, it) { false }
        }
        assertEquals("""
            <no duplicates>
            
            '/'
                'b/' Deleted
                    'd.txt' Deleted
                    'e/' Deleted
                        'g.txt' Deleted
                    'f.txt' Deleted
                'c.txt' Deleted

            type <y> to overwrite nodeFile (else abort): 
            nodeFile 'build/node.yass' written
            archive 'build/backup_' written
         """) {
            main("MD5", "src/test/resources/test2", node, "build/backup_", it) { true }
        }
        assertEquals("""
            <no duplicates>
            
            '/'

            type <y> to overwrite nodeFile (else abort): 
         """) {
            main("MD5", "src/test/resources/test2", node, "build/backup_", it) { false }
            it("\n")
        }
    }
}
