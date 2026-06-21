# Feature Plan 09 — Fastening Pace (Speed Ramp)

**Project:** `avoided_race_app`  
**Working dir:** `/Users/giladshekalim/repo/androidCourse/avoided_race_app`  
**Commit scope:** Implement the "Fastening" pace mode: every 10 seconds, the game tick interval decreases by 25ms, down to a minimum of 150ms. In "Steady" mode, speed stays constant at 500ms as before.

> **Prerequisites:**
> - **Plan 07 (Menu Screen)** must be done first. This plan reads `pace` from the Intent extra set by `MenuActivity` and assumes `gameRunnable` is a named field (required for variable-delay re-scheduling).
> - **Plan 07** also changed `resetGame()` to call `finish()` — the speed ramp Handler must be cancelled there before finishing.

> **Context files:**
> - `CODE.md` → "`startTimer()` — Game Loop" — the current loop uses a fixed `DELAY` constant. This plan changes it to a `var` and adds a second Handler for the ramp.
> - `PROJECT.md` → "Critical Constraints" → "The Handler game loop never stops" — the same risk applies to the ramp Handler; it must be stopped alongside the game Handler.

---

## What This Feature Does

- Changes `DELAY` from a `val` (constant) to a `var` (mutable)
- In **Fastening** mode: a second Handler fires every 10 seconds and decreases `DELAY` by 25ms, capped at 150ms minimum
- In **Steady** mode: `DELAY` stays at 500ms, second Handler never starts
- Speed resets to 500ms when the player finishes a game and returns to the menu (since `MainActivity` is recreated on each launch from the menu)
- The ramp Handler is cancelled in `onDestroy()` and in `resetGame()` before `finish()`

---

## Hidden / Non-Obvious Steps

1. **`DELAY` must change from `val` to `var`:** The current `private val DELAY = 500L` cannot be reassigned. Renaming it to `var` is a one-line change but the refactoring must happen **before** the ramp can work. This change is safe — no other code assigns to `DELAY`.

2. **The game loop reschedules itself with the current value of `DELAY` each tick** — because the Runnable calls `handler.postDelayed(this, DELAY)` at the end of each run. When `DELAY` decreases, the next `postDelayed` will use the new shorter interval automatically. No changes to `gameRunnable` itself are needed.

3. **The ramp is per-game, not persistent** — because `MainActivity` is created fresh every time the player starts a game from the menu (as of Plan 07). `DELAY` is initialized to `BASE_DELAY = 500L` in the field declaration. Nothing needs to be "reset" explicitly.

4. **Two Handlers running concurrently** — `handler` (game loop at ~DELAY ms) and `rampHandler` (speed increase at 10s). Both run on the main thread via `Looper.getMainLooper()`. They don't interfere.

5. **Cancel ramp in `resetGame()` before `finish()`** — if not cancelled, the ramp Runnable would fire after the Activity is destroyed.

---

## Files to Modify

| File | Change |
|---|---|
| `app/src/main/java/com/example/avoided_race_app/MainActivity.kt` | `DELAY` → `var`, add `BASE_DELAY`, add ramp Handler, cancel in cleanup |

No layout changes. No new files.

---

## Step-by-Step Implementation

### Step 1 — Change `DELAY` from `val` to `var`

Find the `DELAY` constant near the top of `MainActivity`:

```kotlin
// BEFORE
private val DELAY = 500L

// AFTER
private val BASE_DELAY = 500L
private var DELAY = BASE_DELAY
```

`BASE_DELAY` marks the starting value semantically. `DELAY` is now mutable.

---

### Step 2 — Add ramp Handler fields

Add alongside the existing `handler` field:

```kotlin
private val rampHandler = Handler(Looper.getMainLooper())
private lateinit var rampRunnable: Runnable

private val RAMP_INTERVAL = 10_000L  // 10 seconds between speed increases
private val RAMP_STEP = 25L          // ms to remove per ramp tick
private val MIN_DELAY = 150L         // fastest the game can go
```

---

### Step 3 — Add `startRamp()` and `stopRamp()`

```kotlin
private fun startRamp() {
    rampRunnable = object : Runnable {
        override fun run() {
            if (DELAY > MIN_DELAY) {
                DELAY = maxOf(MIN_DELAY, DELAY - RAMP_STEP)
                rampHandler.postDelayed(this, RAMP_INTERVAL)  // only keep posting if not at floor
            }
            // Once DELAY == MIN_DELAY: ramp stops itself — no further postDelayed
        }
    }
    rampHandler.postDelayed(rampRunnable, RAMP_INTERVAL)
}

private fun stopRamp() {
    if (::rampRunnable.isInitialized) rampHandler.removeCallbacks(rampRunnable)
}
```

The first ramp fires after 10 seconds. At 500ms tick speed, that's ~20 hazard ticks before the first speed increase. At maximum speed (150ms/tick), obstacles fall 3× faster than the starting speed. The ramp self-terminates when `MIN_DELAY` is reached.

---

### Step 4 — Start ramp in `onCreate()` if fastening mode

In `onCreate()`, after reading `pace` from Intent and after `startTimer()`:

```kotlin
if (pace == GameSettings.PACE_FASTENING) {
    startRamp()
}
```

---

### Step 5 — Cancel ramp in `resetGame()` before `finish()`

In `resetGame()` (updated in Plan 07), add `stopRamp()` before the `postDelayed { finish() }` call:

```kotlin
private fun resetGame() {
    if (::gameRunnable.isInitialized) handler.removeCallbacks(gameRunnable)
    stopRamp()   // ADD — cancel speed ramp before returning to menu

    if (main_LBL_money_lost is android.widget.TextView) {
        (main_LBL_money_lost as android.widget.TextView).text = getString(R.string.bankrupt)
    }
    main_LBL_money_lost.visibility = View.VISIBLE

    // TODO (Plan 10): save score + GPS location here

    handler.postDelayed({ finish() }, 1500)
}
```

---

### Step 6 — Cancel ramp in `onDestroy()`

In `onDestroy()` (added in Plan 07, extended in Plan 08), add `stopRamp()`. Plans 03 and 06 added `soundPool.release()` and `stopOdometer()` — keep those lines. Plan 08 added tilt cleanup — keep those lines if Plan 08 is done.

Full `onDestroy()` after this plan (assuming Plans 03, 06, 07, 08 are done):

```kotlin
override fun onDestroy() {
    super.onDestroy()
    if (::gameRunnable.isInitialized) handler.removeCallbacks(gameRunnable)
    stopRamp()                                               // ADD this plan
    cancelTiltMove()                                         // from Plan 08 (keep if Plan 08 is done)
    sensorManager.unregisterListener(sensorEventListener)   // from Plan 08 (keep if Plan 08 is done)
    stopOdometer()                                           // from Plan 06 — always keep
    soundPool.release()                                      // from Plan 03 — always keep
    // TODO (Plan 10): add fusedLocationClient.removeLocationUpdates() here
}
```

> **If Plan 08 is not yet done:** omit the `cancelTiltMove()` and `sensorManager.unregisterListener()` lines — those fields don't exist yet. Plan 08 will add them.

---

### Step 7 — Verify

Run the app with Fastening mode selected. Confirm:
- [ ] Game starts at normal speed (500ms tick)
- [ ] After 10 seconds, obstacles visibly speed up slightly
- [ ] After 40 seconds (4 ramp ticks), tick is 400ms — noticeably faster
- [ ] Speed eventually caps and stops increasing (won't go below 150ms)
- [ ] Steady mode: speed never changes, ramp Handler never fires
- [ ] Bankrupt → returns to menu → new game starts back at 500ms (fresh `MainActivity`)
- [ ] No crash when pressing back mid-ramp

---

## Commit Message

```
Add fastening pace mode: speed ramps up every 10 seconds

DELAY changed from val to var. Separate rampHandler fires every 10s 
in fastening mode, reducing tick interval by 25ms to a 150ms floor. 
Steady mode unchanged. Ramp stopped in resetGame() and onDestroy().
```
