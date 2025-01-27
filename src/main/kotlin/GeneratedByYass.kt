@file:Suppress(
    "UNCHECKED_CAST",
    "USELESS_CAST",
    "PARAMETER_NAME_CHANGED_ON_OVERRIDE",
    "unused",
    "RemoveRedundantQualifierName",
    "SpellCheckingInspection",
    "RedundantVisibilityModifier",
    "RedundantNullableReturnType",
    "KotlinRedundantDiagnosticSuppress",
    "RedundantSuppression",
    "UNUSED_ANONYMOUS_PARAMETER",
)

package ch.softappeal.diff

public fun createBinarySerializer(): ch.softappeal.yass2.serialize.binary.BinarySerializer =
    object : ch.softappeal.yass2.serialize.binary.BinarySerializer() {
        init {
            initialize(
                ch.softappeal.yass2.serialize.binary.StringBinaryEncoder,
                ch.softappeal.yass2.serialize.binary.IntBinaryEncoder,
                ch.softappeal.yass2.serialize.binary.ByteArrayBinaryEncoder,
                ch.softappeal.yass2.serialize.binary.BinaryEncoder(
                    ch.softappeal.diff.FileNode::class,
                    { i ->
                        writeNoIdRequired(2, i.name)
                        writeNoIdRequired(4, i.digest)
                        writeNoIdRequired(3, i.size)
                    },
                    {
                        val i = ch.softappeal.diff.FileNode(
                            readNoIdRequired(2) as kotlin.String,
                        )
                        i.digest = readNoIdRequired(4) as kotlin.ByteArray
                        i.size = readNoIdRequired(3) as kotlin.Int
                        i
                    }
                ),
                ch.softappeal.yass2.serialize.binary.BinaryEncoder(
                    ch.softappeal.diff.DirectoryNode::class,
                    { i ->
                        writeNoIdRequired(2, i.name)
                        writeNoIdRequired(1, i.nodes)
                    },
                    {
                        val i = ch.softappeal.diff.DirectoryNode(
                            readNoIdRequired(2) as kotlin.String,
                            readNoIdRequired(1) as kotlin.collections.List<ch.softappeal.diff.Node>,
                        )
                        i
                    }
                ),
            )
        }
    }
