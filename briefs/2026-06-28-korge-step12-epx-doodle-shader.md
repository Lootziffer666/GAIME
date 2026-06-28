# Brief: Step 12 — Kantenbewusster Doodle-Upscale-Shader (EPX/Scale2x + Outline + Boil)

**MODELL: Opus-only** (vor Start Modell prüfen — neuer Thread erbt die Wahl NICHT;
`.kiro/steering/handoff-protocol.md`).

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step12-epx-doodle-shader`
**BASE_SHA:** `207a6ba4`

**Pflicht vorab lesen:** der Skill `.claude/skills/gaime-shaders/SKILL.md` (Abschnitt
„Pixel-art upscale — what actually works"). Er fasst das Step-11-Experiment zusammen,
auf dem dieser Brief direkt aufbaut.

---

## Hintergrund (bewiesen in Step 11)

Der „Doodle/Anime4K"-Look scheiterte NICHT an der niedrigen Sprite-Auflösung, sondern an der
Upscale-Methode. Direktvergleich am 64px-Swordsman (~6×):
- **bilinear** → weich/verwaschen.
- **nearest** → scharf, aber blockig.
- **aktueller `DoodleLineFilter`** → sampelt bilinear + dunkelt nur ab → **verwischt** statt Linien
  zu ziehen. Falscher Algorithmus.

Ziel: ein **kantenbewusster, point-sampled** Shader, der die Low-Res-Pixel **scharf** vergrößert,
Diagonalen glättet (keine Treppchen, kein Blur) und feine dunkle Konturen + lebendiges „Boil" zieht
— die Grundlage des angestrebten 1440p-Doodle-Looks.

---

## Aufgabe

`DoodleLineFilter.kt` (existiert, `fixedLocation = 13`, in `ShaderEffects` registriert)
**umbauen** — Fragment-Shader ersetzen. Klasse/Registrierung/Uniforms-Grundgerüst bleiben.

### Teil 1 — Kantenbewusster Upscale (EPX/Scale2x, point-sampled)

Donor-Policy: EPX/Scale2x ist ein **publizierter Regelsatz** (Eric's Pixel Expansion / AdvMAME2x).
Aus dem Konzept neu implementieren, KEIN Fremdcode. Kommentar nennt die Quelle.

Pro Output-Fragment:
1. **Nearest/Point-Sampling**: aus `TexInfoUB.u_TextureSize` den Texel-Schritt bestimmen; die
   Quell-Texel-Koordinate per `floor()` ermitteln (NICHT bilinear sampeln — das war der Bug).
2. Mittel-Texel `P` + 4 Nachbarn lesen: `A`=oben, `B`=rechts, `C`=links, `D`=unten (je point-sampled).
3. **Sub-Quadrant** des Fragments im Quell-Texel über `fract()` bestimmen (P1 oben-links, P2
   oben-rechts, P3 unten-links, P4 unten-rechts).
4. **EPX/Scale2x-Regeln** (exakt) anwenden — Default = `P`, dann bedingt Nachbarfarbe:
   ```
   P1 = (C==A && C!=D && A!=B) ? A : P
   P2 = (A==B && A!=C && B!=D) ? B : P
   P3 = (D==C && D!=B && C!=A) ? C : P
   P4 = (B==D && B!=A && D!=C) ? D : P
   ```
   Farb-Gleichheit mit kleiner Toleranz (Luminanz-/RGB-Distanz < eps), nicht exaktem `==`, damit
   leichte Sprite-Antialiasing-Ränder nicht stören.

Ergebnis: scharfe, kantengeglättete Vergrößerung ohne Blur.

### Teil 2 — Doodle-Outline + Boil (auf dem EPX-Ergebnis)

- **Outline:** Kanten aus den point-gesampelten Nachbarn erkennen (Alpha-Kante = Silhouette,
  oder starke Luminanz-Differenz = Innenlinie) und dort **dunkle, dünne Linie** ziehen
  (abdunkeln + leicht verdicken/dilatieren — nicht nur 0.85× wie bisher). `u_LineStrength` steuert.
- **Boil:** winziger `u_Time`-getriebener Offset auf die Quell-Texel-Auswahl (z.B. ±0.3 Texel,
  `u_Jitter`), sodass die Linien pro Frame minimal zittern. Im Screenshot mit festem `u_Time`
  reproduzierbar.
- **Alpha** des Sprites erhalten (transparente Sprite-Ränder bleiben transparent).

Uniforms bleiben: `u_Time`, `u_LineStrength`, `u_Jitter` (+ ggf. interne eps-Konstante).

---

## Teil 3 — Vergleichs-Beweis (ScreenshotHarness)

**KRITISCH — B007:** `localCurrentDirVfs["build/screenshots"]` + Import NICHT ändern.

Neue Capture `captureDoodleUpscaleCompare()` bei z.B. `Size(1680.0, 620.0)`, registriert in `main()`:
- Swordsman-Frame 0 laden (`SpriteLoader.load("assets/HD/characters/swordsman/PNG/Swordsman_lvl1/Without_shadow/Swordsman_lvl1_Idle_without_shadow.png")[0]`),
  4 Panels ~6× skaliert nebeneinander, je beschriftet:
  1. `bilinear` (`smoothing = true`, kein Filter)
  2. `nearest` (`smoothing = false`, kein Filter)
  3. `EPX+doodle` (neuer Filter)
  4. `EPX only` (Filter mit `u_LineStrength = 0`, zeigt den reinen Upscale)
- `save("doodle_upscale_compare")`.

Außerdem `doodle_1440p` (bestehende Capture) mit dem neuen Filter neu erzeugen — sie soll jetzt
**scharfe, saubere Linien-Figuren** zeigen statt weicher.

**Acceptance-Bild:** In `doodle_upscale_compare.png` muss Panel 3/4 **deutlich schärfer als
bilinear** sein, mit geglätteten Diagonalen (kein Blur) und sichtbarer dünner Kontur — NICHT
verwaschen wie der alte Filter.

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/shader/DoodleLineFilter.kt   (Fragment-Shader ersetzen)
  - game/src/desktopMain/kotlin/game/ScreenshotHarness.kt         (Vergleichs-Capture + doodle_1440p neu)

create:
  - briefs/2026-06-28-korge-step12-epx-doodle-shader-result.md
```

## DO_NOT_TOUCH

```
- game/src/desktopMain/kotlin/game/ScreenshotHarness.kt → localCurrentDirVfs-Zeile + Import (B007)
- game/src/desktopMain/kotlin/game/shader/  ANDERE Filter (Poison/BeerGoggle/Lighting/Rain/
        HeatShimmer/Fog) NICHT ändern — nur DoodleLineFilter.kt
- ShaderEffects.kt  (DoodleLineFilter ist schon registriert — nicht nötig anzufassen)
- core/, composeApp/, tools/mapbuilder/, assets/ (nur lesen), settings.gradle.kts
- docs/KNOWN_BUGS.md  nur lesen
```

---

## ACCEPTANCE

```bash
./gradlew :core:desktopTest                  → BUILD SUCCESSFUL
./gradlew :game:compileKotlinDesktop         → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop   → BUILD SUCCESSFUL
bash scripts/setup-gl.sh; ./gradlew :game:screenshot   → inkl. doodle_upscale_compare.png + doodle_1440p.png
```
Bei GL-„Too many callbacks" einmal wiederholen. Bestehende Screenshots müssen weiter funktionieren.

---

## Kontext / Querverweise

- **Skill `gaime-shaders`** (Pflichtlektüre) — Upscale-Befund, ShaderFilter-Muster, B007,
  „render ≠ logic: PNG ansehen".
- **EPX/Scale2x-Quelle (Konzept):** Wikipedia „Pixel-art scaling algorithms". Regelsatz oben ist
  vollständig — keine weitere Recherche nötig, KEIN Fremdcode kopieren.
- **Shader-Muster:** `HeatShimmerFilter` (UV-Offset per `time`), `PoisonFilter` (UniformBlock/
  FragmentShader/BaseProgramProvider-Struktur). `TexInfoUB.u_TextureSize` für Texel-Schritt.
- **Wichtig:** NICHT bilinear sampeln (der alte Bug). Nearest via `floor(coord/texel)*texel`.
- **Nächste Stufe (NICHT hier):** EPX 3x/4x-Kette für stärkere Glättung; echte Anime4K-CNN-Kette;
  Filter auf die Spiel-Charaktere im echten WorldScene; 1440p-Fenster-Ausgabe.
```
