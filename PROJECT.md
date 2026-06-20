# Avoided Race App — Project Reference

**Package:** `com.example.avoided_race_app`  
**Purpose:** Endless-runner lane-dodge game in Android/Kotlin — player steers a car left/right through 3 lanes to avoid spawning hazards.

---

## What the App Does

- A racing car sits fixed at the bottom of a 3×7 grid
- Hazard icons spawn randomly at the top and "fall" downward one row every 500ms
- Player presses LEFT or RIGHT FAB to change the car's lane
- A collision is detected when a hazard reaches the car's invisible 8th row
- 3 "lives" (money bags) — each collision costs one; losing all triggers a "BANKRUPT!" reset
- Loop is endless: game immediately resets and continues after bankrupt

---

## Architecture

### Pattern: Logic-UI Separation (SRP)

```
┌─────────────────────────────────────────┐
│              MainActivity               │
│  - Owns all UI references               │
│  - Drives the game loop via Handler     │
│  - Delegates all game state to          │
│    LogicManager                         │
└─────────────┬───────────────────────────┘
              │ calls tick(), checkCollision(),
              │ getResourceId(), clearMatrix()
              ▼
┌─────────────────────────────────────────┐
│             LogicManager                │
│  - Owns the 8×3 Int matrix              │
│  - Shifts rows, spawns obstacles        │
│  - Has zero knowledge of Android/UI     │
└─────────────────────────────────────────┘
```

`LogicManager` is pure Kotlin — no Android imports, no `Context`, no `R`. It could be unit-tested with standard JUnit without Robolectric.

---

## Tech Stack

| Item | Value |
|---|---|
| Language | Kotlin |
| AGP | 9.2.1 |
| compileSdk | `release(36)` with `minorApiLevel = 1` (Android 16 Preview) |
| minSdk | 36 |
| targetSdk | 36 |
| Java source/target | VERSION_11 |
| Build scripts | Kotlin DSL (`.gradle.kts`) |
| Dependency catalog | `gradle/libs.versions.toml` (Version Catalog) |
| Gradle toolchain | Foojay resolver convention 1.0.0 |

---

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| `androidx.core:core-ktx` | 1.18.0 | Kotlin Android core extensions |
| `androidx.appcompat:appcompat` | 1.7.1 | `AppCompatActivity`, `AppCompatImageView` |
| `com.google.android.material:material` | 1.14.0 | `ExtendedFloatingActionButton`, `MaterialTextView` |
| `androidx.activity:activity-ktx` | 1.13.0 | `ComponentActivity` with Kotlin extensions |
| `androidx.constraintlayout:constraintlayout` | 2.2.1 | Dependency declared, not used in layouts |
| JUnit / Espresso | standard | Testing boilerplate |

---

## Project Structure

```
avoided_race_app/
├── PROJECT.md                                  ← THIS FILE
├── README.md                                   ← User-facing summary
├── app/
│   ├── build.gradle.kts                        ← App-level build (no kotlin plugin declared)
│   └── src/main/
│       ├── AndroidManifest.xml                 ← VIBRATE permission, single Activity
│       ├── java/com/example/avoided_race_app/
│       │   ├── CODE.md                         ← Code-level deep dive
│       │   ├── MainActivity.kt                 ← UI + game loop + collision handling
│       │   └── LogicManager.kt                 ← Pure game logic, 8×3 matrix
│       └── res/
│           ├── layout/
│           │   ├── LAYOUT.md                   ← Layout structure reference
│           │   └── activity_main.xml           ← Entire game UI in one layout
│           ├── drawable/
│           │   ├── DRAWABLES.md                ← Asset catalog reference
│           │   ├── racing_car.xml              ← Player vehicle (rotated vector SVG)
│           │   ├── money_bag.xml               ← Life indicator icon
│           │   ├── bankrupt.xml                ← Hazard: bankrupt sign
│           │   ├── broken_car.xml              ← Hazard: broken car
│           │   ├── clinic.xml                  ← Hazard: clinic
│           │   ├── fuel.xml                    ← Hazard: fuel spill
│           │   ├── no_tax.xml                  ← Hazard: no tax sign
│           │   ├── road_line.xml               ← Lane divider (layer-list with rotate)
│           │   ├── road_line_dash.xml          ← Dashed line source for road_line
│           │   ├── ic_left.xml                 ← Left FAB icon
│           │   └── ic_right.xml                ← Right FAB icon
│           ├── drawable-night/
│           │   ├── road_line.xml               ← Night-mode lane divider
│           │   └── road_line_dash.xml          ← Night-mode dash source
│           ├── values/
│           │   ├── VALUES.md                   ← Resource values reference
│           │   ├── colors.xml                  ← Day palette
│           │   ├── dimens.xml                  ← All dimension tokens
│           │   ├── strings.xml                 ← App name + 2 overlay strings
│           │   └── themes.xml                  ← Material3 DayNight NoActionBar
│           └── values-night/
│               ├── colors.xml                  ← Night palette (darker backgrounds)
│               └── themes.xml                  ← Night theme override
├── build.gradle.kts                            ← Project-level (no kotlin plugin)
├── gradle/libs.versions.toml                   ← Central dependency catalog
├── settings.gradle.kts                         ← Single module (:app)
└── local.properties                            ← SDK path (not committed)
```

---

## Game Loop Flow

```
onCreate()
  ├── LogicManager(8, 3)        ← 8 rows (7 visible + 1 car/collision), 3 lanes
  ├── findViews()               ← bind 3 car images, 7×3 obstacle grid, 3 life icons
  ├── initViews()               ← attach LEFT/RIGHT FAB listeners
  └── startTimer()              ← Handler.postDelayed(500ms)

Every 500ms tick:
  1. logicManager.tick()        ← shift matrix down 1 row, 30% chance spawn at row 0
  2. checkCollision(lane)       ← check matrix[7][carLane] > 0
     └── if true → handleCollision()
           ├── show "MONEY LOST!" overlay (hide after 1s)
           ├── vibrate(500ms)
           ├── lives--
           └── updateHeartsUI()
  3. refreshUI()                ← sync all 21 obstacle cells + 3 car cells to matrix
  4. if lives <= 0 → resetGame()
       ├── show "BANKRUPT!" overlay (hide after 1.5s)
       ├── logicManager.clearMatrix()
       └── lives = 3
  5. handler.postDelayed(this, 500ms)  ← self-reschedule
```

---

## Grid Coordinate System

```
      Col 0    Col 1    Col 2
Row 0 [  0  ]  [  0  ]  [  0  ]   ← obstacles spawn here (matrix[0][c])
Row 1 [  0  ]  [ fx  ]  [  0  ]   ← fx = R.drawable.fuel resource ID
Row 2 [  0  ]  [  0  ]  [  0  ]
Row 3 [  0  ]  [  0  ]  [  0  ]   ← rendered via obstacleMatrix[r][c]
Row 4 [  0  ]  [  0  ]  [  0  ]   ← each cell = AppCompatImageView
Row 5 [  0  ]  [  0  ]  [  0  ]
Row 6 [  0  ]  [  0  ]  [  0  ]   ← last visible row
──────────────────────────────────
Row 7 [  0  ]  [ fx  ]  [  0  ]   ← INVISIBLE COLLISION ROW
     car[0]   car[1]   car[2]      ← car sits here; collision detected when
                                      matrix[7][carLane] > 0
```

The matrix stores **`R.drawable.*` resource IDs** (non-zero = hazard present). `0` means empty cell.

---

## View Naming Convention

Follows the course-standard `{screen}_{TYPE}_{description}` pattern:

| View ID | Type | Role |
|---|---|---|
| `main_BTN_left` | ExtendedFAB | Move car left |
| `main_BTN_right` | ExtendedFAB | Move car right |
| `main_IMG_car_0/1/2` | AppCompatImageView | Car in lane 0, 1, 2 |
| `main_IMG_heart0/1/2` | AppCompatImageView | Life indicators |
| `main_IMG_obstacle_R_C` | AppCompatImageView | Obstacle at row R, col C |
| `main_LBL_money_lost` | MaterialTextView | Overlay: "MONEY LOST!" / "BANKRUPT!" |
| `main_LAY_hearts` | LinearLayout | Horizontal container for lives |
| `main_LAY_matrix` | LinearLayout | Vertical container for the 8-row game grid |
| `main_LAY_car_row` | LinearLayout | Bottom row housing the 3 car slots |

---

## Key Design Decisions

### Dynamic View ID Resolution
Obstacle cells are found at runtime using `resources.getIdentifier()`:
```kotlin
val id = resources.getIdentifier("main_IMG_obstacle_${r}_${c}", "id", packageName)
```
This avoids 21 separate `findViewById` calls but requires the XML IDs to follow the `main_IMG_obstacle_R_C` naming pattern exactly.

### 30% Spawn Probability
Obstacles don't spawn every tick — a 30% random chance introduces temporal irregularity (some ticks spawn nothing, some spawn one). This creates unpredictable difficulty without a dedicated difficulty system. There is no difficulty scaling over time.

### Collision Row Is Invisible
The 8th matrix row (index 7) is never rendered — obstacles exist there for one tick before being replaced on the next shift. This means the collision visually "feels" like it happens when the hazard is at the very bottom visible row (row 6), because by the time it enters row 7 you can't see it.

### Handler Loop Self-Reschedule
The timer uses an anonymous `Runnable` inside `handler.postDelayed(object : Runnable { ... }, DELAY)`. The runnable re-schedules itself at the end of each tick. No explicit cancel is implemented — the loop runs for the lifetime of the Activity.

---

## What's Missing / Future Addition Points

| Feature | Where to Add |
|---|---|
| Score counter | `LogicManager` (count ticks survived) + new `main_LBL_score` in layout |
| Difficulty scaling | `LogicManager.spawnProbability` increases over time, or `DELAY` decreases |
| High score persistence | New `ScoreManager` singleton (SharedPreferences) initialized in App class |
| Pause / resume | New state flag in `MainActivity`; `handler.removeCallbacks()` on pause |
| Start/Game-over screen | New `GameState` enum; second Activity or Fragment for menu |
| Sound effects | `MediaPlayer` or `SoundPool` for engine hum + crash sound |
| Smooth animation | Replace discrete grid with `ObjectAnimator`/`ValueAnimator` on Y-translation |
| Sensor tilt controls | `SensorEventListener` for accelerometer — alternative to button controls |
| Multiple hazard types | Add speed modifier or point-bonus hazards in `LogicManager.hazardResources` |
| Custom Application class | Add `App : Application()` to init singletons (score manager, sound) at startup |
