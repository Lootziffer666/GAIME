# 40 Sichtbare Weltgesetze — Shader-First Systemkatalog

> Ausgehend vom Kernprinzip „Was du siehst = was gilt": Shader zeigen nicht an,
> dass etwas passiert. Shader SIND das Passieren.

**Status:** Design-Referenz. Teilweise implementiert (markiert).
**Quelle:** Owner-Input, 2026-06-28.

---

## Implementierungsstatus

| # | System | :core | :game | Screenshot |
|---|---|---|---|---|
| 1 | Schmutz/Staub/Ruß | ✅ DirtState | — | — |
| 2 | Fußspuren | ✅ FootprintGrid | — | — |
| 3 | Material-Ermüdung | — | — | — |
| 4 | Druck/Gewicht/Belastung | — | — | — |
| 5 | Wind | ✅ WindState | — | — |
| 6 | Geruch als Shader-Wolke | — | — | — |
| 7 | Klang als sichtbare Wellen | — | — | — |
| 8 | Feuchtigkeit im Mauerwerk | — | — | — |
| 9 | Rost | — | — | — |
| 10 | Öl/Fett/Harz (brennbar) | — | — | — |
| 11 | Schatten als Besitzverhältnis | — | — | — |
| 12 | Erinnerung des Bodens | — | — | — |
| 13 | Angst/Stress-Shader | ✅ PoisonFilter (Pressure) | ✅ | ✅ |
| 14 | Krankheit/Gift/Infektion | ✅ PoisonFilter | ✅ | ✅ |
| 15 | Pflanzen reagieren | — | — | — |
| 16 | Insekten-/Kleintier-Schwärme | — | — | — |
| 17 | Blut als Information | — | — | — |
| 18 | Magie als Brechungsfehler | — | — | — |
| 19 | Tageszeit als Materialverhalten | — | — | — |
| 20 | Temperaturgradienten | — | — | — |
| 21 | Kälte als Kristallwachstum | — | — | — |
| 22 | Wasserströmung als Topologie | ✅ WaterGrid (flow) | ✅ WaterOverlay | ✅ |
| 23 | Kleidung als Weltzustand-Träger | — | — | — |
| 24 | NPC-Stimmung über Farbtemperatur | — | — | — |
| 25 | Besitz/Verbot als sichtbare Ordnung | — | — | — |
| 26 | Lärm-/Lichtverschmutzung | — | — | — |
| 27 | Jahreszeiten als Shader-Migration | — | — | — |
| 28 | Hunger/Durst/Erschöpfung ohne HUD | — | — | — |
| 29 | Unsichtbarkeit als optisches Problem | — | — | — |
| 30 | Reparatur als sichtbarer Eingriff | — | — | — |
| 31 | Reinigung als Mechanik | — | — | — |
| 32 | Oberflächen-Alphabet (Runen) | — | — | — |
| 33 | Lokale Gravitation/Realitätsschwere | — | — | — |
| 34 | Biom-Mischzonen | — | — | — |
| 35 | Karte als Shader-Objekt | — | — | — |
| 36 | Feuer-Nachwirkungen | — | — | — |
| 37 | Nebel als Informationsfilter | — | — | — |
| 38 | Sternenlicht/Mondlicht | — | — | — |
| 39 | Wolken als wandernde Regelzonen | — | — | — |
| 40 | Falsche Sauberkeit als Hinweis | — | — | — |

---

## 1. Schmutz, Staub, Ruß: Weltkontakt als Erinnerung

Spieler sammelt sichtbare Materialspuren von der Umgebung:
- Schlamm an Stiefeln (Moor/Pfütze) → Schritte sichtbar, rutschig
- Staubfilm (Ruinen/Wüste) → sichtbarer in hellem Licht
- Ruß (Feuer/Explosion) → minimaler Kälteschutz, NPCs verdächtig
- Pollen (Wald) → lockt Insekten/Kreaturen
- Asche (Brandgebiet) → markiert deine Passage

Regen wäscht langsam. Die Welt erkennt dich über deine Oberfläche.

**Implementiert:** `core/rpg/weather/DirtState.kt`

---

## 2. Fußspuren als temporäre Shader-Geometrie

Jeder Untergrund bekommt Imprint-Verhalten:
- Schnee: tiefe Abdrücke
- Sand: weiche Spuren, verwehen im Wind
- Moos: dunkle Druckstellen
- Asche: schwarze Schleppspuren
- Blut/Schlamm: Übertragung auf andere Tiles

Gegner können Spuren verfolgen. Regen/Wind löschen. Feuer konserviert kurzzeitig.

**Implementiert:** `core/rpg/weather/FootprintGrid.kt`

---

## 3. Material-Ermüdung: Dinge brechen sichtbar, BEVOR sie brechen

Kein HP-Balken. Stattdessen:
- Holz: feine Risse
- Stein: helle Bruchadern
- Metall: Haarrisse + Verfärbungen
- Seile: Alpha-Mask-Ausfransen
- Glas: Spannungsmuster

Wiederholtes Betreten verstärkt. Regen quillt Holz. Frost sprengt Stein.
Moos kaschiert Risse (macht Gefahren schwerer lesbar).

---

## 4. Druck, Gewicht, Belastung als Shader

- Boden dunkelt unter schweren Objekten
- Teppiche: Druckflächen
- Schnee sackt unter Gewicht
- Holzplanken biegen sich visuell
- Metallplatten: Spannungsfarbe (Hitze-Mapping)

Schwere Rüstung = lauter + tiefere Spuren. Monster kündigen sich durch Bodenringe an.

---

## 5. Wind als sichtbare Kraft

Wind ist nie unsichtbar:
- Gras neigt sich
- Rauch zieht in Richtung
- Regen fällt schräg
- Fackeln flackern, verlieren Radius
- Kleidung bekommt Richtungsoffset

Pfeile abgelenkt. Feuer breitet sich mit Wind aus. Gegen Wind schleichen = sicherer.

**Implementiert:** `core/rpg/weather/WindState.kt`

---

## 6. Geruch als Shader-Wolke

Subtile Diffusionsebene statt HUD:
- Nasse Kleidung: kalte Dunstspur
- Blut: dunkle rote Schlieren
- Essen: warme gelbliche Partikel
- Rauch: graue Wolken

Tiere folgen. Regen schwächt. Wind verschiebt. Feuer überdeckt kurz mit Rauch.

---

## 7–40: [Volltext im Chat-Input des Owners, hier als Referenz-Stichworte]

7. Klang als Wellen (Pfützen-Kreise, Echo-Highlights)
8. Feuchtigkeit im Mauerwerk (Salzausblühungen, Moos-Beschleunigung)
9. Rost (orange→matt→Löcher→Kollaps)
10. Öl/Fett/Harz (brennbar, rutschig, rostschützend)
11. Schatten als Besitz (Dinge altern langsamer im Schatten)
12. Erinnerung des Bodens (Farbtemperatur = Vertrautheit)
13. Angst/Stress (Sehfehler statt Balken) ✅
14. Krankheit/Gift (grüne Adern, Chromatic Aberration) ✅
15. Pflanzen reagieren (Licht/Wärme/Wasser/Nähe)
16. Insekten als Indikatoren (Fliegen bei Leichen, Motten bei Licht)
17. Blut als Information (frisch glänzt, alt dunkelt, Schnee extrem sichtbar)
18. Magie als Brechungsfehler (Pixel falsch sortiert, Licht gegen Richtung)
19. Tageszeit als Materialverhalten (Morgentau, Mittagstrocknung, Nachtpilze)
20. Temperaturgradienten (Seite zum Feuer glüht, Schattenseite kalt)
21. Kälte als Kristallwachstum (Randkristalle→Eisversiegelung→Risse)
22. Wasserströmung als Topologie (Richtung, Tiefe, Temperatur sichtbar) ✅
23. Kleidung als Zustandsträger (Umhang saugt Wasser, Metall reflektiert)
24. NPC-Stimmung über lokale Farbtemperatur
25. Besitz/Verbot als sichtbare Ordnung (eigenes=klar, fremdes=kalter Saum)
26. Lärm-/Lichtverschmutzung (Infrastruktur hat Nebenwirkungen)
27. Jahreszeiten als Shader-Migration (langsame Zustandsverschiebung)
28. Hunger/Durst/Erschöpfung ohne HUD (Kleidung lockerer, Atem schwerer)
29. Unsichtbarkeit als optisches Problem (Regen/Staub/Schatten verraten)
30. Reparatur als sichtbarer Eingriff (neue Bretter andere Farbe)
31. Reinigung als Mechanik (Geheimnisse freilegen, Schuld verwischen)
32. Oberflächen-Alphabet (feuchte Runen leuchten bei Mondlicht)
33. Lokale Gravitation (schwere vs. leichte Realität, sichtbar an Partikeln)
34. Biom-Mischzonen (Matsch zwischen Schnee+Erde, Aschesaum nach Brand)
35. Karte als Shader-Objekt (wird nass, Tinte verläuft, Mondlicht-Details)
36. Feuer-Nachwirkungen (Asche→Glut→Rauch→fruchtbarer Boden)
37. Nebel als Informationsfilter (Silhouetten größer, Feuchtigkeit steigt)
38. Sternenlicht/Mondlicht (silberne Kanten, Pflanzen öffnen, Phasen ändern Puzzles)
39. Wolken als wandernde Regelzonen (Schatten bewegt sich, Solar pausiert)
40. Falsche Sauberkeit (Sterilität = verdächtig, Illusionen ohne Materialgeschichte)

---

## Prototyp-Empfehlung (Favoriten)

Die besten Kandidaten für einen Testlevel (viele Systemverbindungen):

1. **Spuren-System:** Schnee/Sand/Schlamm/Blut/Asche + Regen/Wind/Gegnertracking
2. **Material-Ermüdung:** Risse, Feuchtigkeit, Frost, Feuer, Gewicht
3. **Geruch/Wind:** Tiere, Jagd, Schleichen, Regen, Rauch
4. **Nebel/Licht:** Sichtbarkeit, Feuchtigkeit, Feuer, Gegner-Silhouetten
5. **Rost/Feuchtigkeit/Metall:** langsamer Weltzerfall mit sichtbaren Konsequenzen
6. **Reinigung/Freilegen:** Geheimnisse, Schuld, Hinweise, Ruinenarchäologie

**Idealer erster Testlevel:** Schnee + Fußspuren + Wind + Blut + Fackellicht.
Sofort lesbar, technisch abgrenzbar, beweist "Shader sind State" maximal.
