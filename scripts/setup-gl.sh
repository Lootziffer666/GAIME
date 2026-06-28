#!/usr/bin/env bash
# Installs the headless GL stack needed for the offscreen screenshot harness
# (./gradlew :game:screenshot). KorGE's offscreen renderer uses Mesa EGL with a
# surfaceless platform + the LLVMpipe software rasterizer — no X server required.
#
# Verified packages (Ubuntu 24.04 "noble"):
set -euo pipefail

PKGS=(libegl1 libegl-mesa0 libgl1-mesa-dri libgbm1 mesa-libgallium)

sudo apt-get update -qq
# Install; if the mirror has a transient mesa point-version skew, retry letting
# apt pick a consistent set.
sudo apt-get install -y "${PKGS[@]}" || sudo apt-get install -y --fix-missing "${PKGS[@]}"

echo "GL stack installed. Run the harness with:"
echo "  EGL_PLATFORM=surfaceless LIBGL_ALWAYS_SOFTWARE=1 ./gradlew :game:screenshot"
echo "(the :game:screenshot task already sets these env vars itself.)"
