package com.kaz.midnight

import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.*

class EditDreamActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var dreamId: Int = -1
    private var currentDream: Dream? = null
    private var originalContent: String = "" // track if user changed anything

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_dream)

        db = AppDatabase.getDatabase(this)
        dreamId = intent.getIntExtra("DREAM_ID", -1)

        // toolbar back arrow
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val input = findViewById<EditText>(R.id.editDreamContent)
        val tagInput = findViewById<MultiAutoCompleteTextView>(R.id.tagAutoComplete)
        val updateBtn = findViewById<Button>(R.id.btnUpdateDream)
        val checkFavorite = findViewById<CheckBox>(R.id.checkFavorite)
        val btnInfo = findViewById<ImageButton>(R.id.btnDreamInfo)

        // tag autocomplete setup
        Thread {
            val tagNames = db.dreamDao().getAllTags().map { it.name }
            runOnUiThread {
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tagNames)
                tagInput.setAdapter(adapter)
                tagInput.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
            }
        }.start()

        // load dream data if we're editing an existing one
        if (dreamId != -1) {
            Thread {
                currentDream = db.dreamDao().getById(dreamId)
                val tagsForDream = db.dreamDao().getTagsForDream(dreamId)

                runOnUiThread {
                    currentDream?.let {
                        originalContent = it.content // save original for comparison
                        input.setText(it.content)
                        checkFavorite.isChecked = it.isFavorite
                    }
                    val existingTagsString = tagsForDream.joinToString(", ") { it.name }
                    if (existingTagsString.isNotEmpty()) {
                        tagInput.setText("$existingTagsString, ")
                    }
                }
            }.start()
        }

        // warn user if they try to leave with unsaved changes
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentContent = input.text.toString().trim()

                // nothing changed or new empty dream, just close
                if (currentContent == originalContent.trim()) {
                    finish()
                } else {
                    AlertDialog.Builder(this@EditDreamActivity)
                        .setTitle("Discard Changes?")
                        .setMessage("You have unsaved changes. Are you sure you want to leave?")
                        .setPositiveButton("Discard") { _, _ -> finish() }
                        .setNegativeButton("Keep Editing", null)
                        .show()
                }
            }
        })

        // dream info button
        btnInfo.setOnClickListener {
            if (dreamId == -1) {
                Toast.makeText(this, "New entry: No history yet.", Toast.LENGTH_SHORT).show()
            } else {
                showDreamInfoDialog()
            }
        }

        // save/update dream
        updateBtn.setOnClickListener {
            val content = input.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(this, "Content cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isCurrentlyFavorite = checkFavorite.isChecked
            val rawTags = tagInput.text?.toString() ?: ""
            val enteredTagNames = rawTags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            Thread {
                val formatter = SimpleDateFormat("MMM dd, yyyy | hh:mm a", Locale.getDefault())
                val now = formatter.format(Date())

                // match these to dream.kt: id, content, creationDate, lastModified, isFavorite
                if (dreamId == -1) {
                    val newDream = Dream(
                        content = content,
                        creationDate = now,
                        lastModified = now,
                        isFavorite = isCurrentlyFavorite
                    )
                    dreamId = db.dreamDao().insert(newDream).toInt()
                } else {
                    val updatedDream = Dream(
                        id = dreamId,
                        content = content,
                        creationDate = currentDream?.creationDate ?: now,
                        lastModified = now,
                        isFavorite = isCurrentlyFavorite
                    )
                    db.dreamDao().update(updatedDream)
                    // remove old tag links first
                    db.dreamDao().deleteTagsForDream(dreamId)
                }

                // save the tags
                enteredTagNames.forEach { name ->
                    var tagObj = db.dreamDao().getTagByName(name)
                    if (tagObj == null) {
                        val newTagId = db.dreamDao().insertTag(Tag(name = name, colorHex = Tag.AUTO_HEX)).toInt()
                        tagObj = Tag(id = newTagId, name = name, colorHex = Tag.AUTO_HEX)
                    }
                    db.dreamDao().insertDreamTagCrossRef(DreamTagCrossRef(dreamId, tagObj.id))
                }

                runOnUiThread {
                    originalContent = content
                    finish()
                }
            }.start()
        }
    }

    // toolbar back arrow
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDreamInfoDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_dream_info, null)

        val txtCreated = view.findViewById<TextView>(R.id.textCreated)
        val txtModified = view.findViewById<TextView>(R.id.textModified)
        val txtStats = view.findViewById<TextView>(R.id.textStats) // make sure this id exists in the xml

        // grab current text from the input
        val dreamInput = findViewById<EditText>(R.id.editDreamContent)
        val currentText = dreamInput.text.toString().trim()

        // count words and chars
        val charCount = currentText.length
        // split by whitespace but dont count empty strings as words
        val wordCount = if (currentText.isEmpty()) 0 else currentText.split("\\s+".toRegex()).size

        // fill in the dialog text
        txtCreated.text = currentDream?.creationDate ?: "Not saved yet"
        txtModified.text = currentDream?.lastModified ?: "No modifications"

        // show the stats
        txtStats?.text = "$wordCount words  |  $charCount characters"

        dialog.setContentView(view)
        dialog.show()
    }

}
