# Drawables Reference — `res/drawable/` & `res/drawable-night/`

All assets are **XML vector drawables** — no raster images (no PNG, JPEG, WebP). This means they scale perfectly to any screen density and are small in file size.

---

## Asset Catalog

| File | Type | Used As | Notes |
|---|---|---|---|
| `racing_car.xml` | `<vector>` (complex) | Player car in all 3 lanes | Rotated 270° internally via `<group android:rotation="270">` |
| `money_bag.xml` | `<vector>` (complex) | Life indicator icons (×3) | Gold/yellow coin-bag with "$" detail |
| `bankrupt.xml` | `<vector>` (complex) | Hazard — bankrupt sign | Displayed on obstacle cells; also the "game over" hazard name |
| `broken_car.xml` | `<vector>` (complex) | Hazard — broken car | |
| `clinic.xml` | `<vector>` (complex) | Hazard — clinic/hospital | |
| `fuel.xml` | `<vector>` (complex) | Hazard — fuel spill | |
| `no_tax.xml` | `<vector>` (complex) | Hazard — no-tax sign | |
| `road_line.xml` | `<layer-list>` | Lane divider on road background | Wraps `road_line_dash` with 90° rotation |
| `road_line_dash.xml` | `<shape>` dashed line | Source for road_line | Used only via `road_line.xml` |
| `ic_left.xml` | `<vector>` (simple) | LEFT FAB icon | Arrow pointing left |
| `ic_right.xml` | `<vector>` (simple) | RIGHT FAB icon | Arrow pointing right |
| `ic_launcher_background.xml` | `<vector>` | App icon background | Standard launcher asset |
| `ic_launcher_foreground.xml` | `<vector>` | App icon foreground | Standard launcher asset |

---

## Drawable-Night Variants

| File | Difference from Day |
|---|---|
| `drawable-night/road_line.xml` | Same structure, references `road_line_dash` (which is also in drawable-night/) |
| `drawable-night/road_line_dash.xml` | Likely different color for night mode road appearance |

Android automatically selects `drawable-night/` assets when the device is in dark mode. The game hazards and car do NOT have night variants — only road lane dividers do.

---

## How Hazard Assets Connect to Code

`LogicManager` stores hazard resource IDs:
```kotlin
private val hazardResources = listOf(
    R.drawable.fuel,        // → drawable/fuel.xml
    R.drawable.clinic,      // → drawable/clinic.xml
    R.drawable.no_tax,      // → drawable/no_tax.xml
    R.drawable.broken_car,  // → drawable/broken_car.xml
    R.drawable.bankrupt     // → drawable/bankrupt.xml
)
```

These IDs are stored in the logic matrix as integers. `MainActivity.refreshUI()` reads them:
```kotlin
val resId = logicManager.getResourceId(r, c)
if (resId != 0) {
    obstacleMatrix[r][c].setImageResource(resId)   // ← applies the vector drawable
    obstacleMatrix[r][c].visibility = View.VISIBLE
}
```

**To add a new hazard:** Create a new `<vector>` XML in `drawable/`, then add `R.drawable.your_new_hazard` to `LogicManager.hazardResources`. No other changes needed.

---

## `racing_car.xml` — Design Notes

The car SVG (`512×512dp` viewport) renders a top-down race car in pink/salmon (`#FC7C95`, `#FEAABB`) and grey (`#8690AF`) colors. The car faces *right* in the SVG coordinate system, but the entire vector is wrapped in:
```xml
<group android:rotation="270" android:pivotX="256" android:pivotY="256">
```
This rotates the car 270° (= 90° counter-clockwise), making it face **upward** on screen — the natural direction for an upward-scrolling racing game.

---

## `road_line.xml` — How the Dashed Lane Divider Works

```xml
<!-- road_line.xml -->
<layer-list>
    <item android:left="-500dp" android:right="-500dp">
        <rotate
            android:drawable="@drawable/road_line_dash"
            android:fromDegrees="90"
            android:toDegrees="90" />
    </item>
</layer-list>
```

`road_line_dash.xml` is a dashed horizontal shape. It is rotated 90° to become vertical. The `left="-500dp"` and `right="-500dp"` offsets extend the item far beyond its bounds to prevent clipping artifacts during rotation. The `android:layerType="software"` attribute on the View using this drawable is required — hardware acceleration can cause invisible or garbled output for rotated layer-lists.

---

## Vector File Structure Pattern

All hazard/icon vectors follow the same basic structure:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:viewportWidth="512"
    android:viewportHeight="512"
    android:width="512dp"
    android:height="512dp">
    <path android:pathData="..." android:fillColor="#RRGGBB" ... />
    <!-- multiple paths for different color regions -->
</vector>
```

- All use `512×512` viewport (square)
- Multiple `<path>` elements with different `fillColor` values create the multi-color designs
- Some paths use `fillAlpha` for semi-transparency
- The `strokeColor` often matches `fillColor` (redundant for filled shapes, common in SVG-exported vectors)

The complex vectors (car, money bag, hazards) were almost certainly exported from a design tool (Inkscape, Figma, or similar) rather than hand-coded. They contain many small detail paths with partial-pixel coordinates.

---

## Sizing in the UI

Hazard cells and car cells fill their parent `AppCompatImageView` which is `0dp × match_parent` in a weight-based layout. The views scale the vector drawables to fit. No explicit size is set on the ImageViews — they stretch to fill the equal-weight grid cells.

Life indicator icons are sized via `@dimen/heart_size = 54dp`.

---

## Adding a Custom Asset

1. Create the vector XML in `res/drawable/your_asset.xml`
2. Keep the viewport at `512×512` to match existing assets
3. If it's a hazard: add `R.drawable.your_asset` to `LogicManager.hazardResources`
4. If it needs a night-mode variant: duplicate to `res/drawable-night/your_asset.xml` with adjusted colors
5. Reference sizes via `dimens.xml` tokens, not hardcoded values
