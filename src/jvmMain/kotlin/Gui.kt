package ch.softappeal.diff

import java.awt.*
import javax.swing.*
import javax.swing.event.*
import javax.swing.tree.*

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
                    override fun getTreeCellRendererComponent(tree: JTree, value: Any, sel: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean): Component {
                        val delta = value as Delta
                        setTextNonSelectionColor(when (delta.state) {
                            DeltaState.Same -> Color.GRAY
                            DeltaState.New -> if (delta.fromState == null) Color.BLUE else Color.MAGENTA
                            DeltaState.Deleted, DeltaState.Changed, DeltaState.FileToDir, DeltaState.DirToFile -> Color.RED
                        })
                        return super.getTreeCellRendererComponent(tree, "`${delta.name}`${delta.info()}", sel, expanded, leaf, row, hasFocus)
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
