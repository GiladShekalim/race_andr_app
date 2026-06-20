# Feature Plan 06 — Odometer / Distance Score

**Project:** `avoided_race_app`  
**Working dir:** `/Users/giladshekalim/repo/androidCourse/avoided_race_app`  
**Commit scope:** Add a distance counter that increments the game score by +1 every second from game start, unaffected by collisions.

> **Prerequisite:** Feature 04 (Game Score) must already be implemented. This feature requires `score` field and `updateScoreUI()` to exist in `MainActivity`.

> **Context files:**
> - `CODE.md` → "`startTimer()` — Game Loop" for the existing 500ms `Handler` + self-rescheduling `Runnable` pattern. The odometer uses the same mechanism.
> - `CODE.md` → "`resetGame()`" — the odometer counter and its Handler must be stopped and reset here.
> - `PROJECT.md` → "Critical Constraints" → "The `Handler` game loop never stops" — same caveat applies to the odometer `Handler`; both need a stop mechanism.

---

## What This Feature Does

- Adds a second `Handler` + `Runnable` pair that fires every **1000ms** (1 second) and increments `score` by 1
- Score is incremented from the moment the game becomes active (on first launch, and again on each game restart after bankrupt)
- Collisions do **not** stop or penalize the odometer
- The odometer resets to 0 (score resets, counter timer restarts from 0) when the game resets after bankrupt
- The odometer does **not** add to score while the "BANKRUPT!" overlay is visible — it stops on bankrupt and restarts on the next game

---

## Files to Change

| File | Action |
|---|---|
| `app/src/main/java/com/example/avoided_race_app/MainActivity.kt` | Add `odometerHandler`, `odometerRunnable`, `startOdometer()`, `stopOdometer()` |

No XML layout changes. No `LogicManager` changes. No resource changes.

---

## Step-by-Step Implementation

### Step 1 — Add odometer fields to `MainActivity`

File: `app/src/main/java/com/example/avoided_race_app/MainActivity.kt`

Add these fields alongside the existing `handler` field:

```kotlin
// Existing
private val handler = Handler(Looper.getMainLooper())

// Add these two
private val odometerHandler = Handler(Looper.getMainLooper())
private lateinit var odometerRunnable: Runnable
```

---

### Step 2 — Add `startOdometer()` method

Add a new private method to `MainActivity`:

```kotlin
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
```

**Why `postDelayed` with 1000ms, not `post`:** The first tick fires 1 second after the call, not immediately. This means a new game gives the player 1 full second before the score starts climbing — matching the feel of a "distance odometer" that begins counting as you move.

**Why a self-rescheduling `Runnable`:** This mirrors the existing `startTimer()` game loop pattern exactly. See `CODE.md` → "`startTimer()` — Game Loop" for the same pattern on the 500ms hazard tick. It's the idiomatic approach in this codebase.

---

### Step 3 — Add `stopOdometer()` method

```kotlin
private fun stopOdometer() {
    odometerHandler.removeCallbacks(odometerRunnable)
}
```

`removeCallbacks` cancels any pending `postDelayed` callbacks for that `Runnable`. Without this, the odometer would continue incrementing after bankrupt while the game is in a reset state.

---

### Step 4 — Call `startOdometer()` at game start

In `onCreate()`, after `initViews()` (where the game loop `startTimer()` is called), add:

```kotlin
startOdometer()
```

The game loop and odometer start together.

---

### Step 5 — Stop and restart odometer in `resetGame()`

`resetGame()` is called when `lives == 0` (bankrupt). The odometer must stop and then restart after the BANKRUPT screen disappears.

In `resetGame()`, after `logicManager.clearMatrix()` and the score reset, update the method:

```kotlin
private fun resetGame() {
    if (main_LBL_money_lost is android.widget.TextView) {
        (main_LBL_money_lost as android.widget.TextView).text = getString(R.string.bankrupt)
    }
    main_LBL_money_lost.visibility = View.VISIBLE

    stopOdometer()         // ADD — stop distance counting during bankrupt screen

    handler.postDelayed({
        main_LBL_money_lost.visibility = View.GONE
        startOdometer()    // ADD — restart after bankrupt screen clears
    }, 1500)

    logicManager.clearMatrix()
    lives = 3
    updateHeartsUI()
    score = 0              // from Feature 04
    updateScoreUI()        // from Feature 04
}
```

**Important:** `startOdometer()` is called inside the `postDelayed` block, which fires 1500ms later when the "BANKRUPT!" overlay disappears. This prevents the odometer from starting before the new game visually begins.

---

### Step 6 — Add `onDestroy()` cleanup

If `onDestroy()` already exists (added in Feature 03 for `SoundPool`), add `stopOdometer()` inside it:

```kotlin
override fun onDestroy() {
    super.onDestroy()
    soundPool.release()    // from Feature 03 (if implemented)
    stopOdometer()         // ADD
}
```

If `onDestroy()` does not yet exist, add it:

```kotlin
override fun onDestroy() {
    super.onDestroy()
    stopOdometer()
}
```

This prevents a memory/callback leak if the Activity is destroyed while the odometer is running.

---

### Step 7 — Verify

Run the app. Watch the score display over 5–10 seconds. Confirm:
- [ ] Score increments by 1 every second ("Score: 1", "Score: 2", etc.)
- [ ] Crashing into a hazard does NOT stop the odometer or reduce score
- [ ] Score increments correctly regardless of how often the player moves left/right
- [ ] On bankrupt: odometer stops, score resets to 0 on screen
- [ ] After "BANKRUPT!" disappears: score starts counting from 1 again after 1 second
- [ ] Odometer and game hazard tick run independently (score should not increment twice as fast or not at all)

---

## How Odometer + Coins Work Together (After Both Features Are Implemented)

When both Features 05 and 06 are active, `score` is incremented by two independent systems:

| Source | Amount | Trigger |
|---|---|---|
| Odometer (this feature) | +1 | Every 1000ms automatically |
| Coin collection (Feature 05) | +10 | Car enters a coin's cell at row `rows-1` |

Both call `updateScoreUI()` to refresh the display. They do not conflict because both run on the main thread via the `Handler` loop — no concurrency issues.

---

## Commit Message

```
Add odometer that increments score +1 per second

Second Handler/Runnable pair fires every 1000ms to increment score 
independently of collisions. Stops on bankrupt, restarts after 
BANKRUPT overlay clears. Cleaned up in onDestroy to prevent leak.
```

---

## Notes

- Do NOT use `Timer` or `ScheduledExecutorService` for this feature — posting to a background thread and updating UI from it requires `runOnUiThread()`. The `Handler(Looper.getMainLooper())` approach used here already runs on the main thread and avoids that complexity. See `CODE.md` → "Patterns Table" for the concurrency approach used throughout this project.
- The odometer fires at a 1000ms interval independently of the 500ms game tick. They are not synchronized intentionally — the game tick governs hazard spawning, not time. Syncing them would create coupling with no benefit.
- If a pause feature is added in the future, `stopOdometer()` and `startOdometer()` can be called from the pause/resume actions alongside the existing game `Handler` pause logic described in `PROJECT.md` → "Future Addition Points → Pause/Resume".
