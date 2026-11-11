#!/usr/bin/env python3
"""
Convert landscape paintings from resources/landscape/ to optimized QHD (2560x1440) WebP for Android TV.
Resizes using Lanczos filter and encodes to WebP Q85.
Outputs to apps/screensaver-landscape/src/main/assets/landscapes/.
"""
import os
import sys
from PIL import Image

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT_DIR = os.path.dirname(SCRIPT_DIR)
OUT_DIR = os.path.join(ROOT_DIR, "apps", "screensaver-landscape", "src", "main", "assets", "landscapes")

# Search for resources/landscape directory (support both root repo and worktree execution)
CANDIDATE_DIRS = [
    os.path.join(ROOT_DIR, "resources", "landscape"),
    os.path.join(ROOT_DIR, "..", "..", "resources", "landscape"),
    "/home/lamnh/projects/nova-tv-launcher/resources/landscape",
]

SRC_DIR = None
for c in CANDIDATE_DIRS:
    if os.path.isdir(c):
        SRC_DIR = os.path.abspath(c)
        break

# Mapping from source jpg in resources/landscape to target asset filename
FILE_MAPPING = {
    "01-hay-wain-constable-16x9-3840x2160.jpg": "hay_wain.webp",
    "02-ninth-wave-aivazovsky-16x9-3840x2160.jpg": "ninth_wave.webp",
    "04-chalk-cliffs-rugen-friedrich-16x9-3840x2160.jpg": "chalk_cliffs.webp",
    "05-mont-sainte-victoire-cezanne-16x9-3840x2160.jpg": "mont_sainte_victoire.webp",
    "06-oxbow-thomas-cole-16x9-3840x2160.jpg": "oxbow.webp",
    "07-heart-of-andes-church-16x9-3840x2160.jpg": "heart_of_andes.webp",
    "08-niagara-church-16x9-3840x2160.jpg": "niagara.webp",
    "09-rocky-mountains-bierstadt-16x9-3840x2160.jpg": "rocky_mountains.webp",
    "14-flood-port-marly-sisley-16x9-3840x2160.jpg": "flood_port_marly.webp",
    "15-bridge-narni-corot-16x9-3840x2160.jpg": "bridge_narni.webp",
}

TARGET_WIDTH = 2560
TARGET_HEIGHT = 1440
WEBP_QUALITY = 85

def convert_all():
    if not SRC_DIR:
        print("Error: Could not locate resources/landscape directory in search paths:", file=sys.stderr)
        for c in CANDIDATE_DIRS:
            print(f"  - {c}", file=sys.stderr)
        sys.exit(1)

    print(f"Source directory: {SRC_DIR}")
    print(f"Output directory: {OUT_DIR}")
    os.makedirs(OUT_DIR, exist_ok=True)

    # Clean existing webp files in target directory to avoid obsolete assets
    existing_files = os.listdir(OUT_DIR)
    for f in existing_files:
        if f.endswith(".webp") and f not in FILE_MAPPING.values():
            stale_path = os.path.join(OUT_DIR, f)
            print(f"Removing stale asset: {f}")
            os.remove(stale_path)

    converted_count = 0
    total_bytes = 0

    for src_name, target_name in FILE_MAPPING.items():
        src_path = os.path.join(SRC_DIR, src_name)
        if not os.path.exists(src_path):
            print(f"Error: Missing source image {src_path}", file=sys.stderr)
            sys.exit(1)

        im = Image.open(src_path)
        if im.size != (TARGET_WIDTH, TARGET_HEIGHT):
            im = im.resize((TARGET_WIDTH, TARGET_HEIGHT), Image.Resampling.LANCZOS)

        if im.mode != "RGB":
            im = im.convert("RGB")

        out_path = os.path.join(OUT_DIR, target_name)
        im.save(out_path, format="WEBP", quality=WEBP_QUALITY, method=6)

        sz = os.path.getsize(out_path)
        total_bytes += sz
        converted_count += 1
        print(f"[{converted_count:02d}/10] Converted {src_name} -> {target_name} ({TARGET_WIDTH}x{TARGET_HEIGHT}, {sz / 1024:.1f} KB)")

    print(f"\nSuccessfully converted {converted_count} landscape paintings to QHD WebP.")
    print(f"Total package asset size: {total_bytes / (1024 * 1024):.2f} MB ({total_bytes / converted_count / 1024:.1f} KB avg)")
    assert converted_count == 10, f"Expected 10 paintings, got {converted_count}"

if __name__ == "__main__":
    convert_all()
