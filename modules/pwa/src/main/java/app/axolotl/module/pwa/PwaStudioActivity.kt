package app.axolotl.module.pwa

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.io.File

/** Creates and executes local web modules without a native JavaScript bridge. */
class PwaStudioActivity : ComponentActivity() {
    private lateinit var modules: Spinner
    private lateinit var name: EditText
    private lateinit var html: EditText
    private lateinit var status: TextView
    private lateinit var webView: WebView
    private var moduleIds = listOf<String>()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        modules = Spinner(this)
        name = EditText(this).apply { hint = "Module name (letters, numbers, dashes)" }
        html = EditText(this).apply {
            hint = "HTML body"
            minLines = 8
            setText("<h1>Hello AXOLOTL</h1><button onclick=\"this.textContent='It works'\">Run</button>")
        }
        status = TextView(this).apply { text = "Local-only sandbox · no native bridge · no network" }
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    return request.url.host != APP_HOST
                }
            }
        }
        val save = Button(this).apply { text = "Save module"; setOnClickListener { saveModule() } }
        val load = Button(this).apply { text = "Load selected"; setOnClickListener { loadSelected() } }
        val run = Button(this).apply { text = "Run preview"; setOnClickListener { runPreview() } }
        val delete = Button(this).apply { text = "Delete selected"; setOnClickListener { deleteSelected() } }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 40, 20, 20)
            addView(modules)
            addView(name)
            addView(html)
            addView(LinearLayout(this@PwaStudioActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(save); addView(load); addView(run); addView(delete)
            })
            addView(status)
            addView(webView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        })
        refreshModules()
    }

    private fun saveModule() {
        val id = name.text.toString().trim().lowercase()
        if (!MODULE_ID.matches(id)) {
            status.text = "Use 3–40 lowercase letters, numbers or dashes"
            return
        }
        moduleFile(id).apply { parentFile?.mkdirs(); writeText(html.text.toString()) }
        status.text = "Saved $id"
        refreshModules(id)
    }

    private fun loadSelected() {
        val id = selectedId() ?: return
        name.setText(id)
        html.setText(moduleFile(id).readText())
        status.text = "Loaded $id"
    }

    private fun deleteSelected() {
        val id = selectedId() ?: return
        moduleFile(id).parentFile?.deleteRecursively()
        status.text = "Deleted $id"
        refreshModules()
    }

    private fun runPreview() {
        val id = name.text.toString().trim().lowercase().takeIf(MODULE_ID::matches) ?: "preview"
        val document = sandboxDocument(html.text.toString())
        webView.visibility = View.VISIBLE
        webView.loadDataWithBaseURL("https://$APP_HOST/$id/", document, "text/html", "UTF-8", null)
        status.text = "Running $id in local sandbox"
    }

    private fun refreshModules(select: String? = null) {
        moduleIds = moduleRoot().listFiles()?.filter { moduleFile(it.name).isFile }?.map { it.name }?.sorted().orEmpty()
        modules.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, moduleIds)
        select?.let { wanted -> moduleIds.indexOf(wanted).takeIf { it >= 0 }?.let(modules::setSelection) }
    }

    private fun selectedId(): String? = moduleIds.getOrNull(modules.selectedItemPosition)
    private fun moduleRoot() = File(filesDir, "pwa-modules")
    private fun moduleFile(id: String) = File(moduleRoot(), "$id/index.html")

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val APP_HOST = "appassets.androidplatform.net"
        private val MODULE_ID = Regex("[a-z][a-z0-9-]{2,39}")
        private const val CSP = "default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data: blob:; font-src data:"

        internal fun sandboxDocument(body: String): String =
            "<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width\"><meta http-equiv=\"Content-Security-Policy\" content=\"$CSP\"></head><body>$body</body></html>"
    }
}
