package ch.softappeal.diff

import ch.softappeal.yass2.generate.Mode
import ch.softappeal.yass2.generate.generate
import ch.softappeal.yass2.generate.generateBinarySerializer
import kotlin.test.Test

class GenerateTest {
    @Test
    fun test() {
        generate(
            "src/commonMain/kotlin",
            "ch.softappeal.diff",
            Mode.Verify,
        ) {
            generateBinarySerializer(BaseEncoders, TreeConcreteClasses)
        }
    }
}
