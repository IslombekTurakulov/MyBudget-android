package ru.iuturakulov.mybudget.ui.projects.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.iuturakulov.mybudget.R

class EmojiPickerAdapter(
    private val emojis: List<String>,
    private val onEmojiSelected: (String) -> Unit
) : RecyclerView.Adapter<EmojiPickerAdapter.EmojiViewHolder>() {

    inner class EmojiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emojiTextView: TextView = view.findViewById(R.id.tvEmoji)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_emoji, parent, false)
        return EmojiViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        val emoji = emojis[position]
        holder.emojiTextView.text = emoji

        holder.itemView.setOnClickListener {
            onEmojiSelected(emoji)
        }
    }

    override fun getItemCount(): Int = emojis.size
}
