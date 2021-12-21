package ch.softappeal.but

@Suppress("UNCHECKED_CAST", "RemoveRedundantQualifierName", "SpellCheckingInspection", "RedundantVisibilityModifier")
public fun generatedNodeBinarySerializer(
    baseEncodersSupplier: () -> List<ch.softappeal.yass2.serialize.binary.BaseEncoder<*>>,
): ch.softappeal.yass2.serialize.binary.BinarySerializer =
    ch.softappeal.yass2.serialize.binary.BinarySerializer(baseEncodersSupplier() + listOf(
        ch.softappeal.yass2.serialize.binary.ClassEncoder(ch.softappeal.but.FileNode::class,
            { w, i ->
                w.writeNoIdRequired(2, i.name)
                w.writeNoIdRequired(3, i.digest)
            },
            { r ->
                val i = ch.softappeal.but.FileNode(
                    r.readNoIdRequired(2) as kotlin.String,
                )
                i.digest = r.readNoIdRequired(3) as kotlin.ByteArray
                i
            }
        ),
        ch.softappeal.yass2.serialize.binary.ClassEncoder(ch.softappeal.but.DirectoryNode::class,
            { w, i ->
                w.writeNoIdRequired(2, i.name)
                w.writeNoIdRequired(1, i.nodes)
            },
            { r ->
                val i = ch.softappeal.but.DirectoryNode(
                    r.readNoIdRequired(2) as kotlin.String,
                    r.readNoIdRequired(1) as kotlin.collections.List<ch.softappeal.but.Node>,
                )
                i
            }
        ),
    ))
