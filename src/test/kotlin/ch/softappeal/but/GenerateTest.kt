package ch.softappeal.but

import ch.softappeal.yass2.generate.*
import java.io.*
import kotlin.test.*

private fun generate(fileName: String, code: String) {
    val text = "package ch.softappeal.but\n\n$code"
    print(text)
    val filePath = "src/main/kotlin/ch/softappeal/but/$fileName"
    assertEquals(text, File(filePath).readText().replace("\r\n", "\n"))
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
