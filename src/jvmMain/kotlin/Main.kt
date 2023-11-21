package ch.softappeal.diff

import java.nio.file.*
import java.text.*
import java.util.*
import kotlin.io.path.*
import kotlin.system.*

fun Path.readNode() = readBytes().readNode()
private fun readNodeFromStdIn() = System.`in`.readAllBytes().readNode()

private fun DirectoryNode.writeToStdOut() = System.out.writeBytes(this.write())
private fun DirectoryNode.writeToFile(file: Path) = file.writeBytes(this.write())

fun Path.nodeFile(): Path = resolve("node.yass")

fun script(toolDirectory: Path, algorithm: String, archivePrefix: String?, withGui: Boolean = false) {
    println()
    val root = toolDirectory.resolve("..")
    val newNodeDigestToPaths = NodeDigestToPaths(createDirectoryNode(algorithm, root))
    printDuplicates(newNodeDigestToPaths.digestToPaths)
    println()
    val node = toolDirectory.nodeFile()
    if (node.exists()) {
        val delta = createDirectoryDelta(NodeDigestToPaths(node.readNode()), newNodeDigestToPaths)
        if (withGui) gui(delta)
        delta.print()
        println()
        print("type <y> to accept changes (else abort): ")
        val answer = readln()
        println()
        if (answer != "y") {
            println("ABORTED")
            println()
            if (withGui) exitProcess(1)
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
    if (withGui) exitProcess(0)
}

val USAGE = """
    usage:
        'createNode' algorithm directory > nodeFile
        'printNode' < nodeFile
        'printDuplicates' < nodeFile
        'printSizes' < nodeFile
        'diff' oldNodeFile < newNodeFile
        ( 'script' | 'scriptWithGui' ) algorithm [ archivePrefix ]
""".trimIndent()

fun main(vararg args: String) {
    fun throwInvalidArgs(): Nothing = throw IllegalArgumentException(USAGE)
    if (args.isEmpty()) throwInvalidArgs()
    val command = args[0]
    when {
        command == "createNode" && args.size == 3 -> createDirectoryNode(args[1], Path(args[2])).writeToStdOut()
        command == "printNode" && args.size == 1 -> readNodeFromStdIn().print()
        command == "printDuplicates" && args.size == 1 -> printDuplicates(readNodeFromStdIn().calculateDigestToPaths())
        command == "printSizes" && args.size == 1 -> readNodeFromStdIn().printFilesBySize()
        command == "diff" && args.size == 2 ->
            createDirectoryDelta(NodeDigestToPaths(Path(args[1]).readNode()), NodeDigestToPaths(readNodeFromStdIn())).print()
        (command == "script" || command == "scriptWithGui") && (args.size == 2 || args.size == 3) ->
            script(Path(""), args[1], if (args.size == 2) null else args[2], command == "scriptWithGui")
        else -> throwInvalidArgs()
    }
}

fun testMain(vararg args: String) = main(*args)
