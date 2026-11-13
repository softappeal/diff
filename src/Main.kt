package ch.softappeal.diff

import ch.softappeal.yass2.serialize.fromByteArray
import ch.softappeal.yass2.serialize.toByteArray
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

const val NODE_FILE = "node.yass"

fun script(stdOut: Appendable, stdIn: InputStream, toolDir: Path) {
    with(stdOut) {
        appendLine()
        appendLine("diff ${Node::class.java.`package`.implementationVersion}")
        appendLine()
        val newNode = DirectoryNode("$toolDir/..", "SHA3-256")
        val newNodeDesc = NodeDesc(newNode)
        printDuplicates(newNodeDesc.digestToPaths)
        val nodePath = Path("$toolDir/$NODE_FILE")
        if (nodePath.exists()) {
            val oldNode = NodeSerializer.fromByteArray(nodePath.readBytes()) as DirectoryNode
            val delta = DirectoryDelta(NodeDesc(oldNode), newNodeDesc)
            appendLine("DELTA")
            printDeltas(delta)
            appendLine()
            append("type <y> to accept changes (else abort): ")
            val answer = BufferedReader(InputStreamReader(stdIn)).readLine()
            appendLine()
            if (answer != "y") {
                appendLine("ABORTED")
                appendLine()
                return
            }
        } else {
            appendLine("NODE")
            printNodes(newNode)
            appendLine()
        }
        nodePath.writeBytes(NodeSerializer.toByteArray(newNode))
        appendLine("DONE")
        appendLine()
    }
}

fun main() {
    script(System.out, System.`in`, ".")
}
