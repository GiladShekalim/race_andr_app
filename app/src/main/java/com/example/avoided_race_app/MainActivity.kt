package com.example.avoided_race_app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MainActivity : AppCompatActivity() {

    // 1. State Variables
    private var currentLane = 1 // 0 = Left, 1 = Center, 2 = Right
    private var currentObstacleRow = -1 // -1 means no obstacle is visible yet

    // 2. UI Components - Buttons
    private lateinit var main_BTN_left: ExtendedFloatingActionButton
    private lateinit var main_BTN_right: ExtendedFloatingActionButton
    
    // 3. UI Components - Cars
    private lateinit var main_IMG_car_0: AppCompatImageView
    private lateinit var main_IMG_car_1: AppCompatImageView
    private lateinit var main_IMG_car_2: AppCompatImageView

    // 4. UI Components - Obstacles (Left Lane only for Phase 3)
    private lateinit var main_IMG_obstacle_0_0: AppCompatImageView
    private lateinit var main_IMG_obstacle_1_0: AppCompatImageView
    private lateinit var main_IMG_obstacle_2_0: AppCompatImageView
    private lateinit var main_IMG_obstacle_3_0: AppCompatImageView
    private lateinit var main_IMG_obstacle_4_0: AppCompatImageView
    private lateinit var main_IMG_obstacle_5_0: AppCompatImageView
    private lateinit var main_IMG_obstacle_6_0: AppCompatImageView

    // 5. Timer Components
    private val handler = Handler(Looper.getMainLooper())
    private val DELAY = 1000L // 1 second

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViews()
        initViews()
        startTimer()
    }

    private fun findViews() {
        main_BTN_left = findViewById(R.id.main_BTN_left)
        main_BTN_right = findViewById(R.id.main_BTN_right)

        main_IMG_car_0 = findViewById(R.id.main_IMG_car_0)
        main_IMG_car_1 = findViewById(R.id.main_IMG_car_1)
        main_IMG_car_2 = findViewById(R.id.main_IMG_car_2)

        main_IMG_obstacle_0_0 = findViewById(R.id.main_IMG_obstacle_0_0)
        main_IMG_obstacle_1_0 = findViewById(R.id.main_IMG_obstacle_1_0)
        main_IMG_obstacle_2_0 = findViewById(R.id.main_IMG_obstacle_2_0)
        main_IMG_obstacle_3_0 = findViewById(R.id.main_IMG_obstacle_3_0)
        main_IMG_obstacle_4_0 = findViewById(R.id.main_IMG_obstacle_4_0)
        main_IMG_obstacle_5_0 = findViewById(R.id.main_IMG_obstacle_5_0)
        main_IMG_obstacle_6_0 = findViewById(R.id.main_IMG_obstacle_6_0)
    }

    private fun initViews() {
        main_BTN_left.setOnClickListener {
            if (currentLane > 0) {
                currentLane--
                updateCarUI()
            }
        }

        main_BTN_right.setOnClickListener {
            if (currentLane < 2) {
                currentLane++
                updateCarUI()
            }
        }
    }

    private fun updateCarUI() {
        main_IMG_car_0.visibility = View.INVISIBLE
        main_IMG_car_1.visibility = View.INVISIBLE
        main_IMG_car_2.visibility = View.INVISIBLE

        if (currentLane == 0) {
            main_IMG_car_0.visibility = View.VISIBLE
        } else if (currentLane == 1) {
            main_IMG_car_1.visibility = View.VISIBLE
        } else if (currentLane == 2) {
            main_IMG_car_2.visibility = View.VISIBLE
        }
    }

    /**
     * Starts the game loop
     */
    private fun startTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                moveObstacleDown()
                handler.postDelayed(this, DELAY) // Repeat every 1 second
            }
        }, DELAY)
    }

    /**
     * Logic to increment row and reset to top
     */
    private fun moveObstacleDown() {
        currentObstacleRow++

        // If it goes past the last row (6), reset to the top
        if (currentObstacleRow > 6) {
            currentObstacleRow = 0
        }

        updateObstacleUI()
    }

    /**
     * Updates visibility of left-lane obstacles based on currentObstacleRow
     */
    private fun updateObstacleUI() {
        // First, hide all obstacles in this lane
        main_IMG_obstacle_0_0.visibility = View.INVISIBLE
        main_IMG_obstacle_1_0.visibility = View.INVISIBLE
        main_IMG_obstacle_2_0.visibility = View.INVISIBLE
        main_IMG_obstacle_3_0.visibility = View.INVISIBLE
        main_IMG_obstacle_4_0.visibility = View.INVISIBLE
        main_IMG_obstacle_5_0.visibility = View.INVISIBLE
        main_IMG_obstacle_6_0.visibility = View.INVISIBLE

        // Show only the one at the current row
        if (currentObstacleRow == 0) {
            main_IMG_obstacle_0_0.visibility = View.VISIBLE
        } else if (currentObstacleRow == 1) {
            main_IMG_obstacle_1_0.visibility = View.VISIBLE
        } else if (currentObstacleRow == 2) {
            main_IMG_obstacle_2_0.visibility = View.VISIBLE
        } else if (currentObstacleRow == 3) {
            main_IMG_obstacle_3_0.visibility = View.VISIBLE
        } else if (currentObstacleRow == 4) {
            main_IMG_obstacle_4_0.visibility = View.VISIBLE
        } else if (currentObstacleRow == 5) {
            main_IMG_obstacle_5_0.visibility = View.VISIBLE
        } else if (currentObstacleRow == 6) {
            main_IMG_obstacle_6_0.visibility = View.VISIBLE
        }
    }
}
