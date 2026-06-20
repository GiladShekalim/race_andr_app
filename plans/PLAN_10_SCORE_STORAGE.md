# Feature Plan 10 — Score Storage with Room + GPS Location Capture

**Project:** `avoided_race_app`  
**Working dir:** `/Users/giladshekalim/repo/androidCourse/avoided_race_app`  
**Commit scope:** Set up a Room database to persist game scores and GPS coordinates. Capture the player's last known location at bankrupt time and save the final score + location to Room. This is the data layer that Plans 11 and 12 (scoreboard UI) read from.

> **Prerequisites:**
> - **Plan 04 (Game Score)** must be done — `score` field and `updateScoreUI()` must exist.
> - **Plan 07 (Menu Screen)** must be done — `resetGame()` now calls `finish()` after showing "BANKRUPT!". The score save happens in that gap.

> **Context files:**
> - `CODE.md` → "`resetGame()`" — this is where score saving is triggered. Plan 07 left a `// TODO (Plan 10): save score + GPS location here` comment.
> - `PROJECT.md` → "Tech Stack" for current dependencies; new Room + FusedLocation deps go in `libs.versions.toml`.
> - `26A-10208-L05.md` (reference repo) → "App : Application()" class — Room's `AppDatabase` singleton should be initialized in the Application class, same pattern.
> - `VALUES.md` → no changes here; Room generates its own schema.

---

## What This Feature Does

- Adds **Room** database with a `ScoreEntry` entity (score, timestamp, latitude, longitude)
- Adds **FusedLocationProviderClient** to `MainActivity` that tracks the last known location
- Requests `ACCESS_FINE_LOCATION` at runtime if not already granted
- On bankrupt: saves `ScoreEntry(score, System.currentTimeMillis(), lat, lng)` to Room DB on a background thread, then finishes to menu
- If location permission is denied: saves the score with `lat = 0.0, lng = 0.0` (graceful fallback — score is still saved)
- Room DB is a singleton in the `Application` class

---

## Hidden / Non-Obvious Steps

1. **Room cannot run queries on the main thread by default** — calling `dao.insert()` from `resetGame()` (which runs on the main thread) will throw `IllegalStateException`. Use a coroutine (`lifecycleScope.launch(Dispatchers.IO)`) or `allowMainThreadQueries()` (acceptable for simple local storage at this level).

2. **`FusedLocationProviderClient.lastLocation` is async** — it returns a `Task<Location?>` that may resolve to `null` if the device has never acquired a GPS fix. The plan uses `addOnCompleteListener` to handle this. Score saving must happen inside the callback or use a pre-cached location.

3. **Location must be requested before bankrupt** — `lastLocation` only returns a value if the device has already obtained a GPS fix at some point. To guarantee a non-null location, start requesting location updates early (`onCreate`) and cache the most recent fix. The plan uses `requestLocationUpdates` with a simple listener that caches `lastKnownLocation`.

4. **Runtime permission request flow** — `ACCESS_FINE_LOCATION` must be requested at runtime (not just declared in the manifest). If the user denies it: score saves with `lat=0, lng=0`. The request should happen in `onCreate()` before the game starts, giving the user time to respond.

5. **`Application` class must be declared in `AndroidManifest.xml`** — if the `GameApplication` class is not declared with `android:name`, it won't be used and the DB singleton won't initialize.

6. **Room entity `@PrimaryKey(autoGenerate = true)`** — each score gets a unique row ID. Top-10 query uses `ORDER BY score DESC LIMIT 10`.

7. **`FusedLocationProviderClient` requires `play-services-location` dependency** — add to `libs.versions.toml` and `build.gradle.kts`.

---

## New Dependencies

Add to `gradle/libs.versions.toml`:

```toml
[versions]
room = "2.7.1"
play-services-location = "21.3.0"

[libraries]
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "play-services-location" }
```

Add to `app/build.gradle.kts` in `dependencies {}`:

```kotlin
implementation(libs.room.runtime)
implementation(libs.room.ktx)
kapt(libs.room.compiler)
implementation(libs.play.services.location)
```

Add at the top of `app/build.gradle.kts` plugins block:
```kotlin
id("kotlin-kapt")
```

---

## Files to Create

| File | Purpose |
|---|---|
| `app/src/main/java/com/example/avoided_race_app/db/ScoreEntry.kt` | Room entity |
| `app/src/main/java/com/example/avoided_race_app/db/ScoreDao.kt` | DAO with insert + top-10 query |
| `app/src/main/java/com/example/avoided_race_app/db/AppDatabase.kt` | Room singleton database |
| `app/src/main/java/com/example/avoided_race_app/GameApplication.kt` | Application class holding DB instance |

## Files to Modify

| File | Change |
|---|---|
| `app/src/main/AndroidManifest.xml` | Add location permissions + `GameApplication` in `android:name` |
| `app/src/main/java/com/example/avoided_race_app/MainActivity.kt` | Add location client, permission request, score save on bankrupt |
| `gradle/libs.versions.toml` | Add Room + location versions and libs |
| `app/build.gradle.kts` | Add dependencies + kapt plugin |

---

## Step-by-Step Implementation

### Step 1 — Create `ScoreEntry.kt` (Room Entity)

File: `app/src/main/java/com/example/avoided_race_app/db/ScoreEntry.kt`

```kotlin
package com.example.avoided_race_app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class ScoreEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val score: Int,
    val timestamp: Long,   // System.currentTimeMillis()
    val latitude: Double,
    val longitude: Double
)
```

---

### Step 2 — Create `ScoreDao.kt`

File: `app/src/main/java/com/example/avoided_race_app/db/ScoreDao.kt`

```kotlin
package com.example.avoided_race_app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScoreDao {
    @Insert
    fun insert(entry: ScoreEntry)

    @Query("SELECT * FROM scores ORDER BY score DESC LIMIT 10")
    fun getTop10(): List<ScoreEntry>
}
```

---

### Step 3 — Create `AppDatabase.kt`

File: `app/src/main/java/com/example/avoided_race_app/db/AppDatabase.kt`

```kotlin
package com.example.avoided_race_app.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(entities = [ScoreEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scores_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
```

`fallbackToDestructiveMigration()` drops and recreates the DB on schema version change. Acceptable for local game scores.

---

### Step 4 — Create `GameApplication.kt`

File: `app/src/main/java/com/example/avoided_race_app/GameApplication.kt`

```kotlin
package com.example.avoided_race_app

import android.app.Application
import com.example.avoided_race_app.db.AppDatabase

class GameApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
```

Same pattern as `App : Application()` in the L05 reference repo — see `26A-10208-L05.md` → "App : Application() Class".

---

### Step 5 — Register `GameApplication` in `AndroidManifest.xml`

In `<application>` tag, add `android:name`:

```xml
<application
    android:name=".GameApplication"
    ...>
```

Add permissions (before `<application>`):

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

`INTERNET` is needed now so it's ready for OSMDroid map tiles in Plans 11–12.

---

### Step 6 — Add location tracking to `MainActivity.kt`

**Imports to add:**
```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.avoided_race_app.db.ScoreEntry
import androidx.lifecycle.lifecycleScope
```

**Fields to add:**
```kotlin
private lateinit var fusedLocationClient: FusedLocationProviderClient
private var lastKnownLocation: Location? = null
private lateinit var locationCallback: LocationCallback
private val LOCATION_PERMISSION_REQUEST = 1001
```

**In `onCreate()`, after `initViews()`:**
```kotlin
fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
requestLocationPermission()
```

**Add permission request method:**
```kotlin
private fun requestLocationPermission() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
        startLocationUpdates()
    } else {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST
        )
    }
}

override fun onRequestPermissionsResult(
    requestCode: Int, permissions: Array<String>, grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == LOCATION_PERMISSION_REQUEST &&
        grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        startLocationUpdates()
    }
    // If denied: lastKnownLocation stays null → score saved with lat=0, lng=0
}
```

**Add `startLocationUpdates()` and callback:**
```kotlin
private fun startLocationUpdates() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) return

    locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            lastKnownLocation = result.lastLocation
        }
    }

    val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5000L
    ).build()

    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
}
```

**Stop location updates in `onDestroy()`:**
```kotlin
override fun onDestroy() {
    super.onDestroy()
    if (::gameRunnable.isInitialized) handler.removeCallbacks(gameRunnable)
    stopRamp()
    cancelTiltMove()
    sensorManager.unregisterListener(sensorEventListener)
    if (::locationCallback.isInitialized) fusedLocationClient.removeLocationUpdates(locationCallback)
    // soundPool.release()  — if Plan 03 was implemented
    // stopOdometer()       — if Plan 06 was implemented
}
```

---

### Step 7 — Save score on bankrupt in `resetGame()`

Replace the `// TODO (Plan 10)` comment with the actual save:

```kotlin
private fun resetGame() {
    if (::gameRunnable.isInitialized) handler.removeCallbacks(gameRunnable)
    stopRamp()

    if (main_LBL_money_lost is android.widget.TextView) {
        (main_LBL_money_lost as android.widget.TextView).text = getString(R.string.bankrupt)
    }
    main_LBL_money_lost.visibility = View.VISIBLE

    // Save score with location
    val lat = lastKnownLocation?.latitude ?: 0.0
    val lng = lastKnownLocation?.longitude ?: 0.0
    val entry = ScoreEntry(
        score = score,
        timestamp = System.currentTimeMillis(),
        latitude = lat,
        longitude = lng
    )
    lifecycleScope.launch(Dispatchers.IO) {
        (application as GameApplication).database.scoreDao().insert(entry)
    }

    handler.postDelayed({ finish() }, 1500)
}
```

`lifecycleScope.launch(Dispatchers.IO)` runs the DB insert on a background thread, avoiding main-thread IO. `lifecycleScope` is available in `AppCompatActivity`.

---

### Step 8 — Verify

Run the app and complete a game (let it reach bankrupt). Then connect to the device and pull the DB, or read it from the next session (Plans 11–12 will query it). Confirm:
- [ ] App requests location permission on first launch
- [ ] Game runs normally after granting/denying permission
- [ ] Bankrupt saves score (check via Plan 11 once scoreboard is built)
- [ ] Multiple games accumulate entries in the DB
- [ ] Score of 0 is saved correctly if game ends immediately

---

## Commit Message

```
Add Room database and GPS location capture for score persistence

ScoreEntry entity stores score, timestamp, and GPS coordinates. 
FusedLocationProviderClient caches location during gameplay. 
Score + last known location saved on bankrupt via lifecycleScope IO. 
Graceful fallback to lat=0/lng=0 when location permission denied.
```
