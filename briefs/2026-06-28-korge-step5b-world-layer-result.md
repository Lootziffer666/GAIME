# Result: KorGE Migration Step 5b — World Layer

**Brief:** `briefs/2026-06-28-korge-step5b-world-layer.md`
**Branch:** `kiro/korge-step5b-world-layer`
**PR:** *(nach Push)*
**Datum:** 2026-06-28
**Autor:** Kiro (Opus-only, keine Delegation)
**Status:** ✅ Abgeschlossen — alle Acceptance-Kriterien grün

---

## Acceptance

| Check | Ergebnis |
|---|---|
| `:game:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:core:desktopTest` | ✅ BUILD SUCCESSFUL (unverändert) |
| `:composeApp:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL (unverändert) |

---

## Neue / geänderte Dateien

| Datei | Art |
|---|---|
| `DialogLine.kt` | create |
| `NpcDefinition.kt` | create |
| `MapConfig.kt` | create (MapId, MapExit, MapConfig + interior/exterior factories) |
| `DialogOverlay.kt` | create |
| `HudOverlay.kt` | create |
| `WorldScene.kt` | create |
| `CharacterSprite.kt` | modify (smooth movement, loadFromSheet, Facing.dx/dy) |
| `BattleScene.kt` | modify (Q → WorldScene) |
| `Main.kt` | modify (boot WorldScene) |
| `docs/KORGE_MIGRATION_PLAN.md` | modify (Step 5b ✅) |
| `docs/KNOWN_BUGS.md` | modify (World Layer pitfalls) |

---

## Brief-Abgleich (alle Punkte)

- [x] Schritt 1: DialogLine.kt — data class (speaker, text)
- [x] Schritt 2: NpcDefinition.kt — data class (tileX, tileY, idleSheetPath, facing, dialog)
- [x] Schritt 3: MapConfig.kt — MapId enum, MapExit, MapConfig mit interior()/exterior()/forId()
- [x] Schritt 4a: CharacterSprite smooth movement (startMove, isMoving, visualGridX/Y, 160ms step, moveProgress tick before animation)
- [x] Schritt 4b: CharacterSprite.loadFromSheet() for NPCs
- [x] Schritt 4c: Facing.dx/dy extensions
- [x] Schritt 5: DialogOverlay (show/advance, isActive, individual visibility, screen-fixed)
- [x] Schritt 6: HudOverlay (HP bar, gold, location, update())
- [x] Schritt 7: WorldScene (TMX load, NPCs, smooth movement, dialog E-key, collision + NPC blocking, exit transitions, SPACE→Battle, HUD, camera with visualGrid)
- [x] Schritt 8: Main.kt → WorldScene
- [x] Schritt 9: BattleScene Q → WorldScene
- [x] DO_NOT_TOUCH: keine der gelisteten Dateien angefasst
- [x] Doku: KORGE_MIGRATION_PLAN Step 5b ✅, KNOWN_BUGS erweitert

---

## Pitfalls (dokumentiert in KNOWN_BUGS.md)

- Facing enum Redeclaration (PlayerSprite.kt vs. CharacterSprite.kt)
- SolidRect ist kein Container (addChild kompiliert nicht)
- Text-Wrapping geht nicht automatisch
- moveProgress-Tick muss vor Animation-Check stehen
- Property-Setter-Reihenfolge in startMove()
- companion object var für Scene-Parameter-Übergabe

---

## Exit/Spawn-Koordinaten-Hinweis

Die konfigurierten Exit-Kacheln (8,1) für Interior und (8,22) für Exterior sind
Schätzwerte aus dem Brief. Bei manuellem Lauf lokal prüfen ob diese WALKABLE sind —
falls BLOCKED, auf benachbarte begehbare Kachel korrigieren. Die MapConfig-Architektur
erlaubt triviale Anpassung ohne Strukturänderung.

---

## Nächster Schritt

**Step 5** — Retire the Compose gameplay engine.
