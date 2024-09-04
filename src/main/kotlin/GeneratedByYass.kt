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
)

package ch.softappeal.diff

public fun createSerializer(
    baseEncoders: kotlin.collections.List<ch.softappeal.yass2.serialize.binary.BaseEncoder<out kotlin.Any>>,
): ch.softappeal.yass2.serialize.binary.BinarySerializer =
    ch.softappeal.yass2.serialize.binary.BinarySerializer(baseEncoders + listOf(
        ch.softappeal.yass2.serialize.binary.ClassEncoder(ch.softappeal.diff.FileNode::class, false,
            { w, i ->
                w.writeNoIdRequired(3, i.name)
                w.writeNoIdRequired(5, i.digest)
                w.writeNoIdRequired(4, i.size)
            },
            { r ->
                val i = ch.softappeal.diff.FileNode(
                    r.readNoIdRequired(3) as kotlin.String,
                )
                i.digest = r.readNoIdRequired(5) as kotlin.ByteArray
                i.size = r.readNoIdRequired(4) as kotlin.Int
                i
            }
        ),
        ch.softappeal.yass2.serialize.binary.ClassEncoder(ch.softappeal.diff.DirectoryNode::class, false,
            { w, i ->
                w.writeNoIdRequired(3, i.name)
                w.writeNoIdRequired(1, i.nodes)
            },
            { r ->
                val i = ch.softappeal.diff.DirectoryNode(
                    r.readNoIdRequired(3) as kotlin.String,
                    r.readNoIdRequired(1) as kotlin.collections.List<ch.softappeal.diff.Node>,
                )
                i
            }
        ),
    ))
