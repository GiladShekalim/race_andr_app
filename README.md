# Race Android Game - Kotlin 🏎️💨

---

Control a racing car moving through three lanes. Hazards will spawn and move toward you. Your goal? Don't hit them.

### Key Features:
*   **Three-Lane Action**: Simple left/right controls to navigate your car.
*   **Hazard Variety**: Watch out for various obstacles like broken cars, fuel spills, and "No Tax" signs.
*   **Life System**: You start with 3 money bags. Every collision costs you a bag. Lose them all, and you're bankrupt!
*   **Endless Loop**: The game keeps going as long as you can survive. When you go bankrupt, you get a quick reset to try again.
*   **Tactile Feedback**: Feel the crash with haptic vibration feedback on every collision.
*   **Adaptive UI**: Looks great in both **Day** and **Night** modes, with a clean interface that respects modern device cutouts (like the Pixel 9 camera).

---

## 🛠️ Tech Stack & Tools
This project was built using modern Android development practices to ensure it's lightweight and efficient.

*   **Language**: [Kotlin](https://kotlinlang.org/) (The modern standard for Android)
*   **UI Framework**: XML Layouts with [Material Components](https://material.io/components)
*   **Architecture**: Logic-UI Separation (SOLID principles) — all game state is managed by a dedicated `LogicManager`.
*   **Concurrency**: `Handler` and `Looper` for a smooth 500ms game tick loop.
*   **Graphics**: Custom Vector Drawables (XML-based) for sharp visuals on any screen resolution.

---

## 💡 Under the Hood
I wanted to keep the code as clean as possible. Instead of cluttering the `MainActivity`, I moved the "brain" of the game into a `LogicManager`. This class handles the matrix of obstacles, moves them down the screen on every tick loop, and checks for collisions before the UI even updates.

<img width="438" height="719" alt="Screenshot 2026-05-22 at 20 47 13" src="https://github.com/user-attachments/assets/e7e321aa-e95b-4a9e-b0fa-8ec78d6652cd" />
<img width="434" height="723" alt="Screenshot 2026-05-22 at 20 46 28" src="https://github.com/user-attachments/assets/b6ca9eff-0ee8-4267-9e8f-12c3008e04d8" />
