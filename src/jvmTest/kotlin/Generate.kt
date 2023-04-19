package ch.softappeal.diff

import ch.softappeal.yass2.generate.*

internal fun GenerateAction.execute() {
    execute(
        "src/commonMain/kotlin/GeneratedBinarySerializer.kt",
        "package ch.softappeal.diff\n\n${generateBinarySerializer(NodeBaseEncoders, NodeConcreteClasses)}",
    )
}

fun main() {
    GenerateAction.Write.execute()
}
