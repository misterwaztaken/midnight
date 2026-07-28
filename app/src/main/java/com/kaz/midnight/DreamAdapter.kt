package com.kaz.midnight

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView

class DreamAdapter(
    private var dreams: List<Dream>,
    private val onDreamClick: (Dream) -> Unit
) : RecyclerView.Adapter<DreamAdapter.DreamViewHolder>() {

    class DreamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.dreamDate)
        val contentText: TextView = itemView.findViewById(R.id.dreamText)
        val modifiedText: TextView = itemView.findViewById(R.id.dreamModifiedDate)
        val favoriteStar: ImageView = itemView.findViewById(R.id.imgFavoriteStar)
        // make sure this id exists in item_dream.xml
        val tagContainer: LinearLayout = itemView.findViewById(R.id.tagContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DreamViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dream, parent, false)
        return DreamViewHolder(view)
    }

    override fun onBindViewHolder(holder: DreamViewHolder, position: Int) {
        val dream = dreams[position]

        holder.contentText.text = dream.content
        holder.dateText.text = dream.creationDate
        holder.modifiedText.text = "Last edited: ${dream.lastModified}"

        // favorite star
        holder.favoriteStar.visibility = if (dream.isFavorite) View.VISIBLE else View.GONE

        // draw the colored tag pills
        holder.tagContainer.removeAllViews() // clear old views before reusing

        // dream.tags should have the tag list filled in already
        dream.tags.forEach { tag ->
            val tagPill = TextView(holder.itemView.context)
            tagPill.text = tag.name
            tagPill.textSize = 10f
            tagPill.setPadding(16, 4, 16, 4)
            tagPill.maxLines = 1

            // pill background color
            val tagColor = try {
                Color.parseColor(tag.colorHex)
            } catch (e: Exception) {
                holder.itemView.context.getColor(R.color.text_hint)
            }

            val shape = GradientDrawable()
            shape.cornerRadius = 50f
            shape.setColor(tagColor)
            tagPill.background = shape

            // pick white or black text depending on bg brightness
            val isDark = ColorUtils.calculateLuminance(tagColor) < 0.5
            tagPill.setTextColor(if (isDark) Color.WHITE else Color.BLACK)

            // spacing between pills
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 8, 0)
            tagPill.layoutParams = params

            holder.tagContainer.addView(tagPill)
        }

        holder.itemView.setOnClickListener {
            onDreamClick(dream)
        }
    }

    override fun getItemCount() = dreams.size

    fun updateList(newDreams: List<Dream>) {
        dreams = newDreams
        notifyDataSetChanged()
    }
}
