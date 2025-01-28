package ch.softappeal.diff

import ch.softappeal.yass2.generate.GENERATED_BY_YASS
import ch.softappeal.yass2.generate.GenerateMode
import ch.softappeal.yass2.generate.generateBinarySerializer
import ch.softappeal.yass2.generate.generateFile
import ch.softappeal.yass2.serialize.binary.ByteArrayBinaryEncoder
import ch.softappeal.yass2.serialize.binary.IntBinaryEncoder
import ch.softappeal.yass2.serialize.binary.StringBinaryEncoder
import kotlin.test.Test

class GenerateTest {
    @Test
    fun test() {
        generateFile(
            "src/main/kotlin/$GENERATED_BY_YASS",
            "ch.softappeal.diff",
            GenerateMode.Verify,
        ) {
            generateBinarySerializer(
                listOf(StringBinaryEncoder::class, IntBinaryEncoder::class, ByteArrayBinaryEncoder::class),
                listOf(FileNode::class, DirectoryNode::class),
            )
        }
    }
}
