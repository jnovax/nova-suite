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
