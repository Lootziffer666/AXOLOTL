package app.axolotl

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.ComponentActivity

/** Real Storage Access Framework browser; it never fabricates files or paths. */
class FilesActivity : ComponentActivity() {
    private lateinit var status: TextView
    private lateinit var list: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { text = "Choose a folder to browse" }
        list = ListView(this)
        val choose = Button(this).apply {
            text = "Choose folder"
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_TREE)
            }
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
        val flags = (data?.flags ?: 0) and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        contentResolver.takePersistableUriPermission(tree, flags)
        getPreferences(MODE_PRIVATE).edit().putString(PREF_TREE, tree.toString()).apply()
        showTree(tree)
    }

    private fun showTree(tree: Uri) {
        val documentId = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, documentId)
        val rows = runCatching { queryChildren(children) }.getOrElse {
            status.text = "Folder cannot be read: ${it.message}"
            emptyList()
        }
        status.text = "${rows.size} items · $tree"
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, rows)
        list.emptyView = TextView(this).apply { text = "This folder is empty"; visibility = View.VISIBLE }
    }

    private fun queryChildren(uri: Uri): List<String> {
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
        )
        return contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.describeDocument())
            }
        }.orEmpty()
    }

    private fun Cursor.describeDocument(): String {
        val name = getString(0) ?: "Unnamed"
        val mime = getString(1) ?: "unknown"
        val size = if (isNull(2)) "" else " · ${getLong(2)} bytes"
        return "$name\n$mime$size"
    }

    companion object {
        private const val REQUEST_TREE = 4102
        private const val PREF_TREE = "selected_tree"
    }
}
