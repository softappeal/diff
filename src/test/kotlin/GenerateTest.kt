package ch.softappeal.diff

import ch.softappeal.yass2.generate.generateBinarySerializer
import ch.softappeal.yass2.generate.generateFile
import kotlin.test.Test

class GenerateTest {
    @Test
    fun generate() {
        generateFile("src/main/kotlin") {
            generateBinarySerializer(BinaryEncoderObjects, ConcreteAndEnumClasses)
        }
    }
}
