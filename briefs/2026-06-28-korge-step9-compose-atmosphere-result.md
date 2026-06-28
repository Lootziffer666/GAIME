# Result: Step 9 — Filter-Komposition + Atmosphäre + Season-Politur

**Brief:** `briefs/2026-06-28-korge-step9-compose-atmosphere.md`
**Branch:** `kiro/korge-step9-compose-atmosphere` (PR#47)
**PR:** https://github.com/Lootziffer666/GAIME/pull/47
**Datum:** 2026-06-28 · **Modell:** Opus (gesunder Thread) · Integration-Review: Claude
**Status:** ✅ Fundament (Teil A) solide · ⚠️ drei Overlay-Visuals mit demselben `:game`-Bug

---

## Acceptance

| Check | Ergebnis |
|---|---|
| `:core:desktopTest` | ✅ grün (15+ neue Tests) |
| `:game:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:composeApp:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:game:screenshot` | ✅ 37 PNGs |

---

## Geliefert

### A — Filter-Komposition (Fundament) ✅ DER GEWINN
`ShaderEffects` auf KorGE `ComposedFilter` umgebaut: mehrere Shader wirken jetzt gleichzeitig auf
einem Container. `enable/disable` + die alten `attach*` als Wrapper (rückwärtskompatibel).
**Bewiesen:** `compose_lighting_fog.png` — Lighting UND Fog zugleich (Lichtkegel + Verdunklung +
Nebelschleier). Das war das Kernziel von Step 9 und schaltet alle künftigen Atmosphäre-Features frei.
**Regression:** bestehende Shader-Shots rendern weiter (shader_poison etc. ok).

### D1 — Mondlicht ✅ (Logik)
`DayNightClock.moonIntensity()`/`moonColor()` + Test. Fließt in den Ambient.

### D2 — Material-Ermüdung ⚠️ (Logik ✅, Rendering ❌)
`MaterialFatigue` (Stress-Grid, crack/broken-Schwellen) + Test grün. ABER:
`material_fatigue.png` zeigt **keine sichtbaren Risse** — das `MaterialFatigueOverlay` rendert nicht
(Positions-/Alpha-Problem).

---

## ⚠️ Drei Overlay-Visuals — derselbe `:game`-Bug (Folge-Brief)

Alle drei dieselbe Fehlerklasse: Overlay-Rects landen am Bildrand / sind unsichtbar, statt um die
kamerazentrierte Spieler-Kachel mit kräftigem Alpha gerendert zu werden — genau der in 7d/8
dokumentierte und im Brief explizit zitierte Fehler.

1. **Season-Overlays (Teil C):** Farben jetzt korrekt (orange/braun/rosa/gelb statt grau), ABER
   Blüten/Blätter rendern weiterhin als **Streifen am unteren Bildrand**, nicht verteilt auf der
   Karte. (`spring/summer/autumn_approach.png`)
2. **Material-Risse (Teil D2):** nicht sichtbar (s.o.).
3. **Frozen Approach (Teil B):** Komposition aktiv, aber Ambient zu hell + Schnee-Rect zu opak →
   liest als trüber Schneetag, nicht als verschneite Nacht. Fackel/Nebel zu schwach.

**Diagnose:** `:core` (Logik, Tests, Komposition) ist durchweg solide; die Schwäche liegt
ausschließlich im Overlay-Rendering (mapView-Koordinaten + Alpha + Kamera-Framing). Nächster Brief:
EIN fokussierter „Overlay-Rendering-Korrektheit"-Pass (Season + Material + Frozen-Tuning) mit
`WaterOverlay` als verbindlichem Muster.

---

## DO_NOT_TOUCH — eingehalten ✅
B007 intakt (sechstes Mal); einzelne Shader-Filter unberührt (nur `ShaderEffects.kt`-Manager);
kein composeApp/settings/MapConfig/andere Overlays.

## Integration (Claude)
Nichts hand-gefixt — die Overlay-Mängel sind dieselbe wiederkehrende Klasse und gehen gebündelt in
den nächsten Brief, statt drei Einzelfixes ins Integrationsbudget zu ziehen. Nur der echte Gewinn
(`compose_lighting_fog.png`) in die Gallery übernommen.

## Stand nach Merge
main aktualisiert. Filter-Komposition steht — das Fundament für echte Atmosphäre. Overlay-Rendering
(Seasons/Material/Frozen-Nacht) ist der nächste, klar abgegrenzte Schritt.
