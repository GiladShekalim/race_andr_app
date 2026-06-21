package com.example.avoided_race_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.avoided_race_app.db.ScoreEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScoreTableAdapter(
    private var entries: List<ScoreEntry>,
    private val onRowClick: (ScoreEntry) -> Unit
) : RecyclerView.Adapter<ScoreTableAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rank: TextView = view.findViewById(R.id.row_LBL_rank)
        val score: TextView = view.findViewById(R.id.row_LBL_score)
        val date: TextView = view.findViewById(R.id.row_LBL_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_score_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.rank.text = "#${position + 1}"
        holder.score.text = entry.score.toString()
        holder.date.text = dateFormat.format(Date(entry.timestamp))
        holder.itemView.setOnClickListener { onRowClick(entry) }
    }

    override fun getItemCount() = entries.size

    fun updateEntries(newEntries: List<ScoreEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }
}
