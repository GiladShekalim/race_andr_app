# Feature Plan 12 — Scoreboard Map Fragment (OSMDroid)

**Project:** `avoided_race_app`  
**Working dir:** `/Users/giladshekalim/repo/androidCourse/avoided_race_app`  
**Commit scope:** Add `ScoreMapFragment` (Fragment 2) to the `ScoreboardActivity`. Uses OSMDroid (OpenStreetMap) to display a map with pins for all top-10 score locations. Tapping a row in the score table (Fragment 1) centers the map on that score's location via the `OnScoreSelectedListener` interface already wired in Plan 11.

> **Prerequisites:**
> - **Plan 10 (Score Storage)** — `ScoreEntry` with `latitude`/`longitude` must exist.
> - **Plan 11 (Scoreboard Table)** — `ScoreboardActivity`, `OnScoreSelectedListener` interface, `scoreboard_FRAME_map` container, and the row-click callback must already be in place. Plan 12 fills in the Activity's `onScoreSelected()` method and adds the map fragment to the container.

> **Context files:**
> - `26B-10208-L06.md` (reference repo) → "Fragment-to-Fragment Communication via Activity Mediator" — the `OnScoreSelectedListener` already implemented in Plan 11 is the exact same pattern. Plan 12 only adds the receiver side (Fragment 2).
> - `PROJECT.md` → "Future Addition Points" — map feature noted as using OSMDroid.
> - `PLAN_11_SCOREBOARD_TABLE.md` → Step 3 (`ScoreboardActivity.onScoreSelected` is currently a comment placeholder — this plan fills it in).

---

## What This Feature Does

- Adds `ScoreMapFragment` to the bottom half of `ScoreboardActivity`
- Map shows **all top-10 score locations** as pins when the scoreboard first opens
- **Tapping a row** in Fragment 1 (table) calls `ScoreboardActivity.onScoreSelected(lat, lng)`, which calls `ScoreMapFragment.centerOn(lat, lng)` — the map animates to that score's location and highlights its pin
- OSMDroid renders OpenStreetMap tiles — no API key required
- Map respects `onResume`/`onPause` lifecycle (required by OSMDroid)
- Attribution notice ("© OpenStreetMap contributors") shown on the map

---

## Hidden / Non-Obvious Steps

1. **OSMDroid requires `Configuration.getInstance().load()` before any `MapView` is used** — initialize it in `GameApplication.onCreate()` (not in the Fragment). Failing to do this causes crashes or missing tiles.

2. **OSMDroid needs `INTERNET` permission to download tiles** — already added in Plan 10's `AndroidManifest.xml` step.

3. **`MapView.onResume()` / `MapView.onPause()` must be called from the Fragment's lifecycle** — OSMDroid does not do this automatically. Missing these calls causes tiles to stop loading or internal state corruption.

4. **`GeoPoint` is OSMDroid's coordinate class** — not Android's `Location`. Convert: `GeoPoint(latitude, longitude)`.

5. **Default map center when no row is selected** — if all top-10 scores have `lat=0, lng=0` (no GPS ever fixed), the map would zoom to the null island. Add a guard: if all points are 0,0, use a default center (e.g., Tel Aviv: `GeoPoint(32.0853, 34.7818)`) or show a "No location data" message.

6. **`Marker` in OSMDroid requires a `Drawable` icon** — use `ContextCompat.getDrawable(context, R.drawable.ic_marker)` or the built-in `ResourceProxy` default icon. The plan uses the default OSMDroid marker (no extra drawable needed).

7. **Fragment reference in `ScoreboardActivity`** — `onScoreSelected` must call `ScoreMapFragment.centerOn()`. The Fragment is retrieved via `supportFragmentManager.findFragmentById(R.id.scoreboard_FRAME_map)` cast to `ScoreMapFragment`. Null-safe cast (`as?`) prevents crashes if the map fragment isn't loaded yet.

8. **Pin for the currently selected row** — visually differentiate the selected pin vs. all others. One approach: re-draw all markers in grey, then re-draw the selected one in gold/accent color.

---

## New Dependency

Add to `gradle/libs.versions.toml`:

```toml
[versions]
osmdroid = "6.1.20"

[libraries]
osmdroid = { group = "org.osmdroid", name = "osmdroid-android", version.ref = "osmdroid" }
```

Add to `app/build.gradle.kts` in `dependencies {}`:

```kotlin
implementation(libs.osmdroid)
```

---

## Files to Create

| File | Purpose |
|---|---|
| `app/src/main/java/com/example/avoided_race_app/ScoreMapFragment.kt` | Fragment 2: OSMDroid map with pins |
| `app/src/main/res/layout/fragment_score_map.xml` | Fragment 2 layout (just a MapView) |

## Files to Modify

| File | Change |
|---|---|
| `app/src/main/java/com/example/avoided_race_app/GameApplication.kt` | Initialize OSMDroid Configuration |
| `app/src/main/java/com/example/avoided_race_app/ScoreboardActivity.kt` | Add map Fragment, implement `onScoreSelected` body |
| `gradle/libs.versions.toml` | Add OSMDroid version + library |
| `app/build.gradle.kts` | Add OSMDroid dependency |

---

## Step-by-Step Implementation

### Step 1 — Initialize OSMDroid in `GameApplication`

File: `app/src/main/java/com/example/avoided_race_app/GameApplication.kt`

Add OSMDroid Configuration init to `onCreate()`:

```kotlin
package com.example.avoided_race_app

import android.app.Application
import com.example.avoided_race_app.db.AppDatabase
import org.osmdroid.config.Configuration

class GameApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        // Required before any MapView is created
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
    }
}
```

`userAgentValue` is required by OSMDroid's tile servers — must be a non-empty, non-default string. Using `packageName` (`com.example.avoided_race_app`) is correct.

---

### Step 2 — Create `fragment_score_map.xml`

File: `app/src/main/res/layout/fragment_score_map.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <org.osmdroid.views.MapView
        android:id="@+id/score_MAP"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:clickable="true" />

    <!-- OSMDroid copyright attribution (required by OSM license) -->
    <com.google.android.material.textview.MaterialTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:padding="4dp"
        android:text="© OpenStreetMap contributors"
        android:textSize="10sp"
        android:textColor="#80FFFFFF"
        android:background="#40000000" />

</FrameLayout>
```

---

### Step 3 — Create `ScoreMapFragment.kt`

File: `app/src/main/java/com/example/avoided_race_app/ScoreMapFragment.kt`

```kotlin
package com.example.avoided_race_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.avoided_race_app.db.ScoreEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class ScoreMapFragment : Fragment() {

    private lateinit var mapView: MapView
    private var allEntries: List<ScoreEntry> = emptyList()

    // Default center: Tel Aviv (used when no GPS data exists)
    private val DEFAULT_CENTER = GeoPoint(32.0853, 34.7818)
    private val DEFAULT_ZOOM = 10.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_score_map, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.score_MAP)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(DEFAULT_ZOOM)
        mapView.controller.setCenter(DEFAULT_CENTER)

        loadAndPinAllScores()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()   // required by OSMDroid
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()    // required by OSMDroid
    }

    private fun loadAndPinAllScores() {
        lifecycleScope.launch(Dispatchers.IO) {
            val scores = (requireActivity().application as GameApplication)
                .database.scoreDao().getTop10()
            withContext(Dispatchers.Main) {
                allEntries = scores
                pinAllScores(highlightedEntry = null)
            }
        }
    }

    private fun pinAllScores(highlightedEntry: ScoreEntry?) {
        mapView.overlays.clear()

        val validEntries = allEntries.filter { it.latitude != 0.0 || it.longitude != 0.0 }

        if (validEntries.isEmpty()) {
            mapView.controller.setCenter(DEFAULT_CENTER)
            mapView.controller.setZoom(DEFAULT_ZOOM)
            mapView.invalidate()
            return
        }

        validEntries.forEach { entry ->
            val marker = Marker(mapView)
            marker.position = GeoPoint(entry.latitude, entry.longitude)
            marker.title = "Score: ${entry.score}"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            if (entry == highlightedEntry) {
                // Highlighted pin: use accent color tint (gold)
                marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_location_on_24)
                    ?.mutate()
                    ?.also { it.setTint(0xFFFFD700.toInt()) }
            }
            // Default: OSMDroid uses its own default marker icon when icon is null

            mapView.overlays.add(marker)
        }

        // If first load, center on the highest score's location
        if (highlightedEntry == null && validEntries.isNotEmpty()) {
            val top = validEntries.first()
            mapView.controller.animateTo(GeoPoint(top.latitude, top.longitude))
            mapView.controller.setZoom(12.0)
        }

        mapView.invalidate()
    }

    fun centerOn(latitude: Double, longitude: Double) {
        val target = GeoPoint(latitude, longitude)
        mapView.controller.animateTo(target)
        mapView.controller.setZoom(14.0)

        // Highlight the matching entry
        val selected = allEntries.find { it.latitude == latitude && it.longitude == longitude }
        pinAllScores(highlightedEntry = selected)
    }
}
```

**About `R.drawable.ic_baseline_location_on_24`:** This is a standard Material Design icon available in Android. Add it via: in Android Studio, right-click `res/drawable` → New → Vector Asset → search "location_on". It's a simple map pin SVG. Alternatively, leave `marker.icon = null` for OSMDroid's default orange pin (no custom drawable needed).

---

### Step 4 — Update `ScoreboardActivity.kt` to add map Fragment and wire callback

File: `app/src/main/java/com/example/avoided_race_app/ScoreboardActivity.kt`

```kotlin
package com.example.avoided_race_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ScoreboardActivity : AppCompatActivity(), OnScoreSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scoreboard)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.scoreboard_FRAME_table, ScoreTableFragment())
                .replace(R.id.scoreboard_FRAME_map, ScoreMapFragment())   // ADD
                .commit()
        }
    }

    override fun onScoreSelected(latitude: Double, longitude: Double) {
        // Route row-click from Fragment 1 → Fragment 2
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.scoreboard_FRAME_map) as? ScoreMapFragment
        mapFragment?.centerOn(latitude, longitude)
    }
}
```

This completes the communication flow:
**Row tap (Fragment 1)** → `listener.onScoreSelected(lat, lng)` → **ScoreboardActivity** → `mapFragment.centerOn(lat, lng)` → **Map animates (Fragment 2)**

The pattern is identical to the L06 reference repo's `CallbackHighScoreClicked` mediator — see `26B-10208-L06.md` for the teaching context behind this structure.

---

### Step 5 — Verify

Play several games at different GPS locations, then open the scoreboard. Confirm:
- [ ] Map loads and shows OpenStreetMap tiles
- [ ] Pins appear for all top-10 entries that have valid GPS coordinates
- [ ] Entries with `lat=0, lng=0` are filtered out (don't show a pin at null island)
- [ ] Initial map centers on the #1 score's location (highest score)
- [ ] Tapping a row in the table → map animates to that score's pin location
- [ ] Selected pin is highlighted differently from other pins
- [ ] "© OpenStreetMap contributors" attribution is visible on the map
- [ ] Rotating the screen doesn't duplicate fragments (savedInstanceState guard)
- [ ] Back button from Scoreboard returns to Menu

---

## Commit Message

```
Add OSMDroid map fragment with score location pins to scoreboard

ScoreMapFragment renders all top-10 GPS locations as pins on an 
OpenStreetMap map. Row tap in ScoreTableFragment triggers 
OnScoreSelectedListener → Activity mediator → ScoreMapFragment.centerOn(). 
OSMDroid initialized in GameApplication; lifecycle methods wired.
```
