package com.example.avoided_race_app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MainActivity : AppCompatActivity() {

    // 1. State Variables
    private var currentLane = 1 // 0 = Left, 1 = Center, 2 = Right

    // 2. UI Components - Buttons
    private lateinit var main_BTN_left: ExtendedFloatingActionButton
    private lateinit var main_BTN_right: ExtendedFloatingActionButton
    
    // 3. UI Components - Cars (Explicitly defined one by one)
    private lateinit var main_IMG_car_0: AppCompatImageView
    private lateinit var main_IMG_car_1: AppCompatImageView
    private lateinit var main_IMG_car_2: AppCompatImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViews()
        initViews()
    }

    /**
     * Connects XML IDs to Kotlin variables (Explicitly)
     */
    private fun findViews() {
        main_BTN_left = findViewById(R.id.main_BTN_left)
        main_BTN_right = findViewById(R.id.main_BTN_right)

        main_IMG_car_0 = findViewById(R.id.main_IMG_car_0)
        main_IMG_car_1 = findViewById(R.id.main_IMG_car_1)
        main_IMG_car_2 = findViewById(R.id.main_IMG_car_2)
    }

    /**
     * Sets up button click listeners
     */
    private fun initViews() {
        main_BTN_left.setOnClickListener {
            // Move Left logic
            if (currentLane > 0) {
                currentLane--
                updateCarUI()
            }
        }

        main_BTN_right.setOnClickListener {
            // Move Right logic
            if (currentLane < 2) {
                currentLane++
                updateCarUI()
            }
        }
    }

    /**
     * Updates the visibility of the car ImageViews (Simple IF-ELSE style)
     */
    private fun updateCarUI() {
        // First, hide ALL cars
        main_IMG_car_0.visibility = View.INVISIBLE
        main_IMG_car_1.visibility = View.INVISIBLE
        main_IMG_car_2.visibility = View.INVISIBLE

        // Then, show ONLY the one matching the currentLane
        if (currentLane == 0) {
            main_IMG_car_0.visibility = View.VISIBLE
        } else if (currentLane == 1) {
            main_IMG_car_1.visibility = View.VISIBLE
        } else if (currentLane == 2) {
            main_IMG_car_2.visibility = View.VISIBLE
        }
    }
}
