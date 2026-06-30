# GAIME Shader Editor — Iteration & Analysis Report

Generated: 2026-06-30
Target: `1782823374309.png` (1365x768) — Lum=67.2, Contrast=48.4, RGB=[63.3, 69.6, 64.5]
Scene:  `1782823262240.png` (1364x768) — Lum=112.2 (baseline Delta-E to target: 113.07)

## Full Ranking (all iterations, sorted by Delta-E)

| # | File | Delta-E | Lum | Contrast | RGB |
|---|------|---------|-----|----------|-----|
| 1 | v50_full_storm.png | 71.99 **★** | 52.2 | 32.9 | [49.5, 52.6, 57.4] |
| 2 | v50_spring_dawn.png | 72.62 | 53.4 | 34.2 | [60.2, 51.2, 46.5] |
| 3 | v48_dawn_fog.png | 72.79 | 53.9 | 35.4 | [62.7, 51.6, 42.7] |
| 4 | v50_iter13_twilight_moderate.png | 73.04 | 49.7 | 33.6 | [47.7, 49.9, 53.3] |
| 5 | v48_dusk_storm.png | 73.37 | 54.4 | 38.7 | [49.8, 55.7, 59.7] |
| 6 | v50_iter2_brighter_storm.png | 74.02 | 47.3 | 34.1 | [46.6, 47.5, 48.0] |
| 7 | v50_iter8_predawn_storm.png | 74.02 | 47.0 | 33.9 | [44.9, 47.5, 50.1] |
| 8 | v48_midnight.png | 76.92 | 43.4 | 39.7 | [40.3, 44.8, 45.1] |
| 9 | v48_iter16_late_night_rain.png | 81.92 | 82.3 | 35.4 | [75.1, 84.1, 91.7] |
| 10 | v48_autumn_dusk.png | 81.96 | 46.5 | 36.7 | [65.2, 41.0, 26.2] |
| 11 | v50_iter14_dark_high_contrast.png | 85.6 | 85.0 | 32.8 | [79.0, 86.2, 94.1] |
| 12 | v48_iter7_dusk_moderate.png | 87.02 | 89.7 | 33.1 | [99.4, 87.0, 77.9] |
| 13 | v50_iter3_less_fog.png | 87.18 | 88.9 | 33.1 | [83.8, 90.2, 95.2] |
| 14 | v50_iter15_balanced.png | 91.43 | 91.9 | 32.9 | [85.2, 93.6, 101.2] |
| 15 | v50_iter5_overcast_noon.png | 93.24 | 104.3 | 41.6 | [103.0, 109.9, 79.2] |
| 16 | v50_puddles_only.png | 94.34 | 101.3 | 48.1 | [100.9, 107.4, 71.2] |
| 17 | v50_match_target.png | 96.23 | 105.3 | 47.0 | [105.0, 111.2, 75.9] |
| 18 | v50_wet_no_puddles.png | 100.11 | 102.9 | 57.9 | [103.2, 109.7, 67.3] |
| 19 | v47_storm_preset.png | 101.35 | 98.3 | 39.9 | [107.6, 98.7, 71.6] |
| 20 | v50_iter4_green_shift.png | 103.77 | 103.6 | 31.7 | [98.2, 105.1, 109.5] |
| 21 | v47_iter6_tweaked_storm.png | 104.3 | 99.5 | 40.2 | [110.4, 99.4, 72.0] |
| 22 | v47_light_mist.png | 105.25 | 110.7 | 45.6 | [113.0, 114.2, 85.9] |
| 23 | v47_heavy_storm.png | 106.57 | 97.2 | 38.7 | [107.1, 96.6, 74.7] |
| 24 | v47_iter12_high_w_lowfog.png | 106.79 | 98.4 | 42.7 | [111.2, 97.5, 69.1] |
| 25 | v48_noon_clear.png | 106.96 | 109.0 | 57.6 | [110.9, 113.4, 81.3] |
| 26 | v50_dry_noon.png | 110.12 | 111.2 | 61.5 | [110.8, 119.0, 72.6] |
| 27 | v47_dry.png | 110.29 | 112.2 | 59.2 | [108.9, 118.4, 89.0] |
| 28 | v50_rain_no_wet.png | 111.83 | 112.9 | 61.8 | [112.2, 120.7, 75.1] |
| 29 | v48_iter11_late_dusk.png | 117.7 | 114.3 | 31.7 | [104.1, 116.8, 128.6] |
| 30 | v48_winter_noon.png | 127.86 | 123.2 | 51.1 | [138.2, 123.5, 82.2] |
| 31 | v50_iter9_earlydawn.png | 140.0 | 129.8 | 30.9 | [120.4, 132.2, 142.3] |
| 32 | v50_iter10_midnight_nofog.png | 243.37 | 198.6 | 27.1 | [180.4, 203.5, 222.1] |

## Key Findings

### Best match: `v50_full_storm.png` (Delta-E = 71.99)
- Improvement over raw scene: 41.1 points (36% closer to target)

### Architectural Issues Discovered

1. **Contrast ceiling at ~33** — Target has contrast=48.4 but all shader outputs top out at ~33-42.
   The material grading mixes via `mix(fin, gradeColor, weight)` which averages pixel values toward
   the grade color, flattening the original luminance distribution. Fix: apply contrast boost AFTER grading.

2. **Day/Night hard transition** — Between timeofday=0 (Lum=50) and timeofday=300 (Lum=130) there is
   no stable zone at Lum=67. The `smoothstep` in `isDay/isNight` creates a gap. Fix: adjust the
   smoothstep ranges or add a "overcast" multiplier independent of time-of-day.

3. **Lightning flash bug** — At timeofday=0 + lightning>50, the flash triggers near-permanently
   (v50_iter10 reached Lum=198). The `step(0.98, fract(sin(...)))` hash is biased at certain time values.
   Fix: use a proper temporal trigger (e.g., random interval every 3-8 seconds).

4. **Blue bias from fog/puddle colors** — Default fog and puddle colors shift the output toward blue.
   Target is nearly neutral (R≈G≈B). Puddle default should be neutral grey, not blue.

### What Works Well

- Material classification correctly isolates roof/stone/grass/wood/foliage zones
- Shader transforms scene 36% closer to target vs raw input (113→72 Delta-E)
- Per-material grading applies correctly (visible color shift per zone)
- Rain particles and rivulets render at correct density
- Puddle placement respects material boundaries
- WebGL runs without errors across all 3 editors (17/17 renders successful)

## Iteration History

| Round | Strategy | Best Delta-E | Problem |
|-------|----------|-------------|----------|
| 1 | Initial scenarios (17 configs) | 71.99 | Too dark (Lum=52) |
| 2 | Brighten (reduce fog, shift green) | 74.02 | Overshot to Lum=89-130 |
| 3 | Darken again, reduce darken param | 73.04 | Still stuck at Lum=47-50 |
| 4 | Conclusion: architectural ceiling | — | Contrast/transition issues |

## Recommended Next Steps

1. Add post-grading contrast boost: `fin = mix(vec3(avgLum), fin, 1.0 + u_contrastBoost);`
2. Add overcast dimmer independent of day/night: `fin *= (1.0 - u_overcast * 0.4);`
3. Fix lightning temporal trigger to use proper random intervals
4. Change default puddle color from blue to neutral dark grey
