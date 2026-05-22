package com.example.avoided_race_app

import kotlin.random.Random

/**
 * LogicManager handles the game state and rules.
 * Adheres to SOLID (Single Responsibility Principle) by separating game logic from UI.
 */
class LogicManager(private val rows: Int, private val cols: Int) {

    // 2D Array representing the grid. Stores the resource ID of the hazard, or 0 if empty.
    private val matrix: Array<IntArray> = Array(rows) { IntArray(cols) { 0 } }
    
    // List of available hazard drawables
    private val hazardResources = listOf(
        R.drawable.fuel,
        R.drawable.clinic,
        R.drawable.no_tax,
        R.drawable.broken_car,
        R.drawable.bankrupt
    )

    /**
     * Advances the game state by one tick.
     * Moves all obstacles down one row and spawns a new one at the top.
     */
    fun tick() {
        // 1. Move everything down starting from the bottom
        for (r in rows - 1 downTo 1) {
            for (c in 0 until cols) {
                matrix[r][c] = matrix[r - 1][c]
            }
        }

        // 2. Clear the top row
        for (c in 0 until cols) {
            matrix[0][c] = 0
        }

        // 3. SPATIAL & TEMPORAL RANDOMIZATION:
        // Instead of spawning every tick, we use a "spawn chance" (e.g., 30% chance)
        // This creates irregular timing between hazards.
        if (Random.nextInt(100) < 30) {
            val randomLane = Random.nextInt(cols)
            val randomHazard = hazardResources[Random.nextInt(hazardResources.size)]
            
            // All hazards ALWAYS start at the top row (Row 0)
            matrix[0][randomLane] = randomHazard
        }
    }

    /**
     * Returns the resource ID at a specific cell.
     */
    fun getResourceId(row: Int, col: Int): Int {
        return matrix[row][col]
    }

    /**
     * Checks if a collision occurred at the bottom row.
     */
    fun checkCollision(carLane: Int): Boolean {
        // If the bottom row (row 6) in the car's lane has a hazard (> 0), it's a crash!
        return matrix[rows - 1][carLane] > 0
    }

    /**
     * Clears all hazards from the matrix (Resets the board)
     */
    fun clearMatrix() {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                matrix[r][c] = 0
            }
        }
    }
}
