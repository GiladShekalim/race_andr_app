# Layout Reference — `activity_main.xml`

One file, one screen. The entire game UI lives in `activity_main.xml`. There are no fragments, no second layouts, no `include` tags.

---

## Root Container

```xml
<RelativeLayout
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background_color">
```

`RelativeLayout` is the root because it allows:
- The road background to fill the entire screen independently
- The game matrix to be anchored between the hearts (top) and buttons (bottom)
- The buttons to be pinned to their respective screen corners
- The overlay text to be centered on top of everything with `elevation`

---

## Layer Stacking (top to bottom in XML = back to front)

```
┌─────────────────────────────────────────┐
│  [1] LinearLayout road background       │ ← drawn first (background layer)
│      Two 4dp vertical lines (lane dividers)
├─────────────────────────────────────────┤
│  [2] LinearLayout hearts (lives row)    │ ← top-center
├─────────────────────────────────────────┤
│  [3] LinearLayout game matrix           │ ← fills space between hearts and buttons
│      7 obstacle rows + 1 car row
├─────────────────────────────────────────┤
│  [4] FAB LEFT (bottom-start corner)     │ ← drawn after matrix, appears above
│  [5] FAB RIGHT (bottom-end corner)      │
├─────────────────────────────────────────┤
│  [6] MaterialTextView overlay           │ ← elevation:10dp, drawn last
│      "MONEY LOST!" / "BANKRUPT!"
└─────────────────────────────────────────┘
```

---

## [1] Road Background

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="horizontal"
    android:weightSum="3">

    <View android:layout_weight="1" />          <!-- Lane 0 -->
    <View android:width="4dp" android:background="@drawable/road_line" android:layerType="software" />
    <View android:layout_weight="1" />          <!-- Lane 1 -->
    <View android:width="4dp" android:background="@drawable/road_line" android:layerType="software" />
    <View android:layout_weight="1" />          <!-- Lane 2 -->
</LinearLayout>
```

Three equal-weight `View`s separated by 4dp `road_line` divider views. The dividers use `android:layerType="software"` because `road_line.xml` is a `<layer-list>` containing a `<rotate>` element — hardware acceleration can misrender rotated drawables at certain scales.

The `road_line.xml` drawable wraps `road_line_dash.xml` in a `<layer-list>` with a 90° rotation. `road_line_dash.xml` is a dashed horizontal line; rotating it 90° creates a vertical dashed lane marker.

This background layer fills the full screen but has no click interaction.

---

## [2] Hearts (Lives Indicator)

```xml
<LinearLayout
    android:id="@+id/main_LAY_hearts"
    android:layout_alignParentTop="true"
    android:layout_centerHorizontal="true"
    android:layout_marginTop="@dimen/heart_top_margin"    <!-- 60dp top margin -->
    android:orientation="horizontal">

    <AppCompatImageView android:id="@+id/main_IMG_heart0" android:layout_width="@dimen/heart_size" ... />
    <AppCompatImageView android:id="@+id/main_IMG_heart1" ... />
    <AppCompatImageView android:id="@+id/main_IMG_heart2" ... />
</LinearLayout>
```

- `heart_size` = 54dp, `heart_margin` = 12dp, `heart_top_margin` = 60dp (clears the device camera cutout/notch area)
- `app:srcCompat="@drawable/money_bag"` — uses the money bag SVG as the life icon
- Visibility toggled via `View.INVISIBLE` (not GONE) so the container never resizes

---

## [3] Game Matrix (Main Playing Field)

```xml
<LinearLayout
    android:id="@+id/main_LAY_matrix"
    android:orientation="vertical"
    android:layout_below="@id/main_LAY_hearts"
    android:layout_above="@+id/main_BTN_left"
    android:weightSum="8">
```

The matrix anchors:
- **Top:** below the hearts row (`layout_below`)
- **Bottom:** above the LEFT FAB (`layout_above`) — this makes the matrix automatically resize to fill available space between the two anchors

`weightSum="8"` with 8 child `LinearLayout`s each having `layout_height="0dp"` and `layout_weight="1"` = each row gets exactly 1/8 of the available height.

### Row Structure (repeated 7 times for obstacles, once for car)

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:layout_weight="1"
    android:orientation="horizontal">

    <AppCompatImageView
        android:id="@+id/main_IMG_obstacle_R_0"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:visibility="invisible" />
    <!-- repeated for columns 1, 2 -->
</LinearLayout>
```

Each row: horizontal `LinearLayout` with 3 equal-weight `AppCompatImageView`s, each `0dp×match_parent`. Initial visibility is `invisible` (not `gone`). The `refreshUI()` method sets `setImageResource()` + `VISIBLE` for non-zero cells, `INVISIBLE` for zero cells.

**ID naming pattern:** `main_IMG_obstacle_{row}_{col}` — mandatory. `MainActivity` uses `resources.getIdentifier("main_IMG_obstacle_${r}_${c}", "id", packageName)` to find these views dynamically.

### Car Row (Row 7 — index 7 in weight layout)

```xml
<LinearLayout android:id="@+id/main_LAY_car_row" ...>
    <AppCompatImageView android:id="@+id/main_IMG_car_0" android:visibility="invisible" app:srcCompat="@drawable/racing_car" />
    <AppCompatImageView android:id="@+id/main_IMG_car_1" android:visibility="visible"   app:srcCompat="@drawable/racing_car" />
    <AppCompatImageView android:id="@+id/main_IMG_car_2" android:visibility="invisible" app:srcCompat="@drawable/racing_car" />
</LinearLayout>
```

Car starts in lane 1 (center) — `main_IMG_car_1` is initially `visible`, others `invisible`. The `racing_car` SVG is pre-rotated 270° internally (the car faces upward in the layout). `refreshUI()` toggles which of the three car slots is visible.

---

## [4] & [5] Control FABs

```xml
<ExtendedFloatingActionButton
    android:id="@+id/main_BTN_left"
    android:layout_alignParentBottom="true"
    android:layout_alignParentStart="true"
    android:layout_margin="@dimen/default_margin"    <!-- 16dp -->
    android:text="LEFT"
    app:icon="@drawable/ic_left" />

<ExtendedFloatingActionButton
    android:id="@+id/main_BTN_right"
    android:layout_alignParentBottom="true"
    android:layout_alignParentEnd="true"
    ... />
```

`ExtendedFloatingActionButton` (from Material 3) — shows both icon and text. Positioned at bottom-start and bottom-end corners of the `RelativeLayout`.

Note: `main_LAY_matrix` uses `android:layout_above="@+id/main_BTN_left"` with a forward-reference `@+id/` to declare the FAB's ID before it appears in the XML. This is why the FAB IDs in the matrix's `layout_above` attributes use `@+id/` (first declaration) while the actual FAB elements later use `@+id/` too (Android handles the forward reference).

---

## [6] Overlay Text

```xml
<MaterialTextView
    android:id="@+id/main_LBL_money_lost"
    android:layout_centerInParent="true"
    android:text="@string/money_lost"
    android:textSize="40sp"
    android:textStyle="bold"
    android:textColor="@android:color/holo_red_dark"
    android:alpha="0.7"
    android:visibility="gone"
    android:elevation="10dp" />
```

- `visibility="gone"` at start — takes no space (not `invisible`)
- `elevation="10dp"` ensures it draws on top of all other views
- `alpha="0.7"` — semi-transparent red for dramatic effect
- Text is swapped at runtime: `getString(R.string.money_lost)` → "MONEY LOST!" or `getString(R.string.bankrupt)` → "BANKRUPT!"
- Despite the type being `MaterialTextView`, it's declared as `View` in `MainActivity` (causes runtime cast)

---

## Layout Constraints Summary

| View | Top | Bottom | Start | End |
|---|---|---|---|---|
| Road background | parent | parent | parent | parent |
| Hearts | parent (60dp margin) | — | center | center |
| Game matrix | below hearts | above BTN_left | parent | parent |
| BTN_left | — | parent | parent | — |
| BTN_right | — | parent | — | parent |
| Overlay text | center | center | center | center |

---

## Adding a New UI Element

**Score display:**
```xml
<MaterialTextView
    android:id="@+id/main_LBL_score"
    android:layout_alignParentTop="true"
    android:layout_alignParentEnd="true"
    android:layout_margin="@dimen/default_margin"
    android:textSize="24sp"
    android:textColor="@android:color/white"
    android:text="0" />
```
Anchor to `alignParentTop + alignParentEnd` to sit in the top-right corner without interfering with the centered hearts.

**4th lane:** Add a 4th `AppCompatImageView` to each existing row in the matrix, add a 4th car slot, update `COLS = 4` in `MainActivity`, and add a third lane divider to the road background layout. The `getIdentifier` calls automatically scale.

**Pause button:** Add an `ExtendedFloatingActionButton` anchored `android:layout_alignParentBottom="true"` + `android:layout_centerHorizontal="true"` between the LEFT and RIGHT FABs.
