# Landscape Screensaver Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a standalone native Android TV screensaver module (`apps/screensaver-landscape` - `com.appletv.screensaver.landscape`) featuring 13 classic landscape paintings rendered edge-to-edge with direction-aware Ken Burns motion, 2.5s crossfade, and auto-dismiss frosted glass info pill.

**Architecture:** A native Android TV DreamService & companion Activity hosting a dual-layer FrameLayout. Two hardware-accelerated ImageView layers execute continuous subtle Ken Burns panning (along excess image dimensions) and zooming (1.02f - 1.07f), with a 2,500ms alpha crossfade between 28-second slides. Artwork assets are pre-converted from raw zip files to WebP Q85 (~10.5 MB total).

**Tech Stack:** Android SDK (Java 17, compileSdk 34, minSdk 28), Android DreamService, View Property & ValueAnimator, Python 3 / Pillow for asset preprocessing. Zero external third-party Android runtime dependencies.

**Spec:** `docs/superpowers/specs/2026-11-11-landscape-screensaver-design.md`

## Global Constraints

- Android API level: `minSdk 28`, `compileSdk 34`, `targetSdk 34`.
- Package ID: `com.appletv.screensaver.landscape`.
- Zero external dependencies: rely strictly on Android Framework APIs.
- Only landscape paintings: exactly 13 pieces from `resources/tranh-phong-canh-p1.zip` and `p2.zip` (exclude vertical pieces 11, 13, 16).
- Display rule: 100% fullscreen edge-to-edge center-crop; no letterboxing, pillarboxing, or ambient blur framing.
- Key protection: Never intercept protected remote keys; support D-Pad remote navigation (Right: next painting, any other: show info card).

---

### Task 1: Image Preprocessing & WebP Normalization Pipeline

**Files:**
- Create: `tools/convert_landscape_assets.py`
- Output: `apps/screensaver-landscape/src/main/assets/landscapes/*.webp` (13 files)

**Interfaces:**
- Produces: 13 WebP files at `apps/screensaver-landscape/src/main/assets/landscapes/` with max dimension 2560px, quality 85.

- [ ] **Step 1: Write the python asset conversion script**

Create `tools/convert_landscape_assets.py`:
```python
#!/usr/bin/env python3
"""
Convert landscape paintings from resources/ zip archives to optimized WebP for Android TV.
Filters out vertical paintings (aspect ratio < 1.0).
Resizes to max dimension 2560px using Lanczos filter.
Outputs to apps/screensaver-landscape/src/main/assets/landscapes/.
"""
import io
import os
import sys
import zipfile
from PIL import Image

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT_DIR = os.path.dirname(SCRIPT_DIR)
OUT_DIR = os.path.join(ROOT_DIR, "apps", "screensaver-landscape", "src", "main", "assets", "landscapes")

ZIP_FILES = [
    os.path.join(ROOT_DIR, "resources", "tranh-phong-canh-p1.zip"),
    os.path.join(ROOT_DIR, "resources", "tranh-phong-canh-p2.zip"),
]

# Mapping from zip filename to clean asset id
FILE_MAPPING = {
    "01-hay-wain-constable.jpg": "hay_wain.webp",
    "02-ninth-wave-aivazovsky.jpg": "ninth_wave.webp",
    "03-rain-steam-speed-turner.jpg": "rain_steam_speed.webp",
    "04-chalk-cliffs-rugen-friedrich.jpg": "chalk_cliffs.webp",
    "05-mont-sainte-victoire-cezanne.jpg": "mont_sainte_victoire.webp",
    "06-oxbow-thomas-cole.jpg": "oxbow.webp",
    "07-heart-of-andes-church.jpg": "heart_of_andes.webp",
    "08-niagara-church.jpg": "niagara.webp",
    "09-rocky-mountains-bierstadt.jpg": "rocky_mountains.webp",
    "10-magpie-monet.jpg": "magpie.webp",
    # 11-rouen-cathedral-monet.jpg is vertical -> EXCLUDED
    "12-olive-trees-van-gogh.jpg": "olive_trees.webp",
    # 13-sudden-shower-hiroshige.jpg is vertical -> EXCLUDED
    "14-flood-port-marly-sisley.jpg": "flood_port_marly.webp",
    "15-bridge-narni-corot.jpg": "bridge_narni.webp",
    # 16-plum-park-hiroshige.jpg is vertical -> EXCLUDED
}

MAX_DIM = 2560
WEBP_QUALITY = 85

def convert_all():
    os.makedirs(OUT_DIR, exist_ok=True)
    converted_count = 0

    for zip_path in ZIP_FILES:
        if not os.path.exists(zip_path):
            print(f"Error: Zip file not found: {zip_path}", file=sys.stderr)
            sys.exit(1)
        with zipfile.ZipFile(zip_path, "r") as z:
            for zip_name, target_name in FILE_MAPPING.items():
                if zip_name in z.namelist():
                    data = z.read(zip_name)
                    im = Image.open(io.BytesIO(data))
                    w, h = im.size
                    if w < h:
                        print(f"Skipping vertical painting: {zip_name} ({w}x{h})")
                        continue

                    # Resample if exceeds MAX_DIM
                    if max(w, h) > MAX_DIM:
                        im.thumbnail((MAX_DIM, MAX_DIM), Image.Resampling.LANCZOS)

                    # Ensure RGB mode (no RGBA or CMYK)
                    if im.mode != "RGB":
                        im = im.convert("RGB")

                    out_path = os.path.join(OUT_DIR, target_name)
                    im.save(out_path, format="WEBP", quality=WEBP_QUALITY, method=6)
                    size_kb = os.path.getsize(out_path) / 1024
                    print(f"Converted {zip_name} -> {target_name} ({im.size[0]}x{im.size[1]}, {size_kb:.1f} KB)")
                    converted_count += 1

    print(f"\nTotal converted landscape paintings: {converted_count}")
    assert converted_count == 13, f"Expected 13 paintings, got {converted_count}"

if __name__ == "__main__":
    convert_all()
```

- [ ] **Step 2: Run conversion script**

Run: `python3 tools/convert_landscape_assets.py`
Expected output:
```
Converted 01-hay-wain-constable.jpg -> hay_wain.webp ...
...
Total converted landscape paintings: 13
```

- [ ] **Step 3: Verify output files and sizes**

Run: `ls -lh apps/screensaver-landscape/src/main/assets/landscapes/`
Expected: 13 `.webp` files, total size ~10-11 MB.

- [ ] **Step 4: Commit assets and conversion tool**

```bash
git add tools/convert_landscape_assets.py apps/screensaver-landscape/src/main/assets/landscapes/
git commit -m "feat(screensaver-landscape): generate 13 optimized WebP landscape assets"
```

---

### Task 2: Module Scaffolding & Configuration

**Files:**
- Modify: `settings.gradle`
- Create: `apps/screensaver-landscape/build.gradle`
- Create: `apps/screensaver-landscape/proguard-rules.pro`
- Create: `apps/screensaver-landscape/src/main/AndroidManifest.xml`
- Create: `apps/screensaver-landscape/src/main/res/values/strings.xml`
- Create: `apps/screensaver-landscape/src/main/res/xml/dream_info.xml`

**Interfaces:**
- Consumes: Assets generated in Task 1.
- Produces: Compilable Gradle subproject `:apps:screensaver-landscape` with leanback/DreamService manifest entries.

- [ ] **Step 1: Add subproject to settings.gradle**

Modify `settings.gradle` to append:
`include ':apps:screensaver-landscape'`

- [ ] **Step 2: Create apps/screensaver-landscape/build.gradle**

Create `apps/screensaver-landscape/build.gradle`:
```groovy
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.appletv.screensaver.landscape'
    compileSdk 34

    defaultConfig {
        applicationId "com.appletv.screensaver.landscape"
        minSdk 28
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
        resConfigs "en", "vi"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    lint {
        abortOnError false
        checkReleaseBuilds false
    }

    packaging {
        resources {
            excludes += 'META-INF/*.version'
        }
    }
}

apply from: rootProject.file('gradle/unified-signing.gradle')

dependencies {
    testImplementation 'junit:junit:4.13.2'
}
```

- [ ] **Step 3: Create proguard-rules.pro**

Create `apps/screensaver-landscape/proguard-rules.pro`:
```proguard
-keep public class * extends android.service.dreams.DreamService
-keep public class * extends android.app.Activity
-keepclassmembers class * {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
}
```

- [ ] **Step 4: Create AndroidManifest.xml**

Create `apps/screensaver-landscape/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-feature android:name="android.software.leanback" android:required="false" />
    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />

    <application
        android:allowBackup="true"
        android:icon="@android:drawable/ic_menu_gallery"
        android:label="@string/app_name"
        android:banner="@android:drawable/ic_menu_gallery"
        android:supportsRtl="true">

        <service
            android:name=".LandscapeDreamService"
            android:exported="true"
            android:label="@string/dream_title"
            android:permission="android.permission.BIND_DREAM_SERVICE">
            <intent-filter>
                <action android:name="android.service.dreams.DreamService" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
            <meta-data
                android:name="android.service.dream"
                android:resource="@xml/dream_info" />
        </service>

        <activity
            android:name=".LandscapeActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@android:style/Theme.NoTitleBar.Fullscreen"
            android:launchMode="singleTask"
            android:screenOrientation="landscape"
            android:configChanges="orientation|keyboardHidden|screenSize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="com.appletv.launcher.category.SCREENSAVER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

- [ ] **Step 5: Create strings.xml and dream_info.xml**

Create `apps/screensaver-landscape/src/main/res/values/strings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Tranh phong cảnh</string>
    <string name="dream_title">Tranh phong cảnh thế giới</string>
</resources>
```

Create `apps/screensaver-landscape/src/main/res/xml/dream_info.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<dream xmlns:android="http://schemas.android.com/apk/res/android"
    android:settingsActivity=".LandscapeActivity" />
```

- [ ] **Step 6: Commit scaffolding**

```bash
git add settings.gradle apps/screensaver-landscape/
git commit -m "feat(screensaver-landscape): scaffold project structure and manifest"
```

---

### Task 3: Data Model & Catalog with Unit Tests (TDD)

**Files:**
- Create: `apps/screensaver-landscape/src/main/java/com/appletv/screensaver/landscape/LandscapePiece.java`
- Create: `apps/screensaver-landscape/src/main/java/com/appletv/screensaver/landscape/LandscapeCatalog.java`
- Create: `apps/screensaver-landscape/src/test/java/com/appletv/screensaver/landscape/LandscapeCatalogTest.java`

**Interfaces:**
- Produces:
  - `LandscapePiece(id, filename, titleVi, titleEn, artist, year, museum, aspectRatio)`
  - `LandscapeCatalog.getCount(): int`
  - `LandscapeCatalog.getPiece(int index): LandscapePiece`
  - `LandscapeCatalog.getPieces(): List<LandscapePiece>`
  - `LandscapeCatalog.createShuffledIndices(Random random, int previousIndex): List<Integer>`

- [ ] **Step 1: Write the failing unit tests**

Create `apps/screensaver-landscape/src/test/java/com/appletv/screensaver/landscape/LandscapeCatalogTest.java`:
```java
package com.appletv.screensaver.landscape;

import org.junit.Test;
import java.util.List;
import java.util.Random;
import static org.junit.Assert.*;

public class LandscapeCatalogTest {

    @Test
    public void testCatalogHasExactThirteenPieces() {
        assertEquals(13, LandscapeCatalog.getCount());
        assertEquals(13, LandscapeCatalog.getPieces().size());
    }

    @Test
    public void testPiecesHaveValidMetadata() {
        for (int i = 0; i < LandscapeCatalog.getCount(); i++) {
            LandscapePiece piece = LandscapeCatalog.getPiece(i);
            assertNotNull(piece);
            assertNotNull(piece.id);
            assertFalse(piece.id.isEmpty());
            assertTrue(piece.filename.endsWith(".webp"));
            assertNotNull(piece.titleVi);
            assertFalse(piece.titleVi.isEmpty());
            assertNotNull(piece.titleEn);
            assertFalse(piece.titleEn.isEmpty());
            assertNotNull(piece.artist);
            assertFalse(piece.artist.isEmpty());
            assertNotNull(piece.year);
            assertFalse(piece.year.isEmpty());
            assertTrue("Aspect ratio must be > 1.0 for landscape paintings", piece.aspectRatio > 1.0f);
        }
    }

    @Test
    public void testGetPieceNormalization() {
        LandscapePiece first = LandscapeCatalog.getPiece(0);
        LandscapePiece wrapPositive = LandscapeCatalog.getPiece(13);
        LandscapePiece wrapNegative = LandscapeCatalog.getPiece(-13);
        assertEquals(first.id, wrapPositive.id);
        assertEquals(first.id, wrapNegative.id);
    }

    @Test
    public void testShuffleDoesNotRepeatLastShownIndex() {
        Random random = new Random(42);
        for (int prev = 0; prev < LandscapeCatalog.getCount(); prev++) {
            for (int run = 0; run < 10; run++) {
                List<Integer> shuffled = LandscapeCatalog.createShuffledIndices(random, prev);
                assertEquals(13, shuffled.size());
                assertNotEquals("First item of shuffled playlist must not be previous index", (Integer) prev, shuffled.get(0));
            }
        }
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `./gradlew :apps:screensaver-landscape:test`
Expected: FAIL (compilation error, classes do not exist).

- [ ] **Step 3: Implement LandscapePiece.java**

Create `apps/screensaver-landscape/src/main/java/com/appletv/screensaver/landscape/LandscapePiece.java`:
```java
package com.appletv.screensaver.landscape;

public final class LandscapePiece {
    public final String id;
    public final String filename;
    public final String titleVi;
    public final String titleEn;
    public final String artist;
    public final String year;
    public final String museum;
    public final float aspectRatio;

    public LandscapePiece(String id, String filename, String titleVi, String titleEn,
                          String artist, String year, String museum, float aspectRatio) {
        this.id = id;
        this.filename = filename;
        this.titleVi = titleVi;
        this.titleEn = titleEn;
        this.artist = artist;
        this.year = year;
        this.museum = museum;
        this.aspectRatio = aspectRatio;
    }
}
```

- [ ] **Step 4: Implement LandscapeCatalog.java**

Create `apps/screensaver-landscape/src/main/java/com/appletv/screensaver/landscape/LandscapeCatalog.java`:
```java
package com.appletv.screensaver.landscape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class LandscapeCatalog {

    private static final List<LandscapePiece> PIECES = new ArrayList<>();

    static {
        PIECES.add(new LandscapePiece(
                "hay_wain", "hay_wain.webp",
                "Xe chở cỏ khô", "The Hay Wain",
                "John Constable", "1821",
                "National Gallery, London", 1.45f
        ));
        PIECES.add(new LandscapePiece(
                "ninth_wave", "ninth_wave.webp",
                "Con sóng thứ chín", "The Ninth Wave",
                "Ivan Aivazovsky", "1850",
                "State Russian Museum, St. Petersburg", 1.51f
        ));
        PIECES.add(new LandscapePiece(
                "rain_steam_speed", "rain_steam_speed.webp",
                "Mưa, hơi nước và tốc độ", "Rain, Steam and Speed",
                "J.M.W. Turner", "1844",
                "National Gallery, London", 1.20f
        ));
        PIECES.add(new LandscapePiece(
                "chalk_cliffs", "chalk_cliffs.webp",
                "Vách đá phấn trên đảo Rügen", "Chalk Cliffs on Rügen",
                "Caspar David Friedrich", "1818",
                "Kunstmuseum Winterthur", 1.50f
        ));
        PIECES.add(new LandscapePiece(
                "mont_sainte_victoire", "mont_sainte_victoire.webp",
                "Núi Sainte-Victoire", "Mont Sainte-Victoire",
                "Paul Cézanne", "1904",
                "Philadelphia Museum of Art", 1.38f
        ));
        PIECES.add(new LandscapePiece(
                "oxbow", "oxbow.webp",
                "Khúc quanh sông Oxbow", "The Oxbow",
                "Thomas Cole", "1836",
                "Metropolitan Museum of Art, New York", 1.33f
        ));
        PIECES.add(new LandscapePiece(
                "heart_of_andes", "heart_of_andes.webp",
                "Trái tim dãy Andes", "The Heart of the Andes",
                "Frederic Edwin Church", "1859",
                "Metropolitan Museum of Art, New York", 1.83f
        ));
        PIECES.add(new LandscapePiece(
                "niagara", "niagara.webp",
                "Thác Niagara", "Niagara",
                "Frederic Edwin Church", "1857",
                "National Gallery of Art, Washington D.C.", 2.15f
        ));
        PIECES.add(new LandscapePiece(
                "rocky_mountains", "rocky_mountains.webp",
                "Dãy Rocky", "The Rocky Mountains, Lander's Peak",
                "Albert Bierstadt", "1863",
                "Metropolitan Museum of Art, New York", 1.67f
        ));
        PIECES.add(new LandscapePiece(
                "magpie", "magpie.webp",
                "Chim ác là", "The Magpie",
                "Claude Monet", "1869",
                "Musée d'Orsay, Paris", 1.50f
        ));
        PIECES.add(new LandscapePiece(
                "olive_trees", "olive_trees.webp",
                "Cây ô liu", "The Olive Trees",
                "Vincent van Gogh", "1889",
                "Museum of Modern Art, New York", 1.26f
        ));
        PIECES.add(new LandscapePiece(
                "flood_port_marly", "flood_port_marly.webp",
                "Lũ lụt ở Port-Marly", "Flood at Port-Marly",
                "Alfred Sisley", "1876",
                "Musée d'Orsay, Paris", 1.26f
        ));
        PIECES.add(new LandscapePiece(
                "bridge_narni", "bridge_narni.webp",
                "Cầu ở Narni", "The Bridge at Narni",
                "Camille Corot", "1826",
                "Musée du Louvre, Paris", 1.47f
        ));
    }

    private LandscapeCatalog() {}

    public static List<LandscapePiece> getPieces() {
        return Collections.unmodifiableList(PIECES);
    }

    public static int getCount() {
        return PIECES.size();
    }

    public static LandscapePiece getPiece(int index) {
        if (PIECES.isEmpty()) return null;
        int size = PIECES.size();
        int normalized = ((index % size) + size) % size;
        return PIECES.get(normalized);
    }

    public static List<Integer> createShuffledIndices(Random random, int previousIndex) {
        int count = PIECES.size();
        List<Integer> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(i);
        }
        if (count <= 1) {
            return list;
        }
        Random rng = (random != null) ? random : new Random();
        Collections.shuffle(list, rng);
        if (previousIndex >= 0 && list.get(0) == previousIndex) {
            int swapIndex = 1 + rng.nextInt(count - 1);
            Collections.swap(list, 0, swapIndex);
        }
        return list;
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :apps:screensaver-landscape:test`
Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 6: Commit**

```bash
git add apps/screensaver-landscape/src/main/java/ apps/screensaver-landscape/src/test/
git commit -m "feat(screensaver-landscape): implement LandscapeCatalog and tests"
```

---

### Task 4: UI Components (LandscapeInfoCardView & LandscapeView)

**Files:**
- Create: `apps/screensaver-landscape/src/main/java/com/appletv/screensaver/landscape/LandscapeInfoCardView.java`
- Create: `apps/screensaver-landscape/src/main/java/com/appletv/screensaver/landscape/LandscapeView.java`

**Interfaces:**
- `LandscapeInfoCardView.displayPiece(LandscapePiece piece): void`
- `LandscapeInfoCardView.showBriefly(): void`
- `LandscapeInfoCardView.dismiss(): void`
- `LandscapeView.start(): void`
- `LandscapeView.stop(): void`
- `LandscapeView.next(): void`
- `LandscapeView.handleRemoteKey(int keyCode): boolean`

- [ ] **Step 1: Implement LandscapeInfoCardView.java**

Create `apps/screensaver-landscape/src/main/java/com/appletv/screensaver/landscape/LandscapeInfoCardView.java`:
```java
package com.appletv.screensaver.landscape;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LandscapeInfoCardView extends LinearLayout {

    private final TextView titleViView;
    private final TextView subtitleView;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable fadeOutRunnable = new Runnable() {
        @Override
        public void run() {
            animate()
                    .alpha(0f)
                    .setDuration(1200L)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            setVisibility(GONE);
                        }
                    })
                    .start();
        }
    };

    public LandscapeInfoCardView(Context context) {
        this(context, null);
    }

    public LandscapeInfoCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);

        float density = getResources().getDisplayMetrics().density;
        int padH = (int) (22 * density);
        int padV = (int) (14 * density);
        setPadding(padH, padV, padH, padV);

        // Apple TV frosted dark glass capsule
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xA6101014); // 65% dark glass
        bg.setCornerRadius(16 * density);
        bg.setStroke((int) (1 * density), 0x30FFFFFF); // Subtle bright rim
        setBackground(bg);

        // Vietnamese Primary Title
        titleViView = new TextView(context);
        titleViView.setTextColor(Color.WHITE);
        titleViView.setTextSize(18);
        titleViView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        addView(titleViView);

        // English Title + Artist + Year Subtitle
        subtitleView = new TextView(context);
        subtitleView.setTextColor(0xFFDCDCE2);
        subtitleView.setTextSize(13);
        subtitleView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        subtitleView.setPadding(0, (int) (3 * density), 0, 0);
        addView(subtitleView);
    }

    public void displayPiece(LandscapePiece piece) {
        if (piece == null) return;
        handler.removeCallbacks(fadeOutRunnable);
        animate().cancel();

        titleViView.setText(piece.titleVi);
        subtitleView.setText(String.format("%s — %s, %s", piece.titleEn, piece.artist, piece.year));

        setAlpha(0f);
        setVisibility(VISIBLE);
        animate()
                .alpha(1f)
                .setDuration(800L)
                .setListener(null)
                .start();

        handler.postDelayed(fadeOutRunnable, 5500L);
    }

    public void showBriefly() {
        handler.removeCallbacks(fadeOutRunnable);
        animate().cancel();
        setVisibility(VISIBLE);
        animate()
                .alpha(1f)
                .setDuration(400L)
                .setListener(null)
                .start();
        handler.postDelayed(fadeOutRunnable, 4000L);
    }

    public void dismiss() {
        handler.removeCallbacks(fadeOutRunnable);
        animate().cancel();
        setVisibility(GONE);
    }
}
```

- [ ] **Step 2: Implement LandscapeView.java**

Create `apps/screensaver-landscape/src/main/java/com/appletv/screensaver/landscape/LandscapeView.java`:
```java
package com.appletv.screensaver.landscape;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LandscapeView extends FrameLayout {

    private static final String TAG = "LandscapeView";
    private static final String PREFS_NAME = "landscape_prefs";
    private static final String KEY_LAST_INDEX = "last_piece_index";

    private static final long SLIDE_DURATION = 28_000L;
    private static final long CROSSFADE_DURATION = 2500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Random random = new Random();
    private final List<Integer> playlist = new ArrayList<>();

    private SlideHolder currentSlide;
    private SlideHolder nextSlide;
    private LandscapeInfoCardView cardView;

    private int playlistPosition = 0;
    private int lastShownIndex = -1;
    private boolean running = false;
    private ValueAnimator kenBurnsAnimator;

    private final Runnable nextSlideRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            loadAndTransitionNext();
        }
    };

    public LandscapeView(Context context) {
        this(context, null);
    }

    public LandscapeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setBackgroundColor(0xFF000000);

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            lastShownIndex = prefs.getInt(KEY_LAST_INDEX, -1);
        } catch (Exception e) {
            Log.w(TAG, "Failed reading preferences: " + e.getMessage());
        }

        currentSlide = new SlideHolder(context);
        addView(currentSlide.imageView);

        nextSlide = new SlideHolder(context);
        nextSlide.imageView.setAlpha(0f);
        addView(nextSlide.imageView);

        float density = getResources().getDisplayMetrics().density;
        cardView = new LandscapeInfoCardView(context);
        LayoutParams cardLp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        cardLp.gravity = Gravity.BOTTOM | Gravity.START;
        cardLp.leftMargin = (int) (48 * density);
        cardLp.bottomMargin = (int) (40 * density);
        addView(cardView, cardLp);
    }

    private void initPlaylist() {
        playlist.clear();
        playlist.addAll(LandscapeCatalog.createShuffledIndices(random, lastShownIndex));
        playlistPosition = 0;
    }

    public void start() {
        if (running) return;
        running = true;
        initPlaylist();
        loadAndTransitionNext();
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(nextSlideRunnable);
        if (kenBurnsAnimator != null) {
            kenBurnsAnimator.cancel();
        }
        if (currentSlide != null) {
            currentSlide.imageView.animate().cancel();
        }
        if (nextSlide != null) {
            nextSlide.imageView.animate().cancel();
        }
        if (cardView != null) {
            cardView.dismiss();
        }
        playlist.clear();
    }

    public void next() {
        handler.removeCallbacks(nextSlideRunnable);
        loadAndTransitionNext();
    }

    public boolean handleRemoteKey(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            next();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                   keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                   keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                   keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            if (cardView != null) {
                cardView.showBriefly();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    private void loadAndTransitionNext() {
        if (!running) return;
        if (playlist.isEmpty() || playlistPosition >= playlist.size()) {
            initPlaylist();
        }
        if (playlist.isEmpty()) return;

        final int index = playlist.get(playlistPosition++);
        lastShownIndex = index;

        final LandscapePiece piece = LandscapeCatalog.getPiece(index);
        if (piece == null) return;

        try {
            getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_LAST_INDEX, index)
                    .apply();
        } catch (Exception ignored) {}

        executor.execute(() -> {
            Bitmap bitmap = decodeAsset("landscapes/" + piece.filename);
            if (bitmap == null) return;

            handler.post(() -> {
                if (!running) {
                    bitmap.recycle();
                    return;
                }
                applyTransition(bitmap, piece);
            });
        });
    }

    private void applyTransition(Bitmap bitmap, LandscapePiece piece) {
        nextSlide.setImage(bitmap);
        nextSlide.resetTransform();

        // Directional Ken Burns setup
        final boolean isPanorama = piece.aspectRatio > 1.78f;
        final boolean reverseMotion = random.nextBoolean();

        startKenBurns(nextSlide, isPanorama, reverseMotion);

        // 2.5s Crossfade
        nextSlide.imageView.animate()
                .alpha(1f)
                .setDuration(CROSSFADE_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        SlideHolder temp = currentSlide;
                        currentSlide = nextSlide;
                        nextSlide = temp;
                        nextSlide.imageView.setAlpha(0f);
                        nextSlide.recycle();
                    }
                })
                .start();

        cardView.displayPiece(piece);

        handler.removeCallbacks(nextSlideRunnable);
        handler.postDelayed(nextSlideRunnable, SLIDE_DURATION);
    }

    private void startKenBurns(SlideHolder slide, boolean isPanorama, boolean reverse) {
        if (kenBurnsAnimator != null) {
            kenBurnsAnimator.cancel();
        }

        final float startScale = 1.02f;
        final float endScale = 1.07f;
        final float maxPan = 0.04f; // 4% drift

        kenBurnsAnimator = ValueAnimator.ofFloat(0f, 1f);
        kenBurnsAnimator.setDuration(SLIDE_DURATION);
        kenBurnsAnimator.setInterpolator(new LinearInterpolator());
        kenBurnsAnimator.addUpdateListener(anim -> {
            float frac = anim.getAnimatedFraction();
            float scale = startScale + (endScale - startScale) * frac;
            slide.imageView.setScaleX(scale);
            slide.imageView.setScaleY(scale);

            float drift = (reverse ? (1f - frac) : frac) - 0.5f; // -0.5 to +0.5
            if (isPanorama) {
                // Wide image: drift horizontally
                float tx = slide.imageView.getWidth() * maxPan * drift;
                slide.imageView.setTranslationX(tx);
                slide.imageView.setTranslationY(0f);
            } else {
                // Taller image: drift vertically
                float ty = slide.imageView.getHeight() * maxPan * drift;
                slide.imageView.setTranslationY(ty);
                slide.imageView.setTranslationX(0f);
            }
        });
        kenBurnsAnimator.start();
    }

    private Bitmap decodeAsset(String path) {
        AssetManager am = getContext().getAssets();
        try (InputStream is = am.open(path)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.RGB_565; // High performance, 50% less RAM
            return BitmapFactory.decodeStream(is, null, opts);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding asset " + path, e);
            return null;
        }
    }

    private static class SlideHolder {
        final ImageView imageView;
        private Bitmap currentBitmap;

        SlideHolder(Context context) {
            imageView = new ImageView(context);
            imageView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        void setImage(Bitmap bm) {
            recycle();
            currentBitmap = bm;
            imageView.setImageBitmap(bm);
        }

        void resetTransform() {
            imageView.setAlpha(0f);
            imageView.setScaleX(1.02f);
            imageView.setScaleY(1.02f);
            imageView.setTranslationX(0f);
            imageView.setTranslationY(0f);
        }

        void recycle() {
            if (currentBitmap != null && !currentBitmap.isRecycled()) {
                imageView.setImageBitmap(null);
                currentBitmap.recycle();
                currentBitmap = null;
            }
        }
    }
}
```

- [ ] **Step 3: Compile check**

Run: `./gradlew :apps:screensaver-landscape:compileReleaseJavaWithJavac`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit UI components**

```bash
git add apps/screensaver-landscape/src/main/java/com/appletv/screensaver/landscape/LandscapeInfoCardView.java apps/screensaver-landscape/src/main/java/com/appletv/screensaver/landscape/LandscapeView.java
git commit -m "feat(screensaver-landscape): implement LandscapeView and LandscapeInfoCardView"
```

---

### Task 5: DreamService & Activity Integration

**Files:**
- Create: `apps/screensaver-landscape/src/main/java/com/appletv/screensaver/landscape/LandscapeDreamService.java`
- Create: `apps/screensaver-landscape/src/main/java/com/appletv/screensaver/landscape/LandscapeActivity.java`

**Interfaces:**
- `LandscapeDreamService`: overrides `onAttachedToWindow()`, `onDreamingStarted()`, `onDreamingStopped()`, `onDetachedFromWindow()`.
- `LandscapeActivity`: overrides `onCreate()`, `onKeyDown()`, `onUserInteraction()`.

- [ ] **Step 1: Implement LandscapeDreamService.java**

Create `apps/screensaver-landscape/src/main/java/com/appletv/screensaver/landscape/LandscapeDreamService.java`:
```java
package com.appletv.screensaver.landscape;

import android.service.dreams.DreamService;
import android.view.KeyEvent;

public class LandscapeDreamService extends DreamService {

    private LandscapeView landscapeView;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(true);
        setFullscreen(true);
        setScreenBright(true);

        landscapeView = new LandscapeView(this);
        setContentView(landscapeView);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        if (landscapeView != null) {
            landscapeView.start();
        }
    }

    @Override
    public void onDreamingStopped() {
        if (landscapeView != null) {
            landscapeView.stop();
        }
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        if (landscapeView != null) {
            landscapeView.stop();
            landscapeView = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && landscapeView != null) {
            if (landscapeView.handleRemoteKey(event.getKeyCode())) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
```

- [ ] **Step 2: Implement LandscapeActivity.java**

Create `apps/screensaver-landscape/src/main/java/com/appletv/screensaver/landscape/LandscapeActivity.java`:
```java
package com.appletv.screensaver.landscape;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

public class LandscapeActivity extends Activity {

    private LandscapeView landscapeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        landscapeView = new LandscapeView(this);
        setContentView(landscapeView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (landscapeView != null) {
            landscapeView.start();
        }
    }

    @Override
    protected void onPause() {
        if (landscapeView != null) {
            landscapeView.stop();
        }
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        if (landscapeView != null && landscapeView.handleRemoteKey(keyCode)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
```

- [ ] **Step 3: Compile and test whole module**

Run: `./gradlew :apps:screensaver-landscape:assembleRelease :apps:screensaver-landscape:test`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add apps/screensaver-landscape/src/main/java/com/appletv/screensaver/landscape/LandscapeDreamService.java apps/screensaver-landscape/src/main/java/com/appletv/screensaver/landscape/LandscapeActivity.java
git commit -m "feat(screensaver-landscape): add DreamService and companion Activity"
```

---

### Task 6: Deploy Script & End-to-End Build Verification

**Files:**
- Modify: `tools/deploy.sh`
- Modify: `AGENTS.md` (Update Monorepo Architecture tree)

**Interfaces:**
- `./tools/deploy.sh landscape [TARGET_DEV]`
- `./tools/deploy.sh screensavers [TARGET_DEV]`

- [ ] **Step 1: Update tools/deploy.sh**

In `tools/deploy.sh`:
1. Add `landscape` to case validation list: `launcher|parental-control|parental|flipclock|artmuseum|landscape|screensavers|all`.
2. Add `deploy_landscape()` function:
```bash
deploy_landscape() {
    echo "==> Building Screensaver Landscape APK..."
    (cd "$ROOT_DIR" && ./gradlew :apps:screensaver-landscape:assembleRelease)
    local apk=""
    if [ -d "$ROOT_DIR/apps/screensaver-landscape/build/outputs/apk/release" ]; then
        apk=$(find "$ROOT_DIR/apps/screensaver-landscape/build/outputs/apk/release" -name "*.apk" 2>/dev/null | head -n 1 || true)
    fi
    if [ -z "$apk" ] || [ ! -f "$apk" ]; then
        echo "Error: Landscape Screensaver APK not found" >&2
        exit 1
    fi
    echo "==> Installing Landscape Screensaver to $TARGET_DEV..."
    adb_install "$apk"
}
```
3. Update `screensavers` case to call `deploy_flipclock`, `deploy_artmuseum`, and `deploy_landscape`.
4. Update `all` case to include `deploy_landscape`.

- [ ] **Step 2: Update AGENTS.md monorepo architecture**

Add `apps/screensaver-landscape/` to the tree in `AGENTS.md`.

- [ ] **Step 3: Run full verification build across all modules**

Run: `./gradlew assembleRelease testDebugUnitTest :core:contract:test`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit tool updates and final verification**

```bash
git add tools/deploy.sh AGENTS.md
git commit -m "feat(tools): add landscape screensaver deploy target and update monorepo tree"
```
