@file:Suppress(
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

/*
    0: null - built-in
    1: [] - built-in
    2: kotlin.String - base
    3: kotlin.Int - base
    4: kotlin.ByteArray - base
    5: ch.softappeal.diff.FileNode - class
        name: required 2
        digest: required 4
        size: required 3
    6: ch.softappeal.diff.DirectoryNode - class
        name: required 2
        nodes: required 1
*/
public fun createBinarySerializer(): ch.softappeal.yass2.core.serialize.binary.BinarySerializer =
    object : ch.softappeal.yass2.core.serialize.binary.BinarySerializer() {
        init {
            initialize(
                ch.softappeal.yass2.core.serialize.binary.StringBinaryEncoder,
                ch.softappeal.yass2.core.serialize.binary.IntBinaryEncoder,
                ch.softappeal.yass2.core.serialize.binary.ByteArrayBinaryEncoder,
                ch.softappeal.yass2.core.serialize.binary.BinaryEncoder(
                    ch.softappeal.diff.FileNode::class,
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
                    ch.softappeal.diff.DirectoryNode::class,
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
