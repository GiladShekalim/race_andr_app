# Feature Plan 11 — Scoreboard Screen + Table Fragment (Top 10)

**Project:** `avoided_race_app`  
**Working dir:** `/Users/giladshekalim/repo/androidCourse/avoided_race_app`  
**Commit scope:** Replace the placeholder `ScoreboardActivity` (created in Plan 07) with a real two-panel screen. This plan implements the Activity shell and Fragment 1 — a `RecyclerView` table of the top 10 scores loaded from Room. Fragment 2 (map) is added in Plan 12.

> **Prerequisites:**
> - **Plan 07 (Menu Screen)** — `ScoreboardActivity` exists as a placeholder and is already declared in `AndroidManifest.xml`.
> - **Plan 10 (Score Storage)** — `AppDatabase`, `ScoreDao.getTop10()`, `ScoreEntry`, and `GameApplication` must exist.

> **Context files:**
> - `26B-10208-L06.md` (reference repo) — Fragment-to-Fragment communication via Activity mediator with a callback interface. **This plan uses the exact same pattern.** See "CallbackHighScoreClicked interface" and "Activity as mediator" sections.
> - `CODE.md` → "Patterns Table" — the callback mediator pattern was taught in L06.
> - `LAYOUT.md` → "Layer Stacking" — `activity_scoreboard.xml` is a new layout; it does not inherit anything from `activity_main.xml`.

---

## What This Feature Does

- Replaces the placeholder `ScoreboardActivity` and `activity_scoreboard.xml` with real implementations
- `activity_scoreboard.xml` contains two `FrameLayout` containers — one for each Fragment (side-by-side or stacked)
- `ScoreboardActivity` loads Fragment 1 on creation and implements a communication interface for Fragment 2 (map update)
- **Fragment 1 (`ScoreTableFragment`)**: a `RecyclerView` showing the top 10 scores (rank, score value, date) loaded from Room DB
- Tapping a row calls a callback on the Activity, which will relay it to Fragment 2 (map) in Plan 12
- In Plan 12, Fragment 2 (map) is added alongside Fragment 1

---

## Hidden / Non-Obvious Steps

1. **Two-fragment layout in a single Activity:** The `activity_scoreboard.xml` must contain two `FrameLayout` containers with IDs for both fragments. Even though Fragment 2 (map) is added in Plan 12, the container must be declared now so that Plan 12 can add the fragment to it without changing the layout.

2. **Fragment communication interface on the Activity** — Fragment 1 uses a callback interface (`OnScoreSelectedListener`) that `ScoreboardActivity` implements. This is the L06 pattern verbatim. Fragment 1 casts its `requireActivity()` to the interface in `onAttach()`.

3. **Room query returns `List<ScoreEntry>` — must run on a background thread** — use `lifecycleScope.launch(Dispatchers.IO)` in the Fragment, then `withContext(Dispatchers.Main)` to update the RecyclerView adapter on the main thread.

4. **`RecyclerView` requires a `LinearLayoutManager`** — not set by default. Forgetting this causes a crash ("No layout manager attached; skipping layout").

5. **`ScoreTableAdapter` must be declared** — a simple `RecyclerView.Adapter` that binds `ScoreEntry` data to row views.

6. **Timestamp display** — `ScoreEntry.timestamp` is a `Long` (epoch milliseconds). Format with `SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))` for a readable date.

7. **Back button from `ScoreboardActivity`** — pressing back correctly returns to `MenuActivity` because `ScoreboardActivity` is started with `startActivity()` (no `finish()` on MenuActivity), so the back stack is correct automatically.

---

## Files to Create

| File | Purpose |
|---|---|
| `app/src/main/java/com/example/avoided_race_app/ScoreTableFragment.kt` | Fragment 1: RecyclerView of top 10 |
| `app/src/main/java/com/example/avoided_race_app/ScoreTableAdapter.kt` | RecyclerView adapter for score rows |
| `app/src/main/res/layout/fragment_score_table.xml` | Fragment 1 layout |
| `app/src/main/res/layout/item_score_row.xml` | Single row layout for RecyclerView |

## Files to Replace

| File | Change |
|---|---|
| `app/src/main/java/com/example/avoided_race_app/ScoreboardActivity.kt` | Full replacement — adds Fragment 1, implements interface |
| `app/src/main/res/layout/activity_scoreboard.xml` | Full replacement — two FrameLayout containers |

---

## Step-by-Step Implementation

### Step 1 — Define the Fragment communication interface

Create the interface inside `ScoreboardActivity.kt` (top-level or companion). This interface is the bridge Plan 12 (Fragment 2) will use:

```kotlin
interface OnScoreSelectedListener {
    fun onScoreSelected(latitude: Double, longitude: Double)
}
```

---

### Step 2 — Replace `activity_scoreboard.xml`

Layout: stack vertically — table takes 45% of height, map takes 55% (or side-by-side on landscape). Use a vertical `LinearLayout` with `weightSum`:

File: `app/src/main/res/layout/activity_scoreboard.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/background_color"
    android:weightSum="10">

    <com.google.android.material.textview.MaterialTextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="SCOREBOARD"
        android:textSize="22sp"
        android:textStyle="bold"
        android:textColor="@android:color/white"
        android:gravity="center"
        android:padding="12dp" />

    <!-- Fragment 1: Score Table (top 45%) -->
    <FrameLayout
        android:id="@+id/scoreboard_FRAME_table"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="4.5" />

    <!-- Fragment 2: Map (bottom 55%) — populated in Plan 12 -->
    <FrameLayout
        android:id="@+id/scoreboard_FRAME_map"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="5.5" />

</LinearLayout>
```

---

### Step 3 — Replace `ScoreboardActivity.kt`

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
                // Plan 12 will add: .replace(R.id.scoreboard_FRAME_map, ScoreMapFragment())
                .commit()
        }
    }

    override fun onScoreSelected(latitude: Double, longitude: Double) {
        // Plan 12 will route this to ScoreMapFragment
        // val mapFragment = supportFragmentManager.findFragmentById(R.id.scoreboard_FRAME_map) as? ScoreMapFragment
        // mapFragment?.centerOn(latitude, longitude)
    }
}
```

`savedInstanceState == null` guard prevents duplicate fragments on Activity recreation (e.g., screen rotation).

---

### Step 4 — Create `fragment_score_table.xml`

File: `app/src/main/res/layout/fragment_score_table.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/background_color">

    <!-- Header row -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingHorizontal="16dp"
        android:paddingVertical="8dp">

        <com.google.android.material.textview.MaterialTextView
            android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="1" android:text="#"
            android:textColor="@android:color/white" android:textStyle="bold" />
        <com.google.android.material.textview.MaterialTextView
            android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="2" android:text="SCORE"
            android:textColor="@android:color/white" android:textStyle="bold" />
        <com.google.android.material.textview.MaterialTextView
            android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="3" android:text="DATE"
            android:textColor="@android:color/white" android:textStyle="bold" />
    </LinearLayout>

    <View android:layout_width="match_parent" android:layout_height="1dp"
        android:background="#555555" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/score_table_RV"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</LinearLayout>
```

---

### Step 5 — Create `item_score_row.xml`

File: `app/src/main/res/layout/item_score_row.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:paddingHorizontal="16dp"
    android:paddingVertical="12dp"
    android:background="?attr/selectableItemBackground">

    <com.google.android.material.textview.MaterialTextView
        android:id="@+id/row_LBL_rank"
        android:layout_width="0dp" android:layout_height="wrap_content"
        android:layout_weight="1"
        android:textColor="@android:color/white" />

    <com.google.android.material.textview.MaterialTextView
        android:id="@+id/row_LBL_score"
        android:layout_width="0dp" android:layout_height="wrap_content"
        android:layout_weight="2"
        android:textColor="#FFD700"
        android:textStyle="bold" />

    <com.google.android.material.textview.MaterialTextView
        android:id="@+id/row_LBL_date"
        android:layout_width="0dp" android:layout_height="wrap_content"
        android:layout_weight="3"
        android:textColor="#CCCCCC"
        android:textSize="12sp" />
</LinearLayout>
```

---

### Step 6 — Create `ScoreTableAdapter.kt`

File: `app/src/main/java/com/example/avoided_race_app/ScoreTableAdapter.kt`

```kotlin
package com.example.avoided_race_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.avoided_race_app.db.ScoreEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScoreTableAdapter(
    private var entries: List<ScoreEntry>,
    private val onRowClick: (ScoreEntry) -> Unit
) : RecyclerView.Adapter<ScoreTableAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rank: TextView = view.findViewById(R.id.row_LBL_rank)
        val score: TextView = view.findViewById(R.id.row_LBL_score)
        val date: TextView = view.findViewById(R.id.row_LBL_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_score_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.rank.text = "#${position + 1}"
        holder.score.text = entry.score.toString()
        holder.date.text = dateFormat.format(Date(entry.timestamp))
        holder.itemView.setOnClickListener { onRowClick(entry) }
    }

    override fun getItemCount() = entries.size

    fun updateEntries(newEntries: List<ScoreEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }
}
```

---

### Step 7 — Create `ScoreTableFragment.kt`

File: `app/src/main/java/com/example/avoided_race_app/ScoreTableFragment.kt`

```kotlin
package com.example.avoided_race_app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.avoided_race_app.db.ScoreEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScoreTableFragment : Fragment() {

    private lateinit var adapter: ScoreTableAdapter
    private var listener: OnScoreSelectedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Cast Activity to the callback interface — same pattern as L06 reference
        listener = context as? OnScoreSelectedListener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_score_table, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.score_table_RV)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = ScoreTableAdapter(emptyList()) { entry ->
            listener?.onScoreSelected(entry.latitude, entry.longitude)
        }
        recyclerView.adapter = adapter

        loadScores()
    }

    private fun loadScores() {
        lifecycleScope.launch(Dispatchers.IO) {
            val scores = (requireActivity().application as GameApplication)
                .database.scoreDao().getTop10()
            withContext(Dispatchers.Main) {
                adapter.updateEntries(scores)
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
```

---

### Step 8 — Verify

Run the app, play a game, let it reach bankrupt, return to menu, tap "SCOREBOARD". Confirm:
- [ ] Scoreboard screen opens showing the top-half table and bottom-half (empty placeholder for now)
- [ ] Score entries appear in descending score order
- [ ] Date formatted as "Jun 21, 14:35" style
- [ ] Rank column shows "#1", "#2", etc.
- [ ] Tapping a row does not crash (calls `onScoreSelected` on Activity — no-op for now until Plan 12)
- [ ] Back button returns to Menu
- [ ] Multiple games accumulate — top 10 shown correctly

---

## Commit Message

```
Add ScoreTableFragment with RecyclerView of top 10 scores

ScoreboardActivity shell with two FrameLayout containers. 
ScoreTableFragment loads top 10 from Room on IO dispatcher, 
displays rank/score/date in RecyclerView. Row click fires 
OnScoreSelectedListener interface on Activity (map wiring in Plan 12).
```
