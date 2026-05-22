package com.example.avoided_race_app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MainActivity : AppCompatActivity() {

    // 1. Logic Manager (SOLID: Separate Logic from UI)
    private lateinit var logicManager: LogicManager
    private val ROWS = 7
    private val COLS = 3

    // 2. State Variables
    private var currentCarLane = 1 // 0 = Left, 1 = Center, 2 = Right

    // 3. UI Components
    private lateinit var main_BTN_left: ExtendedFloatingActionButton
    private lateinit var main_BTN_right: ExtendedFloatingActionButton
    
    private lateinit var carImages: Array<AppCompatImageView>
    
    // Matrix of ImageViews to match our logic grid
    private lateinit var obstacleMatrix: Array<Array<AppCompatImageView>>

    // 4. Timer Components
    private val handler = Handler(Looper.getMainLooper())
    private val DELAY = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logicManager = LogicManager(ROWS, COLS)
        
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
            findViewById(R.id.main_IMG_car_2)
        )

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
            if (currentCarLane < 2) {
                currentCarLane++
                refreshUI()
            }
        }
    }

    private fun startTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                logicManager.tick()
                refreshUI()
                handler.postDelayed(this, DELAY)
            }
        }, DELAY)
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
                val resId = logicManager.getResourceId(r, c)
                if (resId != 0) {
                    obstacleMatrix[r][c].setImageResource(resId)
                    obstacleMatrix[r][c].visibility = View.VISIBLE
                } else {
                    obstacleMatrix[r][c].visibility = View.INVISIBLE
                }
            }
        }
    }
}
