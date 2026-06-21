# Feature Plan 07 — Menu Screen + All Three Settings UI

**Project:** `avoided_race_app`  
**Working dir:** `/Users/giladshekalim/repo/androidCourse/avoided_race_app`  
**Commit scope:** Add a `MenuActivity` as the new app entry point, containing all three settings (control mode, scoreboard button, pace mode), wired to SharedPreferences. Update `MainActivity` to read settings from Intent, stop the game loop before exiting, and `finish()` back to menu on bankrupt instead of resetting in-place.

> **Prerequisites:** Features 02–06 (five lanes, crash sound, score, coins + coin sound, odometer) **must** be implemented before this plan. This plan rewrites both `startTimer()` and `resetGame()` — if any prior feature is not in the code yet, the refactored versions shown here will lose that feature silently.

> **Actual current state of `MainActivity.kt` before this plan runs:**
> - `private val DELAY = 500L` — fixed constant (not yet a var; that changes in Plan 09)
> - `ROWS = 9`, `COLS = 5` (Plan 02)
> - `private lateinit var soundPool: SoundPool`, `crashSoundId`, `coinSoundId`, `soundLoaded` (Plans 03 + 05)
> - `score = 0`, `updateScoreUI()` (Plan 04)
> - `coinMatrix` wired via `logicManager.setCoinResource(R.drawable.coin)` (Plan 05)
> - `odometerHandler`, `odometerRunnable`, `startOdometer()`, `stopOdometer()` (Plan 06)
> - `onDestroy()` already calls `soundPool.release()` and `stopOdometer()` (Plans 03 + 06)
> - `startTimer()` uses an **anonymous** inline Runnable — Step 6a extracts it to a named field
> - `resetGame()` resets in-place (lives, score, clearMatrix, odometer) — Step 6c replaces it with finish() flow

> **Context files:**
> - `CODE.md` → "`resetGame()`" — this method is replaced with a `finish()` flow here.
> - `CODE.md` → "`startTimer()` — Game Loop" — the Runnable must be stored as a named field before it can be cancelled on `onDestroy()`.
> - `LAYOUT.md` → "Layer Stacking" — `MenuActivity` uses a separate layout, does not touch `activity_main.xml`.
> - `26B-10208-L06.md` (reference repo) — navigation between activities and SharedPreferences pattern for settings state.
> - `26A-10208-L05.md` (reference repo) — SharedPreferences V3 singleton pattern for settings persistence.

---

## What This Feature Does

- Creates `MenuActivity` as the **new launcher Activity**
- Menu has three settings: **Control Mode** (Buttons / Tilt), **Pace** (Steady / Fastening), and a **Scoreboard** forward button
- Settings are saved to SharedPreferences and restored on next launch
- "Start Game" passes settings to `MainActivity` via `Intent` extras
- `MainActivity` reads the settings, stores the control mode and pace as flags
- On bankrupt: game loop is stopped, control returns to `MenuActivity` via `finish()`
- Creates an **empty placeholder** `ScoreboardActivity` (to avoid crashes when the Scoreboard button is pressed — filled in Plans 11 and 12)

---

## Hidden / Non-Obvious Steps

1. **The game `Runnable` must be stored as a named field** — currently the 500ms tick loop is an anonymous inline Runnable inside `startTimer()`. Before calling `finish()`, you must call `handler.removeCallbacks(gameRunnable)` or the Handler will keep posting to a destroyed Activity and crash the app. This requires refactoring the anonymous Runnable to a named field.

2. **Handler cleanup in `onDestroy()`** — even if `finish()` is called in `resetGame()`, the user can also press the back button mid-game. `onDestroy()` must cancel all running Handlers.

3. **Constants for SharedPreferences keys** — `MenuActivity` writes them, `MainActivity` reads them. Mismatched strings = silently broken settings. Define both the preference file name and all keys as constants in a shared location.

4. **`ScoreboardActivity` must exist** before `MenuActivity` can compile — even if it's empty — or the `startActivity(Intent(this, ScoreboardActivity::class.java))` call won't compile.

5. **AndroidManifest update** — `MenuActivity` must become the launcher (add `MAIN`/`LAUNCHER` intent filter); `MainActivity` must lose its launcher filter but remain declared.

---

## Files to Create

| File | Purpose |
|---|---|
| `app/src/main/java/com/example/avoided_race_app/MenuActivity.kt` | New launcher Activity |
| `app/src/main/res/layout/activity_menu.xml` | Menu screen layout |
| `app/src/main/java/com/example/avoided_race_app/ScoreboardActivity.kt` | Empty placeholder (filled in Plans 11–12) |
| `app/src/main/res/layout/activity_scoreboard.xml` | Empty placeholder layout |
| `app/src/main/java/com/example/avoided_race_app/GameSettings.kt` | SharedPreferences keys + constants |

## Files to Modify

| File | Change |
|---|---|
| `app/src/main/AndroidManifest.xml` | Move launcher to `MenuActivity`, add `ScoreboardActivity` declaration |
| `app/src/main/java/com/example/avoided_race_app/MainActivity.kt` | Store Runnable as field, read Intent extras, finish() on bankrupt, cleanup handlers |

---

## Step-by-Step Implementation

### Step 1 — Create `GameSettings.kt` (shared constants)

File: `app/src/main/java/com/example/avoided_race_app/GameSettings.kt`

```kotlin
package com.example.avoided_race_app

object GameSettings {
    const val PREFS_NAME = "game_settings"

    const val KEY_CONTROL_MODE = "control_mode"
    const val CONTROL_BUTTONS = "buttons"
    const val CONTROL_TILT = "tilt"

    const val KEY_PACE = "pace"
    const val PACE_STEADY = "steady"
    const val PACE_FASTENING = "fastening"

    const val EXTRA_CONTROL_MODE = "extra_control_mode"
    const val EXTRA_PACE = "extra_pace"
}
```

---

### Step 2 — Create `activity_menu.xml`

File: `app/src/main/res/layout/activity_menu.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="32dp"
    android:background="@color/background_color">

    <com.google.android.material.textview.MaterialTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="AVOIDED RACE"
        android:textSize="32sp"
        android:textStyle="bold"
        android:textColor="@android:color/white"
        android:layout_marginBottom="48dp" />

    <!-- Setting 1: Control Mode -->
    <com.google.android.material.textview.MaterialTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Controls"
        android:textSize="16sp"
        android:textColor="@android:color/white"
        android:layout_marginBottom="8dp" />

    <RadioGroup
        android:id="@+id/menu_RG_control_mode"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginBottom="32dp">

        <RadioButton
            android:id="@+id/menu_RB_buttons"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Buttons"
            android:textColor="@android:color/white"
            android:buttonTint="@android:color/white"
            android:layout_marginEnd="24dp" />

        <RadioButton
            android:id="@+id/menu_RB_tilt"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Tilt"
            android:textColor="@android:color/white"
            android:buttonTint="@android:color/white" />
    </RadioGroup>

    <!-- Setting 3: Pace Mode -->
    <com.google.android.material.textview.MaterialTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Pace"
        android:textSize="16sp"
        android:textColor="@android:color/white"
        android:layout_marginBottom="8dp" />

    <RadioGroup
        android:id="@+id/menu_RG_pace"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginBottom="48dp">

        <RadioButton
            android:id="@+id/menu_RB_steady"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Steady"
            android:textColor="@android:color/white"
            android:buttonTint="@android:color/white"
            android:layout_marginEnd="24dp" />

        <RadioButton
            android:id="@+id/menu_RB_fastening"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Fastening"
            android:textColor="@android:color/white"
            android:buttonTint="@android:color/white" />
    </RadioGroup>

    <!-- Start Game -->
    <com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
        android:id="@+id/menu_BTN_start"
        android:layout_width="200dp"
        android:layout_height="wrap_content"
        android:text="START GAME"
        android:layout_marginBottom="16dp" />

    <!-- Scoreboard -->
    <com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
        android:id="@+id/menu_BTN_scoreboard"
        android:layout_width="200dp"
        android:layout_height="wrap_content"
        android:text="SCOREBOARD" />

</LinearLayout>
```

---

### Step 3 — Create `MenuActivity.kt`

File: `app/src/main/java/com/example/avoided_race_app/MenuActivity.kt`

```kotlin
package com.example.avoided_race_app

import android.content.Intent
import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MenuActivity : AppCompatActivity() {

    private lateinit var menu_RG_control_mode: RadioGroup
    private lateinit var menu_RG_pace: RadioGroup
    private lateinit var menu_BTN_start: ExtendedFloatingActionButton
    private lateinit var menu_BTN_scoreboard: ExtendedFloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        menu_RG_control_mode = findViewById(R.id.menu_RG_control_mode)
        menu_RG_pace = findViewById(R.id.menu_RG_pace)
        menu_BTN_start = findViewById(R.id.menu_BTN_start)
        menu_BTN_scoreboard = findViewById(R.id.menu_BTN_scoreboard)

        loadSettings()

        menu_BTN_start.setOnClickListener { startGame() }
        menu_BTN_scoreboard.setOnClickListener {
            startActivity(Intent(this, ScoreboardActivity::class.java))
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
```

---

### Step 4 — Create placeholder `ScoreboardActivity`

File: `app/src/main/java/com/example/avoided_race_app/ScoreboardActivity.kt`

```kotlin
package com.example.avoided_race_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ScoreboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scoreboard)
    }
}
```

File: `app/src/main/res/layout/activity_scoreboard.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/background_color"
    android:gravity="center">

    <com.google.android.material.textview.MaterialTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="SCOREBOARD"
        android:textSize="24sp"
        android:textColor="@android:color/white" />
</LinearLayout>
```

---

### Step 5 — Update `AndroidManifest.xml`

File: `app/src/main/AndroidManifest.xml`

Remove the `<intent-filter>` block from `MainActivity` and add it to `MenuActivity`. Add `ScoreboardActivity` declaration.

```xml
<application ...>

    <!-- NEW: Menu is now the launcher -->
    <activity
        android:name=".MenuActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>

    <!-- Game: no longer launcher -->
    <activity
        android:name=".MainActivity"
        android:exported="false" />

    <!-- Scoreboard: placeholder until Plans 11–12 -->
    <activity
        android:name=".ScoreboardActivity"
        android:exported="false" />

</application>
```

Keep the `<uses-permission android:name="android.permission.VIBRATE" />` line unchanged.

---

### Step 6 — Update `MainActivity.kt`

**6a — Store gameRunnable as a named field:**

Find the anonymous Runnable inside `startTimer()`. The **current actual body** of the anonymous Runnable (after Plans 02–06 are done) is:
```kotlin
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

            if (lives <= 0) {
                resetGame()
            }
            handler.postDelayed(this, DELAY)  // ← BUG: fires even after resetGame()
        }
    }, DELAY)
}
```

Refactor: extract to a named field and fix the post-bankrupt loop bug with `return`:
```kotlin
private lateinit var gameRunnable: Runnable

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
                return  // CRITICAL: stop the loop — handler.postDelayed must NOT fire after resetGame()
            }
            handler.postDelayed(this, DELAY)
        }
    }
    handler.postDelayed(gameRunnable, DELAY)
}
```

The `return` after `resetGame()` is the key bug fix: without it, `handler.postDelayed(this, DELAY)` fires even after `finish()` is scheduled, posting to a destroyed Activity.

> **Note for Plan 09:** Plan 09 changes `DELAY` from `val` to `var`. The gameRunnable already uses `DELAY` as a variable reference, so Plan 09's change is compatible with this extraction — no change to gameRunnable body needed in Plan 09.

**6b — Read settings from Intent:**

Add these fields near the top of the class:
```kotlin
private var controlMode: String = GameSettings.CONTROL_BUTTONS
private var pace: String = GameSettings.PACE_STEADY
```

At the start of `onCreate()`, before `findViews()`:
```kotlin
controlMode = intent.getStringExtra(GameSettings.EXTRA_CONTROL_MODE) ?: GameSettings.CONTROL_BUTTONS
pace = intent.getStringExtra(GameSettings.EXTRA_PACE) ?: GameSettings.PACE_STEADY
```

**6c — Replace `resetGame()` with finish-to-menu flow:**

The **current `resetGame()`** (before this plan) does an in-place reset:
```kotlin
// CURRENT (before Plan 07 — DO NOT keep this)
private fun resetGame() {
    stopOdometer()
    logicManager.clearMatrix()
    lives = 3
    updateHeartsUI()
    score = 0
    updateScoreUI()
    // ... show BANKRUPT! then startOdometer() after 1500ms
}
```

**Replace the entire method** with a finish() flow. No in-place reset is needed because `MainActivity` is recreated fresh on the next game start. Note: `stopOdometer()` is still required to cancel the odometer Handler before returning to menu — without it, the odometer keeps incrementing during the 1500ms BANKRUPT! display and may fire into a destroyed Activity.

```kotlin
private fun resetGame() {
    // Stop all running handlers before returning to menu
    if (::gameRunnable.isInitialized) handler.removeCallbacks(gameRunnable)
    stopOdometer()  // Plan 06 — cancel the +1/sec odometer before finish()

    // Show BANKRUPT! overlay
    if (main_LBL_money_lost is android.widget.TextView) {
        (main_LBL_money_lost as android.widget.TextView).text = getString(R.string.bankrupt)
    }
    main_LBL_money_lost.visibility = View.VISIBLE

    // TODO (Plan 09): stopRamp() goes here when fastening pace is implemented
    // TODO (Plan 10): save score + GPS location here before finishing

    handler.postDelayed({ finish() }, 1500)
}
```

`logicManager.clearMatrix()`, `lives = 3`, `score = 0` are **not** needed here — the fresh `MainActivity` instance starts with clean state every time.

**6d — Update `onDestroy()` for handler cleanup:**

Plans 03 and 06 already added `onDestroy()` with `soundPool.release()` and `stopOdometer()`. This plan extends it to also cancel the gameRunnable. The full `onDestroy()` after this plan:

```kotlin
override fun onDestroy() {
    super.onDestroy()
    if (::gameRunnable.isInitialized) handler.removeCallbacks(gameRunnable)
    stopOdometer()       // from Plan 06 — already present; keep it
    soundPool.release()  // from Plan 03 — already present; keep it
    // TODO (Plan 08): add cancelTiltMove() + sensorManager.unregisterListener() here
    // TODO (Plan 09): add stopRamp() here
    // TODO (Plan 10): add fusedLocationClient.removeLocationUpdates() here
}
```

If the existing `onDestroy()` only has `soundPool.release()` and `stopOdometer()`, add the `gameRunnable` line at the top. Do **not** remove the existing lines.

---

### Step 7 — Verify

Run the app. Confirm:
- [ ] App launches to the Menu screen (not directly to the game)
- [ ] Both RadioGroups pre-select from saved preferences on re-launch
- [ ] "START GAME" opens `MainActivity` and the game runs normally
- [ ] Back button from `MainActivity` returns to Menu
- [ ] Bankrupt shows "BANKRUPT!" for 1.5 seconds then returns to Menu
- [ ] "SCOREBOARD" button opens the placeholder `ScoreboardActivity` (just shows "SCOREBOARD" text)
- [ ] Settings (Tilt / Steady etc.) are remembered after closing and reopening the app

---

## Commit Message

```
Add MenuActivity as launcher with control mode, pace, and scoreboard settings

- New MenuActivity replaces MainActivity as launcher
- Three settings: control mode (Buttons/Tilt), pace (Steady/Fastening)
- Settings saved to SharedPreferences, restored on relaunch
- Settings passed to MainActivity via Intent extras
- Bankrupt now finish()es back to menu instead of in-game reset
- Game Runnable stored as named field; cancelled on onDestroy/bankrupt
- ScoreboardActivity placeholder created (populated in Plans 11–12)
```
