package app.axolotl

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.ComponentActivity

/** Real Storage Access Framework browser; it never fabricates files or paths. */
class FilesActivity : ComponentActivity() {
    private data class DocumentEntry(
        val id: String,
        val name: String,
        val mime: String,
        val size: Long?,
    ) {
        val isDirectory: Boolean get() = mime == DocumentsContract.Document.MIME_TYPE_DIR
        override fun toString(): String = if (isDirectory) "📁 $name" else "$name\n$mime${size?.let { " · $it bytes" }.orEmpty()}"
    }

    private lateinit var status: TextView
    private lateinit var list: ListView
    private var treeUri: Uri? = null
    private val directoryStack = ArrayDeque<String>()
    private var entries: List<DocumentEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { text = "Choose a folder to browse" }
        list = ListView(this).apply {
            setOnItemClickListener { _, _, position, _ -> openEntry(entries[position]) }
        }
        val choose = Button(this).apply {
            text = "Choose folder"
            setOnClickListener { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_TREE) }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 48, 24, 24)
            addView(choose)
            addView(status)
            addView(list, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        })
        getPreferences(MODE_PRIVATE).getString(PREF_TREE, null)?.let { showTree(Uri.parse(it)) }
    }

    @Deprecated("Activity result contract kept dependency-free for the first module slice")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_TREE || resultCode != Activity.RESULT_OK) return
        val tree = data?.data ?: return
        val flags = ((data?.flags ?: 0) and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
        contentResolver.takePersistableUriPermission(tree, flags)
        getPreferences(MODE_PRIVATE).edit().putString(PREF_TREE, tree.toString()).apply()
        showTree(tree)
    }

    private fun showTree(tree: Uri) {
        treeUri = tree
        directoryStack.clear()
        directoryStack.addLast(DocumentsContract.getTreeDocumentId(tree))
        showDirectory(directoryStack.last())
    }

    private fun showDirectory(documentId: String) {
        val tree = treeUri ?: return
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, documentId)
        entries = runCatching { queryChildren(children) }.getOrElse {
            status.text = "Folder cannot be read: ${it.message}"
            emptyList()
        }
        status.text = "${entries.size} items · depth ${directoryStack.size}"
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, entries)
    }

    private fun openEntry(entry: DocumentEntry) {
        if (entry.isDirectory) {
            directoryStack.addLast(entry.id)
            showDirectory(entry.id)
            return
        }
        val tree = treeUri ?: return
        val uri = DocumentsContract.buildDocumentUriUsingTree(tree, entry.id)
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, entry.mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    }

    override fun onBackPressed() {
        if (directoryStack.size > 1) {
            directoryStack.removeLast()
            showDirectory(directoryStack.last())
        } else {
            super.onBackPressed()
        }
    }

    private fun queryChildren(uri: Uri): List<DocumentEntry> {
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
        )
        return contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toEntry()) }
        }.orEmpty().sortedWith(compareByDescending<DocumentEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    private fun Cursor.toEntry() = DocumentEntry(
        id = getString(0),
        name = getString(1) ?: "Unnamed",
        mime = getString(2) ?: "application/octet-stream",
        size = if (isNull(3)) null else getLong(3),
    )

    companion object {
        private const val REQUEST_TREE = 4102
        private const val PREF_TREE = "selected_tree"
    }
}
