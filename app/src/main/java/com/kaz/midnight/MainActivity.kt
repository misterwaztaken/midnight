package com.kaz.midnight

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var adapter: DreamAdapter
    private lateinit var db: AppDatabase
    private lateinit var emptyState: TextView

    // search/filter state
    private var currentSearchQuery = ""
    private var activeFilterTagIds = mutableListOf<Int>()
    private var isFilterFavoriteOnly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)

        // toolbar + drawer setup
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // dream list recycler
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewDreams)
        adapter = DreamAdapter(emptyList()) { dream ->
            val intent = Intent(this, EditDreamActivity::class.java)
            intent.putExtra("DREAM_ID", dream.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // add dream button
        findViewById<FloatingActionButton>(R.id.fabAddDream).setOnClickListener {
            val intent = Intent(this, EditDreamActivity::class.java)
            intent.putExtra("DREAM_ID", -1)
            startActivity(intent)
        }

        emptyState = findViewById(R.id.txtEmptyState)

        // drawer menu clicks
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_all_dreams -> {
                    activeFilterTagIds.clear()
                    isFilterFavoriteOnly = false
                    refreshList()
                }
                R.id.nav_manage_tags -> startActivity(Intent(this, TagsActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    // search stuff
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText ?: ""
                refreshList()
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_filter_tags) {
            showFilterDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // filter dialog
    private fun showFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_filter_dreams, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        // get rid of the ugly gray bg behind the dialog corners
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val chipGroup = dialogView.findViewById<ChipGroup>(R.id.filterChipGroup)
        val checkFavorite = dialogView.findViewById<MaterialCheckBox>(R.id.checkFilterFavorite)
        val btnApply = dialogView.findViewById<Button>(R.id.btnApplyFilter)

        checkFavorite.isChecked = isFilterFavoriteOnly

        // load tags into the chip group
        Thread {
            val allTags = db.dreamDao().getAllTags()
            runOnUiThread {
                allTags.forEach { tag ->
                    val chip = Chip(this)
                    chip.text = tag.name
                    chip.isCheckable = true
                    chip.isChecked = activeFilterTagIds.contains(tag.id)

                    chip.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) activeFilterTagIds.add(tag.id)
                        else activeFilterTagIds.remove(tag.id)
                    }
                    chipGroup.addView(chip)
                }
            }
        }.start()

        btnApply.setOnClickListener {
            isFilterFavoriteOnly = checkFavorite.isChecked
            refreshList()
            dialog.dismiss()
        }
        dialog.show()
    }

    // refresh the dream list
    private fun refreshList() {
        Thread {
            // grab dreams based on current search/filters
            val results = if (activeFilterTagIds.isEmpty()) {
                db.dreamDao().getFilteredDreams("%$currentSearchQuery%", if (isFilterFavoriteOnly) 1 else 0)
            } else {
                db.dreamDao().getDreamsByTags(activeFilterTagIds)
                    .filter { it.content.contains(currentSearchQuery, ignoreCase = true) }
                    .filter { !isFilterFavoriteOnly || it.isFavorite }
                    .distinctBy { it.id }
            }

            // gotta fetch tags for each dream manually since room ignores that field
            results.forEach { dream ->
                dream.tags = db.dreamDao().getTagsForDream(dream.id)
            }

            // update the list on screen
            runOnUiThread {
                val isEmpty = results.isEmpty()
                val rv = findViewById<RecyclerView>(R.id.recyclerViewDreams)
                adapter.updateList(results)
                rv.visibility = if (isEmpty) View.GONE else View.VISIBLE
                emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
