package com.example.avoided_race_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

interface OnScoreSelectedListener {
    fun onScoreSelected(latitude: Double, longitude: Double)
}

class ScoreboardActivity : AppCompatActivity(), OnScoreSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scoreboard)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.scoreboard_FRAME_table, ScoreTableFragment())
                // Plan 12 will add: .replace(R.id.scoreboard_FRAME_map, ScoreMapFragment())
                .commit()
        }
    }

    override fun onScoreSelected(latitude: Double, longitude: Double) {
        // Plan 12 will route this to ScoreMapFragment
        // val mapFragment = supportFragmentManager.findFragmentById(R.id.scoreboard_FRAME_map) as? ScoreMapFragment
        // mapFragment?.centerOn(latitude, longitude)
    }
}
