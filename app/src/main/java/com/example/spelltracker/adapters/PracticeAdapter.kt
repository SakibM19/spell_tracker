package com.example.spelltracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.spelltracker.R
import com.example.spelltracker.data.PracticeWord

class PracticeAdapter(
    private val items: MutableList<PracticeWord>,
    private val onDelete: (PracticeWord) -> Unit
) : RecyclerView.Adapter<PracticeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val wordText: TextView = view.findViewById(R.id.practiceWordText)
        val deleteButton: ImageButton = view.findViewById(R.id.deletePracticeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_practice_word, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.wordText.text = item.word
        holder.deleteButton.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size

    fun removeItem(item: PracticeWord) {
        val index = items.indexOf(item)
        if (index >= 0) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun addItem(item: PracticeWord) {
        items.add(0, item)
        notifyItemInserted(0)
    }
}
