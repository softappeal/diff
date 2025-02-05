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
    "TrailingComma",
)

package ch.softappeal.diff

/*
    1: kotlin.collections.List
    2: kotlin.String
    3: kotlin.Int
    4: kotlin.ByteArray
    5: ch.softappeal.diff.FileNode
        name: required 2
        digest: required 4
        size: required 3
    6: ch.softappeal.diff.DirectoryNode
        name: required 2
        nodes: required 1
*/
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
                        writeRequired(i.name, 2)
                        writeRequired(i.digest, 4)
                        writeRequired(i.size, 3)
                    },
                    {
                        val i = ch.softappeal.diff.FileNode(
                            readRequired(2) as kotlin.String,
                        )
                        i.digest = readRequired(4) as kotlin.ByteArray
                        i.size = readRequired(3) as kotlin.Int
                        i
                    }
                ),
                ch.softappeal.yass2.serialize.binary.BinaryEncoder(
                    ch.softappeal.diff.DirectoryNode::class,
                    { i ->
                        writeRequired(i.name, 2)
                        writeRequired(i.nodes, 1)
                    },
                    {
                        val i = ch.softappeal.diff.DirectoryNode(
                            readRequired(2) as kotlin.String,
                            readRequired(1) as kotlin.collections.List<ch.softappeal.diff.Node>,
                        )
                        i
                    }
                ),
            )
        }
    }
