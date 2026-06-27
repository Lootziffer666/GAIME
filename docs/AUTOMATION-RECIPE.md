# GAIME — Automation Recipe
## Build a satirical RPG in 3 weeks with Claude Code + Kiro

> Distilled from the GAIME development experience. Copy this process into the first message of a new session.

---

## Prerequisites

- Clone or fork `lootziffer666/gaime` as a starting template
- Have Kotlin Multiplatform + Compose Multiplatform installed
- Have Claude Code CLI and Kiro (AWS Toolkit) configured

---

## Session 1 Prompt (Claude Code)

```
Build a [GENRE] game in Kotlin Multiplatform using Compose Multiplatform.
Architecture: `:core` module (pure Kotlin game logic) + `:composeApp` module (Compose UI).
Follow the patterns in docs/DEVELOPMENT-CHRONICLE.md and .kiro/steering/.
Start with: game concept doc → state machine enum → core combat/world system → UI skeleton.
Game concept: [YOUR CONCEPT HERE]
```

---

## State Machine (copy this pattern)

```kotlin
// core/src/commonMain/kotlin/rpg/GamePhase.kt
enum class GamePhase {
    TITLE_SCREEN,
    // List every beat of your game upfront — the compiler enforces completeness
    INTRO,
    EXPLORE_TOWN,
    NPC_DIALOGUE,
    DUNGEON,
    BOSS_COMBAT,
    POST_BOSS,
    VICTORY,
    GAME_OVER,
}
```

Then in your main screen composable:
```kotlin
var phase by remember { mutableStateOf(GamePhase.TITLE_SCREEN) }
// ...
when (phase) {
    GamePhase.TITLE_SCREEN -> TitleView { phase = GamePhase.INTRO }
    GamePhase.INTRO -> DialogueOverlay(...) { phase = GamePhase.EXPLORE_TOWN }
    // Kotlin reports missing cases as compile errors — never miss a phase
}
```

---

## World Setup (copy this pattern)

```kotlin
// One world per location, created once, HP persists across scene transitions
val townWorld = remember {
    GridWorld(GameMaps.town()).also { w ->
        w.entities.add(GridEntity("innkeeper", 5, 8, GridEntityType.NPC, "npc_innkeeper"))
        w.entities.add(GridEntity("guard", 10, 5, GridEntityType.ENEMY, "enemy_guard", maxHp = 2))
        w.entities.add(GridEntity("barrel", 3, 4, GridEntityType.DESTRUCTIBLE, "barrel", maxHp = 1))
    }
}
val townScene = remember(tileset, playerSprite, townBg) {
    WorldScene(townWorld, tileset, playerSprite, background = townBg)
}
townScene.spriteMap = spriteMap  // assign each recomposition
townScene.atmosphere = SceneAtmosphere.TAVERN
```

---

## Dialogue (copy this pattern)

```kotlin
// All dialogue is static lists — write the script first, wire later
private val INN_LINES = listOf(
    DialogueLine("Innkeeper", "Welcome, traveler."),
    DialogueLine("Hero", "How much for a room?"),
    DialogueLine("Innkeeper", "Everything you have."),
    DialogueLine("Innkeeper", "That's our policy."),
)

// Wire: NPC interaction → set dialogue → set phase
"innkeeper" -> {
    dialogueLines = INN_LINES
    dialogueIndex = 0
    phase = GamePhase.NPC_DIALOGUE
}
// Advance: last line → transition back to exploration
GamePhase.NPC_DIALOGUE -> phase = GamePhase.EXPLORE_TOWN
```

---

## Combat Routing (copy this pattern)

```kotlin
// maxHp controls routing — no separate enum needed
GridEntity("boss", 5, 5, GridEntityType.ENEMY, "boss_sprite")           // maxHp = -1 → CombatEngine
GridEntity("guard", 8, 3, GridEntityType.ENEMY, "guard_sprite", maxHp = 3)  // maxHp > 0 → action combat
```

---

## Bark System (copy this pattern)

```kotlin
// In gameplay code — fire events, don't write text
fireAndFlash(BarkEvent.HERO_ENTERS_INN)

// In BarkRegistry — write text, don't touch gameplay
BarkEvent.HERO_ENTERS_INN to BarkDefinition(
    reaction = QuestbookReaction("QUEST NOTED: FIND A ROOM"),
    audioKey = "bark/hero/enters_inn.wav"
)
```

---

## Asset Checklist (per scene)

- [ ] Background PNG (exported from Tiled, W×H where W = mapWidth×48, H = mapHeight×48)
- [ ] NPC sprite PNGs (48×48px each)
- [ ] Enemy sprite PNGs (48×48px each)
- [ ] Bark audio WAV files (1–3 seconds each)
- [ ] Register all PNGs in `composeResources/drawable/`
- [ ] Register all WAVs in `composeResources/files/bark/<speaker>/`
- [ ] Add to `AssetManifest.kt`
- [ ] Add to `spriteMap` in SliceContent
- [ ] Add `imageResource()` calls in SliceContent

---

## Kiro Session Prompt

```
Write unit tests for the combat system in core/src/commonTest/kotlin/.
Test: CombatEngine resolves turn correctly, BossController phases transition,
Inventory buy/use, GridWorld requestAttack kills enemies with 0 HP.
Follow the test patterns in core/src/desktopTest/kotlin/.
```

---

## Release Checklist

- [ ] All `when(phase)` blocks are exhaustive (build fails if not)
- [ ] `./gradlew :core:desktopTest` passes
- [ ] `./gradlew :composeApp:compileKotlinDesktop` passes  
- [ ] All game phases reachable from TITLE_SCREEN
- [ ] GAME_OVER phase exists and wires to onRestart
- [ ] i18n catalog has English (minimum)
- [ ] All sprite keys in entities match keys in spriteMap
- [ ] startingGold value is intentionally uncomfortable

---

## Time Budget (3-week estimate)

| Week | Focus | Claude | Kiro |
|---|---|---|---|
| 1 | Foundation | Game concept → state machine → core systems → UI skeleton | Unit tests, architecture docs |
| 2 | Content | Dialogue, worlds, barks, art integration | More tests, QA scripts |
| 3 | Polish | Action combat, destructibles, items, i18n, atmosphere | Performance review, edge case tests |

**Key shortcuts:**
- Write all dialogue first (it's design, not code)
- Pre-render all scene backgrounds from Tiled (artists work in parallel)
- Use `maxHp == -1` sentinel to defer boss fights until Week 3
- Add atmosphere layer last — it's cosmetic and always works

---

*Generated from GAIME development experience. See `docs/DEVELOPMENT-CHRONICLE.md` for full detail.*
