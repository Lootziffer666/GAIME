# Integration: PR#56 (WorldMaterialFilter + forest_ranger) + PR#57 (Shader-Palette)

**Datum:** 2026-06-30 · Review + Integration: Claude · beide vom Owner/Kiro initiiert (kein Brief).

## Integriert
- **PR#56** — `WorldMaterialFilter` (fixedLocation 14): liest Overlay-Farben als Materialien
  (Wasser/Schnee/Blut/Risse) und rendert sie „physisch". Dazu **forest_ranger** (hi-res
  Character, 412×553-Frames, opaqueBodyH 551) — **als neuer Spieler verdrahtet**.
- **PR#57** — `CausticFilter`(15), `WetSurfaceFilter`(16), `DecayFilter`(17) + `MaterialWeatherFilter`
  (HSV-Material-Klassifikation des gemalten Bildes → Wetter pro Material) + **23 Landschafts-PNGs**.

## Verifiziert
- Build + `:core:desktopTest` grün; 61 Screenshots (alle bestehenden + 10 neue), kein Regress.
- **PNGs angesehen:** Caustic (Lichtflecken), WetSurface (Sättigung), Decay (Moos/Rost-FBM),
  rt_material (reflektierende Pfütze), material_weather (Sonne/Regen/Sturm). Alle rendern sichtbar.
- **forest_ranger** rendert sauber bei ~96px (551px → 96px normalisiert) — beweist die
  Auflösungs-Unabhängigkeit der Step-17-Pipeline. Steht auf dem Boden, liest klar.
- **Donor-Policy:** alle Shader in KorGEs Kotlin-Shader-DSL (reimplementiert) — kein fremder
  GLSL/Header kopiert. Referenzen (thebookofshaders, hoxxep RT-Demo) sind Technik-Inspiration.
- `fixedLocation` kollisionsfrei: 14/15/16/17 neu, disjunkt.

## Notizen (kein Blocker — für später)
1. **Zwei Material-Shader überlappen konzeptionell** (`WorldMaterialFilter` = Overlay-Pass;
   `MaterialWeatherFilter` = Background-HSV-Klassifikation). Komplementär, aber teilen die
   „Farbe→Material"-Logik → spätere Konsolidierung möglich.
2. **Die 23 Landschaften haben baked-in Kreaturen** (rote Imps) → verletzen „figurenfreie Maps"
   (locked). Hier nur **Shader-Test-Hintergründe**, nicht als Spielkarten verdrahtet. Vor Nutzung
   als kanonische Maps müssen sie figurenfrei sein (Pfeiler 3). (Stilisierte Fantasy-Imps, kein
   Content-Problem — geprüft.)
3. **Shader sind Demo (nur Harness)**, noch nicht live in `DoodleWorldScene` verdrahtet (außer der
   forest_ranger-Spielerwechsel). Palette-Aufbau, später live verschalten.
4. `material_weather_storm` zeigt ein leichtes Raster-Artefakt; forest_ranger-Sheet ist 1-reihig
   (keine Facing-Richtungen). Kleinkram.
5. **Spielerwechsel auf forest_ranger** war ungebrieft (in PR#56 gebündelt) — sieht gut aus, aber
   bewusst notiert, falls der Swordsman zurück soll.

## Stray
`ResizedImage_…png` liegt im Repo-Root (Owner-Upload). Unangetastet gelassen.
