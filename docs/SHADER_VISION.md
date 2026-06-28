# Shader Vision — Prozedurale Darstellung als Kunstform

**Status:** Architektur-Entscheidung, 2026-06-28. Step 7a (Infrastruktur) done.

> *"Die Pixel-Art ist das Material. Die Shader sind der Pinsel.
> Jeder Moment im Spiel wird zum Gemälde."*

---

## Philosophie

Traditionelle Pixel-Art-Spiele zeichnen jeden visuellen Effekt als Sprite-Frame.
GAIME geht einen anderen Weg: **die Darstellung selbst wird zum dynamischen Medium**.
Shader transformieren dieselbe Pixel-Art-Basis in Echtzeit — parametrisch,
zeitgesteuert, zustandsabhängig. Das Bild reagiert auf Gameplay, Wetter, Emotionen
und Weltzeit. Kein Frame ist identisch. Die Welt lebt.

Das ist keine Post-Processing-Spielerei. Es ist eine **Rendering-Philosophie**:
- Die Pixel-Art-Assets bleiben klein, sauber, framework-frei (16px/64px Tiles+Sprites)
- Die Shader heben die Darstellung auf ein Level das manuell unmöglich wäre
- Jeder Effekt ist an **Gameplay-State** gebunden (nicht an handgesetzte Trigger)
- Neue Level/Szenen erben Atmosphäre automatisch aus dem System

---

## Effekt-Katalog (planned + implemented)

### ✅ Done (Step 7a)

| # | Effekt | Shader/Mechanismus | Gameplay-Anbindung |
|---|---|---|---|
| 1 | Vergiftung | PoisonFilter: Chromatic Aberration + Vignette-Tunnel | `poisonLevel` aus CombatEngine / Welt-State |
| 2 | Biergoggle | BeerGoggleFilter: Blur + Warmth + Sway | `drunkLevel` steigt mit Bierkonsum, NPCs "sehen besser aus" |

### Step 7b — 2D-Lighting

| # | Effekt | Mechanismus | Gameplay-Anbindung |
|---|---|---|---|
| 3 | Kerzen/Fackeln | Radial-Gradient + Perlin-Flackern, Multiply-Blend | Lichtquellen aus TMX-Layer oder Entity-System |
| 4 | Sprite-Schatten | 1D-Raycast in Light-Map, Sprites blockieren Licht | Dynamisch: NPCs werfen Schatten, Spieler auch |
| 5 | Gewitter-Blitz | screenBrightness-Flash (2 Frames) + Lichtenberg-Overlay | Wetter-State, zufälliger Timer |
| 6 | Tag/Nacht-Zyklus | Globaler Farbfilter (warm→blau) + Lichtquellen-Relevanz | Weltzeit-Counter |

### Step 7c — Wetter & Vegetation & Umgebung

| # | Effekt | Mechanismus | Gameplay-Anbindung |
|---|---|---|---|
| 7 | Regen-Partikel | Shader-Partikel (fallende Streifen + Wind-Drift) | Wetter-State |
| 8 | Pfützen-Bildung | waterDepth-Grid + Reflexions-Shader | waterDepth steigt bei Regen |
| 9 | Rinnen-Abfluss | Flow-Sim (Nachbar-Transfer) + UV-Scroll | Drain-Tiles aus TMX |
| 10 | Pfützen-Trocknung | waterDepth -= evaporation nach Regen-Stop | Wetter-State-Wechsel |
| 11 | Splash beim Durchlaufen | Partikel-Burst + Wellen-Ring | Spieler betritt Pfütze |
| 12 | Schnee-Akkumulation | snowGrid + weißer Overlay (alpha = depth) | Wetter-State |
| 13 | Schnee-Spuren | snowDepth[pos] = 0 beim Betreten, Zuschneien über Zeit | Spieler-Bewegung |
| 14 | Gras-Wegknicken | Vertex-Displacement weg vom Sprite-Zentrum | Spieler betritt Gras-Tile |
| 15 | Baum-Sway + Blätter | Horizontal-sin-Sway + Partikel-Blätter mit Wind-Vektor | Wind-Uniform |
| 16 | Hitzeflimmern | UV-Distortion per sin(y + time) | Nahe Ofen/Schmiede |
| 17 | Nebel/Dunst | Perlin-Noise × Alpha, driftet mit Wind | Outdoor/Sewer/Forest |
| 18 | Funken/Staub | Shader-Partikel (aufsteigend, orange/weiß) | Schmieden, Feuerstellen |

### Step 7d — Charakter-Emotionen & Closeups

| # | Effekt | Mechanismus | Gameplay-Anbindung |
|---|---|---|---|
| 19 | Augen-Reflexion | Additiver 2px Glint, leicht animiert | Closeup-Dialog / Cutscene |
| 20 | Erröten | Pink-Gradient im oberen Sprite-Drittel | Dialog-State (Kompliment/Peinlichkeit) |
| 21 | Wach werden | Letterbox-Open + Blur-Lift | Szenen-Übergang (Inn/Schlaf) |
| 22 | Kurzsichtigkeit | Radialer Blur (Mitte scharf, Rand verschwommen) | Ausrüstung ("Brille verloren") |
| 23 | Atmung | scaleY-Pulse (sin × 0.008) | Immer (Idle), stärker bei Rennen/Kampf |

---

## Technische Architektur

```
game/src/desktopMain/kotlin/game/shader/
├── ShaderEffects.kt       ← Manager + Time-Driver
├── PoisonFilter.kt        ← Chromatic Aberration + Vignette
├── BeerGoggleFilter.kt    ← Blur + Warmth + Sway
├── LightingFilter.kt      ← (Step 7b) Light-Map Multiply
├── WeatherFilter.kt       ← (Step 7c) Regen/Schnee/Pfützen
├── HeatShimmerFilter.kt   ← (Step 7c) UV-Distortion
├── FogFilter.kt           ← (Step 7c) Perlin-Noise Nebel
└── ...
```

Jeder Filter:
- Erbt `ShaderFilter()` (KorGE 6.0 API)
- Hat ein `UniformBlock` mit seinen Parametern
- Wird über `container.filter = MyFilter()` zugewiesen
- `time`-Uniform wird vom zentralen `ShaderEffects`-Manager getrieben
- Gameplay-State (poison, drunk, weather, etc.) steuert die Parameter

**Per-Layer-Differenzierung:**
Verschiedene Layer können verschiedene Shader haben:
- farLayer: Nebel + Hitzeflimmern (Atmosphäre)
- playLayer: Lighting + Schatten (Gameplay)
- nearLayer: roh/pixelig (Tiefenkontrast)
- HUD: kein Shader (immer crisp)

---

## Gameplay-Integration (Bark-System)

Das Bark-System reagiert auf Shader-States:

```
Regen aktiv + Spieler draußen      → Bark: "There's a hole in my boot."
drunkLevel > 0.5 + NPC-Interaktion → Bark: "You look like the back end of a horse."
                                      (mit BeerGoggle: NPC sieht trotzdem gut aus)
poisonLevel > 0.3                   → Bark: "I feel like the back end of a horse."
Schnee-Akkumulation > 0.7           → Bark: "I'm looking for a stable." (Wärme suchen)
Gewitter-Blitz                      → Bark: "It's a trap!" (Reflex-Bark)
```

Die Shader sind nicht nur visuell — sie sind **Bark-Trigger**. Die Welt erzählt sich selbst.

---

## Ziel-Auflösung

```
Virtual: 640×360 (fest, alle Gameplay-Logik + Shader arbeiten damit)
Upscale: Integer-Scale → 1440p (4×) oder 1080p (3×)
Render-Gerüst (Step 11): Hi-res gemalte Hintergründe (KI-generiert, 2560x1440 nativ) +
    Charakter-Layer durch DoodleLineFilter (Anime4K-inspirierter Linien-Shader, neu
    implementiert aus dem Konzept, kein Fremdcode). Ziel: 1440p-Ausgabe, Cartoon-Figuren
    vor scharfem gemaltem Hintergrund. Anime4K HQ A+B CNN-Kette ist als späterer
    Austausch der Shader-Kette vorgesehen (gleiches Gerüst, stärkerer Shader).
Mobile (Android): gleiche Pipeline, gleiche Shader, ES 3.2 kompatibel
```

---

## Abgrenzung zu existierenden Spielen

| Spiel | Ansatz | GAIME |
|---|---|---|
| Octopath Traveler | HD-2D: handgemalte Hintergründe + DoF + Bloom | **Prozedural**: gleiche Effekte entstehen aus Regeln, nicht aus Assets |
| The Last Night | Pixel-Art + handgemalte Beleuchtung pro Szene | **Systemisch**: Licht folgt aus Tile-Daten, nicht aus Art-Direction |
| Eastward | Schöne Pixel-Art, statische Effekte | **Dynamisch**: Regen sammelt sich, Schnee häuft sich, Pfützen trocknen |
| RE:Coded | Realtime-Upscale auf Pixel-Art | **Per-Layer-Shader**: nicht ein Upscaler für alles, sondern Stil pro Tiefenebene |

**GAIME hebt nicht das Bild zur Kunst — sondern die Darstellung.**
