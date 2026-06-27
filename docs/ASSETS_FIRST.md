# Asset-First — Ein ungewöhnlicher Entwicklungsweg

## Die Ausgangslage

GAIME entstand nicht dadurch, dass jemand ein Spiel designt und dann Assets gesucht hat.
Es entstand in der entgegengesetzten Reihenfolge.

Zu Beginn stand eine lizenzierte Asset-Bibliothek: HD-2D-Pixel-Art-Packs in Tiled-TMX-Format,
ein einzelner Sprecher (Voice Jam / Gruff, 213 MP3-Dateien), fertige Gegner-Sprites,
Portrait-Illustrationen. Die Frage war nicht "Was für ein Spiel wollen wir bauen?",
sondern: "Was können wir mit genau diesen Sachen bauen?"

Das ist ungewöhnlich. Es ist aber nicht zufällig.

---

## Die Inventur als Game-Design-Methode

Statt einem Game-Design-Document am Anfang stand eine Bestandsaufnahme:

| Asset-Typ | Inhalt | Design-Konsequenz |
|---|---|---|
| Charakter-Sprites | Barkeep, Guard, Merchant, Citizens, Rogue, Warrior, Mage | Das Spiel hat genau diese NPC-Typen und genau drei Helden |
| Locations (TMX) | Taverne, Keller, Markt, Guild Hall, Kapelle, Tempel, Brücke | Das ist Stokeport — die Stadt entstand aus den Karten |
| Barks (Voice Jam) | 213 kurze Sätze, britisch, ernst, würdevoll | Die Ton-Sprache des Spiels: trocken, britisch, bürokratisch |
| Gegner-Sprites | Ratten, Blobs, Wölfe, Goblins | Kapitel 1 (Keller), Kapitel 2 (Wald), Kapitel 3 (Dungeon) |

Das Genre, der Ton, die Locations, die Charaktere — alles war bereits im Asset-Pack kodiert.
Die Arbeit bestand darin, es zu *lesen*, nicht es zu *erfinden*.

---

## Was dabei entstand

### Der Questbook-Mechanismus

Das zentrale Problem beim Assets-First-Ansatz: NPCs reden in disconnected Barks —
kurze Einzelsätze aus einem Voice Pack, ohne dramatischen Zusammenhang.
Warum sprechen Charaktere so? Das brauchte eine Erklärung, die im *Spieldesign* selbst sitzt,
nicht in der Metaebene.

Die Antwort: Das Questbook ist kaputt. Es hört keine vollständigen Sätze.
Es reagiert auf Stichworte. Es zwingt seine eigene bürokratische Interpretation auf.

Das Questbook als Mechanik ist damit keine erfundene Idee — es ist eine direkte Übersetzung
der Produktionsbedingung in die Spielwelt. Ein System, das Material vorfindet und daraus
Bedeutung ableitet, die das Material selbst nicht hatte. Das ist exakt das,
was Asset-First-Entwicklung tut.

### Der Humor

Die komödiantische Stimme entstand nicht durch Comedy-Writing. Sie entstand durch Kontext-Kollision.

Eine würdevolle Gruff-Stimme sagt ernsthaft: "Spend some coin or get out." —
das ist ein Charakter. Dieselbe Stimme sagt: "You've gotta try this roast cockatrice." —
das ist ein Widerspruch. Der Widerspruch ist der Witz.

Kein Charakter im Spiel versucht, lustig zu sein. Der Humor entsteht ausschließlich durch
das Aufeinandertreffen von ernsthafter Bürokratie und absurdem Kontext.
Das war nicht geplant. Es ist aus dem Stimm-Material entstanden.

### Die Locations

Die Brücken-Karte im Asset-Pack war 1360×1104 Pixel — eine unerwartete, fast absurd
große Tile-Map für eine Brücke. Das wurde keine Entscheidung, das kleinzumachen.
Es wurde zur Spielerfahrung: die überraschende Weite eines Overworld-Übergangs.

Die Kapellen-Karte hatte eine bestimmte Atmosphäre (dunkle Steine, schwaches Licht),
die direkt in den Rendering-Post-Process einfloss:

```kotlin
CHAPEL = SceneAtmosphere(
    gradeStrength = 0.28f, vignette = 0.85f, fog = Color(0xFF140C28), ...
)
```

Die Welt hat eine konsistente Ästhetik, weil sie aus einem konsistenten Asset-Pack kommt.
Das ist gratis — kein Art Director nötig.

---

## Die Stimmen: ein Sprecher, sieben Charaktere

Das größte Einzelproblem von Assets-First bei Audio: ein einzelner Sprecher klingt
nach einem einzelnen Sprecher. Sieben Charaktere müssen sich unterscheiden.

Die Lösung: WAV-Header-Pitching. Kein Pitch-Shifting-DSP, kein Nachbearbeiten.
Das dekodierte Audio wird mit unveränderter Samplerate in eine WAV-Datei geschrieben,
aber der Header-Eintrag für die Samplerate wird manipuliert:

```python
# header_rate niedriger als Decode-Rate → Wiedergabe langsamer → tieferer Ton
header_rate = int(round(DECODE_RATE * pitch_factor))
wf.setframerate(header_rate)    # Pitch durch Header-Manipulation
wf.writeframes(bytes(decoded.samples))  # Samples bleiben unverändert
```

| Charakter | Faktor | Wirkung |
|---|---|---|
| Barkeep | 0.88× | −2.3 Halbtöne — tiefer, rauer, seasoned |
| Guard | 1.05× | +0.8 Halbtöne — scharf, militärisch |
| Merchant | 1.12× | +1.9 Halbtöne — hoch, quirlig, verkäuferisch |
| Guildmaster | 0.82× | −3.4 Halbtöne — sehr tief, befehlend |
| Citizen | 1.08× | +1.3 Halbtöne — leichter, nervös |
| Priest | 0.95× | −0.9 Halbtöne — würdevoll, ruhig |
| Mage | 1.15× | +2.4 Halbtöne — exzentrisch, hoch |

Dieselbe Aufnahme. Sieben Charaktere. Null zusätzliche Produktionskosten.

Das ist Asset-First konsequent zu Ende gedacht: den Ursprung eines Assets
ausreizen, bevor ein weiteres Asset beschafft wird.

---

## Die technische Pipeline

```
Asset-Pack (TMX + PNG-Tilesheets)
        │
        ▼
scripts/tmx_render.py       ← Python, Pillow, pytmx
        │  rendert alle Karten zu PNG-Backgrounds
        ▼
composeResources/drawable/world_*.png
        │
        ▼
SliceScreen.kt              ← Compose Multiplatform
        │  imageResource(Res.drawable.world_chapel_ext)
        │  WorldScene(world, tileset, playerSprite, background)
        │  + SceneAtmosphere (Lighting/Fog/Vignette/Motes)
        ▼
Laufendes Spiel
```

Zwischen "Asset existiert im Pack" und "Location erscheint im Spiel" liegen drei Schritte.
Kein Level-Editor, kein Scene-Export, kein Art-Pipeline-Ticket.

---

## Was dieser Ansatz lehrt

**Art Debt ist real.**
Ein Spiel designen, das man nie angemessen illustrieren kann, ist die häufigste Indie-Falle.
Assets-First eliminiert Art Debt vollständig: Was existiert, existiert.
Was nicht existiert, wird kein Feature.

**Constraints erzwingen Entscheidungen.**
Mit unbegrenzt Zeit und Budget entstehen Spiele nie. Mit einem konkreten Sprecher,
konkreten Karten und konkreten Charakteren entsteht zwangsläufig etwas.

**Inventur ist eine kreative Technik.**
Das ist nicht die Methode für jedes Spiel. Aber es ist die Methode, die verhindert,
dass aus einem Spielkonzept ein Konzept für ein Spiel bleibt.

---

## Phasen-Chronologie

| Phase | Inhalt |
|---|---|
| Phase 1 | UI-Grundstruktur, Portrait-Grafiken, erstes Dialogue-System |
| Phase 2 | HD-2D-Backgrounds (6 Locations), NPC-Sprites, Dungeon-Atlas |
| Phase 3 | Tiled-Renderer, 6 neue Spielwelt-Locations, Asset-Manifest, Atmosphären |
| Phase 3.5 | NPC-Stimmen (Pitch-System, 31 WAV-Dateien, 5 Charaktere) |
| Phase 3.6 | Erweitertes Story-Skript (23 Szenen, Priest + Mage Stimmen) |

Jede Phase ist direkt auf vorhandene Assets angewiesen — nicht auf neues Material.

---

*Dieses Dokument beschreibt den Entwicklungsansatz so wie er war, nicht wie er geplant war.
Er war nicht geplant. Das ist der Punkt.*
