package com.example.avoided_race_app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScoreTableFragment : Fragment() {

    private lateinit var adapter: ScoreTableAdapter
    private var listener: OnScoreSelectedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnScoreSelectedListener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_score_table, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.score_table_RV)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = ScoreTableAdapter(emptyList()) { entry ->
            listener?.onScoreSelected(entry.latitude, entry.longitude)
        }
        recyclerView.adapter = adapter

        loadScores()
    }

    private fun loadScores() {
        lifecycleScope.launch(Dispatchers.IO) {
            val scores = (requireActivity().application as GameApplication)
                .database.scoreDao().getTop10()
            withContext(Dispatchers.Main) {
                adapter.updateEntries(scores)
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
