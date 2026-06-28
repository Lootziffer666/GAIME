# Result: Step 10 — Stokeport-Karten eingebunden + Brücke als Verbinder

**Brief:** `briefs/2026-06-28-korge-step10-stokeport-maps.md`
**Branch:** `kiro/korge-step10-stokeport-maps` (PR#48)
**PR:** https://github.com/Lootziffer666/GAIME/pull/48
**Datum:** 2026-06-28 · Integration-Review + Collision-Fix: Claude
**Status:** ✅ Vollständig (5 Karten begehbar nach `:core`-Fix) · mapbuilder enthalten

---

## Acceptance

| Check | Ergebnis |
|---|---|
| `:core:desktopTest` | ✅ grün (inkl. 2 neue CollisionGrid-Tests) |
| `:game:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:composeApp:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:game:screenshot` | ✅ 42 PNGs (5 neue Karten) |

---

## Geliefert (Kiro)
5 fertige HD-Locations als KorGE-`MapConfig` eingebunden + Overworld-Graph:
`EXTERIOR` (Hub) → CHAPEL / GUILD_HALL / GLASSBLOWERS direkt; **EXTERIOR → BRIDGE → RUINED_TEMPLE**
(Brücke über animiertem Wasser als echter Übergang). Spawn-Koordinaten per B004-Methode aus dem
CollisionGrid abgeleitet + im PR dokumentiert. 5 Screenshot-Captures.

**Kiro hat die zwei nicht selbst lösbaren Blocker korrekt gemeldet statt `core/` (DO_NOT_TOUCH)
eigenmächtig anzufassen** — vorbildliches Handoff-Verhalten.

## Integration-Fix (Claude) — die zwei Blocker behoben
Beide lagen in `:core/CollisionGrid.kt` (Layer-Klassifikation):
1. **BRIDGE:** `Bridges`-Layer war SOLID, und `Bridges.tmx` hat nur Water+Bridges (kein Boden) →
   0 begehbar. Fix: neue Rolle `BRIDGE` + eigener Pass **nach** dem WATER-Pass → die Brücke
   überschreibt das Wasser und ist WALKABLE (man läuft über den Fluss).
2. **RUINED_TEMPLE:** `trees1–6` waren SOLID und überdeckten die ganze Karte → 0 begehbar. Fix:
   `trees*` aus SOLID entfernt → DECORATIVE (nicht blockierend), konsistent mit heroes-home (dort
   liegen Bäume in dekorativen `Objects`-Layern). Boden/Gras/Site bleiben begehbar.
+ 2 Unit-Tests (`bridges layer is walkable`, `trees layers do not block floor`).

**Visuell verifiziert:** `map_bridge.png` (Spieler auf der Steinbrücke über Wasser, sauber
zusammengesetzt — anders als der alte kaputte Compose-Raster) und `map_ruined_temple.png` (Spieler
auf begehbarem Tempelgelände, Bäume dekorativ). Beide committed in `docs/screenshots/`.

## mapbuilder-Pipeline (`tools/mapbuilder/`) — bewusst enthalten
Die Photo→TMX-Pipeline (~4.200 Z. Python: Flask-Server, OpenCV-Segmentierung, WFC, Canvas-Paint-
Editor, TMX-Export) ist **Eigenarbeit des Owners** der letzten Stunden, nicht Scope-Creep. Bleibt
unangetastet im Repo. Reines Python — kein Einfluss auf den Kotlin-Build/Tests. Migrationsplan **§5
aktualisiert**: vom „separate project" zu „now in-repo tool under tools/mapbuilder".

---

## DO_NOT_TOUCH — eingehalten ✅
B007 `localCurrentDirVfs` intakt (siebtes Mal). Kiro hat `core/` korrekt nicht angefasst (Blocker
gemeldet) — der `:core`-Fix kam bewusst von Claude bei der Integration.

## Offen / Folge
- **RUINED_TEMPLE/BRIDGE Spawn fein justieren** falls beim Spielen nötig (Spawns wurden unter der
  0-walkable-Bedingung gewählt; rendern jetzt aber auf sinnvollem Grund).
- **Interiors** der 5 Locations + NPCs/Dialoge: eigener Folge-Brief.
- Step-9-Overlay-Rendering-Fix (Seasons/Material/Frozen-Tuning) weiterhin offen.

## Stand nach Merge
main aktualisiert. Stokeport ist von 1 auf **6 begehbare Orte** gewachsen; die Brücke überbrückt
einen echten Übergang über animiertes Wasser. mapbuilder-Tooling im Repo.
