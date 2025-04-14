package ch.softappeal.diff

import ch.softappeal.yass2.generate.GENERATED_BY_YASS
import ch.softappeal.yass2.generate.generateBinarySerializer
import ch.softappeal.yass2.generate.generateFile
import kotlin.test.Test

class GenerateTest {
    @Test
    fun test() {
        generateFile(
            "src/main/kotlin/$GENERATED_BY_YASS",
            "ch.softappeal.diff",
        ) {
            generateBinarySerializer(EncoderObjects, ConcreteClasses)
        }
    }
}
