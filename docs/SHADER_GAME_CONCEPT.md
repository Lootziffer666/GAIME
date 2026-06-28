# Shader-First Game Concept — "Was du siehst = was gilt"

**Status:** Konzeptnotiz, 2026-06-28. Kein aktives Projekt — Ideenspeicher für das
Folgeprojekt nach GAIME. Die hier dokumentierten Erkenntnisse stammen aus der
GAIME-Shader-Pipeline-Entwicklung (Steps 7a–7d).

---

## Kernprinzip

> **Es gibt keinen Game-State den der Spieler nicht sieht. Die Shader SIND der State.**

Traditionelle Spiele: Code-Logik berechnet State → Shader visualisieren ihn als Feedback.
Shader-First: **Die visuelle Darstellung IST die Mechanik.** Was der Shader zeigt, gilt.
Kein versteckter Bool, kein unsichtbarer Timer. Der Spieler liest die Spielregeln vom
Bildschirm ab — weil der Bildschirm die Spielregeln IST.

---

## Bewiesene Technologie (aus GAIME)

- KorGE 6.0 + Custom `ShaderFilter` pro Layer (per-Container, per-View)
- `UniformBlock` mit Float-Parametern, getrieben von Gameplay-State
- `time`-Uniform für prozedurale Animation
- Per-Tile State-Grids (snowGrid, waterDepth, mossLevel, rustLevel...)
- Mesa EGL headless rendering für CI/QA-Verifikation
- Android (Adreno ES 3.2) + Desktop (GL 4.5) gleiche Pipeline

---

## Kernmechaniken

### 1. Nässe als echte Mechanik

```
Regen aktiv → Spieler-Sprite erhält "Nass-Shader" (dunklere Farben, Wassertropfen-Partikel)
SOLANGE nass:
  - Bewegungsgeschwindigkeit -20%
  - Griffigkeit -30% (Klettern scheitert häufiger)
  - Feuer-Resistenz +50% (Wasser schützt)
  - Eis-Verwundbarkeit +100% (Einfrieren schneller)
  - Schleich-Vorteil (nasse Füße = leise auf Stein? Oder: tropft = hinterlässt Spur = Nachteil)

Trocknung:
  - Feuer-Radius: der Lichtkreis des Feuers IST der Trocknungsbereich.
    Du SIEHST wo du stehen musst. Shader-Intensität des Nass-Effekts sinkt
    proportional zur Nähe zur Lichtquelle.
  - Natürlich: langsam, draußen bei Sonne schneller als in Höhle.
  - Gameplay-Entscheidung: Bleibst du nass (Feuer-Resistenz) oder trocknest du
    (Beweglichkeit)? Der Spieler SIEHT den Trade-off am Charakter.
```

### 2. Zwielicht & Dunkelheit als Sichtbarkeits-Mechanik

```
Tag/Nacht-Zyklus → Lichtstärke sinkt → Zwielicht-Shader:
  - Entfernte Tiles: Kontrast sinkt, Farben entsättigen, Details verschwinden
  - Gegner IM Zwielicht: ihre Sprites werden vom Shader abgedunkelt bis unsichtbar
    → NICHT despawned, NICHT "hidden flag" → visuell NICHT SICHTBAR
  - Spieler im Zwielicht: EBENFALLS schwerer sichtbar für Gegner

Lichtquellen (Fackel, Feuer, Blitz):
  - Enthüllen den Zwielicht-Bereich kurzzeitig
  - Spieler mit Fackel: sieht mehr, IST ABER AUCH sichtbar (Trade-off!)
  - Blitz: 2 Frames lang ALLES sichtbar → Schock-Moment (alle Gegner sichtbar)

Gameplay-Konsequenz:
  - Nachts reisen = gefährlich (du siehst nichts) ABER Schleich-Vorteil
  - Fackel = sicher (du siehst) ABER Gegner sehen dich auch
  - Mondphasen: Vollmond = mehr Grundlicht, Neumond = pechschwarz
```

### 3. Natur erobert Ruinen zurück (akkumulativer Zerfall)

```
Unbesuchte Gebiete: pro Spieltag wachsen Ranken/Moos
  - mossLevel[tile] += 0.001/Tag (unmerklich pro Besuch, massiv nach Wochen)
  - vineLevel[tile] += 0.0005/Tag (Ranken langsamer aber blockierender)
  
Gameplay-Stufung:
  mossLevel < 0.3:  Kosmetisch (grüner Shader-Tint auf Stein)
  mossLevel 0.3-0.6: Rutschig (Bewegung verlangsamt, Klettern erschwert)
  mossLevel 0.6-0.9: Strukturell (Wände brüchig, neue Durchgänge?)
  vineLevel > 0.7:  BLOCKIERT (Ranken verschließen Durchgang physisch)

Der Spieler SIEHT die Entwicklung:
  - Frischer Dungeon: graue Steinwände
  - 10 Spielstunden später: grüner Schimmer an den Rändern
  - 30 Stunden: Ranken bedecken Türrahmen
  - 50 Stunden: Durchgang zugewachsen → neuen Weg finden oder Ranken verbrennen

Feuer/Schwert als Gegenmaßnahme:
  - Ranken verbrennen (Feuer-Bark?) → vineLevel = 0 für diese Tiles
  - Aber: Feuer in bemooster Ruine = Brandgefahr (Feuer breitet sich aus!)
```

### 4. Feuer als physikalische Kraft

```
Feuer ist nicht "Fire Damage = 10 HP":
  Feuer ist ein SHADER-BEREICH mit Eigenschaften:
  - Lichtradius (beleuchtet Zwielicht)
  - Wärmeradius (trocknet nasse Spieler, schmilzt Eis-Barrieren)
  - Schadensradius (innerster Ring: verbrennt)
  - Ausbreitungs-Logik: Feuer springt auf brennbare Tiles (Holz, Ranken, Stoff)

Materialien verglühen:
  - Holztüren: burnLevel steigt → Tür wird dunkler → löst sich auf (Dissolve)
    → Durchgang frei! (Feuer als Schlüssel)
  - Ranken: sofort verbrannt (vineLevel → 0, Feuer-Partikel)
  - Stein: immun (aber: mossLevel sinkt = Stein wird sauber)
  - Metall: glüht (rotTint steigt), wird formbar? Schmieden?

Der Spieler SIEHT:
  - Feuer als dynamischen Lichtkreis
  - Materialien die sich verfärben (dunkler, orange, schwarz)
  - Dissolve-Shader der sie auflöst
  - Keine "press E to burn door" — du LEGST Feuer und WARTEST ob es die Tür erreicht
```

### 5. Vertigo/Perspektive als Narrations-Tool

```
Dolly-Zoom (Vertigo-Shader):
  - Zentrum bleibt fix, Ränder stauchen sich
  - Trigger: Story-Moment (Erkenntnis, Verrat, Boss-Reveal)
  - NICHT als Cutscene — als SPIELMOMENT (du kannst dich noch bewegen, aber die Welt verzerrt sich um dich)

Mögliche Gameplay-Integration:
  - Höhenangst-Charakter: am Klippenrand → Vertigo-Shader → Input invertiert sich leicht
  - Wahnsinn/Corruption: je höher der Wert, desto stärker die permanente Verzerrung
  - "Die Wahrheit sehen": Vertigo = die Welt reißt auf und zeigt die wahre Form dahinter
```

### 6. Temperatur als sichtbares System

```
Kalt (Schnee/Nacht):
  - Charakter: Atem-Dampf-Partikel (kleiner weißer Puff, rhythmisch)
  - Sprite: leicht bläulicher Tint, "Zittern" (minimaler Shake)
  - Gameplay: Ausdauer sinkt schneller, Feuer wird überlebensnotwendig
  
Heiß (Schmiede/Wüste/Feuer-nah):
  - Hitzeflimmern-Shader auf dem Hintergrund
  - Charakter: rötlicher Tint, "Schweiß" (kleine Partikel-Tropfen)
  - Gameplay: Ausdauer sinkt, Metall-Rüstung schadet (erhitzt sich)

Der Spieler SIEHT die Temperatur am Charakter — kein HUD-Element nötig.
```

---

## Designphilosophie: "Show, don't tell, don't code"

Traditionell:
```
Code: player.isHidden = true
UI: "HIDDEN" icon appears
Visual: sprite becomes semi-transparent
```

Shader-First:
```
Shader: Zwielicht-Dunkelheit macht Sprite visuell unsichtbar
Gameplay: Gegner-KI "sieht" nur was im Lichtradius ist (Raycast gegen Lightmap)
Spieler: sieht SICH SELBST verschwinden → versteht die Mechanik ohne UI-Element
```

**Keine HUD-Elemente für Zustände.** Der Spieler LIEST die Zustände am Bildschirm:
- Nass? Siehst du. Tropfen + dunkler Sprite.
- Kalt? Siehst du. Dampfwolke + bläulich.
- Versteckt? Siehst du. Du wirst selbst dunkel.
- Ruine verfällt? Siehst du. Monat für Monat grüner.
- Feuer breitet sich aus? Siehst du. Orange Lichtkreis wächst.

---

## Arkane Mechanik-Ideen (Brainstorm, unvalidiert)

- **Echo-Vision:** Ein Shader der zeigt wo andere Spieler VOR dir waren (Ghost-Trail,
  verblasst über Zeit). Asynchroner Multiplayer ohne Netzwerk.
- **Erinnerungs-Shader:** Gebiete die du schon besucht hast werden in warmeren Farben
  dargestellt. Erstbesuch = kalt/grau. Wiederbesuch = vertraut/warm.
- **Resonanz-Puzzles:** Bestimmte Shader-Frequenzen (Wellen, Pulse) müssen "synchronisiert"
  werden um Türen zu öffnen. Visuelles Rhythmus-Puzzle.
- **Parallelen-Verschiebung:** Zwei Shader-Ebenen (Realität + Geisterwelt) überlagern sich.
  Spieler kann "fokussieren" (Blur-Shift) zwischen beiden. Was in einer Ebene Wand ist,
  ist in der anderen Durchgang.
- **Alterung als Mechanik:** Spieler hat begrenzte "Lebenszeit" (300 Spieltage).
  Falten, graue Haare, langsamere Bewegung — alles per Shader sichtbar.
  Tod = natürlicher Zerfall (Dissolve). Spielziel: bevor du stirbst, hinterlasse etwas.
- **Emotional-Shader:** NPCs die dich mögen = wärmere Farbgebung in ihrer Nähe.
  NPCs die dich hassen = kältere. Du "fühlst" die Beziehung ohne Dialog.

---

## Verbindung zu GAIME

GAIME beweist jede technische Voraussetzung:
- Per-Container-ShaderFilter ✓
- Parametrische Uniforms ✓
- Time-driven Animation ✓
- Per-Tile State-Grids ✓
- Gameplay→Shader-Parameter ✓
- Multi-Layer Differenzierung ✓
- Headless Rendering für QA ✓

Das Folgeprojekt startet mit der GAIME-Engine und dreht den Designregler:
**Shader sind nicht mehr Effekte auf dem Spiel. Sie sind das Spiel.**

---

## Nächste Schritte (wenn das Projekt startet)

1. Einen einzigen Prototyp-Level mit EINER Mechanik (z.B. Zwielicht+Feuer+Sichtbarkeit)
2. Spieltest: Versteht der Spieler die Regel ohne Tutorial, nur durch Beobachtung?
3. Wenn ja: die Mechanik skalieren (weitere Shader-Regeln stapeln)
4. Wenn nein: den Shader deutlicher machen (Intensität, Kontrast, Partikel)
5. Design-Dokument für ein vollständiges Spiel erst NACH dem Prototyp-Beweis
