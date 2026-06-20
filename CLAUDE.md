# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests (JVM, no device needed)
./gradlew test

# Run instrumented tests (requires connected device or emulator at API 36+)
./gradlew connectedAndroidTest

# Full build + lint check
./gradlew build

# Clean build outputs
./gradlew clean
```

Install on a connected device: `adb install app/build/outputs/apk/debug/app-debug.apk`

There is no dev server — UI changes must be verified on a physical device or Android Studio emulator targeting **API 36**.

---

## Architecture

### Two-file source (SRP pattern)

| File | Role |
|---|---|
| `LogicManager.kt` | Pure Kotlin — zero Android imports. Owns the 8×3 `Array<IntArray>` game matrix. Exposes `tick()`, `checkCollision(lane)`, `getResourceId(row,col)`, `clearMatrix()`. |
| `MainActivity.kt` | Android entry point. Owns all UI refs, drives the 500ms `Handler` loop, calls `LogicManager` for state, renders results into `AppCompatImageView` cells. |

### Grid: 8 rows × 3 cols

- Rows 0–6 are rendered (7 visible obstacle rows)
- Row 7 is **invisible** — the collision row. `checkCollision` checks `matrix[7][carLane] > 0`.
- Matrix cells store `R.drawable.*` resource IDs. `0` = empty, non-zero = hazard type.

### Game loop order (every 500ms)

```
tick() → checkCollision() → handleCollision()? → refreshUI() → lives check → resetGame()? → reschedule
```

Tick first so collision sees the shifted-down state. `refreshUI()` always runs after so the post-collision frame is always rendered.

### Dynamic view binding

The 7×3 = 21 obstacle `ImageView`s are bound at runtime:
```kotlin
resources.getIdentifier("main_IMG_obstacle_${r}_${c}", "id", packageName)
```
XML IDs **must** match `main_IMG_obstacle_R_C` exactly — no compile-time check.

---

## Critical Constraints

- **minSdk = 36** (Android 16 Preview). Will not run on any device below API 36.
- `main_LBL_money_lost` is typed as `View` in `MainActivity`. Cast to `TextView` or `MaterialTextView` before calling `.text` — or change the field type.
- The `Handler` game loop never stops — no `onPause`/`onDestroy` cancel exists. Adding pause requires storing the `Runnable` reference and calling `handler.removeCallbacks(runnable)`.
- Car position is **not** reset on bankrupt — the player keeps their lane.
- Collision does **not** clear obstacles — only `resetGame()` calls `clearMatrix()`. The colliding hazard naturally scrolls off on the next tick.

---

## Day / Night System

The app uses Material3 `DayNight.NoActionBar`. Two resource qualifier folders provide the night-mode overrides:

| Folder | Overrides |
|---|---|
| `res/values/` | Day colors (`background_color` = `#424242`, `road_color` = `#444444`) |
| `res/values-night/` | Night colors (`background_color` = `#1A1A1A`, `road_color` = `#222222`) |
| `res/drawable/` | White dashed lane markers (`road_line_dash.xml`) |
| `res/drawable-night/` | White dashed lane markers (same stroke — works on dark bg) |

No code switch needed. Android resolves the correct resource folder automatically based on system theme.

---

## Adding Features — Quick Reference

| Feature | Touch points |
|---|---|
| New hazard type | Add SVG to `res/drawable/`, append `R.drawable.*` to `LogicManager.hazardResources` |
| 4th lane | `COLS = 4` in `MainActivity`, add 4th `AppCompatImageView` to each XML row + car row + road divider |
| Score counter | `LogicManager.tick()` increments a `score: Int`; add `main_LBL_score` `MaterialTextView` to XML |
| Pause/resume | Store the `Runnable` reference; `handler.removeCallbacks(runnable)` on pause, `handler.postDelayed(runnable, DELAY)` on resume |
| Difficulty scaling | Decrease `DELAY` or increase spawn probability (30%) in `LogicManager.tick()` over time |

See `app/src/main/FEATURES.md` for detailed step-by-step implementation plans.

---

## Documentation Map

| File | Purpose |
|---|---|
| `PROJECT.md` | Full architecture diagram, grid coordinate system, view naming, game loop flow, future features list |
| `app/src/main/java/com/example/avoided_race_app/CODE.md` | Per-method deep dive for both Kotlin source files |
| `app/src/main/res/layout/LAYOUT.md` | XML layout structure, layer stacking, constraint anchoring |
| `app/src/main/res/drawable/DRAWABLES.md` | All SVG assets, dashed-line technique, night-mode variants, how to add new drawables |
| `app/src/main/res/values/VALUES.md` | Color tokens, dimension system, string resources, theme hierarchy |
| `app/src/main/FEATURES.md` | Step-by-step implementation guide for each planned future feature |
