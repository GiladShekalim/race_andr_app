# Feature Plan 04 — Game Score Display

**Project:** `avoided_race_app`  
**Working dir:** `/Users/giladshekalim/repo/androidCourse/avoided_race_app`  
**Commit scope:** Add a score variable and a score label to the UI. Score is not yet incremented by gameplay — that is handled in Features 05 (coins) and 06 (odometer). This feature only establishes the score field and its display.

> **Context files:**
> - `LAYOUT.md` → "Layer Stacking" and "Adding a New UI Element → Score display" for where and how to place the score label without disrupting existing layout anchors.
> - `VALUES.md` → "`strings.xml`" and "Adding New Resource Values" for the string/dimen tokens to add.
> - `CODE.md` → "`resetGame()`" — the score must be reset here.
> - `PROJECT.md` → "What's Missing / Future Addition Points" — score counter and score display are listed as planned additions.

---

## What This Feature Does

- Adds an integer `score` variable to `MainActivity` (starts at 0)
- Adds a `MaterialTextView` to `activity_main.xml` showing the current score in the top-right corner
- Resets the score to 0 on bankrupt (`resetGame()`)
- Exposes a `updateScoreUI()` private method that future features (coins, odometer) will call to refresh the display

The score display uses the format: `"Score: 0"` updating to `"Score: 42"` etc.

---

## Files to Change

| File | Action |
|---|---|
| `app/src/main/res/values/strings.xml` | Add score label string |
| `app/src/main/res/values/dimens.xml` | Add score text size dimension |
| `app/src/main/res/layout/activity_main.xml` | Add score `MaterialTextView` |
| `app/src/main/java/com/example/avoided_race_app/MainActivity.kt` | Add `score` field, `updateScoreUI()`, reset in `resetGame()` |

---

## Step-by-Step Implementation

### Step 1 — Add string resource

File: `app/src/main/res/values/strings.xml`

```xml
<resources>
    <string name="app_name">avoided_race_app</string>
    <string name="money_lost">MONEY LOST!</string>
    <string name="bankrupt">BANKRUPT!</string>
    <string name="score_label">Score: %d</string>   <!-- ADD THIS LINE -->
</resources>
```

`%d` is a standard Android format placeholder for an integer, used with `getString(R.string.score_label, score)`.

---

### Step 2 — Add dimension resource

File: `app/src/main/res/values/dimens.xml`

Add at the end of the existing list:
```xml
<dimen name="score_text_size">22sp</dimen>
```

---

### Step 3 — Add score label to layout

File: `app/src/main/res/layout/activity_main.xml`

Add the following `MaterialTextView` as the **last child** of the root `RelativeLayout` (after the existing `main_LBL_money_lost` element). Adding it last ensures it draws above all other views by default.

```xml
<!-- Score Display -->
<com.google.android.material.textview.MaterialTextView
    android:id="@+id/main_LBL_score"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_alignParentTop="true"
    android:layout_alignParentEnd="true"
    android:layout_margin="@dimen/default_margin"
    android:text="@string/score_label"
    android:textSize="@dimen/score_text_size"
    android:textStyle="bold"
    android:textColor="@android:color/white"
    android:elevation="2dp" />
```

**Why top-right:** The hearts are centered at top. The score at top-right is the standard game UI position and doesn't conflict with the hearts container or any other anchored element. See `LAYOUT.md` → "Adding a New UI Element — Score display" for layout constraint rationale.

**`layout_margin="@dimen/default_margin"`** reuses the existing 16dp spacing token — no new dimen needed for the margin.

---

### Step 4 — Add score field and methods to `MainActivity`

File: `app/src/main/java/com/example/avoided_race_app/MainActivity.kt`

**4a — Add field declaration** (near the other state variables at the top of the class):
```kotlin
// 2. State Variables
private var currentCarLane = 1
private var lives = 3
private var score = 0              // ADD THIS LINE
```

**4b — Add view binding** in `findViews()`:

Add the field declaration near the other UI component fields:
```kotlin
private lateinit var main_LBL_score: com.google.android.material.textview.MaterialTextView
```

Inside `findViews()`, add the binding:
```kotlin
main_LBL_score = findViewById(R.id.main_LBL_score)
```

**4c — Add `updateScoreUI()` method:**
```kotlin
private fun updateScoreUI() {
    main_LBL_score.text = getString(R.string.score_label, score)
}
```

**4d — Call `updateScoreUI()` in `initViews()`** to show "Score: 0" on startup:

At the end of `initViews()`, add:
```kotlin
updateScoreUI()
```

**4e — Reset score in `resetGame()`:**

Inside `resetGame()`, after `lives = 3`, add:
```kotlin
score = 0
updateScoreUI()
```

Full updated `resetGame()` for reference:
```kotlin
private fun resetGame() {
    if (main_LBL_money_lost is android.widget.TextView) {
        (main_LBL_money_lost as android.widget.TextView).text = getString(R.string.bankrupt)
    }
    main_LBL_money_lost.visibility = View.VISIBLE
    handler.postDelayed({ main_LBL_money_lost.visibility = View.GONE }, 1500)

    logicManager.clearMatrix()
    lives = 3
    updateHeartsUI()
    score = 0           // ADD THIS
    updateScoreUI()     // ADD THIS
}
```

---

### Step 5 — Verify

Run the app. Confirm:
- [ ] "Score: 0" displays in the top-right corner
- [ ] Score display does not overlap the money bag icons (hearts are centered, score is right-aligned)
- [ ] Score resets to "Score: 0" on bankrupt
- [ ] Score label is readable against the dark road background (`#424242` day / `#1A1A1A` night)
- [ ] Score value stays at 0 during gameplay (no increment logic yet — that comes in Features 05 and 06)

---

## Commit Message

```
Add game score display to top-right corner

Adds score Int field to MainActivity, a MaterialTextView in the 
layout (top-right), updateScoreUI() helper, and score reset on 
bankrupt. Score is not yet incremented — coin (+10) and odometer 
(+1/s) logic follow in separate commits.
```

---

## Notes for Features 05 and 06

The `updateScoreUI()` method and `score` field are the integration points for the next two features:
- **Feature 05 (Coins):** Will call `score += 10; updateScoreUI()` when a coin is collected.
- **Feature 06 (Odometer):** Will call `score++; updateScoreUI()` every second.

Both features assume `score` is a `private var` in `MainActivity` and `updateScoreUI()` exists and refreshes the label. Do not change these names.
