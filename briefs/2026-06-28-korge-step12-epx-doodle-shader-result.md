# Result: Step 12 — Kantenbewusster Doodle-Upscale-Shader (EPX/Scale2x + Outline + Boil)

**Brief:** briefs/2026-06-28-korge-step12-epx-doodle-shader.md
**Branch:** kiro/korge-step12-epx-doodle-shader
**Datum:** 2026-06-28

## Was wurde umgesetzt

### Teil 1 — EPX/Scale2x Point-Sampled Upscale
- `DoodleLineFilter.kt` Fragment-Shader komplett ersetzt
- **Point-sampling** via `floor(uv/texel)*texel + texel*0.5` (KEIN bilinear — der Step-11-Bug)
- Center-Texel P + 4 Nachbarn A/B/C/D point-gesampelt
- Sub-Quadrant per `fract()` bestimmt (welcher der 4 EPX-Pixel dieses Fragment ist)
- **EPX/Scale2x-Regeln** exakt implementiert (mit Luminanz-Toleranz eps=0.12 für Antialiasing-Ränder)
- Ergebnis: scharfe Diagonalen-Glättung ohne Blur

### Teil 2 — Outline + Boil
- **Edge detection:** max Luminanz-Gradient + Alpha-Discontinuity → kombinierter Edge-Factor
- **Outline:** Kanten um 90% abgedunkelt (`darkMul = 1 - edgeFactor * 0.9`), `u_LineStrength` steuert
- **Boil:** sin-basierter Texel-Offset (±0.3 Texel Amplitude), `u_Jitter` + `u_Time` getrieben
- Alpha des Sprites erhalten (transparente Ränder bleiben transparent)

### Teil 3 — Vergleichs-Screenshot
- `doodle_upscale_compare.png` (1680x620, 32KB): 4 Panels nebeneinander:
  1. bilinear (weich/verwaschen)
  2. nearest (scharf aber blockig)
  3. EPX + doodle (scharf + geglättete Diagonalen + dunkle Outlines)
  4. EPX only (lineStrength=0, reiner Upscale ohne Outlines)
- `doodle_1440p.png` (2560x1440, 4.6MB): mit neuem EPX-Shader statt altem bilinearen

## Testergebnis

```
./gradlew :core:desktopTest             → BUILD SUCCESSFUL
./gradlew :game:compileKotlinDesktop    → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop → BUILD SUCCESSFUL
./gradlew :game:screenshot              → 43 PNGs (bestehende + doodle_upscale_compare.png)
```

Keine Regressionen. Alle bestehenden Screenshots funktionieren unverändert.

## Abweichungen vom Brief

Keine.

## Neu entdeckte Bugs / Pitfalls

- **EPX-Regeln in GLSL:** Da GLSL kein if/else auf Vektor-Zuordnungen erlaubt (in KorGE's
  FragmentShaderDefault DSL), werden die Regeln als `step()`-basierte Multiplikatoren implementiert
  und per `mix()`/Interpolation auf den korrekten Sub-Quadranten angewendet.
- **Toleranz-basierte Gleichheit:** Exaktes `==` für Farben funktioniert nicht auf
  Antialiasing-Rändern von Sprites. `abs(lumA-lumB) < eps` mit eps=0.12 liefert robuste Ergebnisse.

## Was nicht angefasst wurde

DO_NOT_TOUCH komplett eingehalten:
- ScreenshotHarness `localCurrentDirVfs`-Zeile + Import unverändert (B007)
- Andere Shader-Filter unberührt
- ShaderEffects.kt nicht geändert (DoodleLineFilter war schon registriert)
- core/, composeApp/, tools/mapbuilder/, assets/ unberührt
