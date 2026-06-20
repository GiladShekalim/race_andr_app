# Feature Implementation Guide — `avoided_race_app`

Step-by-step plans for each planned future feature. Each entry lists every file that needs changing and the exact approach, so a future session can implement without re-deriving architecture.

---

## 1. Score Counter

**Goal:** Display how many ticks the player has survived, incrementing every 500ms.

**Files to change:**

`LogicManager.kt`
```kotlin
var score: Int = 0
    private set

fun tick() {
    // ... existing shift logic ...
    score++
}

fun resetScore() { score = 0 }
```

`res/layout/activity_main.xml` — add inside the root `RelativeLayout`, before the overlay text:
```xml
<com.google.android.material.textview.MaterialTextView
    android:id="@+id/main_LBL_score"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_alignParentTop="true"
    android:layout_alignParentEnd="true"
    android:layout_margin="@dimen/default_margin"
    android:textSize="24sp"
    android:textColor="@android:color/white"
    android:text="0" />
```

`MainActivity.kt`
- Add `private lateinit var main_LBL_score: MaterialTextView` (import `com.google.android.material.textview.MaterialTextView`)
- Bind in `findViews()`: `main_LBL_score = findViewById(R.id.main_LBL_score)`
- Update in the game loop after `refreshUI()`: `main_LBL_score.text = logicManager.score.toString()`
- Reset in `resetGame()`: `logicManager.resetScore()` + `main_LBL_score.text = "0"`

---

## 2. High Score Persistence

**Goal:** Persist the best score across app sessions using `SharedPreferences`.

**Prerequisite:** Score counter (Feature 1) must be implemented first.

**New file: `ScoreManager.kt`**
```kotlin
class ScoreManager(context: Context) {
    private val prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)

    var highScore: Int
        get() = prefs.getInt("high_score", 0)
        private set(value) { prefs.edit().putInt("high_score", value).apply() }

    fun submitScore(score: Int) {
        if (score > highScore) highScore = score
    }
}
```

`MainActivity.kt`
- Add `private lateinit var scoreManager: ScoreManager`
- Initialize in `onCreate()`: `scoreManager = ScoreManager(this)`
- Call `scoreManager.submitScore(logicManager.score)` inside `resetGame()` before clearing the score
- Display `scoreManager.highScore` in a new `main_LBL_high_score` `MaterialTextView` (top-start corner)

**No Application class needed** — `ScoreManager` is stateless; a new instance per Activity is fine.

---

## 3. Pause / Resume

**Goal:** Allow the player to pause the game loop and resume it.

**Files to change:**

`MainActivity.kt`
```kotlin
// Store the Runnable so it can be cancelled
private lateinit var gameRunnable: Runnable
private var isPaused = false

private fun startTimer() {
    gameRunnable = object : Runnable {
        override fun run() {
            // ... existing tick logic ...
            handler.postDelayed(this, DELAY)
        }
    }
    handler.postDelayed(gameRunnable, DELAY)
}

fun pauseGame() {
    isPaused = true
    handler.removeCallbacks(gameRunnable)
}

fun resumeGame() {
    if (isPaused) {
        isPaused = false
        handler.postDelayed(gameRunnable, DELAY)
    }
}

override fun onPause() {
    super.onPause()
    pauseGame()
}

override fun onResume() {
    super.onResume()
    resumeGame()
}
```

`res/layout/activity_main.xml` — add a center-bottom FAB between LEFT and RIGHT:
```xml
<com.google.android.material.floatingactionbutton.FloatingActionButton
    android:id="@+id/main_BTN_pause"
    android:layout_alignParentBottom="true"
    android:layout_centerHorizontal="true"
    android:layout_margin="@dimen/default_margin"
    app:srcCompat="@drawable/ic_pause" />
```

Add `ic_pause.xml` to `res/drawable/` (24×24 vector, two vertical bars).

Wire in `initViews()`:
```kotlin
main_BTN_pause.setOnClickListener {
    if (isPaused) resumeGame() else pauseGame()
    main_BTN_pause.setImageResource(
        if (isPaused) R.drawable.ic_play else R.drawable.ic_pause
    )
}
```

---

## 4. Difficulty Scaling

**Goal:** Make the game progressively harder over time — more frequent obstacles and/or faster tick rate.

**Approach A — Spawn rate scaling (simplest):**

`LogicManager.kt`
```kotlin
private var spawnChance = 30  // starts at 30%
private val maxSpawnChance = 70

fun tick() {
    // ... shift logic ...
    if (Random.nextInt(100) < spawnChance) {
        // spawn
    }
    score++
    // Every 20 ticks, increase spawn chance by 5%
    if (score % 20 == 0 && spawnChance < maxSpawnChance) {
        spawnChance += 5
    }
}

fun clearMatrix() {
    // ... zero matrix ...
    spawnChance = 30  // reset on bankrupt
}
```

**Approach B — Tick speed scaling (more dramatic):**

`MainActivity.kt`
```kotlin
private var currentDelay = 500L
private val minDelay = 200L

// In the game loop, after logicManager.tick():
if (logicManager.score % 30 == 0 && currentDelay > minDelay) {
    currentDelay = (currentDelay - 25).coerceAtLeast(minDelay)
}
// Use currentDelay in handler.postDelayed(this, currentDelay)

// In resetGame():
currentDelay = 500L
```

**Recommendation:** Start with Approach A (spawn rate) — it keeps the visual pacing constant while increasing intensity. Add Approach B later for a speed-rush feel at high scores.

---

## 5. Start Screen / Game-Over Screen

**Goal:** Add a distinct start state and a game-over state instead of the instant reset loop.

**New file: `GameState.kt`**
```kotlin
enum class GameState { IDLE, PLAYING, GAME_OVER }
```

**Approach:** Second Activity (simpler than Fragment for this scale)

`StartActivity.kt` — simple screen with app title and "PLAY" button that launches `MainActivity`.

`AndroidManifest.xml`:
```xml
<activity android:name=".StartActivity" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
<activity android:name=".MainActivity" />
```

Remove the `MAIN`/`LAUNCHER` intent filter from `MainActivity`.

`MainActivity.kt` — in `resetGame()`, instead of immediately continuing, show a "BANKRUPT!" overlay with a "PLAY AGAIN" button that re-initializes state. Or call `finish()` and let `StartActivity` handle replay.

---

## 6. Sound Effects

**Goal:** Play a crash sound on collision and optionally an engine hum loop.

**Recommended API:** `SoundPool` (not `MediaPlayer`) — low-latency, handles simultaneous short clips.

`MainActivity.kt`
```kotlin
private lateinit var soundPool: SoundPool
private var crashSoundId: Int = 0

override fun onCreate(...) {
    val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    soundPool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attrs).build()
    crashSoundId = soundPool.load(this, R.raw.crash, 1)
    // ...
}

private fun handleCollision() {
    soundPool.play(crashSoundId, 1f, 1f, 0, 0, 1f)
    // ... existing collision logic ...
}

override fun onDestroy() {
    super.onDestroy()
    soundPool.release()
}
```

**Asset placement:** Add `res/raw/crash.mp3` (or `.ogg` — smaller) for the crash clip.

---

## 7. Sensor Tilt Controls

**Goal:** Let the player tilt the phone left/right to steer instead of tapping buttons.

`MainActivity.kt`
```kotlin
private lateinit var sensorManager: SensorManager
private var accelerometer: Sensor? = null
private val TILT_THRESHOLD = 3.0f  // tune this

private val sensorListener = object : SensorEventListener {
    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]  // negative = tilt right, positive = tilt left
        if (x > TILT_THRESHOLD && currentCarLane > 0) {
            currentCarLane--
            refreshUI()
        } else if (x < -TILT_THRESHOLD && currentCarLane < COLS - 1) {
            currentCarLane++
            refreshUI()
        }
    }
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}

override fun onResume() {
    super.onResume()
    sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
}

override fun onPause() {
    super.onPause()
    sensorManager.unregisterListener(sensorListener)
}
```

Initialize in `onCreate()`:
```kotlin
sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
```

**Debounce concern:** `SENSOR_DELAY_GAME` fires ~50 times/sec. Without debouncing, a single tilt can move multiple lanes. Add a cooldown:
```kotlin
private var lastLaneChangeTime = 0L
// Inside onSensorChanged, before changing lane:
val now = System.currentTimeMillis()
if (now - lastLaneChangeTime < 300) return
lastLaneChangeTime = now
```

---

## 8. Smooth Animation (Object-based instead of discrete grid)

**Goal:** Replace the discrete row-shift illusion with smooth Y-axis translation of obstacle icons.

This is a significant architectural change and should not be attempted until the game logic is otherwise stable.

**Approach:**
- Each active hazard becomes a `data class Hazard(val lane: Int, var yFraction: Float, val drawableId: Int)` managed by `LogicManager`.
- On each tick, `yFraction` increments by `1/7` (one row's worth of height).
- `MainActivity` translates each hazard's `AppCompatImageView` using `view.translationY = yFraction * matrixHeight`.
- Collision detection changes from row-index check to `yFraction >= 1.0f`.
- The 21-cell static grid approach is abandoned in favor of a recycled pool of `ImageView`s.

**Estimated scope:** ~2–3 hours of refactoring. Affects `LogicManager`, `MainActivity`, and `activity_main.xml` structure.
