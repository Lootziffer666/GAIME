# GAIME — Development Chronicle
## "Quest Accepted: Unfortunately"

> A living record of how this game was built, decision by decision, from blank repo to playable vertical slice. Intended as a blueprint for building future games in shorter time using Claude Code + Kiro (AWS AI assistant).

---

## Project at a Glance

| Property | Value |
|---|---|
| Engine | Compose Multiplatform (Android + Desktop) |
| Language | Kotlin Multiplatform |
| Core module | `:core` — pure Kotlin, no Compose, fully testable |
| UI module | `:composeApp` — Compose Multiplatform rendering |
| Planned migration | KorGE `:game` module for advanced rendering |
| Art style | HD-2D (pixel sprites, pre-rendered backgrounds, cinematic atmosphere) |
| Genre | Top-down action RPG with satirical bureaucratic tone |
| Build time | ~80 commits over approximately 3 weeks |
| AI assistants | Claude Code (Anthropic) + Kiro (AWS) |

---

## Technology Stack

```
composeApp/
  src/
    commonMain/        ← Compose UI, screens, game loop
    androidMain/       ← Android entry point, audio player
    desktopMain/       ← Desktop entry point, gamepad poller
core/
  src/
    commonMain/        ← Pure Kotlin game logic (testable without UI)
      rpg/
        combat/        ← CombatEngine, Combatant, BossController
        world/         ← GridWorld, GridEntity, TileMap, Camera
        bark/          ← Bark event system (reactive narrative)
        items/         ← Item, ItemCatalog, Inventory
        i18n/          ← Localization (9 languages)
        questbook/     ← QuestPressure, reaction system
        ...
```

**Key framework decisions:**
- Compose Multiplatform chosen over KorGE for faster initial iteration (Compose is already known by the dev team; KorGE migration is planned for Phase 5).
- `:core` module extracted early to enable unit testing without spinning up the UI. This paid off immediately: CombatEngine, BossController, and bark logic were all test-driven.
- Assets live in `composeApp/src/commonMain/composeResources/` — Compose Resources handles cross-platform loading automatically.

---

## Phase-by-Phase Chronicle

### Phase 0 — Project Bootstrap (Commits: `9cd1c21` → `3b447e40`)

**What was built:**
- KMP project foundation with `composeApp` module
- Compose Multiplatform canvas engine (`Scene` interface, `GameCanvas` composable)
- State machine scaffolding
- Two demo scenes: `LetterSwarm`, `SpriteIdle`
- Android lifecycle wiring + Desktop always-on-top window

**Key pattern established:** The `Scene` interface — `update(deltaTime)` + `draw(DrawScope)` — became the rendering contract for all subsequent scenes including `WorldScene`. This was never changed.

```kotlin
interface Scene {
    val name: String
    fun update(deltaTime: Float)
    fun draw(drawScope: DrawScope)
    fun onPointerMove(x: Float, y: Float)
    fun onPointerExit()
}
```

---

### Phase 1 — Core RPG Systems (Commits: `54d83dd4` → `0f38509f`)

**What was built:**
- `CombatEngine`: turn-based combat with party vs. enemy list
- `Combatant`: HP, attack, heal, shield, `takeDamage()`
- `EnemyArchetype`: factory for enemy instances
- `BossController` / `BossControllerInterface`: boss AI with phase transitions
- `SlicePhase` enum: single enum tracks the entire game state machine
- `SliceDirector`: orchestrates narrative flow, bark firing, room transitions
- `BarkRegistry` / `BarkEventBus`: event-driven reactive bark system
- Bark audio system: platform audio player with WAV file playback
- Sludge Blob: ranged slow attack style, random targeting
- QuestPressure: LOW / MEDIUM / HIGH narrative tension tracker

**Architecture anchor:** `SlicePhase` as exhaustive enum is the central state machine. Every phase transition is a `phase = SlicePhase.X` assignment. This keeps all state in one place and avoids scattered boolean flags.

**Kiro's contribution:** Initial task system (`task.json` files), semantic review framework for feature completeness checks, combat unit tests.

---

### Phase 2 — NPC Dialogue + Voice (Commits: `43da42e8` → `637bb06e`)

**What was built:**
- NPC tavern dialogue system
- `DialogueLine(speaker, text, audioFile?)` data class
- `DialogueOverlay` composable: speaker portrait + text box
- Bark audio integration: each dialogue line can reference a WAV
- 102 voice line entries across all bark registries
- Expanded voice: barkeep, guard, citizen, merchant, guildmaster, mage, priest

**Pattern:** Dialogue is stored as `List<DialogueLine>`. Phase transitions on last line. This pattern repeats identically for every NPC and cutscene — very mechanical to extend.

```kotlin
"merchant" -> {
    dialogueLines = CHAPTER2_MERCHANT_LINES
    dialogueIndex = 0
    phase = SlicePhase.CHAPTER2_MARKET_NPC
}
// In advanceDialogue():
SlicePhase.CHAPTER2_MARKET_NPC -> phase = SlicePhase.CHAPTER2_MARKET
```

---

### Phase 3 — HD Art + Worlds (Commits: `b1d14ae8` → `200e2916`)

**What was built:**
- Full swap from placeholder art to HD sprites (48px tiles, 768×432 scene backgrounds)
- `WorldScene`: tile-based world renderer using `ImageBitmap` atlas blitting
- `GridWorld` / `GridActor`: deterministic tile-step movement with interpolation
- `Camera`: follows player, clamps to map bounds
- `TileMap` / `TileMapParser`: Tiled-compatible map format
- `BakedMaps` / `GameMaps`: code-registered maps for all locations
- Pre-rendered HD backgrounds for tavern, sewer, boss arena, market, forest, all world connectors
- `SceneAtmosphere`: TAVERN, SEWER, CHAPEL, GUILD_HALL, MARKET, FOREST, BRIDGE lighting presets
- `RenderMetrics`: `LEGACY_TILE` (16px), `SCREEN_TILE` (48px) constants

**HD-2D rendering pipeline:**
1. Background: pre-rendered PNG baked from Tiled map, scaled to world size
2. Entities: sprites blitted at tile positions (48×48px each)
3. Atmosphere: Canvas-drawn overlay (Brush gradients, particle motes, color grade, vignette)
4. UI: Compose widgets over the canvas

**Asset pipeline (from `docs/ASSET_PIPELINE.md`):**
- Artist delivers: `aseprite` (sprites), `.tmx` (Tiled maps), `.wav` (audio)
- Claude/Kiro converts to: `composeResources/drawable/*.png` + `composeResources/files/bark/**/*.wav`
- Registration: `AssetManifest.kt` (compile-time enum of all assets)

---

### Phase 3.5 — World Expansion: 6 New Scenes (Commits: `5573ba7d` → `2f419f62`)

**What was built:**
- 6 new world connector scenes: Heroes Home Exterior, Guildhall, Chapel Exterior, Temple Exterior, Glassblowers District, Stone Bridge
- 23 new dialogue scenes (priest, mage, elder, bridge citizen, Medusa boss intro/outro)
- Chapter 3 boss Medusa (placeholder — no fight yet)
- Gamepad/controller support: `ControllerPoller` interface, `LinuxJoystickPoller` (Linux `/dev/input/js*`)
- Full i18n: German localization + 8 other languages (EN, DE, ES, FR, IT, PT, RU, ZH, JA)
- Language picker on title screen
- `Localizer.localize(text, locale)` via static translation catalog

**Kiro's contribution:** `.kiro/steering/` directory established as architecture decision log. `rendering-engine.md` locked Compose Multiplatform as the renderer.

---

### Phase 4 — Action Combat Layer 1 (Commits: `5e743716`)

**What was built:**
- Hybrid combat system: regular enemies (rats, wolves) → action RPG on tile grid; boss enemies → CombatEngine (unchanged)
- `GridEntity.maxHp`: `-1` = boss/NPC (handled by CombatEngine), `> 0` = action-combat HP
- `GridEntity.solid`: `false` = walkable destructible (tall grass), `true` = blocking
- `GridWorld.requestAttack()`: hits tile in front of player, damages enemies/destructibles
- Z key / gamepad button 1 = attack
- E key / gamepad button 0 = interact (NPC dialogue)
- Removed 3 dead `SlicePhase` combat cases from all transition handlers

**Architecture decision (`.kiro/steering/action-combat.md`):**
> Regular enemies die via real-time Z-key attack on the tile grid. Boss encounters keep the existing tactical CombatEngine on a separate screen. The sentinel `GridEntity.maxHp == -1` routes to CombatEngine; `maxHp > 0` routes to action combat.

**Enemy HP table:**
| Enemy | HP | Notes |
|---|---|---|
| Sewer rat | 1 | One-hit kill |
| Sewer blob | 2 | Two hits |
| Forest wolf | 2 | Two hits |
| Rat Accountant | -1 | Boss → CombatEngine |
| Tax Collector Badger | -1 | Boss → CombatEngine |

---

### Phase 4.1 — Destructibles (Commits: `c14add37`)

**What was built:**
- 4 destructible types: `"grass_tall"` (non-solid, walk-through), `"crate"`, `"barrel"`, `"wall_cracked"` (solid, require attack)
- All drawn via Compose Canvas primitives in `WorldScene.drawDestructible()` — no sprites needed
- Damage state visible: red tint overlay when `hp < maxHp`; cracked walls show wider crack
- Placed in: sewer, boss room (atmosphere), market, forest, temple exterior

**Pattern: procedural Canvas drawing for props:**
```kotlin
// Saves on art assets; gives designer-controllable damage states
if (entity.type == GridEntityType.DESTRUCTIBLE) {
    drawDestructible(drawScope, entity, ex, ey)
} else {
    val sprite = spriteMap[entity.sprite] ?: continue
    drawSprite(drawScope, sprite, ex, ey)
}
```

---

### Phase 4.2 — Item System + Shop (Commits: this session)

**What was built:**
- `core/rpg/items/Item.kt`: data class (id, name, description, type, price, effectValue)
- `core/rpg/items/ItemCatalog.kt`: 8 items (4 potions, 4 weapons) with in-world-tone descriptions
- `core/rpg/items/Inventory.kt`: gold tracking, potion stacks, weapon equip (raises `party[*].attackPower`)
- `SlicePhase.SHOP`: new phase for merchant shop overlay
- `ShopView` composable: dark RPG shop UI, shows items with prices, buy buttons, equipped badges
- Merchant NPC now opens shop directly (E key near merchant in market)
- `I` key uses cheapest available potion on Nib during exploration
- Esc key closes shop
- Starting gold: 50g (barely enough for one Minor Potion — intentionally uncomfortable)

**Item catalog:**

| Item | Type | Price | Effect |
|---|---|---|---|
| Minor Potion | POTION | 30g | +5 HP |
| Potion | POTION | 80g | +15 HP |
| Grand Potion | POTION | 200g | +30 HP |
| Elixir | POTION | 500g | Full restore |
| Short Sword | WEAPON | 120g | Attack +2 |
| Staff of Marginally Better Lighting | WEAPON | 200g | Attack +3 |
| Longsword | WEAPON | 350g | Attack +4 |
| Warhammer | WEAPON | 500g | Attack +5 |

**Gold economy:** Starting 50g covers exactly one Minor Potion. Players must survive action combat and then return to buy upgrades. Elixir and Warhammer are both 500g — the "late game" tier that requires real resource management.

---

## Architecture Reference

### State Machine

The entire game is a single `SlicePhase` enum with ~40 values. All transitions are explicit assignments (`phase = SlicePhase.X`). No hidden state.

```
TITLE_SCREEN
  └─ INTRO_CUTSCENE → TAVERN
       └─ NPC_DIALOGUE (overlay) → TAVERN
       └─ FALLING_CUTSCENE → SEWER
            └─ BOSS_ROOM → BOSS_COMBAT → POST_BOSS → QUESTBOOK_FULL
                 └─ RETURN_CUTSCENE → CHAPTER2_MARKET_NPC → CHAPTER2_MARKET
                      └─ SHOP (overlay) → CHAPTER2_MARKET
                      └─ CHAPTER2_FOREST → CHAPTER2_SHRINE
                           └─ CHAPTER2_BOSS_INTRO → CHAPTER2_BOSS_COMBAT → CHAPTER2_POST_BOSS
                                └─ CHAPTER2_QUESTBOOK_PAGE2 → CHAPTER2_RETURN → VICTORY
```

World connectors (`HEROES_HOME_EXT`, `CHAPTER2_GUILDHALL`, etc.) are side branches accessible from multiple main phases.

### Bark System

Narrative reactions are event-driven, not scripted. `BarkEvent` enum values are fired at gameplay moments. `BarkRegistry` maps events to `BarkDefinition` (text, audio file, questbook reaction). `SliceDirector.fireBark(event)` evaluates context and returns a `BarkOutcome`.

This decouples narrative writing from gameplay code — designers add bark lines in `BarkRegistry` without touching `SliceScreen.kt`.

### Combat Routing

```kotlin
GridEntity.maxHp == -1  →  Boss entity → approach queues CombatEngine transition
GridEntity.maxHp  > 0   →  Action entity → Z key deals 1 HP damage, entity removed at 0
GridEntity.type == NPC  →  Talk entity → E key opens dialogue
GridEntity.type == DESTRUCTIBLE, solid = false  →  Walk through to destroy (tall grass)
GridEntity.type == DESTRUCTIBLE, solid = true   →  Z key to break (crates, barrels, walls)
```

### World Scenes

Each location has:
1. `GridWorld` — map + entities (constructed once via `remember { }`, persists HP)
2. `WorldScene` — renderer (background image + entity sprites + atmosphere)
3. Scene callbacks: `onTrigger` (tile triggers), `onEntityInteraction` (E key on NPC)

### Input System

```
Keyboard:  W/A/S/D or arrow keys → move
           E → interact (NPC/trigger)
           Z → attack
           I → use potion
           Esc → close shop
           
Gamepad:   Left stick / D-pad → move
           Button 0 (South face) → interact
           Button 1 (West face) → attack
```

### i18n

All dialogue lines go through `Localizer.localize(text, locale)`. The catalog is in `DialogueTranslations.kt`. `GameLocale.current` is a global that the title screen language picker writes to. Currently 9 languages; adding a new one requires:
1. Adding `Locale.XX` to the enum
2. Adding `DialogueTranslations.XX` map
3. Adding the language to the title screen picker

---

## Reusable Patterns for Future Games

### 1. The `SlicePhase` Pattern
Use a single exhaustive enum for the game state machine. All renders and transitions are `when(phase)` expressions. Kotlin enforces exhaustiveness — the compiler catches missing cases.

### 2. The `GridWorld` + `WorldScene` Pattern
Split world logic (pure Kotlin, testable) from rendering (Compose Canvas). `GridWorld` drives movement and physics; `WorldScene` is a pure renderer that reads `GridWorld` state each frame.

### 3. The Bark System
Write gameplay code that fires `BarkEvent` values. Write narrative text in `BarkRegistry`. The two never directly reference each other. Designer adds new lines without touching gameplay code.

### 4. Pre-rendered Background + Sprite Overlay
Artist delivers `background.png` from Tiled. Claude/Kiro registers it in `AssetManifest.kt`. `WorldScene` scales background to world size, blits sprites on top. No need for a full tile renderer once backgrounds are baked.

### 5. `maxHp == -1` Sentinel
Use a single field value as a routing sentinel between combat systems. Negative = CombatEngine; positive = action combat. Avoids adding a separate boolean or enum field.

### 6. `Inventory` in `:core`
Keep economy logic (gold, item stacks, equip/use) in the pure Kotlin module. Compose only reads values for display and calls methods for transactions. Makes the economy unit-testable.

### 7. Procedural Prop Rendering
For environmental props that don't need animation, draw them with Compose Canvas primitives (`drawRect`, `drawLine`) in a dedicated function. Saves on art assets; damage state is a single boolean parameter.

---

## Asset Pipeline (Summary)

Full details in `docs/ASSET_PIPELINE.md`. Short version:

```
Artist delivers:
  .aseprite → export to .png → composeResources/drawable/
  .tmx (Tiled) → export to .png background
  .wav → composeResources/files/bark/<speaker>/<key>.wav

Claude/Kiro registers:
  drawable → gaime.resources.Res.drawable.X (auto-generated by Compose Resources)
  audio → hardcoded path string in BarkDefinition("bark/speaker/key.wav")
  sprites → AssetManifest.kt enum entry
  entity → GridEntity(id, tileX, tileY, type, spriteKey)
```

---

## Automation Recipe for Future Games

This is the process that worked. A future Claude+Kiro session can follow this exactly:

### Week 1: Foundation
1. Clone this repo as template (or start from the KMP scaffold in `feat/korge-game-module`)
2. Write the game concept doc first (`docs/GAME_CONCEPT_LOCK.md` pattern)
3. Lock the art style in `.kiro/steering/rendering-engine.md`
4. Implement the state machine enum (`SlicePhase` equivalent) — list all narrative beats upfront
5. Build `CombatEngine` equivalent if the game has combat

### Week 2: Content
6. Write all dialogue upfront in `private val X_LINES = listOf(DialogueLine(...))` — treat it like a script
7. Build worlds as `GridWorld` instances with entity lists
8. Integrate bark/reaction system for narrative reactivity
9. Deliver art assets and register them in `AssetManifest.kt`

### Week 3: Polish
10. Add atmosphere overlays (`drawAtmosphere()` equivalents)
11. Add action combat, destructibles, items in that order (Layer 1 → Layer 2 → Layer 3)
12. i18n: add translation catalog, wire `Localizer`, add language picker
13. Add gamepad support last (input abstraction is already in place)

### Key shortcuts:
- Use `when(phase)` exhaustiveness for correctness — the compiler is the QA tool
- Use `remember { }` for world state — HP carries across scene transitions automatically  
- Use `key(resetKey) { SliceContent(...) }` for instant full game reset
- Use `version` int that increments on state changes to drive Compose recomposition
- Use `:core` module for all game logic — lets Kiro write unit tests while Claude writes UI

---

## Known Gaps (as of this chronicle)

These features are designed but not yet implemented:

| Feature | Status | Notes |
|---|---|---|
| Party sprites (Brugg + Vellum follow Nib) | Design done | Need 3-sprite formation on GridWorld |
| Language selection persistence | Not started | `SharedPreferences` / `UserDefaults` |
| ES/FR/IT/PT/RU/ZH/JA full translations | ~40% | German (DE) is complete |
| Sprite animation system | Not started | Currently static sprites only |
| KorGE `:game` module migration | Not started | See `docs/HD48_MIGRATION.md` |
| Co-op multiplayer | Explicitly deferred | "Lass Coop erstmal außen vor" |
| Inventory during combat (potion use mid-fight) | Not started | I key only works during exploration |
| Gold from combat drops | Not started | All gold currently comes from starting 50g |
| Chapter 3 Medusa fight | Not started | Intro dialogue exists, no CombatEngine fight |

---

## Contributors

- **Claude Code (Anthropic)** — Architecture, Compose UI, combat system, world system, bark system, action combat, destructibles, item system, i18n, atmosphere rendering, gamepad support
- **Kiro (AWS)** — Task system, semantic review framework, unit tests, `.kiro/steering/` architecture docs, Chapter 2 combat data layer, bash scripts
- **Lootziffer666** — Game concept, art direction, dialogue writing, design decisions, QA

---

*Last updated: 2026-06-27. Covers commits `a96750da` through `c14add37` and the item system session.*
