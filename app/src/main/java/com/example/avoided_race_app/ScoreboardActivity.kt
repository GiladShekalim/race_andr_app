package com.example.avoided_race_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

interface OnScoreSelectedListener {
    fun onScoreSelected(entryId: Int, latitude: Double, longitude: Double)
}

class ScoreboardActivity : AppCompatActivity(), OnScoreSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scoreboard)

        findViewById<android.widget.ImageButton>(R.id.scoreboard_BTN_back).setOnClickListener { finish() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.scoreboard_FRAME_table, ScoreTableFragment())
                .replace(R.id.scoreboard_FRAME_map, ScoreMapFragment())
                .commit()
        }
    }

    override fun onScoreSelected(entryId: Int, latitude: Double, longitude: Double) {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.scoreboard_FRAME_map) as? ScoreMapFragment
        mapFragment?.centerOn(latitude, longitude, entryId)
    }
}
