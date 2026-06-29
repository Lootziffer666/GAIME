# GAIME — Engine-Vision & First-Class-Roadmap

> Stand: 2026-06-29. Dieses Dokument richtet alle folgenden Briefs aus.
> Es definiert, was wir bauen (eine Engine), wofür „first-class" konkret steht,
> und in welcher Reihenfolge wir dahin kommen.

## Die Umbenennung: Engine vs. Spiel

Seit gestern entwickeln wir nicht mehr primär ein Spiel. Wir haben eine **Engine**.
Das ist keine semantische Spielerei — es ändert, woran wir Fortschritt messen.

- **GAIME** = die Engine. **G**ame · **AI** · **M**ap · **E**ngine.
  Eine *AI-getriebene, physics-empowered Post-16-Bit-Engine, die sich ihre Maps
  selbst erschafft.*
- **„Quest Accepted: Unfortunately"** = der Flagship-Titel, das Schaufenster.
  Beweist die Engine, verkauft sie nicht als Tech-Demo, sondern als spielbares Werk.

Fortschritt heißt ab jetzt: **Macht die Engine ein neues, spielbares, physikalisch
lebendiges Gebiet aus einem Bild — in Minuten, reproduzierbar, sichtbar?**

## Die These (der USP in einem Satz)

> Male ein Bild. Die Engine macht daraus eine begehbare Welt, in der die Umgebung
> Information trägt (Blut, Nebel, Fußspuren, Material-Ermüdung), Figuren als
> getuschte Doodles vor scharf gemalten Hintergründen bei 1440p leben, und der
> Humor ein System ist, kein Gag-Text.

Drei Achsen, die zusammen einzigartig sind — einzeln gibt es sie, in Kombination nicht:
1. **AI-Map-Authoring** (Bild → Segmentierung → Grid → spielbar).
2. **Umwelt als Information** (Post-16-Bit-Physik: 40-Systeme-These).
3. **Doodle-auf-gemalt** (1440p, EPX-Outline + Boil; später Anime4K HQ A+B).

## Was „first-class" konkret heißt — die 5 Pfeiler

Der Abstand zwischen „beeindruckende Sammlung von Systemen" und „erstklassige Engine":

1. **Ein einziges World-Runtime.** Heute existieren zwei Welt-Renderpfade
   (`WorldScene` Tilemap, `DoodleWorldScene` Bild+Grid) plus `composeApp` als dritter
   Halb-Renderer. First-class = **ein** `World`-Modell, in das alles einsteckt: Map
   (Bild+Grid), Entities, Physik-Ticks, Overlays, Shader, Input, Kamera, Dialog,
   Kampf, Übergänge. Keine Gabelung „in welcher Welt?".

2. **Sichtbarkeit = Fertigkeit.** Ein System ist erst fertig, wenn es im Bild zu
   sehen ist — nicht, wenn der Test grün ist. Heute hat `:core` ~13 Physik-Systeme
   mit getesteter Logik, aber mehrere rendern nachweislich nicht
   (`WORLD_PHYSICS_40_SYSTEMS.md`: „⚠️ Overlay rendert nicht", „roh positioniert").
   First-class schließt diese Lücke (`BaseOverlay`: Kamera-Raum + Alpha-Floor *einmal*
   korrekt) und priorisiert ab jetzt nach „sichtbar?", nicht „getestet?".

3. **Der AI-Map-Loop ist robust und geschlossen.** Nicht „Python laufen lassen, TMX
   von Hand nachbessern". First-class = Prompt → Bild → Segmentierung → Grid →
   **automatische Spielbarkeits-Prüfung** (begehbarer Spawn? gültiger Pfad
   Eingang→Ausgang? Brücke verbindet wirklich?) → spielbar. Die HSV-Sprödigkeit
   (Himmel/blaue Dächer → fälschlich Wasser) wird gehärtet oder durch einen
   verifizierenden Schritt aufgefangen.

4. **Designer-Ergonomie: der innere Loop.** Von Idee zu spielbar in Minuten, ohne
   Code. Bild in einen Ordner legen → Map erscheint. Quest/Dialog als Daten (YAML/
   Tiled-Properties) → erscheint im Spiel. Der Brief→Kiro-Loop bleibt für Engine-Arbeit;
   das *Content*-Authoring wird datengetrieben, damit der Designer (du) ohne Kotlin auskommt.

5. **Eine Vertical Slice, die alles auf einmal beweist.** Ein vollständig poliertes
   Gebiet — Taverne (innen) ↔ Wildwood (außen) ↔ Brücke (Verbinder) — das gleichzeitig
   zeigt: AI-authored Map, sichtbare Physik, Doodle-Render, Comedy-Bark, Dialog, Kampf,
   Übergänge. Das ist der Trailer-Moment und der Engine-Beweis in einem.

## Die Architektur-Spine

```
            ┌──────────────── GAIME World Runtime (ein Modell) ────────────────┐
 Bild ─► mapbuilder ─► TMX ─► CollisionGrid ─┐                                  │
 (Art-Direction-Doc)         + Playability-Check │  Entities · Physik-Ticks      │
                                                 │  Overlays(BaseOverlay) ·      │
 Content (YAML: Quests/Dialog/NPCs) ────────────►│  Shader · Kamera · Input ·    │
                                                 │  Dialog · Kampf · Übergänge   │
                                                 └──────────► DoodleWorldScene @1440p
```

`:core` bleibt engine-agnostisch + getestet (Logik). `:game` rendert. Der
Render-Pfad konvergiert auf **eine** Scene. `composeApp` wird retiret.

## Roadmap (Brief-Sequenz)

| # | Brief | Pfeiler | Warum jetzt |
|---|---|---|---|
| 1 | **Unified `World`-Runtime** — `WorldScene`-Features (NPC/Dialog/Kampf/Bark/Kamera) in den Bild+Grid-Pfad migrieren; `WorldScene` + `composeApp` retiren | 1 | Die Spine. Alles andere steckt hier ein. |
| 2 | **`BaseOverlay` + Render-Parität** — die ⚠️-Systeme aus dem 40-Doc sichtbar machen | 2 | Verwandelt unsichtbare Logik in sichtbare Macht. |
| 3 | **Geschlossener AI-Map-Loop** — mapbuilder härten + automatische Spielbarkeits-Verifikation | 3 | Macht „Bild rein, Map raus" verlässlich. |
| 4 | **Datengetriebenes Content-Authoring** — Quests/Dialog/NPCs als Daten | 4 | Designer-Loop ohne Kotlin. |
| 5 | **Vertical Slice** — Taverne ↔ Wildwood ↔ Brücke, voll poliert | 5 | Der Beweis + Trailer. |

## Was das NICHT ist (Scope-Disziplin)

- **Kein** zweites Render-Framework. KorGE bleibt (locked, `rendering-engine.md`).
- **Keine** neuen `:core`-Systeme, bevor die bestehenden sichtbar sind (Pfeiler 2 vor Breite).
- **Kein** Fremdcode im Baum (Anime4K/EPX reimplementiert — Donor-Policy).
- **Kein** Feature in zwei Welten parallel — erst konvergieren (Pfeiler 1).
