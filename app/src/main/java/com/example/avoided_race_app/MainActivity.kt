package com.example.avoided_race_app

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MainActivity : AppCompatActivity() {

    // 1. Logic Manager (SOLID: Separate Logic from UI)
    private lateinit var logicManager: LogicManager
    private val ROWS = 9
    private val COLS = 5

    // 2. State Variables
    private var currentCarLane = 2 // center of 5 lanes (0, 1, 2, 3, 4)
    private var lives = 3
    private var score = 0

    // 3. UI Components
    private lateinit var main_BTN_left: ExtendedFloatingActionButton
    private lateinit var main_BTN_right: ExtendedFloatingActionButton
    
    private lateinit var carImages: Array<AppCompatImageView>
    
    // Matrix of ImageViews to match our logic grid
    private lateinit var obstacleMatrix: Array<Array<AppCompatImageView>>

    // Lives (Hearts/Money Bags)
    private lateinit var hearts: Array<AppCompatImageView>

    // Feedback UI
    private lateinit var main_LBL_money_lost: View
    private lateinit var main_LBL_score: com.google.android.material.textview.MaterialTextView

    // 4. Sound
    private lateinit var soundPool: SoundPool
    private var crashSoundId: Int = 0
    private var coinSoundId: Int = 0
    private var soundLoaded: Boolean = false

    // 5. Timer Components
    private val handler = Handler(Looper.getMainLooper())
    private val DELAY = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundLoaded = true
        }

        crashSoundId = soundPool.load(this, R.raw.crash_sound, 1)
        coinSoundId = soundPool.load(this, R.raw.coin_sound, 1)

        // LogicManager handles 8 rows: 7 for obstacles + 1 for collision check
        logicManager = LogicManager(ROWS + 1, COLS)
        logicManager.setCoinResource(R.drawable.coin)
        
        findViews()
        initViews()
        startTimer()
    }

    private fun findViews() {
        main_BTN_left = findViewById(R.id.main_BTN_left)
        main_BTN_right = findViewById(R.id.main_BTN_right)

        // Find Car Images
        carImages = arrayOf(
            findViewById(R.id.main_IMG_car_0),
            findViewById(R.id.main_IMG_car_1),
            findViewById(R.id.main_IMG_car_2),
            findViewById(R.id.main_IMG_car_3),
            findViewById(R.id.main_IMG_car_4)
        )

        // Find Heart Images
        hearts = arrayOf(
            findViewById(R.id.main_IMG_heart0),
            findViewById(R.id.main_IMG_heart1),
            findViewById(R.id.main_IMG_heart2)
        )

        // Find Feedback Text
        main_LBL_money_lost = findViewById(R.id.main_LBL_money_lost)
        main_LBL_score = findViewById(R.id.main_LBL_score)

        // Initialize the 2D Array for Obstacles
        obstacleMatrix = Array(ROWS) { r ->
            Array(COLS) { c ->
                val id = resources.getIdentifier("main_IMG_obstacle_${r}_${c}", "id", packageName)
                findViewById(id)
            }
        }
    }

    private fun initViews() {
        main_BTN_left.setOnClickListener {
            if (currentCarLane > 0) {
                currentCarLane--
                refreshUI()
            }
        }

        main_BTN_right.setOnClickListener {
            if (currentCarLane < COLS - 1) {
                currentCarLane++
                refreshUI()
            }
        }
        updateScoreUI()
    }

    private fun startTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                logicManager.tick()
                logicManager.tickCoins()

                if (logicManager.checkCollision(currentCarLane)) {
                    handleCollision()
                } else if (logicManager.checkCoinCollection(currentCarLane)) {
                    score += 10
                    updateScoreUI()
                    playCoinSound()
                }

                refreshUI()

                // If no bags are left, reset the game and keep going
                if (lives <= 0) {
                    resetGame()
                }
                
                // Keep the loop going (endless)
                handler.postDelayed(this, DELAY)
            }
        }, DELAY)
    }

    private fun handleCollision() {
        // Show the "MONEY LOST!" text
        if (main_LBL_money_lost is android.widget.TextView) {
            (main_LBL_money_lost as android.widget.TextView).text = getString(R.string.money_lost)
        }
        main_LBL_money_lost.visibility = View.VISIBLE
        
        // Hide it after 1 second (1000ms)
        handler.postDelayed({
            main_LBL_money_lost.visibility = View.GONE
        }, 1000)

        // Temporary log before vibration
        Log.d("Collision", "Crash detected! Triggering vibration.")
        vibrate()
        playCrashSound()

        // Remove one money bag at a time
        lives--
        updateHeartsUI()
    }

    private fun resetGame() {
        // Show "BANKRUPT!" text
        if (main_LBL_money_lost is android.widget.TextView) {
            (main_LBL_money_lost as android.widget.TextView).text = getString(R.string.bankrupt)
        }
        main_LBL_money_lost.visibility = View.VISIBLE
        
        // Hide it after 1.5 seconds
        handler.postDelayed({
            main_LBL_money_lost.visibility = View.GONE
        }, 1500)

        // Make all hazards invisible
        logicManager.clearMatrix()
        
        // Reset lives to original (3)
        lives = 3
        updateHeartsUI()
        score = 0
        updateScoreUI()
    }

    private fun updateScoreUI() {
        main_LBL_score.text = getString(R.string.score_label, score)
    }

    private fun playCrashSound() {
        if (soundLoaded) {
            soundPool.play(crashSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    private fun playCoinSound() {
        if (soundLoaded) {
            soundPool.play(coinSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    private fun vibrate() {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(500)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
    }

    private fun updateHeartsUI() {
        for (i in hearts.indices) {
            // If lives is 2, heart index 2 (the 3rd one) becomes invisible
            // If lives is 1, heart index 1 and 2 become invisible
            // If lives is 0, all become invisible
            if (i >= lives) {
                hearts[i].visibility = View.INVISIBLE
            } else {
                hearts[i].visibility = View.VISIBLE
            }
        }
    }

    /**
     * Updates the entire screen (Car and Matrix)
     */
    private fun refreshUI() {
        // 1. Update Car Visibility
        for (i in carImages.indices) {
            carImages[i].visibility = if (i == currentCarLane) View.VISIBLE else View.INVISIBLE
        }

        // 2. Update Obstacle Matrix
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                val hazardResId = logicManager.getResourceId(r, c)
                val coinResId = logicManager.getCoinResourceId(r, c)
                val resId = if (hazardResId > 0) hazardResId else coinResId

                obstacleMatrix[r][c].apply {
                    if (resId > 0) {
                        setImageResource(resId)
                        visibility = View.VISIBLE
                    } else {
                        visibility = View.INVISIBLE
                    }
                }
            }
        }
    }
}
