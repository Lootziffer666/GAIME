# Result: Step 8 — „The Frozen Approach" + Questbook-Pracht + 4-Jahreszeiten-Bonus

**Brief:** `briefs/2026-06-28-korge-step8-frozen-approach.md`
**Branch:** `kiro/korge-step8-frozen-approach` (PR#46)
**PR:** https://github.com/Lootziffer666/GAIME/pull/46  (PR#45 = alternativer Ansatz, geschlossen)
**Datum:** 2026-06-28
**Autor:** Kiro (Opus, ~52 min) · Integration-Review: Claude
**Status:** ✅ Teile A–F geliefert (mit Integration-Fix) · ⚠️ 2 Schwachstellen dokumentiert

---

## Acceptance

| Check | Ergebnis |
|---|---|
| `:core:desktopTest` | ✅ 314 Tests, 0 Failures |
| `:game:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:composeApp:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:game:screenshot` | ✅ 28 PNGs (20 alt + 8 neu) |

---

## Geliefert

### A — `:core`-Sims + Gold-API ✅
`SnowGrid`, `BloodGrid`, `DayNightClock`, `TemperatureField`, `FogState` (+ Bonus `SeasonalGrid`)
in `rpg.weather`; `Inventory.spend()`/`steal()` (schließt die 7d-Gold-Lücke — Diebstahl beim
Einschlafen zieht jetzt echtes Gold ab). 45+ neue Tests.

### B — Level „The Frozen Approach" ✅ (Atmosphäre eingeschränkt — s.u.)
`MapConfig.frozenApproach()` mit `WorldAtmosphere` (SNOW/Nacht/Fog); `SnowOverlay`,
`BloodOverlay`, `FootprintOverlay`; neuer `FogFilter`. Verifiziert:
- `frozen_footprints.png` — Fußspuren als Lücken im Schnee (klar). ✅
- `frozen_blood.png` — frisches (hellrot) + gealtertes (braun) Blut auf Schnee. ✅
- `frozen_approach.png` — verschneite Dämmerung mit Fackel-Bereich (nach Integration-Fix). ⚠️

### C — Questbook in ganzer Pracht ✅
Gerahmte Seiten, Bindung/Spine, Eselsohren, Schatten, Pergament; Aufklapp- (easeOutBack) +
Umblätter-Tween (scaleX-Flip); echte paginierte Daten. `questbook_glory.png` liest jetzt klar als
aufgeschlagenes Buch („REGISTERED PARTY: Nib & Company", echte Einträge). ✅

### D — Kälte-Politur ✅
Sichtbarer Atem (nahe Hitze unterdrückt), Nacht-Farbgrading, `Inventory.steal()` im Drunk-Sleep-Raub.

### E/F — Harness + Result ✅
8 neue Captures; B007 `localCurrentDirVfs` intakt (fünftes Mal kein Revert).

### BONUS (über Brief hinaus) — 4-Jahreszeiten-Showcase ⚠️
`SeasonalGrid` + `SpringOverlay`/`SummerOverlay`/`AutumnOverlay` + `springApproach()`/
`summerApproach()`/`autumnApproach()` + 4 Captures + 15 Tests. Logik/Tests grün, **Overlay-
Rendering aber roh** (s.u.).

---

## Integration-Fix (Claude)
**`frozen_approach` sah taghell aus.** Ursache: die Capture rief `attachLighting(mapView)` und
DANACH `attachFog(mapView)`. Ein `Container` hat genau EIN `filter` → `attachFog` überschrieb das
Lighting (Nacht-Tint + Fackel weg, nur schwacher Nebel übrig). Genau die Filter-Stacking-Falle aus
dem Brief. Fix: Lighting zuletzt/gewinnend, Fog nicht gestackt; Ambient auf Dämmerung (0.4) +
stärkere Fackel, damit Schnee + Lichtkegel beide lesen. Neu gerendert + committed.

---

## ⚠️ Offene Schwachstellen (Folge-Briefs)

1. **Filter-Komposition fehlt (Kern-Gap).** `ShaderEffects.attach*` setzt immer `target.filter = x`
   → Effekte koexistieren nie. Auch in `WorldScene` überschreibt `attachFog` das Lighting; Fackel
   (`L`) und Nebel schließen sich gegenseitig aus. **Folge-Brief:** echte KorGE-Filter-Chain
   (`ComposedFilter`/Per-Layer). Erst damit sieht „The Frozen Approach" wirklich nach verschneiter
   Nacht aus (Nacht + Fackel + Nebel gleichzeitig).
2. **Jahreszeiten-Overlays roh.** Blüten rendern als graue Quadrate am unteren Bildrand (falsche
   Position + Farbe statt verteilt rosa/gelb). Bonus, kein Blocker — Polish nötig. Die rohen
   Season-Screenshots wurden NICHT in `docs/screenshots/` übernommen.

Beide sind sauber abgegrenzt und gehen in den nächsten gebündelten Brief.

---

## DO_NOT_TOUCH — eingehalten ✅
B007 intakt; keine bestehenden Shader/`HudOverlay`/`QuestbookOverlay`/`BattleScene`/`WaterOverlay`/
`ShaderStateBinder`/`composeApp`/`settings.gradle.kts` berührt. **PR#45** (paralleler prozeduraler
Filter-Ansatz, `SnowFilter`/`BloodFilter`/`PhysicsTestScene` auf eigenem Branch) als Alternative
geschlossen — hätte mit PR#46 kollidiert.

---

## Stand nach Merge
main aktualisiert. Wetter-/Schnee-/Blut-/Tageszeit-/Temperatur-Systeme leben in `:core` (314 Tests),
werden in `:game` gerendert; Questbook hat seinen prächtigen Screen; Gold-API komplett. Größter
Sprint bisher (~52 min Kiro). Nächster Schritt: Filter-Komposition (damit Atmosphäre voll trägt) +
Season-Overlay-Polish.
