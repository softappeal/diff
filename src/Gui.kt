package ch.softappeal.diff

import java.awt.Color
import java.awt.Component
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.event.TreeModelListener
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeModel
import javax.swing.tree.TreePath

private val ColorSame = Color.GRAY
private val ColorMoved = Color.BLUE
private val ColorChanged = Color.MAGENTA
private val ColorDeleted = Color.RED
private val ColorNew = Color(0, 102, 0)

fun gui(root: DirectoryDelta) {
    SwingUtilities.invokeLater {
        object : JFrame() {
            init {
                setSize(800, 1000)
                title = "diff"
                setLocationRelativeTo(null)
                isVisible = true
                val tree = JTree(object : TreeModel {
                    override fun getRoot() = root
                    override fun getChild(parent: Any, index: Int) = (parent as DirectoryDelta).deltas[index]
                    override fun getIndexOfChild(parent: Any, child: Any) = (parent as DirectoryDelta).deltas.indexOf(child)
                    override fun getChildCount(parent: Any) = if (parent is FileDelta) 0 else (parent as DirectoryDelta).deltas.size
                    override fun isLeaf(delta: Any) = delta is FileDelta
                    override fun addTreeModelListener(l: TreeModelListener) = Unit
                    override fun removeTreeModelListener(l: TreeModelListener) = Unit
                    override fun valueForPathChanged(path: TreePath, newValue: Any) = Unit
                })
                tree.cellRenderer = object : DefaultTreeCellRenderer() {
                    override fun getTreeCellRendererComponent(
                        tree: JTree, value: Any, sel: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean,
                    ): Component {
                        val delta = value as Delta
                        setTextNonSelectionColor(when (delta.state) {
                            DeltaState.Same -> ColorSame
                            DeltaState.Smaller, DeltaState.Bigger, DeltaState.Changed, DeltaState.DirToFile -> ColorChanged
                            DeltaState.New -> if (delta.fromState == null) ColorNew else ColorMoved
                            DeltaState.Deleted, DeltaState.FileToDir -> ColorDeleted
                        })
                        return super.getTreeCellRendererComponent(
                            tree, "`${delta.name}`${delta.info()}", sel, expanded, leaf, row, hasFocus
                        )
                    }
                }
                tree.isRootVisible = true
                tree.isEditable = false
                fun expandAll(path: TreePath) {
                    val delta = path.lastPathComponent
                    if (delta is DirectoryDelta) {
                        delta.deltas.forEach { expandAll(path.pathByAddingChild(it)) }
                        tree.expandPath(path)
                    }
                }
                expandAll(TreePath(root))
                contentPane = JScrollPane(tree)
                validate()
            }
        }
    }
}
