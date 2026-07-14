package com.example.spelltracker.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.spelltracker.R
import com.example.spelltracker.data.MistakeWord

class MistakeAdapter(
    private val items: MutableList<MistakeWord>,
    private val onAddToPractice: (MistakeWord) -> Unit,
    private val onDelete: (MistakeWord) -> Unit
) : RecyclerView.Adapter<MistakeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val wordText: TextView = view.findViewById(R.id.wordText)
        val suggestionText: TextView = view.findViewById(R.id.suggestionText)
        val timeText: TextView = view.findViewById(R.id.timeText)
        val addToPracticeButton: Button = view.findViewById(R.id.addToPracticeButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteMistakeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mistake_word, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.wordText.text = item.word
        holder.suggestionText.text = item.suggestion ?: "no suggestion"
        holder.timeText.text = DateUtils.getRelativeTimeSpanString(item.timestamp)
        holder.addToPracticeButton.setOnClickListener { onAddToPractice(item) }
        holder.deleteButton.setOnClickListener {
            onDelete(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun removeItem(item: MistakeWord) {
        val index = items.indexOf(item)
        if (index >= 0) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
