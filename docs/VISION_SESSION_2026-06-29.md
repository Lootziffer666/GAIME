# Vision Session — 2026-06-29

> Aufgezeichnet nach Step 14 (Unified World Runtime). 1,5 Stunden Gespräch über
> die Gesamtvision, GAIME's Rolle in ANVIL, und die langfristige Strategie.
> Nicht editiert, nur destilliert.

---

## GAIME ist eine Ausprägung Anvils

GAIME ist nicht das Hauptprodukt. Es ist der **Beweis**, dass ANVIL funktioniert.
GAIME entstand in wenigen Tagen — der Weg dahin (ANVIL's Methodik, Prinzipien,
Gate-Disziplin, Brief-System) hat Monate gebraucht.

ANVIL ist die Werkbank. GAIME ist das erste Werkstück.

```
ANVIL (Methode, Werkbank, AI Studio)
  ├── produziert GAIME (Engine + Quest Accepted Unfortunately)
  ├── produziert FLOW (Sprach-Toolchain)
  ├── produziert CatchIt (UX)
  ├── produziert [nächstes Projekt]
  └── Das ist das autarke Studio.
```

---

## Das Ziel: Autarkes AI Indie Studio

**Langfristiges Ziel:** Idee, Screenshot-Mockups, Art Directive rein — Spiel raus.
Nicht für mich allein. Für jeden. Kindergartenkinder mit einem Satz.

**Der Weg:**

1. Quest Accepted Unfortunately fertigstellen (Beweis, Trailer, Trainingscorpus)
2. Git-History analysieren (was hat funktioniert, wo war Reibung)
3. AI den optimalen Workflow perfektionieren lassen
4. 16-Bit RPGs sind der Anfang
5. Ziel: 2.200 Unreal Assets über AI UE5-Plugin eigenständig zu Spielen assemblieren
6. Langfristig: Assets entstehen autark via Tripo/Rodin

---

## GAIME's Doppelbedeutung

```
GAIME = Game · AI · Map · Engine
      → Was es PRODUZIERT (Spiele aus Bildern via AI)

GAIME = Graphical Artificial Immersion Measurement Engine
      → Was es IST (ein System das Immersion erzeugt und misst)
```

---

## Die Kernthese: Immersion = Glaubhaftigkeit, nicht Realismus

> "Niemand hat zu Pitfall gesagt: Also realistisch ist das jetzt nicht."
> "Niemand sagt zu Chrono Trigger: Das ist aber verdammt schlecht gealtert."

Immersion entsteht nicht aus Auflösung. Sie entsteht aus **innerer Logik**. Wenn sich
nichts widerspricht, ist der Spieler drin — egal ob 16-Bit oder 4K.

---

## Shader als Sprache

**These:** Ein einzelner Sprite-Frame + Shader = lebender Organismus.

```
CharacterState {
    musculature: Float   // -1.0 (mager) bis 1.0 (muskulös)
    wetness: Float       // 0.0 (trocken) bis 1.0 (durchnässt)
    dirt: Float          // 0.0 (sauber) bis 1.0 (verdreckt)
    exhaustion: Float    // 0.0 (ausgeruht) bis 1.0 (am Limit)
    age: Float           // 0.0 (Kind) bis 1.0 (Greis)
}
```

Fünf Floats. Ein Shader. Unendliche Kombinationen. Null zusätzliche Assets.

**Warum Shader statt Assets:**
- Kein Team, kein Budget → Mathematik ist kostenlos
- Dynamisch statt statisch (Pfütze ENTSTEHT, WÄCHST, FLIEßT AB)
- Skaliert von 16-Bit bis 4K ohne Code-Änderung
- Ein Regelwerk, unendliche Zustände

---

## Die 40-Systeme-These: Weltlogik

Die 40 Physik-Systeme sind keine Feature-Liste. Sie sind eine **Weltformel**:

> Zeit × Material × Umgebungsbedingungen = visueller Zustand

Beispiele bereits angelegt in GAIME:
- Regen → respektiert Dächer → sammelt sich → Pfützen → Abfluss → Gulli
- Feuer → Materialermüdung in der Umgebung (ohne Berührung)
- Schnee + Blut → vermischen sich
- Jahreszeiten → Natur holt sich Besitz zurück
- NPCs → Baby wird Kind, Alter stirbt, Training macht stark, Hunger macht mager
- Atem → leichte Verschiebung (ruhig) vs. starke (außer Atem)

Alles aus einem System: "Zeit wirkt auf Materie."

---

## Immersion ist messbar

```
Immersions-Bruch = Regelverstoß

  Regen fällt durch Dach?               → Bruch.
  Pfütze verschwindet nie?              → Bruch.
  NPC altert nicht obwohl Zeit vergeht? → Bruch.
  Feuer hinterlässt keine Spuren?       → Bruch.

Immersion = Abwesenheit von Brüchen = 40/40 Regeln konsistent.
```

Kein Spiel misst das. Kein Studio automatisiert das. GAIME kann es.

---

## Edith Finch als Referenz

What Remains of Edith Finch: Jeder Raum erzählt ein Leben, nicht durch Text,
sondern durch Zustand. Staub, Vergilbung, Kondenswasser — alles von Hand.

GAIME-Äquivalent: Ein Parameter ("verlassen seit 30 Jahren") → Shader rechnet
Staub, Verfall, Moos, Rost automatisch. Aus Geometrie + Material + Zeit.

---

## Wo GAIME in ANVIL gehört

GAIME ist ein **Projekt** in der ANVIL-Registry (wie CatchIt, FLOW, Borderline):

```json
{
  "id": "gaime",
  "name": "GAIME",
  "repo": "Lootziffer666/GAIME",
  "status": "active",
  "gate_phase": "Pfeiler 1 done, Pfeiler 2 next",
  "notes": "Graphical Artificial Immersion Measurement Engine."
}
```

---

## Der Immersion-Layer als eigenständiges Produkt (Zukunft)

```
Phase 1: GAIME intern (40 Systeme für Quest Accepted)
Phase 2: GAIME als Engine (jedes 2D-Spiel erbt die 40 Systeme)
Phase 3: Immersion-Layer als Framework (über beliebige Geometrie stülpen)
Phase 4: UE5 Plugin (gleiche Regeln, 3D-Scope, 2.200 Assets)
```

Der Layer liest Geometrie + Material-Tags + Zeitparameter und macht jede Welt
lebendig. Ohne manuelles Platzieren von Effekten.

---

## Das Bar-Szenario (Anvils Endvision)

> Du lernst jemanden kennen. Ideen entstehen im Gespräch. LLM zeichnet auf.
> Du tippst "start". 5 Minuten später kann jeder vor Ort eine Micro-Demo
> ausprobieren. Auf dem Smartphone. Zusammengestellt aus der Asset-Datenbank.

Die Distanz zwischen Idee und Vergessen — eliminiert.

---

## Tools werden intelligent (Zukunftsvision)

SpeedTree, Nanite, Lumen — heute dumme Werkzeuge. Wissen WAS sie können,
nicht WANN und WO und WARUM.

Mit ANVIL als Dirigent: Kontext bestimmt die technische Entscheidung.
- Vordergrund → Nanite an
- Hintergrund → Nanite aus, LOD reicht
- Innenraum → Lumen an
- Offene Wüste → Baked reicht
- Wald-Biom markiert → SpeedTree mit passenden Parametern

---

## Warum AI bisher widerstand

LLMs haben kein Problem mit der Vision. Sie haben eine **Lücke** — diese Kombination
existiert nicht in den Trainingsdaten. Also feuern sie den sichersten Default:
"Das ist sehr ambitioniert."

Das ist kein Urteil. Das ist Vergesslichkeit. Und jedes Mal wenn AI bremst, ist das
ein Signal: **das hier ist neu genug, dass es nirgends als abgeschlossener Gedanke existiert.**

Wenn Quest Accepted fertig ist und die Git-History analysiert — dann existiert es.
Dann ist es in der Welt. Dann werden zukünftige AI-Modelle sagen: "Ja, so wie ANVIL."

---

## Zusammenfassung in einem Satz

> Nicht alles neu erfinden, sondern das was wir bereits können, für jeden
> zugänglich machen — in einer Form, die ich bisher nirgends gesehen habe.

---

*Dokumentiert nach Session vom 2026-06-29. Step 14 PR: #52.*
