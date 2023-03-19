package ch.softappeal.diff

@Suppress("RedundantSuppression", "UNCHECKED_CAST", "RemoveRedundantQualifierName", "SpellCheckingInspection", "RedundantVisibilityModifier")
public fun generatedBinarySerializer(
    baseEncoders: List<ch.softappeal.yass2.serialize.binary.BaseEncoder<*>>,
): ch.softappeal.yass2.serialize.binary.BinarySerializer =
    ch.softappeal.yass2.serialize.binary.BinarySerializer(baseEncoders + listOf(
        ch.softappeal.yass2.serialize.binary.ClassEncoder(ch.softappeal.diff.FileNode::class, false,
            { w, i ->
                w.writeNoIdRequired(3, i.name)
                w.writeNoIdRequired(4, i.digest)
            },
            { r ->
                val i = ch.softappeal.diff.FileNode(
                    r.readNoIdRequired(3) as kotlin.String,
                )
                i.digest = r.readNoIdRequired(4) as kotlin.ByteArray
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
