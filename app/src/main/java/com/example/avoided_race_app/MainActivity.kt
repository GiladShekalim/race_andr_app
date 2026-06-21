package com.example.avoided_race_app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.avoided_race_app.db.ScoreEntry
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // 1. Logic Manager (SOLID: Separate Logic from UI)
    private lateinit var logicManager: LogicManager
    private val ROWS = 9
    private val COLS = 5

    // 2. State Variables
    private var currentCarLane = 2 // center of 5 lanes (0, 1, 2, 3, 4)
    private var lives = 3
    private var score = 0
    private var controlMode: String = GameSettings.CONTROL_BUTTONS
    private var pace: String = GameSettings.PACE_STEADY

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
    private val BASE_DELAY = 500L
    private var DELAY = BASE_DELAY
    private lateinit var gameRunnable: Runnable
    private val odometerHandler = Handler(Looper.getMainLooper())
    private lateinit var odometerRunnable: Runnable
    private val rampHandler = Handler(Looper.getMainLooper())
    private lateinit var rampRunnable: Runnable
    private val RAMP_INTERVAL = 10_000L
    private val RAMP_STEP = 25L
    private val MIN_DELAY = 150L

    // 6. Tilt / Sensor
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val tiltHandler = Handler(Looper.getMainLooper())
    private var tiltRunnable: Runnable? = null
    private val TILT_THRESHOLD = 3.0f

    // 7. Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null
    private lateinit var locationCallback: LocationCallback

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

        controlMode = intent.getStringExtra(GameSettings.EXTRA_CONTROL_MODE) ?: GameSettings.CONTROL_BUTTONS
        pace = intent.getStringExtra(GameSettings.EXTRA_PACE) ?: GameSettings.PACE_STEADY

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // LogicManager handles 8 rows: 7 for obstacles + 1 for collision check
        logicManager = LogicManager(ROWS + 1, COLS)
        logicManager.setCoinResource(R.drawable.coin)
        
        findViews()
        initViews()
        startTimer()
        startOdometer()
        if (pace == GameSettings.PACE_FASTENING) startRamp()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()
    }

    private fun findViews() {
        findViewById<android.widget.ImageButton>(R.id.main_BTN_back).setOnClickListener { finish() }
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
        main_BTN_left.setOnClickListener { moveCar(-1) }
        main_BTN_right.setOnClickListener { moveCar(1) }

        if (controlMode == GameSettings.CONTROL_TILT) {
            main_BTN_left.visibility = View.GONE
            main_BTN_right.visibility = View.GONE
        }

        updateScoreUI()
    }

    private fun startTimer() {
        gameRunnable = object : Runnable {
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

                if (lives <= 0) {
                    resetGame()
                    return
                }

                handler.postDelayed(this, DELAY)
            }
        }
        handler.postDelayed(gameRunnable, DELAY)
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
        if (::gameRunnable.isInitialized) handler.removeCallbacks(gameRunnable)
        stopRamp()
        stopOdometer()

        if (main_LBL_money_lost is android.widget.TextView) {
            (main_LBL_money_lost as android.widget.TextView).text = getString(R.string.bankrupt)
        }
        main_LBL_money_lost.visibility = View.VISIBLE

        val lat = lastKnownLocation?.latitude ?: 0.0
        val lng = lastKnownLocation?.longitude ?: 0.0
        val entry = ScoreEntry(
            score = score,
            timestamp = System.currentTimeMillis(),
            latitude = lat,
            longitude = lng
        )
        lifecycleScope.launch(Dispatchers.IO) {
            (application as GameApplication).database.scoreDao().insert(entry)
        }

        handler.postDelayed({ finish() }, 1500)
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        // Seed immediately with the device's last cached fix so short games still get a location
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) lastKnownLocation = location
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                // lastLocation is nullable on API 31+ — use locations list as primary source
                lastKnownLocation = result.locations.lastOrNull() ?: result.lastLocation
                    ?: lastKnownLocation
            }
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2000L
        ).build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    private val sensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* required, unused */ }

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            if (controlMode != GameSettings.CONTROL_TILT) return

            val tiltX = event.values[0]
            when {
                tiltX > TILT_THRESHOLD  -> scheduleTiltMove(direction = -1, tiltMagnitude = tiltX)
                tiltX < -TILT_THRESHOLD -> scheduleTiltMove(direction = 1, tiltMagnitude = -tiltX)
                else                    -> cancelTiltMove()
            }
        }
    }

    private fun scheduleTiltMove(direction: Int, tiltMagnitude: Float) {
        tiltRunnable?.let { tiltHandler.removeCallbacks(it) }
        val cooldown = maxOf(150L, 500L - ((tiltMagnitude - TILT_THRESHOLD) * 50).toLong())
        tiltRunnable = object : Runnable {
            override fun run() {
                moveCar(direction)
                tiltHandler.postDelayed(this, cooldown)
            }
        }
        tiltHandler.post(tiltRunnable!!)
    }

    private fun cancelTiltMove() {
        tiltRunnable?.let { tiltHandler.removeCallbacks(it) }
        tiltRunnable = null
    }

    private fun moveCar(direction: Int) {
        val newLane = currentCarLane + direction
        if (newLane in 0 until COLS) {
            currentCarLane = newLane
            refreshUI()
        }
    }

    override fun onResume() {
        super.onResume()
        if (controlMode == GameSettings.CONTROL_TILT) {
            accelerometer?.let {
                sensorManager.registerListener(
                    sensorEventListener,
                    it,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorEventListener)
        cancelTiltMove()
    }

    private fun startRamp() {
        rampRunnable = object : Runnable {
            override fun run() {
                if (DELAY > MIN_DELAY) {
                    DELAY = maxOf(MIN_DELAY, DELAY - RAMP_STEP)
                    rampHandler.postDelayed(this, RAMP_INTERVAL)
                }
            }
        }
        rampHandler.postDelayed(rampRunnable, RAMP_INTERVAL)
    }

    private fun stopRamp() {
        if (::rampRunnable.isInitialized) rampHandler.removeCallbacks(rampRunnable)
    }

    private fun startOdometer() {
        odometerRunnable = object : Runnable {
            override fun run() {
                score++
                updateScoreUI()
                odometerHandler.postDelayed(this, 1000)
            }
        }
        odometerHandler.postDelayed(odometerRunnable, 1000)
    }

    private fun stopOdometer() {
        odometerHandler.removeCallbacks(odometerRunnable)
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
        if (::gameRunnable.isInitialized) handler.removeCallbacks(gameRunnable)
        stopRamp()
        cancelTiltMove()
        sensorManager.unregisterListener(sensorEventListener)
        stopOdometer()
        soundPool.release()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
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
