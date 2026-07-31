package com.kaz.midnight

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.textfield.TextInputLayout

class SettingsActivity : AppCompatActivity() {

    private fun performImport(uri: android.net.Uri) {
        val db = AppDatabase.getDatabase(this)

        Thread {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader().use { it?.readText() } ?: ""

                var importedCount = 0
                var duplicateCount = 0

                try {
                    // try new format (ExportData with tags and cross-refs)
                    val backup = com.google.gson.Gson().fromJson(jsonString, ExportData::class.java)

                    // insert tags, mapping old ids to new ids
                    val tagIdMap = mutableMapOf<Int, Int>()
                    backup.tags.forEach { tag ->
                        val existing = db.dreamDao().getTagByName(tag.name)
                        if (existing != null) {
                            tagIdMap[tag.id] = existing.id
                        } else {
                            val newId = db.dreamDao().insertTag(tag.copy(id = 0))
                            tagIdMap[tag.id] = newId.toInt()
                        }
                    }

                    // insert dreams, mapping old ids to new ids
                    val dreamIdMap = mutableMapOf<Int, Int>()
                    var dups = 0
                    backup.dreams.forEach { dream ->
                        val existing = db.dreamDao().findDuplicate(dream.content, dream.creationDate)
                        if (existing == null) {
                            val newId = db.dreamDao().insert(dream.copy(id = 0))
                            dreamIdMap[dream.id] = newId.toInt()
                        } else {
                            dreamIdMap[dream.id] = existing.id
                            dups++
                        }
                    }

                    // restore cross-refs using mapped ids
                    backup.crossRefs.forEach { ref ->
                        val newDreamId = dreamIdMap[ref.dreamId]
                        val newTagId = tagIdMap[ref.tagId]
                        if (newDreamId != null && newTagId != null) {
                            db.dreamDao().insertDreamTagCrossRef(
                                DreamTagCrossRef(dreamId = newDreamId, tagId = newTagId)
                            )
                        }
                    }

                    importedCount = backup.dreams.size - dups
                    duplicateCount = dups
                } catch (e: Exception) {
                    // fall back to old format (just a list of dreams, no tags)
                    val listType = object : com.google.gson.reflect.TypeToken<List<Dream>>() {}.type
                    val importedDreams: List<Dream> = com.google.gson.Gson().fromJson(jsonString, listType)

                    var dups = 0
                    importedDreams.forEach { dream ->
                        val existing = db.dreamDao().findDuplicate(dream.content, dream.creationDate)
                        if (existing == null) {
                            db.dreamDao().insert(dream.copy(id = 0))
                        } else {
                            dups++
                        }
                    }

                    importedCount = importedDreams.size - dups
                    duplicateCount = dups
                }

                runOnUiThread {
                    Toast.makeText(this, "Imported $importedCount dreams ($duplicateCount duplicates skipped)", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Import failed: Check file format", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { performImport(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // load theme before anything else
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val savedTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_YES)
        AppCompatDelegate.setDefaultNightMode(savedTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // toolbar setup
        val toolbar = findViewById<Toolbar>(R.id.settingsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // buttons
        val btnImport = findViewById<Button>(R.id.btnImportDreams)
        btnImport.setOnClickListener {
            importLauncher.launch(arrayOf("application/json", "text/json", "text/plain", "application/octet-stream"))
        }

        val btnExport = findViewById<Button>(R.id.btnExportData)
        btnExport.setOnClickListener {
            startActivity(Intent(this, ExportActivity::class.java))
        }

        val btnAbout = findViewById<Button>(R.id.btnAbout)
        btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        // theme picker (custom adapter that doesnt filter anything)
        val themeSelector = findViewById<AutoCompleteTextView>(R.id.themeSelector)
        val themes = arrayOf("Light", "Dark", "System Default")

        // custom adapter so the dropdown doesnt filter out options
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, themes) {
            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        results.values = themes
                        results.count = themes.size
                        return results
                    }
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }

        themeSelector.setAdapter(adapter)

        // set the current theme from prefs
        val currentMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_YES)
        val initialText = when(currentMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> "Light"
            AppCompatDelegate.MODE_NIGHT_YES -> "Dark"
            else -> "System Default"
        }
        themeSelector.setText(initialText, false)

        // open dropdown on click
        themeSelector.setOnClickListener {
            themeSelector.showDropDown()
        }

        themeSelector.setOnItemClickListener { _, _, position, _ ->
            val selectedTheme = themes[position]
            val mode = when (position) {
                0 -> AppCompatDelegate.MODE_NIGHT_NO
                1 -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }

            prefs.edit().putInt("theme_mode", mode).apply()
            AppCompatDelegate.setDefaultNightMode(mode)

            // post so the ui is ready before we reset the text
            themeSelector.post {
                themeSelector.setText(selectedTheme, false)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView
        searchView.queryHint = "Search settings..."

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterSettings(newText ?: "")
                return true
            }
        })
        return true
    }

    private fun filterSettings(query: String) {
        val q = query.lowercase()

        val btnImport = findViewById<Button>(R.id.btnImportDreams)
        val btnExport = findViewById<Button>(R.id.btnExportData)
        val btnAbout = findViewById<Button>(R.id.btnAbout)
        val themeLayout = findViewById<TextInputLayout>(R.id.themeInputLayout)
        val dataHeader = findViewById<View>(R.id.headerData)
        val appearanceHeader = findViewById<View>(R.id.headerAppearance)

        val showData = q.isEmpty() || "import export data backup json restore dreams".contains(q)
        val showAppearance = q.isEmpty() || "theme appearance light dark system style".contains(q)
        val showAbout = q.isEmpty() || "about midnight version kaz info credits".contains(q)

        dataHeader.visibility = if (showData) View.VISIBLE else View.GONE
        btnImport.visibility = if (showData) View.VISIBLE else View.GONE
        btnExport.visibility = if (showData) View.VISIBLE else View.GONE
        appearanceHeader.visibility = if (showAppearance) View.VISIBLE else View.GONE
        themeLayout.visibility = if (showAppearance) View.VISIBLE else View.GONE
        btnAbout.visibility = if (showAbout) View.VISIBLE else View.GONE
    }
}
