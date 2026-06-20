# Feature Plan 01 — App Icon

**Project:** `avoided_race_app`  
**Working dir:** `/Users/giladshekalim/repo/androidCourse/avoided_race_app`  
**Commit scope:** Replace default Android launcher icon with a custom provided image.

> **Context files:** See `PROJECT.md` → "Project Structure" for the full file tree, and `res/drawable/DRAWABLES.md` → "Asset Catalog" for how existing vector assets are structured.

---

## What This Feature Does

Replaces the default Android robot launcher icon (currently `ic_launcher_foreground.xml` — the generic template) with a custom image provided by the user. The icon must appear correctly in the Android launcher, app switcher, and settings — both in round and square crop variants.

The image file will be provided at the time of implementation.

---

## Pre-Implementation Checklist

Before starting, the user must provide:
- [ ] The icon image file (PNG, JPG, or SVG). Ideal source size: **1024×1024px or larger**, square, with transparent background if possible.

---

## Files to Change

| File | Action |
|---|---|
| `app/src/main/res/mipmap-hdpi/ic_launcher.webp` | Replace |
| `app/src/main/res/mipmap-hdpi/ic_launcher_round.webp` | Replace |
| `app/src/main/res/mipmap-mdpi/ic_launcher.webp` | Replace |
| `app/src/main/res/mipmap-mdpi/ic_launcher_round.webp` | Replace |
| `app/src/main/res/mipmap-xhdpi/ic_launcher.webp` | Replace |
| `app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp` | Replace |
| `app/src/main/res/mipmap-xxhdpi/ic_launcher.webp` | Replace |
| `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp` | Replace |
| `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp` | Replace |
| `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp` | Replace |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Replace with image-based foreground |
| `app/src/main/res/drawable/ic_launcher_background.xml` | Optionally update background color |
| `app/src/main/res/mipmap-anydpi/ic_launcher.xml` | Keep as-is (adaptive icon XML) |
| `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml` | Keep as-is |

---

## Step-by-Step Implementation

### Step 1 — Understand the current icon system

The app uses Android **Adaptive Icons** (API 26+). Two files define them:

`mipmap-anydpi/ic_launcher.xml`:
```xml
<adaptive-icon>
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
```

The `ic_launcher_background.xml` is a solid color or gradient. The `ic_launcher_foreground.xml` is the main visual. Legacy mipmap `.webp` files serve older API levels.

### Step 2 — Prepare the provided image

When the image is provided, determine its format:

**If PNG/JPG** (raster):
- Place the original (largest size) at `app/src/main/res/drawable/ic_launcher_foreground.png`
- Delete `ic_launcher_foreground.xml` (the current vector placeholder)
- Generate density-specific WebP files at these pixel sizes:

| Density folder | Size (px) | File |
|---|---|---|
| `mipmap-mdpi` | 48×48 | `ic_launcher.webp`, `ic_launcher_round.webp` |
| `mipmap-hdpi` | 72×72 | same |
| `mipmap-xhdpi` | 96×96 | same |
| `mipmap-xxhdpi` | 144×144 | same |
| `mipmap-xxxhdpi` | 192×192 | same |

To generate WebP files from a PNG using `cwebp` (if installed):
```bash
cwebp -q 90 source_1024.png -o ic_launcher.webp
```
Or use ImageMagick:
```bash
convert source_1024.png -resize 192x192 mipmap-xxxhdpi/ic_launcher.webp
# repeat for each density
```

**If SVG**:
- Convert to an Android `<vector>` XML drawable and place it as `ic_launcher_foreground.xml`, replacing the existing one.

### Step 3 — Update the foreground drawable

**Option A — PNG foreground (adaptive icon):**

Update `mipmap-anydpi/ic_launcher.xml` to reference the PNG:
```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
```
(No change needed to the XML itself — only the `ic_launcher_foreground` drawable changes.)

**Option B — If the image should fill the entire icon with no separate background:**

Replace `ic_launcher_background.xml` with a solid white or black fill:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:viewportWidth="108" android:viewportHeight="108"
    android:width="108dp" android:height="108dp">
    <path android:fillColor="#FFFFFF"
        android:pathData="M0,0h108v108h-108z"/>
</vector>
```
Adjust color to complement the icon image.

### Step 4 — Verify `AndroidManifest.xml` references

Confirm these lines remain unchanged in `app/src/main/AndroidManifest.xml`:
```xml
android:icon="@mipmap/ic_launcher"
android:roundIcon="@mipmap/ic_launcher_round"
```
These are already correct and do not need editing.

### Step 5 — Build and verify

Run the app on a device or emulator. Long-press the app in the launcher to see the icon. Check both the square and round variants.

If the icon looks correct → commit.

---

## Commit Message

```
Add custom app launcher icon

Replace default Android launcher icon placeholder with provided 
custom image. Updated all mipmap density variants and adaptive 
icon foreground drawable.
```

---

## Notes for Future Sessions

- Do NOT change `AndroidManifest.xml` icon references — they already point to the correct mipmap resources.
- Do NOT alter `mipmap-anydpi/ic_launcher.xml` or `ic_launcher_round.xml` unless switching icon strategies.
- The background color in `ic_launcher_background.xml` can be updated to match the image's dominant color for a polished look in app drawers that show the full adaptive icon shape.
