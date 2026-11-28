package ch.softappeal.diff

import java.security.MessageDigest
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

const val NODE_FILE = "diff.ser"
private const val DIGEST_ALGORITHM = "SHA3-256"

fun Appendable.printDuplicates(node: DirNode) {
    val digestToPaths = buildMap {
        fun Node.visit(current: Path) {
            val next = if (name.isEmpty()) "" else current.Path(name)
            when (this) {
                is FileNode -> getOrPut(digest!!) { mutableListOf() }.add(next)
                is DirNode -> children.forEach { it.visit(next) }
            }
        }
        node.visit("")
    }
    val duplicates = digestToPaths.filter { it.value.size != 1 }
    if (duplicates.isEmpty()) return
    appendLine("- DUPLICATES")
    duplicates.forEach { [digest, paths] ->
        appendLine("    - `$digest`")
        paths.forEach { appendLine("        - `$it`") }
    }
    appendLine()
}

fun Appendable.script(rootDir: Path) {
    appendLine("diff ${Node::class.java.`package`.implementationVersion}")
    appendLine()
    val newNode = createDirNodeFromFileSystem(rootDir, DIGEST_ALGORITHM)
    printDuplicates(newNode)
    val nodePath = Path("$rootDir/$NODE_FILE")
    if (nodePath.exists()) {
        val oldNode = nodePath.readBytes().fromByteArray(MessageDigest.getInstance(DIGEST_ALGORITHM).digestLength)
        appendLine("DIFF")
        printNode(createDiff(oldNode, newNode))
    } else {
        appendLine("NODE")
        printNode(newNode)
    }
    nodePath.writeBytes(newNode.toByteArray())
}

fun main() {
    System.out.script(".")
}
