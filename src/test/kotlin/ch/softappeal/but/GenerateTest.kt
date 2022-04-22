package ch.softappeal.but

import ch.softappeal.yass2.generate.*
import kotlin.test.*

private fun generate(fileName: String, code: String) {
    GenerateAction.Verify.execute(
        "src/main/kotlin/ch/softappeal/but/$fileName",
        "package ch.softappeal.but\n\n$code",
    )
}

class GenerateTest {
    @Test
    fun generateBinarySerializer() {
        generate(
            "GeneratedNodeBinarySerializer.kt",
            generateBinarySerializer(NodeBaseEncoders, NodeConcreteClasses, name = "generatedNodeBinarySerializer")
        )
    }
}
