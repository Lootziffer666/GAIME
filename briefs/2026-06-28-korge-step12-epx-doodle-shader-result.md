# Result: Step 12 — Kantenbewusster Doodle-Upscale-Shader (EPX/Scale2x + Outline + Boil)

**Brief:** `briefs/2026-06-28-korge-step12-epx-doodle-shader.md`
**Branch:** `kiro/korge-step12-epx-doodle-shader` (PR#50)
**PR:** https://github.com/Lootziffer666/GAIME/pull/50
**Datum:** 2026-06-28 · Integration-Review + Fix: Claude
**Status:** ✅ Funktioniert (nach Integration-Fix) — der Doodle-Look liest jetzt

---

## Acceptance

| Check | Ergebnis |
|---|---|
| `:core:desktopTest` | ✅ BUILD SUCCESSFUL |
| `:game:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:composeApp:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:game:screenshot` | ✅ inkl. `doodle_upscale_compare.png` |

---

## Geliefert
- `DoodleLineFilter.kt` Fragment-Shader komplett ersetzt:
  - **Point-Sampling** (`floor`) statt bilinear — der Step-11-Bug.
  - **EPX/Scale2x-Regeln** exakt (step()-basiert, eps=0.12 Toleranz) — publizierter Algorithmus,
    neu implementiert (Donor-Policy, kein Fremdcode).
  - **Outline** (Luminanz- + Alpha-Kanten → abdunkeln) + **Boil** (`u_Time`-Jitter).
- Vergleichs-Capture `doodle_upscale_compare.png`: bilinear | nearest | EPX+doodle | EPX-only.

## Integration-Fix (Claude) — Shader rendert erst nach Korrektur
Kiros Shader **kompilierte grün, produzierte aber leere/transparente Ausgabe** (EPX-Panels komplett
leer). Ursache: Die EPX-Mathe rechnete in normalisierten 0..1-Koordinaten, aber KorGEs `tex()`
erwartet **Pixel-Koordinaten** (`coords01 * texSize`, wie alle anderen Filter). Es sampelte die
(0,0)-Ecke → überall transparent. **Fix:** Sample-Koordinaten mit `texSize` multipliziert.
„render ≠ logic" (Skill) hat es gefangen — der Compile war grün, das Bild leer.

**Nach dem Fix verifiziert:** Vergleich + Zoom zeigen, dass EPX+doodle **scharf** ist (nicht die
weiche Step-11-Suppe) und klare dunkle Konturen um Haare/Augen/Körper zieht — der getuschte
Cartoon-/Doodle-Look liest jetzt deutlich gegenüber dem blanken nearest.

## Anmerkung / mögliche Folge-Politur
- Die **EPX-Diagonalen-Glättung** selbst ist subtil; der dominante Effekt ist die **Outline**. Wer
  stärkere Glättung will: EPX 3x/4x-Kette oder die finale `mix()`-Verblendung durch harte Quadrant-
  Auswahl (`step`) ersetzen (die Verblendung weicht minimal auf). Aktuell aber gut genug.
- Auf der echten 78×78-Gameplay-Skala (kleine Figuren) bleibt die Linie fein — Art-Direction am
  Bewegtbild mit dem Owner.

## DO_NOT_TOUCH — eingehalten ✅
B007 intakt; andere Shader/ShaderEffects/core/composeApp/mapbuilder/assets unberührt.

## Stand nach Merge
main aktualisiert. Der Doodle-Upscale funktioniert: scharfe, kantengeglättete, getuschte Figuren
aus den niedrig aufgelösten Sprites — die Grundlage für den 1440p-Doodle-Look steht und liest.
