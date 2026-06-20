# Feature Plan 08 — Tilt Control (Accelerometer Lane Switching)

**Project:** `avoided_race_app`  
**Working dir:** `/Users/giladshekalim/repo/androidCourse/avoided_race_app`  
**Commit scope:** Implement accelerometer-based lane switching in `MainActivity`. When tilt mode is selected, tilting the phone past a threshold continuously shifts lanes. Higher tilt angle = shorter cooldown between lane shifts.

> **Prerequisites:**
> - **Plan 07 (Menu Screen)** must be done first. This plan reads `controlMode` from the Intent extra set by `MenuActivity`. It also assumes the game Runnable is stored as a named field (`gameRunnable`) so `onPause`/`onResume` can manage both the game loop and the sensor independently.

> **Context files:**
> - `CODE.md` → "`initViews()` — Input Handling" — the FAB left/right buttons must be hidden in tilt mode.
> - `LAYOUT.md` → "[5] Control Buttons (FABs)" — describes how the two FABs are positioned; hiding them requires only `visibility = View.GONE`.
> - `PROJECT.md` → "Critical Constraints" — the Handler loop has no pause mechanism; same caveat applies to the sensor — it must be unregistered in `onPause`.

---

## What This Feature Does

- Registers an **accelerometer sensor** in `MainActivity`
- When `controlMode == CONTROL_TILT`:
  - The LEFT and RIGHT FABs are hidden (`View.GONE`)
  - Phone tilt right → car moves right; tilt left → car moves left
  - Holding the tilt past the threshold continuously switches lanes, with a cooldown
  - Higher tilt angle → shorter cooldown (faster repeated switching)
  - The sensor is unregistered on `onPause()` and re-registered on `onResume()`
- When `controlMode == CONTROL_BUTTONS` (default): no changes to existing behavior

---

## Hidden / Non-Obvious Steps

1. **Phone portrait orientation and the accelerometer X-axis:** In portrait mode, `SensorEvent.values[0]` is the X-axis. Tilting the phone to the **right** gives a **negative** X value; tilting **left** gives a **positive** X value (gravity pulls in the opposite direction of the tilt). The thresholds below account for this — verify on a real device and swap signs if needed.

2. **Continuous switching needs its own Handler:** The sensor fires `onSensorChanged` many times per second. The lane-switch logic must use a **separate cooldown Handler** to limit how often the car actually moves — otherwise one tilt fires 30+ switches per second.

3. **Cooldown calculation:** The base cooldown is 500ms. As the absolute tilt value grows beyond the threshold, the cooldown decreases. The formula: `cooldown = max(150L, 500L - ((|tilt| - THRESHOLD) * 50).toLong())`.

4. **One-direction lock per sensor read:** The Runnable approach posts a "move" callback. The `onSensorChanged` callback must cancel any pending move in the opposite direction before posting a new one — otherwise rapid micro-tilts in both directions will queue conflicting moves.

5. **Sensor accuracy changes:** Implement `onAccuracyChanged` even if empty — required by the `SensorEventListener` interface.

6. **Unregister in `onPause()`, re-register in `onResume()`** — if the game is paused (user switches app), the sensor keeps firing otherwise. Also cancel the tilt Handler in `onPause`.

---

## Files to Modify

| File | Change |
|---|---|
| `app/src/main/java/com/example/avoided_race_app/MainActivity.kt` | Add `SensorManager`, `SensorEventListener`, tilt Handler, `onPause`/`onResume` |

No layout changes. No new files.

---

## Step-by-Step Implementation

### Step 1 — Add imports to `MainActivity.kt`

```kotlin
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.View
```

---

### Step 2 — Add tilt fields to `MainActivity`

Add near the top of the class, alongside other fields:

```kotlin
private lateinit var sensorManager: SensorManager
private var accelerometer: Sensor? = null

private val tiltHandler = Handler(Looper.getMainLooper())
private var tiltRunnable: Runnable? = null

private val TILT_THRESHOLD = 3.0f   // m/s² — tilt must exceed this to trigger a switch
```

---

### Step 3 — Initialize sensor in `onCreate()`

After reading `controlMode` from Intent (set in Plan 07), add:

```kotlin
sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
```

---

### Step 4 — Show/hide FABs based on mode

In `initViews()`, after the FAB click listeners are set up, add:

```kotlin
if (controlMode == GameSettings.CONTROL_TILT) {
    main_BTN_left.visibility = View.GONE
    main_BTN_right.visibility = View.GONE
}
```

The FABs are `ExtendedFloatingActionButton`s. `View.GONE` removes them from layout entirely, freeing the screen space.

---

### Step 5 — Implement `SensorEventListener`

Add the listener as an inner object in `MainActivity`:

```kotlin
private val sensorEventListener = object : SensorEventListener {

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* required, unused */ }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        if (controlMode != GameSettings.CONTROL_TILT) return

        val tiltX = event.values[0]
        // Positive X = phone tilted LEFT → move car left
        // Negative X = phone tilted RIGHT → move car right

        when {
            tiltX > TILT_THRESHOLD -> scheduleTiltMove(direction = -1, tiltMagnitude = tiltX)
            tiltX < -TILT_THRESHOLD -> scheduleTiltMove(direction = 1, tiltMagnitude = -tiltX)
            else -> cancelTiltMove()
        }
    }
}
```

---

### Step 6 — Add `scheduleTiltMove()` and `cancelTiltMove()`

```kotlin
private fun scheduleTiltMove(direction: Int, tiltMagnitude: Float) {
    // Cancel any existing pending tilt move (prevents opposite-direction conflict)
    tiltRunnable?.let { tiltHandler.removeCallbacks(it) }

    val cooldown = maxOf(150L, 500L - ((tiltMagnitude - TILT_THRESHOLD) * 50).toLong())

    tiltRunnable = object : Runnable {
        override fun run() {
            moveCar(direction)
            // Reschedule as long as listener keeps calling scheduleTiltMove
            // (it won't if tilt drops below threshold, due to cancelTiltMove)
            tiltHandler.postDelayed(this, cooldown)
        }
    }
    // Post with 0 delay for immediate first switch, then cooldown takes effect
    tiltHandler.post(tiltRunnable!!)
}

private fun cancelTiltMove() {
    tiltRunnable?.let { tiltHandler.removeCallbacks(it) }
    tiltRunnable = null
}
```

---

### Step 7 — Extract lane-move logic into `moveCar()`

The current left/right logic is inline in the FAB click listeners (Plan 07 did not change this). Extract it into a shared method:

```kotlin
private fun moveCar(direction: Int) {
    // direction: -1 = left, +1 = right
    val newLane = currentCarLane + direction
    if (newLane in 0 until COLS) {
        currentCarLane = newLane
        refreshUI()
    }
}
```

Then update the FAB click listeners to call `moveCar()` instead of inlining:

```kotlin
main_BTN_left.setOnClickListener { moveCar(-1) }
main_BTN_right.setOnClickListener { moveCar(1) }
```

This ensures both input modes call identical lane-switch logic.

---

### Step 8 — Register/unregister sensor in lifecycle methods

```kotlin
override fun onResume() {
    super.onResume()
    if (controlMode == GameSettings.CONTROL_TILT) {
        accelerometer?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_GAME  // ~20ms — fast enough for responsive tilt
            )
        }
    }
}

override fun onPause() {
    super.onPause()
    sensorManager.unregisterListener(sensorEventListener)
    cancelTiltMove()
}
```

`SENSOR_DELAY_GAME` (~20ms interval) gives responsive tilt without overwhelming the processor.

---

### Step 9 — Add tilt cleanup to `onDestroy()`

In the existing `onDestroy()` (added in Plan 07), add:

```kotlin
override fun onDestroy() {
    super.onDestroy()
    if (::gameRunnable.isInitialized) handler.removeCallbacks(gameRunnable)
    cancelTiltMove()
    sensorManager.unregisterListener(sensorEventListener)
    // soundPool.release() — if Plan 03 was implemented
    // stopOdometer()      — if Plan 06 was implemented
}
```

---

### Step 10 — Verify

Test on a **physical device** (emulators rarely support accelerometer):
- [ ] In Buttons mode: FABs are visible, taps work, sensor not active
- [ ] In Tilt mode: FABs are hidden, tilting the device right moves car right
- [ ] Tilting left moves car left
- [ ] Holding a tilt continuously shifts lanes with the cooldown
- [ ] Steeper tilt = faster repeated switching (shorter cooldown)
- [ ] Returning phone to level stops the lane switching
- [ ] Lane boundaries still work (can't go past lane 0 or COLS-1)
- [ ] No crash when switching apps mid-game (sensor properly unregistered)

---

## Commit Message

```
Add accelerometer tilt control for lane switching

SensorEventListener on TYPE_ACCELEROMETER drives continuous lane 
switching when tilt mode is selected. Higher tilt angle shortens 
the cooldown between switches (min 150ms). FABs hidden in tilt mode. 
Sensor unregistered in onPause/onDestroy to prevent battery drain.
```
