package com.wilfried.tech.toletters.utils

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wilfried.tech.toletters.R


class HistoryAdapter(
    context: Context,
    private val history: MutableSet<String> = mutableSetOf()
) :
    RecyclerView.Adapter<HistoryAdapter.Companion.HistoryViewHolder>() {

    private val sharedPreferences =
        context.getSharedPreferences(HISTORY_PREFS_NAME, Context.MODE_PRIVATE)

    init {
        val oldHistories = sharedPreferences.getStringSet(HISTORY_PREFS_KEY, mutableSetOf())
        if (oldHistories != null) {
            history.addAll(oldHistories)

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        return HistoryViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.history_item, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return history.size
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(history.elementAt(history.size - position - 1))
    }

    fun add(value: String) {
        if (history.size == 50) {
            history.remove(history.elementAt(0))
        }
        history.add(value)
        notifyItemInserted(history.indexOf(value))
        save()
    }

    fun save() {
        sharedPreferences.edit().apply {
            putStringSet(HISTORY_PREFS_KEY, history)
            apply()
        }
        Log.i("History", "History saved")
    }

    fun isEmpty(): Boolean {
        return history.isEmpty()
    }

    companion object {
        private const val HISTORY_PREFS_NAME = "convert_history"
        private const val HISTORY_PREFS_KEY = "convert_history_key"

        class HistoryViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            private val textView: TextView = itemView.findViewById(R.id.history_content)

            fun bind(text: String) {
                textView.text = text
            }
        }
    }
}

