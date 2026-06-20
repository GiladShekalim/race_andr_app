# Feature Plan 05 — Coins on the Road

**Project:** `avoided_race_app`  
**Working dir:** `/Users/giladshekalim/repo/androidCourse/avoided_race_app`  
**Commit scope:** Add collectible coins that fall like hazards but give +10 score on collection rather than losing a life.

> **Prerequisite:** Feature 04 (Game Score) must already be implemented. This feature requires `score` field and `updateScoreUI()` to exist in `MainActivity`.

> **Context files:**
> - `CODE.md` → "`LogicManager.kt` — `tick()`" for the hazard shift+spawn algorithm this feature mirrors, and "`checkCollision()`" for the coin collection check pattern to follow.
> - `CODE.md` → "`MainActivity.kt` — `refreshUI()`" for how ImageView cells are currently updated each tick — coin cells need the same treatment.
> - `DRAWABLES.md` → "Adding a Custom Asset" for how to add the coin drawable, and "Hazard → Code Connection" for how drawable IDs flow into the matrix.
> - `PROJECT.md` → "Game Loop Flow" for where in the tick sequence coin checks should be inserted.

---

## What This Feature Does

- Adds a `coinMatrix: Array<IntArray>` to `LogicManager` that operates in parallel to the hazard `matrix`
- Coins fall at the same speed as hazards (same tick rate)
- Coins spawn at row 0 with the same random timing logic BUT only in cells that don't already have a hazard
- When the car's lane contains a coin in the final row (row `rows-1`), the coin is collected: `+10` score, coin cell cleared — no life lost
- A new `coin.xml` vector drawable is added to `res/drawable/`
- Coins and hazards never overlap (spawn guard prevents it)
- `refreshUI()` renders both matrices into the same `obstacleMatrix` ImageViews (coins display in the same grid cells as hazards)

---

## Files to Change

| File | Action |
|---|---|
| `app/src/main/res/drawable/coin.xml` | **Create** — new coin vector drawable |
| `app/src/main/res/raw/coin_sound.ogg` | **Create** — coin collection sound effect |
| `app/src/main/java/com/example/avoided_race_app/LogicManager.kt` | Add `coinMatrix`, `tickCoins()`, `checkCoinCollection()`, update `clearMatrix()` |
| `app/src/main/java/com/example/avoided_race_app/MainActivity.kt` | Add `coinSoundId`, load sound, call `tickCoins()` and `checkCoinCollection()` in game loop, add `playCoinSound()`, update `refreshUI()` |

No XML layout changes. The existing `obstacleMatrix` ImageViews display both hazards and coins.

> **Note on SoundPool:** Plan 03 already set up a `SoundPool` with `setMaxStreams(3)`. This plan adds a second sound to the same pool — no SoundPool rebuild needed, only a new load call and ID field.

---

## Step-by-Step Implementation

### Step 1 — Add `res/raw/coin_sound.ogg`

Create the `res/raw/` directory if it doesn't exist (Plan 03 already created it).

Place a short coin-collection sound file at:
```
app/src/main/res/raw/coin_sound.ogg
```

Any short (< 0.5s) positive chime or ding sound works. Accepted formats: `.ogg`, `.wav`, `.mp3`. Use the same naming convention as `crash_sound.ogg`.

---

### Step 2 — Add `coinSoundId` to `MainActivity`

**2a — Add field** (near `crashSoundId` declared in Plan 03):
```kotlin
private var coinSoundId: Int = 0
```

**2b — Load in `onCreate()`** immediately after the `crashSoundId` load line:
```kotlin
crashSoundId = soundPool.load(this, R.raw.crash_sound, 1)
coinSoundId = soundPool.load(this, R.raw.coin_sound, 1)  // ADD
```

**2c — Add `playCoinSound()` method** (alongside `playCrashSound()`):
```kotlin
private fun playCoinSound() {
    if (soundLoaded) {
        soundPool.play(coinSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }
}
```

---

### Step 4 — Create `res/drawable/coin.xml`

Create a simple gold circle vector drawable. File: `app/src/main/res/drawable/coin.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="64dp"
    android:height="64dp"
    android:viewportWidth="64"
    android:viewportHeight="64">
    <!-- Outer gold circle -->
    <path
        android:fillColor="#FFD700"
        android:pathData="M32,4a28,28 0 1,0 0,56a28,28 0 1,0 0,-56z" />
    <!-- Inner highlight ring -->
    <path
        android:fillColor="#FFF176"
        android:pathData="M32,10a22,22 0 1,0 0,44a22,22 0 1,0 0,-44z" />
    <!-- Dollar sign or coin mark center -->
    <path
        android:fillColor="#F9A825"
        android:pathData="M28,20h8v4h-4v2h4v4h-4v2h4v4h-8v-4h4v-2h-4v-4h4v-2h-4z" />
</vector>
```

The center path renders a simplified `$` shape. Adjust colors or the center mark as needed — the outer/inner gold circles are the most important visual element. See `DRAWABLES.md` → "Vector File Structure Pattern" for the standard viewport and path conventions used in this project.

---

### Step 5 — Update `LogicManager.kt`

File: `app/src/main/java/com/example/avoided_race_app/LogicManager.kt`

**2a — Add coin resource ID field:**

In the class body, alongside `hazardResources`, add:
```kotlin
private var coinResourceId: Int = 0

fun setCoinResource(resId: Int) {
    coinResourceId = resId
}
```

**2b — Add `coinMatrix` field:**

After the existing `matrix` declaration:
```kotlin
private val matrix: Array<IntArray> = Array(rows) { IntArray(cols) { 0 } }
private val coinMatrix: Array<IntArray> = Array(rows) { IntArray(cols) { 0 } }  // ADD
```

**2c — Add `tickCoins()` method:**

Add this method after `tick()`:
```kotlin
fun tickCoins() {
    if (coinResourceId == 0) return  // guard: no coin resource set yet

    // Shift existing coins down one row (bottom-to-top to avoid overwriting)
    for (r in rows - 1 downTo 1) {
        for (c in 0 until cols) {
            coinMatrix[r][c] = coinMatrix[r - 1][c]
        }
    }

    // Spawn a coin at row 0 with 20% probability, only in a cell free of hazards
    coinMatrix[0] = IntArray(cols) { 0 }  // clear the spawn row
    if (Random.nextInt(100) < 20) {
        val spawnCol = Random.nextInt(cols)
        if (matrix[0][spawnCol] == 0) {  // only spawn if no hazard in that cell
            coinMatrix[0][spawnCol] = coinResourceId
        }
    }
}
```

**Why bottom-to-top:** Same reason as `tick()` for hazards — shifting top-to-bottom would overwrite cells before they've been moved. See `CODE.md` → "`tick()` — Shift Algorithm" for the detailed explanation.

**2d — Add `checkCoinCollection()` method:**

```kotlin
fun checkCoinCollection(carLane: Int): Boolean {
    val collected = coinMatrix[rows - 1][carLane] != 0
    if (collected) {
        coinMatrix[rows - 1][carLane] = 0  // clear the collected coin
    }
    return collected
}
```

**2e — Add `getCoinResourceId()` accessor for `refreshUI()`:**

```kotlin
fun getCoinResourceId(row: Int, col: Int): Int = coinMatrix[row][col]
```

**2f — Update `clearMatrix()` to also clear coins:**

```kotlin
fun clearMatrix() {
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            matrix[r][c] = 0
            coinMatrix[r][c] = 0  // ADD
        }
    }
}
```

---

### Step 6 — Update `MainActivity.kt`

File: `app/src/main/java/com/example/avoided_race_app/MainActivity.kt`

**6a — Pass coin resource ID to `LogicManager` in `onCreate()`:**

After `logicManager` is initialized (find the `LogicManager(ROWS + 1, COLS, ...)` constructor call), add:
```kotlin
logicManager.setCoinResource(R.drawable.coin)
```

**6b — Call `tickCoins()` in the game loop, and play coin sound on collection:**

In `startTimer()`, find the existing tick sequence:
```kotlin
logicManager.tick()
if (logicManager.checkCollision(currentCarLane)) {
    handleCollision()
}
refreshUI()
```

Update to:
```kotlin
logicManager.tick()
logicManager.tickCoins()   // ADD — tick coins after hazards

if (logicManager.checkCollision(currentCarLane)) {
    handleCollision()
} else if (logicManager.checkCoinCollection(currentCarLane)) {
    score += 10             // ADD — collect coin
    updateScoreUI()         // ADD — refresh display
    playCoinSound()         // ADD — play coin collection sound (added in Step 2c)
}

refreshUI()
```

**Important:** Use `else if` — a cell can't be both a hazard collision and a coin collection in the same tick. The hazard check takes priority.

**6c — Update `refreshUI()` to render coins:**

Currently `refreshUI()` sets each ImageView's resource based on `logicManager.getResourceId(r, c)`. Update each cell to check both matrices:

Find the inner loop in `refreshUI()`. It currently reads:
```kotlin
for (r in 0 until ROWS) {
    for (c in 0 until COLS) {
        val resId = logicManager.getResourceId(r, c)
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
```

Update to check `coinMatrix` as a fallback when the hazard cell is empty:
```kotlin
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
```

Since hazards and coins never share the same cell (spawn guard in `tickCoins()`), at most one of `hazardResId` or `coinResId` will be non-zero per cell.

---

### Step 7 — Verify

Run the app. Confirm:
- [ ] Gold coin objects appear on the road and fall downward
- [ ] Driving into a coin increases score by 10 (confirm "Score: 10", "Score: 20", etc.)
- [ ] Driving into a coin does NOT reduce lives
- [ ] Driving into a hazard still reduces lives (coin collection does not interfere)
- [ ] Coins never appear on a cell already occupied by a hazard
- [ ] Coins disappear from the grid after collection or after passing the bottom row
- [ ] `clearMatrix()` correctly clears both hazards and coins on game reset
- [ ] Score and coins both reset on bankrupt
- [ ] A distinct sound plays when a coin is collected (different from crash sound)

---

## Commit Message

```
Add collectible coins that award +10 score on collection

Parallel coinMatrix in LogicManager falls alongside hazards. Coins 
spawn at 20% probability in hazard-free cells. Car collection (same 
row check as collision) awards +10 score without losing a life. 
refreshUI() renders coins in the same obstacle ImageViews as hazards.
```

---

## Notes

- `tickCoins()` is called **after** `tick()` in the game loop. This ensures the hazard spawn for that tick is committed before the coin spawn guard checks `matrix[0][spawnCol]`.
- The 20% coin spawn rate (vs 30% hazard rate) keeps coins less common than hazards, making collection feel rewarding.
- If Feature 02 (five lanes) has been implemented, `COLS` will be 5 and `ROWS` will be 9 — the `LogicManager` parameters passed from `MainActivity` handle this automatically. No changes to this plan are needed.
