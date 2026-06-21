# Race Android Game - Kotlin 🏎️💨

---

Control a racing car moving through five lanes. Hazards will spawn and move toward you. Your goal? Don't hit them.

### Key Features:
*   **Five-Lane Action**: Simple left/right controls — or tilt your phone — to navigate your car across a wider road.
*   **Hazard Variety**: Watch out for various obstacles like broken cars, fuel spills, and "No Tax" signs.
*   **Collectible Coins**: Gold coins fall alongside hazards. Collect them for +10 score each, with a distinct sound on pickup.
*   **Life System**: You start with 3 money bags. Every collision costs you a bag. Lose them all, and you're bankrupt!
*   **Score & Odometer**: Your score climbs every second you survive, plus every coin you collect. Race to beat your own record.
*   **Tilt Control**: Switch between button controls and accelerometer tilt steering from the menu. Tilt harder for faster lane changes.
*   **Pace Modes**: Choose **Steady** for a constant challenge, or **Fastening** to ramp up speed every 10 seconds until things get intense.
*   **Sound Effects**: Voice-acted crash and coin sounds powered by ElevenLabs, played with zero-latency `SoundPool`.
*   **Tactile Feedback**: Feel the crash with haptic vibration feedback on every collision.
*   **Adaptive UI**: Looks great in both **Day** and **Night** modes, with a clean interface that respects modern device cutouts (like the Pixel 9 camera).

---

### v2 — Scoreboard & GPS
*   **Persistent Scoreboard**: Every game result is saved to a local Room database — score, timestamp, and GPS coordinates.
*   **Top 10 Table**: Open the scoreboard from the menu to see your top 10 scores ranked by score, with date and time.
*   **Location Map**: Tap any row in the scoreboard to see where that game was played on an OpenStreetMap. The pin is draggable — move it and press **Update Location** to correct the position.

---

## 🛠️ Tech Stack & Tools
This project was built using modern Android development practices to ensure it's lightweight and efficient.

*   **Language**: [Kotlin](https://kotlinlang.org/) (The modern standard for Android)
*   **UI Framework**: XML Layouts with [Material Components](https://material.io/components)
*   **Architecture**: Logic-UI Separation (SOLID principles) — all game state is managed by a dedicated `LogicManager`.
*   **Concurrency**: `Handler` and `Looper` for the game tick loop; `lifecycleScope` with `Dispatchers.IO` for all database operations.
*   **Persistence**: [Room](https://developer.android.com/training/data-storage/room) for score storage; `SharedPreferences` for settings.
*   **Location**: Fused Location Provider (`play-services-location`) for GPS capture at game end.
*   **Maps**: [OSMDroid](https://github.com/osmdroid/osmdroid) (OpenStreetMap) — no API key required.
*   **Graphics**: Custom Vector Drawables (XML-based) for sharp visuals on any screen resolution.
*   **Annotation Processing**: KSP for Room code generation.

---

## 💡 Under the Hood
I wanted to keep the code as clean as possible. Instead of cluttering the `MainActivity`, I moved the "brain" of the game into a `LogicManager`. This class handles the matrix of obstacles and coins, moves them down the screen on every tick loop, and checks for collisions before the UI even updates.

## v2
<img width="413" height="846" alt="image" src="https://github.com/user-attachments/assets/269b78f5-8587-4587-b420-2bca793d8cc2" />
<img width="413" height="844" alt="image" src="https://github.com/user-attachments/assets/40be63c4-28b1-44cc-a0a8-f3e6b7ea1a9c" />


## v1
<img width="438" height="719" alt="Screenshot 2026-05-22 at 20 47 13" src="https://github.com/user-attachments/assets/e7e321aa-e95b-4a9e-b0fa-8ec78d6652cd" />
<img width="434" height="723" alt="Screenshot 2026-05-22 at 20 46 28" src="https://github.com/user-attachments/assets/b6ca9eff-0ee8-4267-9e8f-12c3008e04d8" />
