package com.kaz.midnight

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TagsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TagAdapter
    private var currentSearchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tags)

        // toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbarTags)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Tags"

        db = AppDatabase.getDatabase(this)

        // tag list
        recyclerView = findViewById(R.id.recyclerTags)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // adapter with click handlers
        adapter = TagAdapter(
            tags = emptyList(),
            onTagClick = { tagToEdit -> showEditTagDialog(tagToEdit) },
            onDeleteClick = { tagToDelete -> confirmDelete(tagToDelete) }
        )
        recyclerView.adapter = adapter

        // load tags
        loadTags()

        // add tag button
        findViewById<FloatingActionButton>(R.id.fabAddTag).setOnClickListener {
            showEditTagDialog(null)
        }
    }

    private fun loadTags() {
        Thread {
            val tags = db.dreamDao().getAllTags()
            runOnUiThread {
                adapter.updateTags(tags)
            }
        }.start()
    }

    private fun showEditTagDialog(tag: Tag?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_tag, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.editTagName)
        val colorPreview = dialogView.findViewById<View>(R.id.colorPreview)
        val hexInput = dialogView.findViewById<EditText>(R.id.editTagColor)
        val btnPickColor = dialogView.findViewById<Button>(R.id.btnPickColor)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)

        dialogTitle.text = if (tag == null) "New Tag" else "Edit Tag"
        btnConfirm.text = if (tag == null) "Create" else "Update"

        var selectedColor = Color.parseColor(tag?.colorHex ?: Tag.DEFAULT_HEX)

        fun updatePreview(color: Int) {
            selectedColor = color
            val hex = String.format("#%06X", 0xFFFFFF and color)
            hexInput.setText(hex)
            (colorPreview.background as GradientDrawable).setColor(color)
        }

        if (tag != null) {
            nameInput.setText(tag.name)
            updatePreview(Color.parseColor(tag.colorHex))
        } else {
            updatePreview(selectedColor)
        }

        fun showColorPicker(currentColor: Int) {
            val pickerLayout = layoutInflater.inflate(R.layout.dialog_color_picker, null)
            val wheel = pickerLayout.findViewById<CircleColorWheel>(R.id.colorWheel)
            val hexField = pickerLayout.findViewById<EditText>(R.id.pickerHexInput)
            val sliderR = pickerLayout.findViewById<SeekBar>(R.id.sliderR)
            val sliderG = pickerLayout.findViewById<SeekBar>(R.id.sliderG)
            val sliderB = pickerLayout.findViewById<SeekBar>(R.id.sliderB)
            val btnApply = pickerLayout.findViewById<Button>(R.id.btnApplyColor)

            wheel.selectedColor = currentColor

            var r = Color.red(currentColor)
            var g = Color.green(currentColor)
            var b = Color.blue(currentColor)
            val hex = String.format("#%06X", 0xFFFFFF and currentColor)
            hexField.setText(hex)

            sliderR.progress = r
            sliderG.progress = g
            sliderB.progress = b

            var updating = false

            val syncFromWheel: (Int) -> Unit = { color ->
                if (!updating) {
                    updating = true
                    r = Color.red(color)
                    g = Color.green(color)
                    b = Color.blue(color)
                    sliderR.progress = r
                    sliderG.progress = g
                    sliderB.progress = b
                    hexField.setText(String.format("#%06X", 0xFFFFFF and color))
                    updating = false
                }
            }

            wheel.onColorChanged = { color -> syncFromWheel(color) }

            val syncFromSliders = {
                if (!updating) {
                    updating = true
                    val color = Color.rgb(sliderR.progress, sliderG.progress, sliderB.progress)
                    r = sliderR.progress
                    g = sliderG.progress
                    b = sliderB.progress
                    wheel.selectedColor = color
                    hexField.setText(String.format("#%06X", 0xFFFFFF and color))
                    updating = false
                }
            }

            val onSliderChange = object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) syncFromSliders()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }

            sliderR.setOnSeekBarChangeListener(onSliderChange)
            sliderG.setOnSeekBarChangeListener(onSliderChange)
            sliderB.setOnSeekBarChangeListener(onSliderChange)

            val pickerDialog = AlertDialog.Builder(this)
                .setView(pickerLayout)
                .setCancelable(true)
                .create()

            pickerDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            btnApply.setOnClickListener {
                val hexText = hexField.text.toString().trim()
                val parsed = try {
                    Color.parseColor(if (hexText.startsWith("#")) hexText else "#$hexText")
                } catch (_: Exception) {
                    Color.rgb(sliderR.progress, sliderG.progress, sliderB.progress)
                }
                updatePreview(parsed)
                pickerDialog.dismiss()
            }

            pickerDialog.show()
        }

        btnPickColor.setOnClickListener { showColorPicker(selectedColor) }

        val tagDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        tagDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { tagDialog.dismiss() }

        btnConfirm.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val color = hexInput.text.toString().trim().ifEmpty { Tag.DEFAULT_HEX }

            if (name.isNotEmpty()) {
                Thread {
                    if (tag == null) {
                        db.dreamDao().insertTag(Tag(name = name, colorHex = color))
                    } else {
                        db.dreamDao().updateTag(tag.copy(name = name, colorHex = color))
                    }
                    loadTags()
                }.start()
                tagDialog.dismiss()
            }
        }

        tagDialog.show()
    }

    private fun confirmDelete(tag: Tag) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Delete Tag?")
            .setMessage("Remove '${tag.name}'? This won't delete your dreams.")
            .setPositiveButton("Delete") { _, _ ->
                Thread {
                    db.dreamDao().deleteTag(tag)
                    loadTags()
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // search/filter

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.tags_menu, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as androidx.appcompat.widget.SearchView

        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText ?: ""
                filterTags(currentSearchQuery)
                return true
            }
        })
        return true
    }

    private fun filterTags(query: String) {
        Thread {
            val allTags = db.dreamDao().getAllTags()
            val filtered = allTags.filter { it.name.contains(query, ignoreCase = true) }
            runOnUiThread {
                adapter.updateTags(filtered)
            }
        }.start()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
