# 🌲 Landscape Art Screensaver Architecture & Design Spec

## 1. Overview & Objective
Create a standalone, native Android TV screensaver module (`apps/screensaver-landscape` - `com.appletv.screensaver.landscape`) featuring classic landscape paintings from the public museum archives in `resources/tranh-phong-canh-p1.zip` and `resources/tranh-phong-canh-p2.zip`.

### Key Requirements
- **Fullscreen Edge-to-Edge**: No borders, no pillarboxing/letterboxing, no framed display style. Fill the 16:9 TV screen completely with center-crop.
- **Strictly Landscape Only**: Filter out and exclude all portrait/vertical paintings (exclude 3 vertical pieces: Monet's Rouen Cathedral, Hiroshige's Sudden Shower, and Hiroshige's Plum Park).
- **Cinematic Apple TV Motion**: Smooth 2.5s crossfade between pieces and gentle, slow Ken Burns pan/zoom (~28s per piece) utilizing cropped excess image dimensions (vertical drift for height-dominant paintings, horizontal drift for panorama-dominant paintings).
- **Frosted Glass Info Pill**: Semi-transparent dark glass capsule showing Vietnamese & English title, artist, and year in the bottom-left corner; automatically fades out after 5.5s.
- **Lightweight Monorepo Citizen**: Built with pure Android Framework (zero heavy third-party libraries), optimized WebP Q85 assets (~10.5 MB total), and fully integrated with Launcher discovery and deploy tools.

---

## 2. Image Selection & Normalization Pipeline

### 2.1 Master Catalog (13 Landscape Paintings)

| No | Filename in Assets | Vietnamese Title | English Title | Artist | Year | Location / Museum | Aspect Ratio | Motion Direction |
|:---:|---|---|---|---|:---:|---|:---:|:---:|
| 01 | `hay_wain.webp` | Xe chở cỏ khô | The Hay Wain | John Constable | 1821 | National Gallery, London | 1.45 | Vertical drift + gentle zoom |
| 02 | `ninth_wave.webp` | Con sóng thứ chín | The Ninth Wave | Ivan Aivazovsky | 1850 | State Russian Museum | 1.51 | Vertical drift + gentle zoom |
| 03 | `rain_steam_speed.webp` | Mưa, hơi nước và tốc độ | Rain, Steam and Speed | J.M.W. Turner | 1844 | National Gallery, London | 1.20 | Vertical drift + gentle zoom |
| 04 | `chalk_cliffs.webp` | Vách đá phấn trên đảo Rügen | Chalk Cliffs on Rügen | Caspar David Friedrich | 1818 | Kunstmuseum Winterthur | 1.50 | Vertical drift + gentle zoom |
| 05 | `mont_sainte_victoire.webp` | Núi Sainte-Victoire | Mont Sainte-Victoire | Paul Cézanne | 1904 | Philadelphia Museum of Art | 1.38 | Vertical drift + gentle zoom |
| 06 | `oxbow.webp` | Khúc quanh sông Oxbow | The Oxbow | Thomas Cole | 1836 | Metropolitan Museum of Art | 1.33 | Vertical drift + gentle zoom |
| 07 | `heart_of_andes.webp` | Trái tim dãy Andes | The Heart of the Andes | Frederic Edwin Church | 1859 | Metropolitan Museum of Art | 1.83 | Horizontal panorama drift |
| 08 | `niagara.webp` | Thác Niagara | Niagara | Frederic Edwin Church | 1857 | National Gallery of Art, DC | 2.15 | Horizontal panorama drift |
| 09 | `rocky_mountains.webp` | Dãy Rocky | The Rocky Mountains | Albert Bierstadt | 1863 | Metropolitan Museum of Art | 1.67 | Vertical drift + gentle zoom |
| 10 | `magpie.webp` | Chim ác là | The Magpie | Claude Monet | 1869 | Musée d'Orsay, Paris | 1.50 | Vertical drift + gentle zoom |
| 11 | `olive_trees.webp` | Cây ô liu | The Olive Trees | Vincent van Gogh | 1889 | MoMA, New York | 1.26 | Vertical drift + gentle zoom |
| 12 | `flood_port_marly.webp` | Lũ lụt ở Port-Marly | Flood at Port-Marly | Alfred Sisley | 1876 | Musée d'Orsay, Paris | 1.26 | Vertical drift + gentle zoom |
| 13 | `bridge_narni.webp` | Cầu ở Narni | The Bridge at Narni | Camille Corot | 1826 | Musée du Louvre, Paris | 1.47 | Vertical drift + gentle zoom |

*Excluded Vertical Artworks:*
- `11-rouen-cathedral-monet.jpg` (aspect ratio 0.64)
- `13-sudden-shower-hiroshige.jpg` (aspect ratio 0.66)
- `16-plum-park-hiroshige.jpg` (aspect ratio 0.87)

### 2.2 Optimization Pipeline
- Source images extracted from `resources/tranh-phong-canh-p1.zip` and `resources/tranh-phong-canh-p2.zip`.
- High-quality Lanczos resampling targeting max dimension 2560px (height >= 1440px), maintaining native aspect ratios without distortion.
- Encoded to Google WebP Q85 into `apps/screensaver-landscape/src/main/assets/landscapes/`.
- Total uncompressed package size: ~10.5 MB (instant memory-efficient decoding on Android TV).

---

## 3. Application Architecture (`apps/screensaver-landscape`)

```
apps/screensaver-landscape/
├── build.gradle
├── proguard-rules.pro
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── assets/
    │   │   └── landscapes/*.webp
    │   ├── java/com/appletv/screensaver/landscape/
    │   │   ├── LandscapeDreamService.java       # Standard Android DreamService
    │   │   ├── LandscapeActivity.java           # Standalone Fullscreen Preview/Launch Activity
    │   │   ├── LandscapeView.java               # Dual-layer animated container & Ken Burns engine
    │   │   ├── LandscapeInfoCardView.java       # Auto-dismiss frosted glass info capsule
    │   │   ├── LandscapeCatalog.java            # Artwork catalog & non-repeating shuffle algorithm
    │   │   └── LandscapePiece.java              # Immutable metadata model
    │   └── res/
    │       ├── values/
    │       │   └── strings.xml
    │       └── xml/
    │           └── dream_info.xml
    └── test/
        └── java/com/appletv/screensaver/landscape/
            └── LandscapeCatalogTest.java
```

### 3.1 Module Configuration
- `applicationId`: `com.appletv.screensaver.landscape`
- `minSdk`: 28, `targetSdk`: 34, `compileSdk`: 34
- `resConfigs`: "en", "vi"
- Signing: inherits `gradle/unified-signing.gradle`

### 3.2 Discovery & System Integration
- `LandscapeDreamService`:
  - Permission: `android.permission.BIND_DREAM_SERVICE`
  - Action: `android.service.dreams.DreamService`
  - Meta-data: `@xml/dream_info`
- `LandscapeActivity`:
  - Theme: `@android:style/Theme.NoTitleBar.Fullscreen`
  - Orientation: `landscape`
  - Launch mode: `singleTask`
  - Category: `android.intent.category.DEFAULT` and `com.appletv.launcher.category.SCREENSAVER` (detected automatically by `ScreensaverDiscovery.java` in Launcher).

---

## 4. Visual Effects & Motion Pipeline

### 4.1 Dual-Layer View Architecture
`LandscapeView` maintains two slide holders: `currentSlide` and `nextSlide`.
Each slide holder contains:
1. `ImageView` with `ScaleType.CENTER_CROP`.
2. Hardware layer acceleration during active animation.

### 4.2 Ken Burns Animation Engine
Each slide is displayed for 28 seconds:
- **Aspect Ratio Analysis**:
  - If `aspectRatio < 1.70` (taller than 16:9): Excess content is vertical. Pan vertically between `[-3% to +3%]` translation or gentle vertical sweep, combined with scale `1.02f -> 1.07f`.
  - If `aspectRatio > 1.80` (wider than 16:9): Excess content is horizontal. Pan horizontally across `[-4% to +4%]` translation, combined with scale `1.01f -> 1.05f`.
  - Direction (top-to-bottom vs bottom-to-top, left-to-right vs right-to-left) is pseudorandomly assigned per slide.
- **Crossfade**:
  - `nextSlide` starts with alpha 0.0f and immediately begins its Ken Burns motion.
  - Crossfades in over 2,500ms using a smooth `FastOutSlowInInterpolator` (or `AccelerateDecelerateInterpolator`).
  - At animation end, `currentSlide` is recycled and swapped.

### 4.3 Frosted Glass Info Card (`LandscapeInfoCardView`)
- Position: Bottom-left corner (`leftMargin = 48dp`, `bottomMargin = 40dp`).
- Styling: Rounded rectangle capsule (`16dp` radius), 60% alpha dark glass (`0x99101014`) with subtle `1dp` white glass border (`0x30FFFFFF`).
- Content:
  - Line 1: Vietnamese title (Bold white `0xFFFFFFFF`, 18sp).
  - Line 2: English title in italics + Artist + Year (`0xFFD0D0D8`, 13sp).
- Behavior:
  - Fades in over 800ms when a new piece begins.
  - Remains visible for 5.5s.
  - Fades out smoothly over 1,200ms.
  - Remote interaction: Any D-Pad press (Up/Down/Left/Center) wakes up the card for 4s; D-Pad Right immediately advances to the next painting.

---

## 5. Tooling & Monorepo Build Updates
1. `settings.gradle`: add `include ':apps:screensaver-landscape'`.
2. `tools/deploy.sh`:
   - Add target `landscape` to build & deploy `apps/screensaver-landscape`.
   - Update target `screensavers` to build & deploy `flipclock`, `artmuseum`, and `landscape`.

---

## 6. Testing & Quality Assurance
- **Unit Tests**:
  - `LandscapeCatalogTest`: verifies all 13 items load correctly, titles are non-empty, files exist in assets, and shuffling never repeats the last seen index.
- **Runtime Verification**:
  - Run `./gradlew :apps:screensaver-landscape:assembleRelease` to ensure clean build, zero compiler warnings, and valid signing.
  - Verify memory usage remains < 35 MB during active crossfading on Android TV.
