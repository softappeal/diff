package ch.softappeal.but

import ch.softappeal.yass2.generate.*

internal fun GenerateAction.execute() {
    execute(
        "src/main/kotlin/ch/softappeal/but/GeneratedBinarySerializer.kt",
        "package ch.softappeal.but\n\n${generateBinarySerializer(NodeBaseEncoders, NodeConcreteClasses)}",
    )
}

fun main() {
    GenerateAction.Write.execute()
}
