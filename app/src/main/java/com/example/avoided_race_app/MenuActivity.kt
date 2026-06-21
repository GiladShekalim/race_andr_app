package com.example.avoided_race_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MenuActivity : AppCompatActivity() {

    private lateinit var menu_RG_control_mode: RadioGroup
    private lateinit var menu_RG_pace: RadioGroup
    private lateinit var menu_BTN_start: ExtendedFloatingActionButton
    private lateinit var menu_BTN_scoreboard: ExtendedFloatingActionButton

    private val LOCATION_PERMISSION_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        menu_RG_control_mode = findViewById(R.id.menu_RG_control_mode)
        menu_RG_pace = findViewById(R.id.menu_RG_pace)
        menu_BTN_start = findViewById(R.id.menu_BTN_start)
        menu_BTN_scoreboard = findViewById(R.id.menu_BTN_scoreboard)

        loadSettings()
        requestLocationPermission()

        menu_BTN_start.setOnClickListener { startGame() }
        menu_BTN_scoreboard.setOnClickListener {
            startActivity(Intent(this, ScoreboardActivity::class.java))
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    override fun onStop() {
        super.onStop()
        saveSettings()
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences(GameSettings.PREFS_NAME, MODE_PRIVATE)
        val controlMode = prefs.getString(GameSettings.KEY_CONTROL_MODE, GameSettings.CONTROL_BUTTONS)
        val pace = prefs.getString(GameSettings.KEY_PACE, GameSettings.PACE_STEADY)

        if (controlMode == GameSettings.CONTROL_TILT)
            menu_RG_control_mode.check(R.id.menu_RB_tilt)
        else
            menu_RG_control_mode.check(R.id.menu_RB_buttons)

        if (pace == GameSettings.PACE_FASTENING)
            menu_RG_pace.check(R.id.menu_RB_fastening)
        else
            menu_RG_pace.check(R.id.menu_RB_steady)
    }

    private fun saveSettings() {
        val controlMode = if (menu_RG_control_mode.checkedRadioButtonId == R.id.menu_RB_tilt)
            GameSettings.CONTROL_TILT else GameSettings.CONTROL_BUTTONS
        val pace = if (menu_RG_pace.checkedRadioButtonId == R.id.menu_RB_fastening)
            GameSettings.PACE_FASTENING else GameSettings.PACE_STEADY

        getSharedPreferences(GameSettings.PREFS_NAME, MODE_PRIVATE).edit()
            .putString(GameSettings.KEY_CONTROL_MODE, controlMode)
            .putString(GameSettings.KEY_PACE, pace)
            .apply()
    }

    private fun startGame() {
        saveSettings()
        val controlMode = if (menu_RG_control_mode.checkedRadioButtonId == R.id.menu_RB_tilt)
            GameSettings.CONTROL_TILT else GameSettings.CONTROL_BUTTONS
        val pace = if (menu_RG_pace.checkedRadioButtonId == R.id.menu_RB_fastening)
            GameSettings.PACE_FASTENING else GameSettings.PACE_STEADY

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(GameSettings.EXTRA_CONTROL_MODE, controlMode)
            putExtra(GameSettings.EXTRA_PACE, pace)
        }
        startActivity(intent)
    }
}
