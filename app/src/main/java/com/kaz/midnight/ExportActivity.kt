package com.kaz.midnight

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.gson.Gson
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class ExportActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var formatHint: TextView
    private var selectedFormat = "JSON"

    // file picker to save export
    private val createFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { saveFileToUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export)

        db = AppDatabase.getDatabase(this)

        val toolbar = findViewById<Toolbar>(R.id.exportToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        formatHint = findViewById(R.id.txtFormatHint)
        val formatSelector = findViewById<AutoCompleteTextView>(R.id.exportFormatSelector)
        val btnConfirm = findViewById<Button>(R.id.btnConfirmExport)

        // format dropdown
        val formats = arrayOf("JSON (.json)", "Plain Text (.txt)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, formats)
        formatSelector.setAdapter(adapter)

        formatSelector.setOnItemClickListener { _, _, position, _ ->
            selectedFormat = if (position == 0) "JSON" else "TXT"
            formatHint.text = when (selectedFormat) {
                "JSON" -> "Use JSON to export your dreams in a format that Midnight can read to re-import."
                else -> "Plain Text is for reading only. Midnight cannot re-import this format."
            }
        }

        btnConfirm.setOnClickListener {
            // open file picker
            val fileName = if (selectedFormat == "JSON") "midnight_backup.json" else "my_dreams.txt"
            val mimeType = if (selectedFormat == "JSON") "application/json" else "text/plain"

            // open system folder picker
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
            startActivityForResult(intent, 101)
        }
    }

    // write the data to the picked file
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                saveFileToUri(uri)
            }
        }
    }

    private fun saveFileToUri(uri: Uri) {
        Thread {
            val dreams = db.dreamDao().getAllSync()
            val tags = db.dreamDao().getAllTags()
            val crossRefs = db.dreamDao().getAllCrossRefs()

            val content = if (selectedFormat == "JSON") {
                Gson().toJson(ExportData(dreams, tags, crossRefs))
            } else {
                buildString {
                    dreams.forEach {
                        append("DATE: ${it.creationDate}\nCONTENT: ${it.content}\n\n---\n\n")
                    }
                }
            }

            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                        writer.write(content)
                    }
                }
                runOnUiThread { Toast.makeText(this, "Export Successful!", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Export Failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }
}
