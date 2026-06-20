# Feature Plan 02 — Five-Lane Road

**Project:** `avoided_race_app`  
**Working dir:** `/Users/giladshekalim/repo/androidCourse/avoided_race_app`  
**Commit scope:** Expand the game from 3 lanes and 7 rows to 5 lanes and 9 rows.

> **Context files:**
> - `PROJECT.md` → "Grid Coordinate System" and "Key Design Decisions → Dynamic View ID Resolution" explain the matrix structure.
> - `CODE.md` → "`MainActivity.kt` — `findViews()`" explains how the 2D ImageView array is dynamically bound, and "`initViews()` — Input Handling" shows the boundary check that must be updated.
> - `LAYOUT.md` → "[3] Game Matrix" and "[1] Road Background" explain the weight-based grid and lane divider structure.

---

## What This Feature Does

Widens the game from 3 lanes to 5 lanes and adds 2 more visible obstacle rows (7→9). All hazards become slightly smaller due to the increased grid density. The player car starts in the center lane (lane 2 of 5). Lane dividers increase from 2 to 4.

---

## Files to Change

| File | Change Summary |
|---|---|
| `app/src/main/java/com/example/avoided_race_app/MainActivity.kt` | `ROWS = 9`, `COLS = 5`, fix lane boundary, update initial car lane, expand `carImages` array |
| `app/src/main/res/layout/activity_main.xml` | Add 2 ImageViews per existing row, add 2 new obstacle rows (rows 7 & 8), add 2 more car slots, add 2 more lane dividers, update weight sums |

`LogicManager.kt` requires **zero changes** — it is already fully parameterized with `rows` and `cols` passed in the constructor.

---

## Step-by-Step Implementation

### Step 1 — Update `MainActivity.kt` constants

File: `app/src/main/java/com/example/avoided_race_app/MainActivity.kt`

**Change 1a — Grid dimensions:**
```kotlin
// BEFORE
private val ROWS = 7
private val COLS = 3

// AFTER
private val ROWS = 9
private val COLS = 5
```

**Change 1b — Starting lane (center of 5):**
```kotlin
// BEFORE
private var currentCarLane = 1  // center of 3 lanes (0, 1, 2)

// AFTER
private var currentCarLane = 2  // center of 5 lanes (0, 1, 2, 3, 4)
```

**Change 1c — Right boundary check in `initViews()`:**
```kotlin
// BEFORE
main_BTN_right.setOnClickListener {
    if (currentCarLane < 2) {

// AFTER
main_BTN_right.setOnClickListener {
    if (currentCarLane < COLS - 1) {
```
The left check (`if (currentCarLane > 0)`) is already correct — no change needed.

**Change 1d — `carImages` array in `findViews()`:**
```kotlin
// BEFORE
carImages = arrayOf(
    findViewById(R.id.main_IMG_car_0),
    findViewById(R.id.main_IMG_car_1),
    findViewById(R.id.main_IMG_car_2)
)

// AFTER
carImages = arrayOf(
    findViewById(R.id.main_IMG_car_0),
    findViewById(R.id.main_IMG_car_1),
    findViewById(R.id.main_IMG_car_2),
    findViewById(R.id.main_IMG_car_3),
    findViewById(R.id.main_IMG_car_4)
)
```

The `obstacleMatrix` binding loop (`Array(ROWS) { r -> Array(COLS) { c -> ... getIdentifier(...) } }`) requires **no change** — it scales automatically with the updated `ROWS` and `COLS` values.

---

### Step 2 — Update `activity_main.xml` — Road Background

File: `app/src/main/res/layout/activity_main.xml`

Find the road background `LinearLayout` (the first child of the root `RelativeLayout`). It currently has `weightSum="3"` with 2 dividers between 3 lane spacers.

**Change: from 3 lanes to 5 lanes (add 2 more spacer+divider pairs):**

```xml
<!-- BEFORE -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="horizontal"
    android:weightSum="3">

    <View android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" />
    <View android:layout_width="4dp" android:layout_height="match_parent" android:background="@drawable/road_line" android:layerType="software" />
    <View android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" />
    <View android:layout_width="4dp" android:layout_height="match_parent" android:background="@drawable/road_line" android:layerType="software" />
    <View android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" />
</LinearLayout>

<!-- AFTER -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="horizontal"
    android:weightSum="5">

    <View android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" />
    <View android:layout_width="4dp" android:layout_height="match_parent" android:background="@drawable/road_line" android:layerType="software" />
    <View android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" />
    <View android:layout_width="4dp" android:layout_height="match_parent" android:background="@drawable/road_line" android:layerType="software" />
    <View android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" />
    <View android:layout_width="4dp" android:layout_height="match_parent" android:background="@drawable/road_line" android:layerType="software" />
    <View android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" />
    <View android:layout_width="4dp" android:layout_height="match_parent" android:background="@drawable/road_line" android:layerType="software" />
    <View android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" />
</LinearLayout>
```

---

### Step 3 — Update `activity_main.xml` — Game Matrix container

Find `main_LAY_matrix`. Change `weightSum` from `"8"` (7 obstacle rows + 1 car row) to `"10"` (9 obstacle rows + 1 car row):

```xml
<!-- BEFORE -->
<LinearLayout
    android:id="@+id/main_LAY_matrix"
    ...
    android:weightSum="8">

<!-- AFTER -->
<LinearLayout
    android:id="@+id/main_LAY_matrix"
    ...
    android:weightSum="10">
```

---

### Step 4 — Update `activity_main.xml` — Expand each existing obstacle row (rows 0–6)

For each of the 7 existing obstacle row `LinearLayout`s, add 2 more `AppCompatImageView` children — one for column 3 and one for column 4.

**Template for the 2 new columns to add inside each row:**
```xml
<androidx.appcompat.widget.AppCompatImageView
    android:id="@+id/main_IMG_obstacle_R_3"
    android:layout_width="0dp"
    android:layout_height="match_parent"
    android:layout_weight="1"
    android:visibility="invisible"
    tools:srcCompat="@drawable/fuel" />

<androidx.appcompat.widget.AppCompatImageView
    android:id="@+id/main_IMG_obstacle_R_4"
    android:layout_width="0dp"
    android:layout_height="match_parent"
    android:layout_weight="1"
    android:visibility="invisible"
    tools:srcCompat="@drawable/fuel" />
```

Replace `R` with the actual row number (0 through 6). Do this for all 7 existing rows.

After adding, each row should contain **5** `AppCompatImageView`s (columns 0 through 4).

---

### Step 5 — Add 2 new obstacle rows to `activity_main.xml` (rows 7 and 8)

Insert the following two complete row blocks **before** the car row (`main_LAY_car_row`), after the existing row 6 block.

**Row 7:**
```xml
<!-- Obstacle Row 7 -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:layout_weight="1"
    android:orientation="horizontal">
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/main_IMG_obstacle_7_0"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:visibility="invisible"
        tools:srcCompat="@drawable/broken_car" />
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/main_IMG_obstacle_7_1"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:visibility="invisible"
        tools:srcCompat="@drawable/broken_car" />
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/main_IMG_obstacle_7_2"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:visibility="invisible"
        tools:srcCompat="@drawable/broken_car" />
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/main_IMG_obstacle_7_3"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:visibility="invisible"
        tools:srcCompat="@drawable/broken_car" />
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/main_IMG_obstacle_7_4"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:visibility="invisible"
        tools:srcCompat="@drawable/broken_car" />
</LinearLayout>
```

**Row 8:** Copy the same structure, replace all `7_` with `8_`.

---

### Step 6 — Update the car row in `activity_main.xml`

Find `main_LAY_car_row`. Currently has 3 `AppCompatImageView`s (car_0, car_1, car_2). Replace the entire block:

```xml
<!-- Car Row (Row 9 — visual row 10 in weight layout) -->
<LinearLayout
    android:id="@+id/main_LAY_car_row"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:layout_weight="1"
    android:orientation="horizontal">
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/main_IMG_car_0"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:visibility="invisible"
        app:srcCompat="@drawable/racing_car" />
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/main_IMG_car_1"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:visibility="invisible"
        app:srcCompat="@drawable/racing_car" />
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/main_IMG_car_2"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:visibility="visible"
        app:srcCompat="@drawable/racing_car" />
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/main_IMG_car_3"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:visibility="invisible"
        app:srcCompat="@drawable/racing_car" />
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/main_IMG_car_4"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:visibility="invisible"
        app:srcCompat="@drawable/racing_car" />
</LinearLayout>
```

`main_IMG_car_2` starts `visible` (center lane of 5). All others start `invisible`.

---

### Step 7 — Verify

Run the app. Confirm:
- [ ] 5 lanes visible with 4 lane dividers
- [ ] Car starts in the center (3rd lane from left)
- [ ] LEFT button stops at lane 0 (can't go further left)
- [ ] RIGHT button stops at lane 4 (can't go further right)
- [ ] Hazards appear in all 5 lanes and fall correctly
- [ ] Collision detection still works correctly
- [ ] Game reset clears the full 9×5 grid

---

## Commit Message

```
Expand road from 3 lanes to 5 lanes with 9 obstacle rows

- COLS: 3 → 5, ROWS: 7 → 9
- Car starts at center lane (index 2 of 5)
- Fixed right boundary check to use COLS-1
- Added 2 lane dividers and 2 columns to each obstacle row
- Added 2 new obstacle rows (rows 7 and 8)
- Expanded car row to 5 slots (car_0 through car_4)
- LogicManager unchanged (already parameterized)
```
