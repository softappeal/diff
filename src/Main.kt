package ch.softappeal.diff

import java.security.MessageDigest
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

const val NODE_FILE = "diff.ser"
private const val DIGEST_ALGORITHM = "SHA3-256"

fun Appendable.script(toolDir: Path) {
    appendLine("diff ${Node::class.java.`package`.implementationVersion}")
    appendLine()
    val newNode = DirNode(toolDir, DIGEST_ALGORITHM)
    printDuplicates(newNode)
    val nodePath = Path("$toolDir/$NODE_FILE")
    if (nodePath.exists()) {
        val oldNode = nodePath.readBytes().fromByteArray(MessageDigest.getInstance(DIGEST_ALGORITHM).digestLength)
        val delta = DirDelta(oldNode, newNode)
        appendLine("DELTA")
        printDelta(delta)
    } else {
        appendLine("NODE")
        printNode(newNode)
    }
    nodePath.writeBytes(newNode.toByteArray())
}

fun main() {
    System.out.script(".")
}
