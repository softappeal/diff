package ch.softappeal.diff

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.security.MessageDigest
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

const val NODE_FILE = "diff.ser"
private const val DIGEST_ALGORITHM = "SHA3-256"

fun script(stdOut: Appendable, stdIn: InputStream, toolDir: Path) {
    with(stdOut) {
        appendLine()
        appendLine("diff ${Node::class.java.`package`.implementationVersion}")
        appendLine()
        val newNode = DirNode("$toolDir/..", DIGEST_ALGORITHM)
        printDuplicates(newNode)
        val nodePath = Path("$toolDir/$NODE_FILE")
        if (nodePath.exists()) {
            val oldNode = nodePath.readBytes().fromByteArray(MessageDigest.getInstance(DIGEST_ALGORITHM).digestLength)
            val delta = DirDelta(oldNode, newNode)
            appendLine("DELTA")
            printDelta(delta)
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
            printNode(newNode)
            appendLine()
        }
        nodePath.writeBytes(newNode.toByteArray())
        appendLine("DONE")
        appendLine()
    }
}

fun main() {
    script(System.out, System.`in`, ".")
}
