package ch.softappeal.diff

import ch.softappeal.yass2.generate.*
import kotlin.io.path.*

internal fun GenerateAction.execute() {
    execute(
        Path("src/commonMain/kotlin/NodeSerializer.kt"),
        "package ch.softappeal.diff\n\n${generateBinarySerializer(::NodeBaseEncoders, NodeConcreteClasses, name = "NodeSerializer")}",
    )
}

fun main() {
    GenerateAction.Write.execute()
}
