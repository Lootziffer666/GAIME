#!/usr/bin/env python3
"""
Extract bark WAV files from 7z archives and organize them into the
composeResources/files/bark/ directory structure.

Voice mapping:
  gruff  -> brugg   (187 WAVs)
  raspy  -> nib     (191 WAVs)
  whiny  -> vellum  (194 WAVs)

This script is idempotent - it can be re-run safely. It will:
1. Extract 7z archives (if .extracted/ does not already exist)
2. Copy and rename WAVs into the target directory with sanitized filenames
3. Generate a JSON manifest listing all files per character

Filename sanitization rules:
- Lowercase
- Replace spaces with underscores
- Remove apostrophes and parentheses
- Keep .wav extension
"""

import json
import os
import re
import shutil
import sys
from pathlib import Path

# Try to import py7zr for archive extraction
try:
    import py7zr
except ImportError:
    py7zr = None

# Project root is one level up from scripts/
PROJECT_ROOT = Path(__file__).resolve().parent.parent

# Voice-to-character mapping
VOICE_MAP = {
    "gruff": "brugg",
    "raspy": "nib",
    "whiny": "vellum",
}

# Archive definitions (relative to project root)
ARCHIVES = {
    "gruff": ["gruff.7z.part1", "gruff.7z.part2"],
    "raspy": ["raspy.7z.part1", "raspy.7z.part2", "raspy.7z.part3"],
    "whiny": ["whiny.7z.part1", "whiny.7z.part2", "whiny.7z.part3"],
}

EXTRACTED_DIR = PROJECT_ROOT / ".extracted"
TARGET_BASE = PROJECT_ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "bark"
MANIFEST_PATH = PROJECT_ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "bark" / "bark_audio_manifest.json"


def sanitize_filename(name: str) -> str:
    """Sanitize a WAV filename: lowercase, underscores for spaces, remove special chars."""
    # Lowercase
    name = name.lower()
    # Remove apostrophes
    name = name.replace("'", "")
    # Remove parentheses
    name = name.replace("(", "").replace(")", "")
    # Replace spaces with underscores
    name = name.replace(" ", "_")
    # Remove any remaining special characters except underscores, hyphens, dots, and alphanumeric
    name = re.sub(r"[^a-z0-9_.\-]", "", name)
    # Collapse multiple underscores
    name = re.sub(r"_+", "_", name)
    return name


def extract_archives():
    """Extract 7z archives into .extracted/ directory."""
    if py7zr is None:
        print("ERROR: py7zr is not installed. Install with: pip install py7zr")
        sys.exit(1)

    for voice, parts in ARCHIVES.items():
        extract_target = EXTRACTED_DIR / voice
        # Check if already extracted
        wav_dir = extract_target / voice
        if wav_dir.exists() and any(wav_dir.glob("*.wav")):
            print(f"  [SKIP] {voice} already extracted to {extract_target}")
            continue

        # Find the first part file (py7zr handles multipart from the first part)
        first_part = PROJECT_ROOT / parts[0]
        if not first_part.exists():
            print(f"  [WARN] Archive not found: {first_part}, skipping extraction")
            continue

        print(f"  Extracting {voice} from {first_part}...")
        extract_target.mkdir(parents=True, exist_ok=True)
        with py7zr.SevenZipFile(first_part, mode="r") as archive:
            archive.extractall(path=extract_target)
        print(f"  [DONE] Extracted {voice}")


def copy_and_rename_wavs():
    """Copy WAVs from .extracted/ into composeResources/files/bark/ with sanitized names."""
    manifest = {}

    for voice, character in VOICE_MAP.items():
        source_dir = EXTRACTED_DIR / voice / voice
        target_dir = TARGET_BASE / character

        if not source_dir.exists():
            print(f"  [ERROR] Source directory not found: {source_dir}")
            sys.exit(1)

        # Create target directory
        target_dir.mkdir(parents=True, exist_ok=True)

        # Get all WAV files
        wav_files = sorted([f for f in source_dir.iterdir() if f.suffix.lower() == ".wav"])
        copied_files = []

        for wav_file in wav_files:
            sanitized_name = sanitize_filename(wav_file.name)
            target_path = target_dir / sanitized_name

            # Copy file
            shutil.copy2(wav_file, target_path)
            copied_files.append(sanitized_name)

        manifest[character] = sorted(copied_files)
        print(f"  [DONE] {character}: copied {len(copied_files)} WAVs")

    return manifest


def write_manifest(manifest: dict):
    """Write the bark audio manifest JSON file."""
    MANIFEST_PATH.parent.mkdir(parents=True, exist_ok=True)
    with open(MANIFEST_PATH, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2, ensure_ascii=False)
    print(f"  [DONE] Manifest written to {MANIFEST_PATH.relative_to(PROJECT_ROOT)}")


def main():
    print("=== Bark Audio Extraction Script ===\n")

    print("Step 1: Extracting archives...")
    extract_archives()

    print("\nStep 2: Copying and renaming WAVs...")
    manifest = copy_and_rename_wavs()

    print("\nStep 3: Writing manifest...")
    write_manifest(manifest)

    print("\n=== Summary ===")
    for character, files in manifest.items():
        print(f"  {character}: {len(files)} WAV files")

    total = sum(len(f) for f in manifest.values())
    print(f"  Total: {total} WAV files")
    print("\nDone!")


if __name__ == "__main__":
    main()
