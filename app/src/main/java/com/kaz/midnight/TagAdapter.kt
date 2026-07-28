package com.kaz.midnight

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TagAdapter(
    private var tags: List<Tag>,
    private val onTagClick: (Tag) -> Unit,   // for editing
    private val onDeleteClick: (Tag) -> Unit // for deleting
) : RecyclerView.Adapter<TagAdapter.TagViewHolder>() {

    class TagViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tagName: TextView = view.findViewById(R.id.textTagName)
        val tagColorIndicator: View = view.findViewById(R.id.viewTagColor)
        val deleteBtn: ImageButton = view.findViewById(R.id.btnDeleteTag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = tags[position]
        holder.tagName.text = tag.name

        // color dot
        val background = holder.tagColorIndicator.background as GradientDrawable
        background.setColor(Color.parseColor(tag.colorHex))

        // click handlers
        // tap row to edit
        holder.itemView.setOnClickListener { onTagClick(tag) }

        // tap trash to delete
        holder.deleteBtn.setOnClickListener { onDeleteClick(tag) }
    }

    override fun getItemCount() = tags.size

    fun updateTags(newTags: List<Tag>) {
        this.tags = newTags
        notifyDataSetChanged()
    }
}
