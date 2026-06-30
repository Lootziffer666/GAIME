"""
Full-Resolution Pixel-Level Material Map for GAIME.

Segments the HD source image at FULL PIXEL RESOLUTION (no tile grid, no upscaling).
Every single pixel gets a material classification based on its HSV color.

This produces a 2560x1440 single-channel PNG where each pixel's value
encodes the Material ID. The KorGE MaterialWeatherFilter binds this as
a second texture and reads exact per-pixel material information.

No tile artifacts. No blocky boundaries. No nearest-neighbor hacks.
The material boundary IS the pixel boundary in the source image.

Encoding (R channel, evenly spaced for shader decode):
  R=0   → GRASS       (matId=0)
  R=36  → STONE_PATH  (matId=1)
  R=73  → WOOD        (matId=2)
  R=109 → ROOF        (matId=3)
  R=146 → FOLIAGE     (matId=4)
  R=182 → WATER       (matId=5)
  R=219 → FLOWERS     (matId=6)
  R=255 → UNKNOWN     (matId=7)

Shader decode: matId = floor(R / 255.0 * 7.0 + 0.5)

Performance: Vectorized numpy — ~0.1s for 2560x1440 (3.7M pixels).
"""

import cv2
import numpy as np
import os
from pathlib import Path
from collections import Counter

# Material IDs
MAT_GRASS = 0
MAT_STONE = 1
MAT_WOOD = 2
MAT_ROOF = 3
MAT_FOLIAGE = 4
MAT_WATER = 5
MAT_FLOWERS = 6
MAT_UNKNOWN = 7

MATERIAL_NAMES = {
    MAT_GRASS: "GRASS",
    MAT_STONE: "STONE_PATH",
    MAT_WOOD: "WOOD",
    MAT_ROOF: "ROOF",
    MAT_FOLIAGE: "FOLIAGE",
    MAT_WATER: "WATER",
    MAT_FLOWERS: "FLOWERS",
    MAT_UNKNOWN: "UNKNOWN",
}

# R-channel encoding values (shader decodes via: floor(R/255*7+0.5))
MATERIAL_R = {
    MAT_GRASS: 0,
    MAT_STONE: 36,
    MAT_WOOD: 73,
    MAT_ROOF: 109,
    MAT_FOLIAGE: 146,
    MAT_WATER: 182,
    MAT_FLOWERS: 219,
    MAT_UNKNOWN: 255,
}

# Preview colors (BGR for OpenCV)
PREVIEW_COLORS = {
    MAT_GRASS: (80, 200, 80),
    MAT_STONE: (180, 180, 180),
    MAT_WOOD: (50, 90, 140),
    MAT_ROOF: (30, 50, 160),
    MAT_FOLIAGE: (30, 100, 15),
    MAT_WATER: (200, 150, 60),
    MAT_FLOWERS: (180, 100, 200),
    MAT_UNKNOWN: (128, 128, 128),
}


def segment_fullres(image_path: str) -> np.ndarray:
    """
    Segment image at full pixel resolution using vectorized HSV classification.
    
    Returns:
        Material ID array (uint8), same dimensions as source image.
        Values are raw IDs (0-7), NOT the R-channel encoding.
    """
    img = cv2.imread(image_path)
    if img is None:
        raise FileNotFoundError(f"Cannot read: {image_path}")
    
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    h, w = img.shape[:2]
    
    H = hsv[:, :, 0].astype(np.int16)
    S = hsv[:, :, 1].astype(np.int16)
    V = hsv[:, :, 2].astype(np.int16)
    
    # Default: GRASS
    mat = np.full((h, w), MAT_GRASS, dtype=np.uint8)
    
    # === Classification order: specific → general, later overrides earlier ===
    
    # FOLIAGE: very dark OR (green/cyan hue + dark)
    mat[(V < 45)] = MAT_FOLIAGE
    mat[(H >= 25) & (H <= 105) & (S > 30) & (V >= 45) & (V < 110)] = MAT_FOLIAGE
    
    # GRASS: green hue, bright enough
    mat[(H >= 25) & (H <= 85) & (S > 50) & (V >= 110)] = MAT_GRASS
    
    # WATER: blue hue
    mat[(H >= 90) & (H <= 130) & (S > 50) & (V > 50)] = MAT_WATER
    
    # FLOWERS: pink/magenta (H>150 or H<5, saturated, not too dark)
    mat[((H > 150) | (H < 5)) & (S > 80) & (V > 50)] = MAT_FLOWERS
    
    # === Warm browns (H=5-25): the critical split ===
    warm = (H >= 5) & (H <= 25) & (S > 80)
    
    # STONE_PATH: brightest warm tones (V>210) — sandy/beige cobblestones
    mat[warm & (V > 210)] = MAT_STONE
    
    # ROOF: reddish hue (H<14), medium saturation, medium-high value
    mat[warm & (H < 14) & (S > 120) & (V >= 100) & (V <= 210)] = MAT_ROOF
    
    # WOOD: remaining warm browns at lower values (building walls, doors)
    wood_mask = warm & (V >= 45) & (V <= 180)
    # Don't overwrite stone or roof
    wood_mask = wood_mask & (mat != MAT_STONE) & (mat != MAT_ROOF)
    mat[wood_mask] = MAT_WOOD
    
    # === Desaturated zones ===
    # Bright desaturated = stone (grey highlights, light surfaces)
    mat[(S < 40) & (V > 160)] = MAT_STONE
    
    # Medium desaturated = wood (building shadows, structural grey)
    # But not if it's already foliage (dark green reads as low-sat)
    desat_mid = (S < 40) & (V >= 50) & (V <= 160)
    mat[desat_mid & (mat != MAT_FOLIAGE)] = MAT_WOOD
    
    return mat


def encode_material_map(mat: np.ndarray) -> np.ndarray:
    """Convert raw material IDs (0-7) to R-channel encoding for the shader."""
    encoded = np.zeros_like(mat)
    for mat_id, r_value in MATERIAL_R.items():
        encoded[mat == mat_id] = r_value
    return encoded


def generate_preview(mat: np.ndarray, scale: int = 1) -> np.ndarray:
    """Generate a color-coded preview of the material map."""
    h, w = mat.shape
    preview = np.zeros((h, w, 3), dtype=np.uint8)
    for mat_id, color in PREVIEW_COLORS.items():
        mask = mat == mat_id
        preview[mask] = color
    
    if scale > 1:
        preview = cv2.resize(preview, (w // scale, h // scale), interpolation=cv2.INTER_NEAREST)
    return preview


def run_pipeline(image_path: str, output_dir: str) -> dict:
    """
    Run full-resolution material segmentation pipeline.
    
    Produces:
    - village_material_fullres.png: R-channel encoded material map (for shader)
    - village_material_preview.png: Color-coded human-readable preview
    - Also copies to assets/materials/ if in GAIME repo
    """
    os.makedirs(output_dir, exist_ok=True)
    
    print(f"[fullres] Loading: {image_path}")
    img = cv2.imread(image_path)
    if img is None:
        raise FileNotFoundError(f"Cannot read: {image_path}")
    h, w = img.shape[:2]
    print(f"[fullres] Size: {w}x{h} = {w*h:,} pixels")
    
    # Segment
    print("[fullres] Segmenting at full pixel resolution...")
    import time
    t0 = time.time()
    mat = segment_fullres(image_path)
    elapsed = time.time() - t0
    print(f"[fullres] Done in {elapsed:.2f}s")
    
    # Stats
    counts = Counter(mat.flatten().tolist())
    print("[fullres] Material distribution:")
    for mid in sorted(counts.keys()):
        pct = counts[mid] / mat.size * 100
        name = MATERIAL_NAMES.get(mid, "?")
        print(f"  {name:12s}: {pct:5.1f}%")
    
    # Encode for shader
    encoded = encode_material_map(mat)
    
    # Save material map
    fullres_path = os.path.join(output_dir, "village_material_fullres.png")
    cv2.imwrite(fullres_path, encoded)
    size_kb = os.path.getsize(fullres_path) / 1024
    print(f"[fullres] Material map: {fullres_path} ({size_kb:.0f} KB)")
    
    # Save preview (quarter res for manageable file size)
    preview = generate_preview(mat)
    preview_path = os.path.join(output_dir, "village_material_preview.png")
    cv2.imwrite(preview_path, preview)
    preview_kb = os.path.getsize(preview_path) / 1024
    print(f"[fullres] Preview: {preview_path} ({preview_kb:.0f} KB)")
    
    # Copy to assets/materials/ if in GAIME repo
    repo_root = Path(image_path).parent
    assets_dir = repo_root / "assets" / "materials"
    if (repo_root / "assets").exists():
        assets_dir.mkdir(parents=True, exist_ok=True)
        asset_fullres = str(assets_dir / "village_material_fullres.png")
        cv2.imwrite(asset_fullres, encoded)
        print(f"[fullres] Asset: {asset_fullres}")
        asset_preview = str(assets_dir / "village_material_preview.png")
        cv2.imwrite(asset_preview, preview)
        print(f"[fullres] Asset: {asset_preview}")
    
    return {
        "mat": mat,
        "encoded": encoded,
        "fullres_path": fullres_path,
        "preview_path": preview_path,
    }


if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(
        description="Full-resolution pixel-level material segmentation for GAIME"
    )
    parser.add_argument("image", help="Path to HD source image")
    parser.add_argument("-o", "--output", default="output", help="Output directory")
    
    args = parser.parse_args()
    run_pipeline(args.image, args.output)
