# Die Ontologie des Sichtbaren

## Eine Designbibel für Shader-First-Systemarchitekturen

**Status:** Theoretische Fundierung. Kreatives Referenzdokument.

**Quelle:** Copywriter, 2026-06-28.

---

## Das fundamentale Paradigma

In der traditionellen Spieleentwicklung herrscht eine strikte Trennung zwischen
der logischen Zustandsebene eines Spiels und seiner visuellen Präsentation. Die
Spiellogik berechnet im Hintergrund verborgene Variablen, Booleans und
Statuswerte, während Grafikprozessoren und Shader diese Daten lediglich
kosmetisch interpretieren, um Feedback an den Spieler zu senden. Dieses
weitverbreitete Muster führt im modernen Spieldesign jedoch häufig zu einer
spürbaren ludonarrativen Dissonanz: Die Spielwelt behauptet visuell eine
bestimmte Beschaffenheit, während die physikalischen Interaktionsgrenzen starr
und entkoppelt bleiben.

Das innovative **Shader-First-Paradigma** bricht radikal mit dieser Konvention.
Die leitende Prämisse lautet:

> **Es gibt keinen Spielzustand, den der Spieler nicht sieht — die Shader selbst
> bilden den Zustand ab.**

Was auf dem Bildschirm visuell gerendert wird, definiert im selben Moment die
physikalischen, mechanischen und ontologischen Regeln der Spielwelt. Wenn ein
Pfützen-Filter eine Oberfläche nass und rutschig zeichnet, existiert im
Spielcode kein separates Zustandssignal — die physikalische Reibung wird direkt
aus den Parametern des Oberflächen-Shaders abgeleitet.

Durch diese technologische Verschmelzung entsteht eine **absolute ontologische
Kongruenz**. Der Spieler liest die Regeln und Gefahren der Umgebung nicht länger
über künstliche Benutzeroberflächen ab, sondern rein intuitiv über die visuelle
Materialität der Welt.

> Zeigen, nicht codieren. Zeigen, nicht erklären.

---

## Vergleich: Traditionell vs. Shader-First

| Kriterium | Traditioneller Ansatz | Shader-First-Paradigma |
|---|---|---|
| **Zustandsverwaltung** | CPU-Speicher, unsichtbare Zustandsvariablen | Framebuffer, Render-Targets, GPU-Zustandsgitter |
| **Kausale Richtung** | Spiellogik berechnet Zustand → Shader visualisiert kosmetisch | Shader definiert Zustand → Physik leitet Regeln direkt ab |
| **Grafische Benutzeroberfläche** | HUD-Elemente, numerische Anzeigen, Statussymbole | Organische Materialänderung, Farbwechsel, Partikeleffekte |
| **Interaktionsraum** | Statische Kollisionsboxen und feste Trigger-Zonen | Dynamisch berechnete Materialgrenzen basierend auf aktiven Filtern |

### Beispiel: Tarnzustand

**Traditionell:**
- Unsichtbares Flag definiert den Tarnzustand
- Interface blendet ein Statussymbol ein
- Charakter-Sprite wird teiltransparent gezeichnet
- Drei getrennte Systeme, die synchron gehalten werden müssen

**Shader-First:**
- Der Dämmerungs-Shader blendet den Spieler-Sprite visuell aus
- Gegner-KI berechnet Entdeckungschancen via Raycast gegen die Lichtkarte
- Der Spieler erfasst seinen Tarnungsgrad organisch durch Beobachtung des
  eigenen Verschmelzens mit der Dunkelheit
- **Ein** System, das Darstellung und Mechanik gleichzeitig ist

---

## Wissenschaftliche Fundierung

### Kognitionswissenschaftliche Grundlage

Der Ansatz stützt sich auf die Arbeiten des Gestaltpsychologen **Rudolf Arnheim**,
insbesondere auf seine Werke zur visuellen Wahrnehmung und zur Dynamik visueller
Zentren. Arnheim wies nach, dass visuelle Wahrnehmung kein passiver
Registriervorgang ist, sondern ein **aktiver, interpretativer Denkprozess**, bei
dem das Gehirn visuelle Kräfte, Richtungen und Gewichte in einer Szene balanciert.

Der Mensch nimmt die Welt nicht als unbeschriebene Geometrie wahr, sondern legt
einen subjektiven Schleier aus gelernten Mustern und Erwartungen darüber.

In der Wahrnehmungspsychologie wird diese Dualität als eine konstante
Überlagerung von realer und perzeptiver Welt auf einer einzigen Konstruktion
verstanden. Shader-First-Systeme machen sich diese kognitive Eigenschaft zunutze,
indem sie die Grenze zwischen dem physischen Objekt und der perzeptiven
Interpretation im Spielcode kollabieren lassen.

### Aufmerksamkeitssteuerung und visuelle Suche

Im Bereich der visuellen Aufmerksamkeit unterscheidet die Forschung zwischen:
- **Bottom-up-Aufmerksamkeit:** Automatische, reaktive Prozesse — getriggert
  durch physikalische Reize (Farbe, Helligkeit, Bewegung, Kontraste)
- **Top-down-Aufmerksamkeit:** Kontrollierte, erwartungsgesteuerte Prozesse —
  entspringen dem Wissen und den Absichten des Betrachters

Das von Jeremy Wolfe beschriebene **Guided Search Model** zeigt, dass effiziente
visuelle Suchen durch eine Kombination beider Systeme gesteuert werden:
1. Grundlegende Merkmale werden in einer **präattentiven Phase** vorsortiert
2. In einer **fokussierten Phase** werden sie integriert

```
                      +-----------------------+
                      | Visueller Reiz        |
                      +-----------+-----------+
                                  |
                                  v
                      +-----------------------+
                      | Präattentive Phase    |
                      | (Bottom-Up-Filterung) |
                      +-----------+-----------+
                                  |
                                  v
                      +-----------------------+
                      | Fokussierte Phase     |
                      | (Top-Down-Integration)|
                      +-----------+-----------+
                                  |
                    +-------------+-------------+
                    |                           |
                    v                           v
     +-----------------------------+   +-----------------------------+
     | Erkenntnis kollabiert       |   | Peripherie verbleibt        |
     | (Objekt wird physisch)      |   | (Zustand unbestimmt)        |
     +-----------------------------+   +-----------------------------+
```

Shader-First-Systeme nutzen diese Phasen, um Gameplay-Zustände zu codieren. Da
Materialien, Kontraste und Lichtverhältnisse in Echtzeit manipuliert werden, kann
die visuelle Aufmerksamkeitssteuerung des Spielers präzise gelenkt werden — die
kognitive Belastung sinkt, die Orientierung verbessert sich.

---

## Implikationen für drei Genres

### Komödiantisches RPG (GAIME: "Quest Accepted: Unfortunately!")

Die Shader sind **Naturgesetze mit Persönlichkeit**:
- Der Nass-Shader hält Feuchtigkeit für Sicherheit
- Der Feuer-Shader betrachtet Brennbares als Einladung
- Der Moos-Shader hält unbewirtschaftete Architektur für Lebensraum
- Die Bark-Pipeline reagiert auf Shader-States ("There's a hole in my boot.")

Ontologische Kongruenz hier = **Comedy**: Die Welt verhält sich konsistent absurd,
und der Spieler liest die Absurdität direkt am Bildschirm.

### Psychologischer Horror ("Das Haus hat dich falsch erkannt")

Die Shader sind **feindliche Interpretationsgewalten**:
- Was sichtbar wird, darf nicht immer gewusst werden
- Erkenntnis ist toxisch — Beobachtung ist Mitschuld
- Die Sichtbarkeit jagt dich

Ontologische Kongruenz hier = **Grauen**: Die Wahrnehmung des Spielers wird zur
Waffe gegen ihn selbst. Das Monster entsteht im Moment des Erkennens.

### Kinderspiel ("Klecks & die Welt, die sich verguckt hat")

Die Shader sind **Spielgefährten mit Macken**:
- Matsch macht aus Fehlern Spielzeug
- Flausch macht die Welt sicherer, aber ungenauer
- Mut ist sichtbar, nicht numerisch

Ontologische Kongruenz hier = **Intuition**: Kinder lesen die Spielregeln
direkt am Material ab, ohne Tutorial, ohne HUD, ohne Abstraktion.

---

## Der Leitsatz

> **Was der Shader zeigt, gilt. Punkt.**

Drei Spiele. Eine Engine. Eine Philosophie. Drei emotionale Extreme.

---

## Orientierung und räumliche Kognition

Für die Gestaltung dynamischer Spielarchitekturen liefert die Umweltpsychologie
wertvolle Taxonomien. **Kevin Lynch** definiert in seinen Analysen des Stadtbildes
fünf grundlegende Elemente, über die Menschen Räume strukturieren: **Wege**
(Paths), **Grenzlinien** (Edges), **Bereiche** (Districts), **Knotenpunkte**
(Nodes) und **Wahrzeichen** (Landmarks).

Gleichzeitig beschreibt **Francis D.K. Ching**, wie architektonische
Ordnungsprinzipien räumliche Beziehungen und Organisationen bestimmen. In
Kombination mit **Barbara Tverskys** Theorie der räumlichen Kognition, die zeigt,
dass Menschen Umgebungen als kognitive Collagen aus Fragmenten und subjektiven
Verknüpfungen speichern, wird deutlich, dass eine Spielwelt kein starrer Kasten
sein muss.

Indem ein Shader-First-System die Kontraste, Farben und geometrischen Übergänge
dieser strukturellen Elemente dynamisch verschiebt, verändert sich nicht nur die
ästhetische Wahrnehmung der Architektur, sondern auch das **räumliche
Navigationsverhalten** und die **kognitive Karte** des Spielers.

---

## Neurobiologie des Audiosystems und visuelle Validierung

Die enge Verknüpfung von Grafik und Ton basiert auf tief verwurzelten
neurobiologischen Mechanismen. Studien zur Neurowissenschaft des Audiodesigns
zeigen, dass das menschliche Gehirn visuelle und auditive Reize nicht isoliert
verarbeitet, sondern in einem ständigen Abgleich synchronisiert.

Asynchroner oder qualitativ minderwertiger Ton führt unmittelbar dazu, dass das
visuelle Gesamtergebnis vom Gehirn als fehlerhaft, unrealistisch oder gar
abstoßend bewertet wird.

Zusätzlich lassen sich evolutionäre Überlebensmechanismen wie der
**Schreckreflex** (Startle Circuit) gezielt über audioreaktive Frequenzbänder
ansteuern. Tiefe Frequenzen rufen unbewusste Bedrohungsszenarien hervor, während
die synchrone Kopplung von visuellen Flimmer-Effekten und auditiven Rhythmen die
Immersion dramatisch verstärkt.

---

## Abgrenzung von bestehenden Branchen-Benchmarks

### Alan Wake 2 (2023)

Demonstriert eindrucksvoll modernste Grafiktechnologie für psychologischen Horror.
Über mechanische Räume ("Mind Place", "Writer's Room") manipuliert der Spieler
Plot-Details auf einer virtuellen Pinnwand → direkte Veränderung der Geometrie.

**Abgrenzung:** Basiert auf klassischen, geskripteten Zustandswechseln. Ein
hochentwickeltes Menüsystem, das Geometrie-Instanzen austauscht — kein
kontinuierliches, shader-getriebenes Zustandssystem.

### Superliminal (2019)

Nutzt erzwungene Perspektive als primäre Gameplay-Mechanik. Objekte verändern
ihre physische Größe basierend auf der Projektion aus der Spielersicht.

**Abgrenzung:** Beschränkt sich auf Skalierung starrer Kollisionskörper. Eine
shader-basierte Transformation von Materialeigenschaften oder NPC-Verhaltensweisen
findet nicht statt.

### Viewfinder (2023)

2D-Fotografien überschreiben im 3D-Raum die bestehende Geometrie physisch
(Tiefendaten via Raycast + dynamische Mesherzeugung).

**Abgrenzung:** Resultat bleibt statisch — projiziertes Bild erzeugt starre
Kollisionsgeometrie, interagiert nicht dynamisch mit Nässe, Temperatur oder
sozialen NPC-Strukturen.

---

## Systemischer Anwendungsfall: Die Bäckerei mit Regierungsambitionen

Durch eine Überladung des Krümel-Shaders hat die örtliche Dorfbäckerei beschlossen,
sich zur wichtigsten politischen Institution auszurufen:
- Ameisen tragen offizielle Quittungen
- Enten belagern den Eingang im bürokratischen Gänsemarsch
- Der Bäcker verkauft Brot nur noch mit Passierschein
- Der Bürgermeister steckt im Hefeteig (gerendert als Rosinenbrötchen)

**Lösung via Shader-Wechselwirkung:**
1. **Matsch-Shader** auf Hefeteig → wird flüssig, Bürgermeister rutscht raus
2. **Glitzer-Shader** auf Brötchen → wird zum "Staatsbrot", Ameisen wenden sich ab
3. **Flausch-Shader** auf wütende Enten → werden zu friedlichen Sofakissen
4. **Schattenkuschel-Shader** dimmt Ofenlicht → Nachtmäuse finden den Hauptschalter
5. **Mut-Shader** stabilisiert den Bäcker → akzeptiert, dass Schlangen keine
   Staatsgründungen sind

**Belohnung:** Der Zimtwirbel-Shader (rotiert Wegführungen).

---

## KLECKSWELT: Zustandsschichtung (State Layering)

Die physikalischen Zustände schichten sich additiv über mathematische
Shader-Schnittstellen. Die resultierende Materialphysik berechnet sich dynamisch:

```
Zustand_Eis    = Nass-Shader ∩ Kalt-Shader    → extrem rutschige, fragile Gleitfläche
Zustand_Matsch = Nass-Shader ∩ Dreck-Shader   → formbare Masse, schwere Objekte sinken
Zustand_Schlaf = Flausch-Shader ∩ Dunkel-Shader → NPCs in Reichweite schlafen ein
```

### Physikalisches Werkzeugset

| Werkzeug | Shader-Parameter-Effekt |
|---|---|
| Gießkanne | Erhöht lokalen Nässewert im Shader-Gitter |
| Fächer | Trocknet / bläst Staub |
| Laterne | Dimmt/verstärkt Schattenkuschel-Kontrast |
| Kreide | Zeichnet Barrieren (Shader interpretiert als unüberwindbar für NPCs) |
| Decke | Dämpft Geräusche + lokaler Flausch |
| Bürste | Entfernt Moos/Staub |
| Lupe | Vergrößert Details (Risiko: macht Dinge "zu wichtig" = Glitzer) |
| Thermoskanne | Transportiert Hitze/Kälte |
| Klebeband | Fixiert lose Physik / blockiert rotierende Effekte |
| Topfdeckel | Macht Schallwellen farblich sichtbar |

---

## Horror-Variante: Die zehn Kernsysteme

| System | Name | Kern-Mechanik |
|---|---|---|
| 1 | Erkenntnis-Shader | E(t) = ∫(Fokus × Kontrast × cos(θ)) dt — Eindeutigkeit manifestiert |
| 2 | Peripherie-Horror | Feinde existieren nur im Augenwinkel |
| 3 | Pareidolie-Shader | Strukturen formen sich zu starrenden Halb-Gesichtern |
| 4 | Scham-Horror | Feigheit hinterlässt sichtbare Nachwirkung |
| 5 | Trauer-Shader | Verlust stört Render-Latenz (Farben verzögert, Schatten hängen) |
| 6 | Unschärfe als Raubtier | Blur verschluckt Kategorien, normalisiert Fehler |
| 7 | Kompression als Körperhorror | Blockartefakte werden zu wegrationierten Menschen |
| 8 | Denoiser-Kult | Obsessive Rauschentfernung → Identitäts-Auslöschung |
| 9 | Speicher-Infektion | Saves frieren Fehlinterpretationen ein |
| 10 | Architektur-Shader | Räume passen sich der Erwartung des Spielers an |

---

## Mechanischer Kern: Kinderspiel vs. Horror

| Dimension | Klecks (Kinderspiel) | Das Haus (Horror) |
|---|---|---|
| **Wahrnehmungseffekt** | Matsch + Flausch fangen Fehler ab, bieten Schutz | Unschärfe + Kompression verschmelzen Kategorien, löschen Identität |
| **Triebkraft** | Fehlerfreundlichkeit, Neugierde, Nonsens | Visuelle Vorsicht, Blickvermeidung, paranoider Zweifel |
| **Schatten/Dunkelheit** | Schattenkuscheln schützt scheue Wesen | Peripherie-Horror engt Bewegungsraum ein |
| **Zustandsspeicherung** | Filterfreundschaften + Entdeckungsstufen | Speichertransgression (visuelle Traumata ins Savegame) |

---

## Die Shader-Filter-Tabelle (Kinderspiel)

| Filter-ID | Name | Visuelle Repräsentation | Primärer Physik-Parameter | Gameplay-Auswirkung | Trade-off |
|---|---|---|---|---|---|
| S_M01 | Matsch-Shader | Weiche, fließende Erdtöne; dicke Pinselstriche | Reibung → 0.1; Oberflächenverformung | Abhänge → Rutschen; konserviert Trails | Bewegung wird zäh; Schuhe schwerer |
| S_K02 | Krümel-Shader | Goldgelbe Partikelströme; glühende Pfade | Attraktivität für Tier-KI; Wegfindung | Ameisen-Brücken; Enten folgen | Zu viele Enten versperren Kamera |
| S_F03 | Flausch-Shader | Weichgezeichnete Kanten; wolkenartige Texturen | Elastizität → minimal; Kantenrundung | Bouncing; eliminiert Stürze | Mechanische Präzision verloren |
| S_G04 | Glitzer-Shader | Prismatische Reflexionen; Übersättigung | NPC-Prioritätswert → maximal | Plunder → Questobjekt | Händler verlangen Wucherpreise |
| S_S05 | Schattenkuschel | Kühle Violett-Töne; gedimmtes Licht | Sichtbarkeitsgrenze für scheue Entitäten | Verborgene Pfade + scheue Helfer | Taschenlampen-Reichweite sinkt |
| S_C06 | Mut-Shader | Scharfe Konturen; hohe Farbbrillanz | Kollisionsstabilität schwankender Plattformen | Stabilisiert Weg; verkürzt Sprungweiten | Keine (reine Belohnung) |

---

## Technische Implementierung und audioreaktive Echtzeit-Kopplung

### Das GAIME-Technologiefundament

Die Rendering-Architektur basiert auf einer optimierten Kopplung von
CPU-Zustandsgittern und GPU-Schnittstellen:

- **Engine-Framework:** KorGE 6.0 mit dedizierten ShaderFilter-Instanzen auf
  Container- und View-Ebene
- **CPU-GPU-Schnittstelle:** Dynamische UniformBlock-Strukturen, die
  Echtzeit-Gameplay-Variablen als Float-Parameter an die GPU übergeben
- **Zustandsgitter:** 2D Per-Tile-Statusgitter (snowGrid, waterDepth, mossLevel,
  rustLevel)
- **Plattformunabhängigkeit:** Identische Codebase für Desktop (OpenGL 4.5) und
  Mobile (Adreno ES 3.2)
- **Qualitätssicherung:** Headless-Rendering via Mesa EGL in CI

### Optimierungsstrategien (AAA-Praxis)

Aus der VALORANT-Technologie (Riot Games):

- **Optimierte Specular-Berechnung:** Vorab berechnete statische
  Panorama-HDR-Bilder statt virtueller Lichtquellen. Technical Artists zeichnen
  Glanzpunkte direkt auf Texturkarten.
- **Half-Lambert-Aufhellung:** Mathematische Verschiebung der diffusen
  Lichtabnahme → weiche Schatten ohne teure Global Illumination.

### Audioreaktive Echtzeit-Kopplung

Rhythmische Synchronisation via FFT-Analyse mit Blackman-Harris-Fensterfunktion:

```
Echtzeit-Audio-Stream
        │
        ▼
FFT-Analyse (64–8192 Samples, Blackman-Harris-Fenster)
        │
        ▼
Extraktion der Frequenzbänder (Bass, Mitten, Höhen)
        │
        ▼
Exponentielle Skalierung: A_final = (A_bass)^8
        │
        ▼
Doppelte exponentielle Glättung (verhindert visuelles Jittering)
        │
        ▼  Uniform-Schnittstelle (Echtzeit-Werte)
GPU-Vertex-Shader: Dynamische Deformations-Maske
```

### Technische Integration: Praxisbeispiele (Somerville, Jumpship)

- **Procedural Breath Synchronization:** Wwise-Callback liefert Atem-Sample-Länge
  → normalisierter Fortschrittswert → Animations-Controller → Brustkorb synchron
- **Velocity-driven Clothing Rustle:** Relative Geschwindigkeit von
  Arm-/Ellbogenknochen vs. Becken → RTPC an Sound-Treiber → prozedurale
  Kleidungsgeräusche
- **Wetness Footstep Interpolation:** Nässe-RTPC steuert optische Abdunkelung UND
  interpoliert trockene/nasse Fußstapfen-Samples synchron

---

## Erweiterte systemische Potenziale (Ideen-Bibliothek)

| Konzept | Mechanismus | Implikation |
|---|---|---|
| **Echo-Vision** | Temporaler Framebuffer speichert Bewegungszyklen → leuchtende Geisterpfade | Asynchrones Koop ohne Echtzeit-Netzwerk |
| **Memory-Shader** | Besuchte Areale warm/hochgesättigt, unbekannte grau/kontrastarm | Geschwindigkeit + Ausdauer = Vertrautheit |
| **Resonanz-Puzzles** | Schallwellen synchronisieren Vertex-Frequenzen zweier Plattformen | Physik-Puzzle in der Render-Pipeline |
| **Parallelen-Verschiebung** | Zwei Welten auf separaten Shader-Ebenen, Fokus steuert Kollision | Wand in Realität = Torbogen in Geisterdimension |
| **Chronologischer Verfall** | 300 In-Game-Tage → Alterungs-Shader (Falten, graue Haare) → Dissolve-Tod | Endlichkeit als Spielphysik |
| **NPC-Emotions-Shader** | Sympathie = warme Aura (Regeneration), Feindschaft = kalte Aberration (Verzerrung) | Soziale Beziehungen als sichtbare Physik |

---

## Weitreichende Implikationen für die Spieleindustrie

### Aufbrechen traditioneller Entwicklungspipelines

Shader-First erzwingt interdisziplinäre Synergie: Technical Artists und
System-Designer arbeiten gemeinsam an hybriden Systemen, bei denen jede visuelle
Zeile direkt das mechanische Balancing beeinflusst. QA testet nicht über
Log-Files, sondern über automatisierte Framebuffer-Bildanalysen.

### Revolution der Systemischen KI (GOAP)

NPCs navigieren nicht über unsichtbare NavMeshes, sondern wahrnehmungsbasiert:
Flausch-Shader → Boden federt → KI berechnet lautlose Anschleichpfade. Gegner
reagieren auf tatsächliche Licht-/Shader-Intensität, nicht auf Trigger-Zonen.
Hochgradig emergentes Gameplay ohne manuelles Scripting.

### Effizienzsteigerung durch prozedurale Render-Animationen

Kopplung von inverser Kinematik mit physikalischen Shader-Parametern: Skelett
passt sich automatisch an gerenderte Bodenbeschaffenheit an. Getrennte
Animations-Loops für trocken/nass/Eis/Schlamm/Frösteln entfallen.

### Evolution der Immersive Sim

Feuer ist kein numerischer Statuseffekt sondern physisch simulierter
Licht+Dissolve-Shader: Türen verkohlen langsam, Hitze schmilzt Eis, nasse
Kleidung trocknet. Jegliche Barriere zwischen Spieler und Spielumgebung
wird restlos abgebaut.

### Automatisierte QA durch visuelle KI-Agenten

Der gesamte Spielzustand ist visuell kodiert → Test-KI braucht keinen
Code-Zugriff. Agent operiert wie ein menschlicher Spieler: Screenshot →
Analyse → Controller-Input. Visuelle Fehler führen sofort zu veränderter
Routenführung → emergente Systemfehler werden vollautomatisch detektiert.

---

## Strategische Handlungsempfehlungen für Studios

### Phase 1: Technische Fundierung

- Performante CPU-GPU-Brücke (streng typisierte UniformBlock-Pakete)
- Modulares 2D-Statusgitter (Chunks, lokale Materialwerte)
- Headless-Rendering-Tests in CI/CD-Pipeline

### Phase 2: Interdisziplinäre Teamstrukturierung

- Gemischte Feature-Teams (Technical Artists + Gameplay-Programmer + Sound +
  System-Design)
- Kein Shader-Code ohne zugehörige physikalische Interaktionsformel
- QA über Bildanalyse statt Log-Files

### Phase 3: Radikales Prototyping

- Ein Level, kein HUD, rein visuelle Regelkommunikation
- Spieler muss Regeln durch Materialbeobachtung erfassen
- Bei Scheitern: Shader-Intensität/Kontrast nachjustieren, **keine** Tutorials

---

## Quellenangaben

1. Game Design Blog — Eduardo J. Reyes (eduardojreyes.com)
2. How Complex AI Can Promote Emergent Narrative — Joe Duffy
3. A Study Through Theories of Art & Visual Perception — Lee Yu See Jolly (NTU)
4. From Attention to Interaction: Visual Perception Taxonomy (ResearchGate)
5. The Neuroscience of Game Audio — GDC Vault
6. Video Game Graphics and Players' Perception of Subjective Realism — Bournemouth
7. Alan Wake 2 — Wikipedia
8. Top 12 Video Games with Best In-Game Graphics — Juego Studios
9. Most Unique Mechanic/Idea — r/IndieGaming (Reddit)
10. Analysis of Mechanics and Narrative of Superliminal — Game Developer
11. Designing the Mind-Bending Perspective Puzzles of Superliminal — Game Developer
12. Viewfinder (video game) — Wikipedia
13. Detail in a Game That Blew You Away — r/gamedev (Reddit)
14. Horror Game Idea Based on Pareidolia — r/gameideas (Reddit)
15. Pareidolia in a Built Environment — PMC
16. VALORANT Shaders and Gameplay Clarity — Riot Games
17. Audio Reactive Visuals in Unity — Simon Swartout (Medium)
18. Somerville Audio Deep-Dive — GDC 2024 (YouTube)
19. Game AI Automated Testing: Technology Evolution — WeTest
20. Can a Systemic Game Be Small? — r/gamedesign (Reddit)
21. Systemic Game Design: How to Learn? — r/gamedesign (Reddit)
22. Procedural Animation in Video Games — Bluebird International
23. Procedural Generation — Wikipedia
24. Procedural Generation — Meegle
