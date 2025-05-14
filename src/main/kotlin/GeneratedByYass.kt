@file:Suppress(
    "unused",
    "UNCHECKED_CAST",
    "USELESS_CAST",
    "PARAMETER_NAME_CHANGED_ON_OVERRIDE",
    "RemoveRedundantQualifierName",
    "SpellCheckingInspection",
    "RedundantVisibilityModifier",
    "REDUNDANT_VISIBILITY_MODIFIER",
    "RedundantSuppression",
    "UNUSED_ANONYMOUS_PARAMETER",
    "KotlinRedundantDiagnosticSuppress",
)

package ch.softappeal.diff

public object BinarySerializer : ch.softappeal.yass2.core.serialize.binary.BinarySerializer() {
    init {
        initialize(
            // kotlin.collections.List: 1
            ch.softappeal.yass2.core.serialize.binary.StringBinaryEncoder, // 2
            ch.softappeal.yass2.core.serialize.binary.IntBinaryEncoder, // 3
            ch.softappeal.yass2.core.serialize.binary.ByteArrayBinaryEncoder, // 4
            ch.softappeal.yass2.core.serialize.binary.BinaryEncoder(
                ch.softappeal.diff.FileNode::class, // 5
                { i ->
                    writeRequired(i.name, 2)
                    writeRequired(i.digest, 4)
                    writeRequired(i.size, 3)
                },
                {
                    ch.softappeal.diff.FileNode(
                        readRequired(2) as kotlin.String,
                    ).apply {
                        digest = readRequired(4) as kotlin.ByteArray
                        size = readRequired(3) as kotlin.Int
                    }
                }
            ),
            ch.softappeal.yass2.core.serialize.binary.BinaryEncoder(
                ch.softappeal.diff.DirectoryNode::class, // 6
                { i ->
                    writeRequired(i.name, 2)
                    writeRequired(i.nodes, 1)
                },
                {
                    ch.softappeal.diff.DirectoryNode(
                        readRequired(2) as kotlin.String,
                        readRequired(1) as kotlin.collections.List<ch.softappeal.diff.Node>,
                    )
                }
            ),
        )
    }
}
