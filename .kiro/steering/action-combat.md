# Steering: Hybrid Combat System

## Decision (approved by user)

GAIME uses a **hybrid combat model**:

| Enemy type | Combat style | Implementation |
|---|---|---|
| Regular mobs (rats, wolves, blobs) | Real-time action RPG | On the tile grid, in `GridWorld` |
| Boss encounters (Rat Accountant, Tax Collector Badger, Medusa) | Tactical turn-based | `CombatEngine` on a dedicated screen |

Reference: Chrono Trigger / The Legend of Zelda: A Link to the Past for the action side;
existing `CombatEngine` for the boss side.

## Architecture sentinel

`GridEntity.maxHp` encodes which system owns an enemy:
- `maxHp == -1` → NPC or boss. Movement into tile queues a `pendingEntityInteraction`
  which `SliceScreen` picks up and routes to `CombatEngine` (existing path, unchanged).
- `maxHp > 0` → Action-combat mob. Movement into tile is blocked but no interaction is
  queued. Player presses the attack button (Z key / west face gamepad button) to call
  `GridWorld.requestAttack()`, which hits the tile directly in front of `player.facing`.

## Attack mechanics (Layer 1)

- Hitbox: one tile ahead of the player in their current facing direction.
- Damage: 1 HP per swing (tunable per-enemy via `maxHp` at construction).
- Kill: entity removed from `GridWorld.entities` when `hp` reaches 0. Path opens.
- No invincibility frames, no knockback — those come in Layer 3/4.

## Standard enemy HP values

| Enemy | `maxHp` |
|---|---|
| Sewer rat | 1 (one-hit kill) |
| Forest wolf | 2 |
| Sludge blob | 2 |

## Input bindings

| Platform | Attack button |
|---|---|
| Keyboard (desktop) | Z |
| Gamepad (Linux) | Button 1 (west face: X / Square / Y) |
| Android | On-screen attack button (Layer 4) |

## What did NOT change

- Boss `GridEntity` instances keep `maxHp = -1` and still route through `CombatEngine`.
- `SlicePhase.BOSS_COMBAT` and `CHAPTER2_BOSS_COMBAT` are untouched.
- `CombatEngine`, `CombatScreen`, and all boss battle logic are untouched.
- `SEWER_COMBAT`, `MINI_DUNGEON_COMBAT`, and `CHAPTER2_FOREST_COMBAT` phases are removed
  from `SliceScreen`'s entity-interaction handler (they were the old turn-based triggers
  for regular enemies; those enemies now die via `requestAttack()` instead).
