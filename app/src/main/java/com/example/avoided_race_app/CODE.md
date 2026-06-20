# Source Code Reference — `com.example.avoided_race_app`

Two Kotlin files. Zero third-party dependencies in the source. No ViewModel, no LiveData, no coroutines.

---

## File Map

| File | Lines | Role |
|---|---|---|
| `MainActivity.kt` | 212 | Android entry point — owns UI, game loop, collision response |
| `LogicManager.kt` | 77 | Pure game logic — 8×3 matrix of resource IDs |

---

## `LogicManager.kt` — Game Brain

### Class Signature
```kotlin
class LogicManager(private val rows: Int, private val cols: Int)
```
Constructed with `LogicManager(8, 3)` from `MainActivity`. No Android imports — pure Kotlin.

### Internal State
```kotlin
private val matrix: Array<IntArray> = Array(rows) { IntArray(cols) { 0 } }
```
A 2D array of `Int`. Each cell stores either `0` (empty) or an `R.drawable.*` resource ID (hazard present). Using `IntArray` (primitive int array) instead of `Array<Int>` is more memory-efficient.

```kotlin
private val hazardResources = listOf(
    R.drawable.fuel,
    R.drawable.clinic,
    R.drawable.no_tax,
    R.drawable.broken_car,
    R.drawable.bankrupt
)
```
Five distinct hazard types. All equally probable when selected.

### `tick()` — Core Shift Algorithm
```kotlin
fun tick() {
    // Shift down: start from bottom, copy row above into current row
    for (r in rows - 1 downTo 1) {
        for (c in 0 until cols) {
            matrix[r][c] = matrix[r - 1][c]
        }
    }
    // Clear top row
    for (c in 0 until cols) {
        matrix[0][c] = 0
    }
    // 30% chance: spawn one hazard in a random lane at row 0
    if (Random.nextInt(100) < 30) {
        val randomLane = Random.nextInt(cols)
        val randomHazard = hazardResources[Random.nextInt(hazardResources.size)]
        matrix[0][randomLane] = randomHazard
    }
}
```

**Why top-to-bottom shift starts from the bottom:** If you iterate top→bottom, you'd overwrite each row with its updated value before copying it to the next. Starting from `rows-1 downTo 1` ensures each row copies the value from the row above *before* that row is modified.

**Spawn constraints:** Only one hazard per tick maximum. Only one random lane per tick. Can produce "bursts" where multiple ticks in a row spawn (random), or gaps with nothing for several ticks.

### `checkCollision(carLane: Int): Boolean`
```kotlin
return matrix[rows - 1][carLane] > 0
```
Checks the **last row** (`rows-1 = 7`). This is the invisible collision row beneath the 7 visible obstacle rows. A hazard there in the car's lane → crash.

### `getResourceId(row: Int, col: Int): Int`
```kotlin
return matrix[row][col]
```
Read-only accessor. Returns `0` for empty cells, non-zero resource ID for hazards.

### `clearMatrix()`
```kotlin
for (r in 0 until rows) {
    for (c in 0 until cols) {
        matrix[r][c] = 0
    }
}
```
Full reset. Called on bankrupt. Sets all 24 cells to 0.

---

## `MainActivity.kt` — UI Controller

### Constants and State Fields
```kotlin
private val ROWS = 7        // visible obstacle rows
private val COLS = 3        // lanes

private var currentCarLane = 1   // 0=Left, 1=Center, 2=Right
private var lives = 3

private val handler = Handler(Looper.getMainLooper())
private val DELAY = 500L         // ms per game tick
```

`ROWS` and `COLS` are local constants (not in `Constants.kt`). The `handler` is bound to the main thread's looper — all UI updates happen on the main thread naturally.

### `findViews()` — View Binding

Standard `findViews()` / `initViews()` pattern. The interesting part is the 2D array binding:

```kotlin
obstacleMatrix = Array(ROWS) { r ->
    Array(COLS) { c ->
        val id = resources.getIdentifier("main_IMG_obstacle_${r}_${c}", "id", packageName)
        findViewById(id)
    }
}
```

**`resources.getIdentifier(name, defType, defPackage)`** resolves a view ID by name string at runtime. This creates the full 7×3 = 21 cell binding without 21 lines of code. The string format `"main_IMG_obstacle_${r}_${c}"` must exactly match the XML `android:id` values.

**Tradeoff:** `getIdentifier` is slower than a direct R.id reference and bypasses compile-time checking. If you rename an ID in XML without updating the format string here, it will crash at runtime with `NotFoundException`. For a small static matrix this is acceptable.

### `initViews()` — Input Handling
```kotlin
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
```

Boundary checks prevent `currentCarLane` from going below 0 or above 2. `refreshUI()` is called immediately on button press (not waiting for the next tick) so the car moves responsively.

### `startTimer()` — Game Loop
```kotlin
handler.postDelayed(object : Runnable {
    override fun run() {
        logicManager.tick()
        if (logicManager.checkCollision(currentCarLane)) {
            handleCollision()
        }
        refreshUI()
        if (lives <= 0) {
            resetGame()
        }
        handler.postDelayed(this, DELAY)   // self-reschedule
    }
}, DELAY)
```

**Order matters:** `tick()` → `checkCollision()` → `refreshUI()` → check lives.
- Tick first so collision check sees the *new* state
- Check collision before refreshUI so we can handle it (show overlay, vibrate) before rendering
- `refreshUI()` always runs — shows current state whether collision happened or not
- `resetGame()` runs after `refreshUI()` so the last frame is rendered before clearing

**No cancel mechanism:** The loop runs forever. If you navigate away or rotate (though rotation isn't tested), the handler leak persists. Adding `handler.removeCallbacks(...)` in `onDestroy()` would fix this.

### `handleCollision()`
```kotlin
private fun handleCollision() {
    (main_LBL_money_lost as? android.widget.TextView)?.text = getString(R.string.money_lost)
    main_LBL_money_lost.visibility = View.VISIBLE
    handler.postDelayed({ main_LBL_money_lost.visibility = View.GONE }, 1000)
    
    vibrate()
    lives--
    updateHeartsUI()
}
```

**The cast pattern:** `main_LBL_money_lost` is declared as `View` (not `MaterialTextView`). Changing text requires casting to `TextView`. This works but is fragile — a cleaner approach would declare it as `MaterialTextView` directly (MaterialTextView inherits from TextView).

**Collision does NOT reset obstacles.** Only `resetGame()` does that. So after a collision, the same hazard that caused it continues to exist and will be shifted off the bottom on the next tick naturally.

### `resetGame()`
```kotlin
private fun resetGame() {
    (main_LBL_money_lost as? android.widget.TextView)?.text = getString(R.string.bankrupt)
    main_LBL_money_lost.visibility = View.VISIBLE
    handler.postDelayed({ main_LBL_money_lost.visibility = View.GONE }, 1500)
    logicManager.clearMatrix()
    lives = 3
    updateHeartsUI()
}
```

Called within the game loop tick (not from a separate timer). The overlay shows for 1.5s but the game loop keeps running during that time — the grid is already cleared so nothing to collide with. The car position is NOT reset (player keeps their lane from before bankrupt).

### `vibrate()`
```kotlin
private fun vibrate() {
    val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        v.vibrate(500)
    }
```

Uses the deprecated `VIBRATOR_SERVICE` path (works for API 36 but best practice would use `VibratorManager` for API 31+, as shown in L05's `SignalManager`). 500ms single vibration.

### `updateHeartsUI()`
```kotlin
for (i in hearts.indices) {
    hearts[i].visibility = if (i >= lives) View.INVISIBLE else View.VISIBLE
}
```

`View.INVISIBLE` (not `GONE`) — preserves layout space so hearts container doesn't change size. Icons vanish right-to-left as lives decrease: `hearts[2]` disappears first (when `lives = 2`, `i=2 >= 2 → INVISIBLE`).

### `refreshUI()`
```kotlin
private fun refreshUI() {
    // 1. Show only the car image in currentCarLane
    for (i in carImages.indices) {
        carImages[i].visibility = if (i == currentCarLane) View.VISIBLE else View.INVISIBLE
    }
    // 2. Sync every obstacle cell with the logic matrix
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
```

Full repaint of 21 obstacle cells + 3 car cells every call. Called both from button listeners (immediate car movement) and from the tick loop (obstacle movement). No dirty-flag optimization — always redraws everything.

**`setImageResource(resId)`** is called on every non-empty cell every tick even if the same resource was already set. This is fine for this scale (21 cells), but for larger grids you'd cache the last-set resource and skip redundant calls.

---

## Patterns Used

| Pattern | Where |
|---|---|
| SRP via class separation | `LogicManager` holds state, `MainActivity` holds UI |
| `findViews()` / `initViews()` split | Both classes follow this convention |
| View naming: `{screen}_{TYPE}_{desc}` | All view IDs in XML and Kotlin |
| Handler + Runnable self-reschedule | `startTimer()` — same as L04's `HandlerRunnableActivity` |
| Dynamic ID resolution | `resources.getIdentifier()` for the 7×3 matrix |
| SDK version branching | `vibrate()` checks `Build.VERSION.SDK_INT >= O` |

---

## Things to Know Before Editing

1. **`obstacleMatrix` indices match `LogicManager` rows 0-6 only.** Row 7 in the logic matrix is the collision row and has no corresponding ImageView.

2. **`main_LBL_money_lost` is typed as `View`.** Cast to `TextView`/`MaterialTextView` before calling `.text`. Or change the field type to `MaterialTextView`.

3. **Adding a new hazard** only requires adding its `R.drawable.*` to `LogicManager.hazardResources`. No other changes needed.

4. **Adding a 4th lane** requires: updating `COLS`, adding 3 more `AppCompatImageView` rows to the XML (and a 4th car slot), and the `resources.getIdentifier` call will still work automatically.

5. **The game loop never stops.** There is no `onPause()` override. If you add a pause feature, store the Runnable reference and call `handler.removeCallbacks(runnable)`.

6. **No `Application` class.** All singletons/managers must be initialized in `onCreate()`. If you add a `ScoreManager` or `SoundManager`, you'll likely want to create `App : Application()` and register it in the manifest.
