# Result: KorGE Migration Step 5a — Real Sprites + BattleScene + Audio

**Brief:** `briefs/2026-06-28-korge-step5a-real-sprites-battle-audio.md`
**Branch:** `kiro/korge-step5a-sprites-battle-audio`
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

| Datei | Art | Beschreibung |
|---|---|---|
| `SpriteLoader.kt` | create | CraftPix-Sheet-Loader, horizontale Frames, Fallback |
| `CharacterSprite.kt` | create | Swordsman/Vampire, 5 Animationen, Facing, Grid-Position |
| `AudioManager.kt` | create | BGM (readMusic), SFX (readSound), graceful degradation |
| `BattleScene.kt` | create | Rundenbasierter Kampf, CombatEngine, HP-Bars, VICTORY/DEFEAT |
| `TiledMapScene.kt` | modify | CharacterSprite, BGM, SPACE→BattleScene |
| `Main.kt` | modify | Kommentar-Update (Step 5a) |
| `docs/KORGE_MIGRATION_PLAN.md` | modify | Step 5a ✅ |

---

## Pitfalls (für KNOWN_BUGS)

| Pitfall | Fix |
|---|---|
| `changeTo<>()` ist suspend — geht nicht direkt in `addUpdater {}` | `launch { sceneContainer.changeTo<>() }` im Scene-Scope |
| `SpriteLoader.buildFallbackBitmap()` war private, wird aber von `CharacterSprite` benötigt | `internal` visibility |
| `sceneContainer` (ohne Klammer) = Property auf Scene, nicht `sceneContainer()` Factory | Property verwenden innerhalb einer Scene |
| KorGE Audio in headless Sandbox: kein Audio-Backend vorhanden | try/catch in AudioManager, graceful no-op |

---

## Design-Entscheidungen

- **`SpriteLoader.load()` fängt Exceptions ab** → gibt Fallback-Frame zurück. So
  kompiliert und „läuft" der Code auch ohne Assets auf dem Classpath.
- **`CharacterSprite` in `mapView` Container** (nicht Scene-Root) → skaliert/scrollt
  korrekt mit der Kamera. In `BattleScene` liegt er direkt im Scene-Root (kein Map).
- **`addUpdater { launch { changeTo } }`** statt direktem suspend-Call → KorGE's
  `addUpdater`-Lambda ist nicht suspend; Scene erbt CoroutineScope → `launch` ok.
- **Kampflogik im `addUpdater`** reagiert auf `justPressed` → exakt ein Angriff pro
  Tastendruck, nicht kontinuierlich.
- **HP-Bars als `solidRect`** mit `.width`-Manipulation → einfachste KorGE-Lösung,
  keine Custom-Views nötig.

---

## Nächster Schritt

**Step 5** — Retire the Compose gameplay engine (`:composeApp` Gameplay-Code entfernen,
nur noch Non-Gameplay-UI behalten falls nötig).
