# Feature Plan 03 — Crash Sound

**Project:** `avoided_race_app`  
**Working dir:** `/Users/giladshekalim/repo/androidCourse/avoided_race_app`  
**Commit scope:** Play a short crash sound effect every time the player loses a life (collision with a hazard).

> **Context files:**
> - `CODE.md` → "`handleCollision()`" describes exactly where and when a collision response is triggered — this is where the sound call is added.
> - `PROJECT.md` → "What's Missing / Future Addition Points" notes sound effects as a planned addition using `SoundPool`.

---

## What This Feature Does

Plays a short audio clip immediately when `handleCollision()` is called — that is, whenever an obstacle reaches the car's lane and a life is deducted. The sound plays once per collision and does not loop. The existing vibration is kept; sound is added alongside it.

---

## Technology Choice: `SoundPool`

Use `SoundPool` (not `MediaPlayer`). Reasons:
- `SoundPool` is designed for short, low-latency game sounds
- Sounds are pre-loaded into memory at startup — no disk seek delay at play time
- Can play while already playing (no blocking)
- `MediaPlayer` has significant startup latency and is suited for music/long audio

---

## Files to Change

| File | Action |
|---|---|
| `app/src/main/res/raw/crash_sound.ogg` | **Create** — add the crash audio file here |
| `app/src/main/java/com/example/avoided_race_app/MainActivity.kt` | Add `SoundPool` init, load, play, and release |

No XML layout changes. No `LogicManager` changes.

---

## Step-by-Step Implementation

### Step 1 — Add the sound file

Create the directory if it doesn't exist:
```
app/src/main/res/raw/
```

Place the crash sound file in it:
```
app/src/main/res/raw/crash_sound.ogg
```

**Accepted formats:** `.ogg` (preferred for Android — smallest, best quality), `.wav`, `.mp3`.  
**Recommended specs:** Mono, 44100 Hz, duration < 1 second.

If the file is provided in a different format, rename it to `crash_sound.ogg` (or `.wav`) and place it as above.

---

### Step 2 — Add `SoundPool` fields to `MainActivity`

File: `app/src/main/java/com/example/avoided_race_app/MainActivity.kt`

Add these imports at the top:
```kotlin
import android.media.AudioAttributes
import android.media.SoundPool
```

Add these fields to the `MainActivity` class (near the other field declarations at the top):
```kotlin
private lateinit var soundPool: SoundPool
private var crashSoundId: Int = 0
private var soundLoaded: Boolean = false
```

---

### Step 3 — Initialize `SoundPool` in `onCreate()`

Add the following block at the **start** of `onCreate()`, before `findViews()`:

```kotlin
val audioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_GAME)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

soundPool = SoundPool.Builder()
    .setMaxStreams(3)
    .setAudioAttributes(audioAttributes)
    .build()

soundPool.setOnLoadCompleteListener { _, _, status ->
    if (status == 0) soundLoaded = true
}

crashSoundId = soundPool.load(this, R.raw.crash_sound, 1)
```

**Explanation:**
- `setUsage(USAGE_GAME)` — correct audio category for game sounds; respects Do Not Disturb in game mode.
- `setMaxStreams(3)` — allows up to 3 simultaneous crash sounds (rapid collisions won't cut each other off).
- `setOnLoadCompleteListener` — sets `soundLoaded = true` only after the file is fully buffered. Guards against playing before the sound is ready.
- `soundPool.load(this, R.raw.crash_sound, 1)` — loads `crash_sound.ogg` from `res/raw/` and returns an integer ID stored in `crashSoundId`.

---

### Step 4 — Play the sound in `handleCollision()`

Inside `handleCollision()`, add the sound play call **alongside** the existing `vibrate()` call:

```kotlin
private fun handleCollision() {
    // Show the "MONEY LOST!" text
    if (main_LBL_money_lost is android.widget.TextView) {
        (main_LBL_money_lost as android.widget.TextView).text = getString(R.string.money_lost)
    }
    main_LBL_money_lost.visibility = View.VISIBLE
    handler.postDelayed({ main_LBL_money_lost.visibility = View.GONE }, 1000)

    vibrate()
    playCrashSound()   // ← ADD THIS LINE

    lives--
    updateHeartsUI()
}
```

Add the new private method:
```kotlin
private fun playCrashSound() {
    if (soundLoaded) {
        soundPool.play(
            crashSoundId,
            1.0f,   // left volume (0.0 to 1.0)
            1.0f,   // right volume
            1,      // priority (0 = lowest)
            0,      // loop (0 = no loop, -1 = loop forever)
            1.0f    // playback rate (1.0 = normal speed)
        )
    }
}
```

---

### Step 5 — Release `SoundPool` in `onDestroy()`

Add `onDestroy()` override to `MainActivity` (it doesn't currently exist):

```kotlin
override fun onDestroy() {
    super.onDestroy()
    soundPool.release()
}
```

This releases the audio buffer memory when the Activity is destroyed. Without this, the `SoundPool` object leaks.

---

### Step 6 — Add `VIBRATE` permission check

The `VIBRATE` permission is already declared in `AndroidManifest.xml`. No new permission is needed for `SoundPool` — audio playback at normal volume does not require a manifest permission.

---

### Step 7 — Verify

Build and run. Trigger a collision by steering into a hazard. Confirm:
- [ ] A crash sound plays immediately on collision
- [ ] The existing vibration still fires
- [ ] "MONEY LOST!" text overlay still appears
- [ ] Rapid collisions don't cause audio errors or silence
- [ ] Sound plays correctly in both day and night modes
- [ ] App doesn't crash on exit (soundPool.release() works)

---

## Commit Message

```
Add crash sound effect on collision using SoundPool

Play short crash audio clip in handleCollision() alongside 
existing vibration. SoundPool pre-loads res/raw/crash_sound.ogg 
at startup for low-latency game audio. Released in onDestroy().
```

---

## Notes

- If the provided audio file is in `.mp3` format, rename it to `crash_sound.mp3` and update the `soundPool.load()` call — the R.raw reference will resolve by filename without extension.
- `soundLoaded` guard prevents a crash if `handleCollision()` fires before the async load completes (rare but possible on very slow devices).
- If vibration is found to be redundant with sound, the `vibrate()` call in `handleCollision()` can be removed or made optional — see `CODE.md` → "`vibrate()`" for its current implementation.
