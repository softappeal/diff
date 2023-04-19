package ch.softappeal.diff

import java.nio.file.*
import java.text.*
import java.util.*
import kotlin.io.path.*

fun Path.readNode() = readBytes().readNode()
private fun readNodeFromStdIn() = System.`in`.readAllBytes().readNode()

private fun DirectoryNode.writeToStdOut() = System.out.writeBytes(this.write())
private fun DirectoryNode.writeToFile(file: Path) = file.writeBytes(this.write())

fun Path.nodeFile(): Path = resolve("node.yass")

fun script(toolDirectory: Path, algorithm: String, withDuplicates: Boolean, archivePrefix: String?) {
    println()
    val root = toolDirectory.resolve("..")
    val newNodeDigestToPaths = NodeDigestToPaths(createDirectoryNode(algorithm, root))
    if (withDuplicates) {
        printDuplicates(newNodeDigestToPaths.digestToPaths)
        println()
    }
    val node = toolDirectory.nodeFile()
    if (node.exists()) {
        createDirectoryDelta(NodeDigestToPaths(node.readNode()), newNodeDigestToPaths).print()
        println()
        print("type <y> to accept changes (else abort): ")
        val answer = readln()
        println()
        if (answer != "y") {
            println("ABORTED")
            println()
            return
        }
    }
    newNodeDigestToPaths.node.writeToFile(node)
    if (archivePrefix != null) {
        val zipFile = "$archivePrefix${SimpleDateFormat("yyyy.MM.dd-HH.mm.ss").format(Date())}.zip"
        createZipFile(root, Path(zipFile))
        println("archive '$zipFile' written")
        println()
    }
    println("DONE")
    println()
}

val USAGE = """
    usage:
        'createNode' algorithm directory > nodeFile
        'printNode' < nodeFile
        'printDuplicates' < nodeFile
        'diff' oldNodeFile < newNodeFile
        ( 'script' | 'scriptNoDuplicates' ) algorithm [ archivePrefix ]
""".trimIndent()

fun main(vararg args: String) {
    fun throwInvalidArgs(): Nothing = throw IllegalArgumentException(USAGE)
    if (args.isEmpty()) throwInvalidArgs()
    val command = args[0]
    when {
        command == "createNode" && args.size == 3 -> createDirectoryNode(args[1], Path(args[2])).writeToStdOut()
        command == "printNode" && args.size == 1 -> readNodeFromStdIn().print()
        command == "printDuplicates" && args.size == 1 -> printDuplicates(readNodeFromStdIn().calculateDigestToPaths())
        command == "diff" && args.size == 2 ->
            createDirectoryDelta(NodeDigestToPaths(Path(args[1]).readNode()), NodeDigestToPaths(readNodeFromStdIn())).print()
        (command == "script" || command == "scriptNoDuplicates") && (args.size == 2 || args.size == 3) ->
            script(Path(""), args[1], command == "script", if (args.size == 2) null else args[2])
        else -> throwInvalidArgs()
    }
}

fun testMain(vararg args: String) = main(*args)
