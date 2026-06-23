# Bark Trigger Table

**Technical Mapping for Implementation**

---

## Design Rule

The Questbook is NOT a random generator.
It is always understandably wrong.
The humor comes from the system interpreting consistently, bureaucratically, and WRONG -- not from arbitrary things happening.

Every bark is a stable enum key. No bark may uncontrollably change global world logic. Every trigger needs Scope, Cooldown, and Failsafe.

---

## Bark Types

| Type | Description |
|------|-------------|
| `SAFE_BARK` | Atmosphere only, no mechanical effect |
| `TRIGGER_BARK` | Triggers a clear, deterministic effect |
| `PRESSURE_BARK` | Increases Quest Pressure |
| `UTILITY_BARK` | Consciously used by player for puzzle/progress |
| `DANGER_BARK` | Helps but creates risk (pressure increase + effect) |

---

## Quest Pressure Levels

| Level | Effects |
|-------|---------|
| LOW | Markers, small hints, harmless reactions |
| MEDIUM | Enemies, traps, false quest markers |
| HIGH | Boss/chaos reaction, contained within current map |

---

## Bark Table

| Bark Key | Character | Audio Text | Bark Type | Trigger Scope | Effect | Quest Pressure Change | Cooldown | Safe/Failsafe Behavior | Used In Slice? |
|----------|-----------|------------|-----------|---------------|--------|----------------------|----------|------------------------|----------------|
| `NIB_SMELL_TREASURE` | Nib | "Ich riech Gold... oder zumindest was Glänzendes!" | TRIGGER_BARK | Current room | Quest marker appears on nearest interactable object | None | 30s | If no valid target: marker appears on Nib herself ("Source: Self") | YES |
| `NIB_SMELL_SEWAGE` | Nib | "Bäh, das stinkt nach totem Goldfisch." | SAFE_BARK | Current room | Questbook flavor text only: "Olfactory hazard documented" | None | 15s | No effect if already triggered this room | NO |
| `NIB_IT_WASNT_ME` | Nib | "Ich war das nicht! Gar nicht! Nie!" | PRESSURE_BARK | Current map | Questbook files incident report, false quest marker spawns | LOW -> MEDIUM | 45s | If already at HIGH: flavor text only, no additional markers | YES |
| `NIB_SHORTCUT` | Nib | "Da muss es nen Abkürzung geben..." | TRIGGER_BARK | Current room | Highlights nearest hidden path or interactable wall | None | 60s | If no hidden path exists: marker points to entrance ("Shortcut: Go Back") | NO |
| `NIB_POCKET_CHECK` | Nib | "Mal schauen was die so dabeihaben..." | DANGER_BARK | Current room | Reveals item drops on enemies but alerts nearest enemy | None -> +1 step | 30s | If no enemies present: Questbook notes "No pockets found in vicinity" | NO |
| `NIB_INNOCENT_WHISTLE` | Nib | "*pfeift unschuldig*" | SAFE_BARK | Current room | Questbook notes: "Ambient noise: classified as non-threatening" | None | 10s | Always safe, no mechanical effect | NO |
| `BRUGG_ATTACK` | Brugg | "ATTACKE!" | UTILITY_BARK | Current room | Destroys nearest breakable obstacle / deals double damage to destructibles | None | 20s | If nothing breakable: Brugg punches air, Questbook notes "Aggression: Unfocused" | YES |
| `BRUGG_FALL_BACK` | Brugg | "RÜCKZUG! ...oder so." | TRIGGER_BARK | Current room | Party movement speed +50% for 5 seconds | None | 45s | If already at max speed: no stack, flavor text only | NO |
| `BRUGG_THAT_WASNT_SO_BAD` | Brugg | "Halb so wild." | SAFE_BARK | Current room | Questbook flavor text: structural assessment filed | None | 30s | Always safe, no mechanical effect | YES |
| `BRUGG_WHAT_ARE_YOUR_ORDERS` | Brugg | "Was sind die Befehle?" | PRESSURE_BARK | Current map | Questbook generates a new sub-objective marker | None -> +1 step | 60s | If at HIGH pressure: Questbook responds "Orders: Survive" (no new marker) | NO |
| `BRUGG_HUNGRY` | Brugg | "Ich hab Hunger." | SAFE_BARK | Current room | Questbook notes: "Provision request filed (Priority: Low)" | None | 20s | Always safe, no mechanical effect | NO |
| `BRUGG_PROTECT` | Brugg | "Niemand kommt hier durch!" | DANGER_BARK | Current room | Brugg gains temporary shield (3 hits) but all enemies aggro to Brugg | None -> +1 step | 60s | If no enemies present: shield applies, no aggro, Questbook notes "Perimeter: Secure" | NO |
| `VELLUM_CALLS_FOR_FLAME` | Vellum | "Ignis! Kontrollierte Verbrennung!" | UTILITY_BARK | Current room | Burns flammable objects/enemies weak to fire | None | 25s | If nothing flammable: small flame VFX on ground, Questbook: "Burn permit: No valid target" | YES |
| `VELLUM_CALLS_FOR_ICE` | Vellum | "Glacies! Kristallstruktur, bitte!" | UTILITY_BARK | Current room | Freezes water surfaces (creates walkable path) or slows enemies | None | 25s | If no valid target: frost VFX on ground, Questbook: "Cooling request: Denied (no substrate)" | NO |
| `VELLUM_KNOWLEDGE_IS_THE_ANSWER` | Vellum | "Wissen ist immer die Antwort." | TRIGGER_BARK | Current room | Highlights interactive/puzzle elements in room | None | 30s | If no puzzles present: Questbook: "Research complete: No findings" | YES |
| `VELLUM_THIS_CHANGES_EVERYTHING` | Vellum | "Das... das ändert alles!" | PRESSURE_BARK | Current map | Questbook registers a major misinterpretation (context-dependent) | MEDIUM -> HIGH | 90s | If already at HIGH: flavor text only ("Amendment: Already on record") | YES |
| `VELLUM_TECHNICALLY_CORRECT` | Vellum | "Technisch gesehen ist das korrekt." | SAFE_BARK | Current room | Questbook: "Accuracy confirmed. Filed under: Pedantry" | None | 15s | Always safe, no mechanical effect | NO |
| `VELLUM_READ_THE_FINE_PRINT` | Vellum | "Steht alles im Kleingedruckten!" | TRIGGER_BARK | Current room | Reveals hidden text on signs/objects in current room | None | 40s | If no readable objects: Questbook: "No fine print located (font size: adequate)" | NO |
| `NIB_TREASURE_FOUND` | Nib | "JACKPOT! ...oh. Oder auch nicht." | DANGER_BARK | Current room | Opens nearest container but may trigger trap | None -> +1 step | 30s | If no containers: Questbook: "Treasure audit: Inconclusive" | NO |
| `BRUGG_SMASH` | Brugg | "KAPUTT!" | DANGER_BARK | Current room | Destroys ALL breakables in room (including potentially helpful ones) | None -> +1 step | 60s | If nothing to destroy: ground shake VFX, Questbook: "Destruction permit: Void (nothing to demolish)" | NO |

---

## Implementation Notes

1. **Enum Registration:** Every bark key maps to a `BarkEvent` enum value in code: `BarkEvent.NIB_SMELL_TREASURE`, `BarkEvent.BRUGG_ATTACK`, etc.

2. **Trigger Pipeline:**
   ```
   BarkEvent fired -> QuestbookProcessor receives key -> Lookup reaction table -> Apply effect (scoped to current room/map) -> Update Quest Pressure if applicable -> Display UI feedback
   ```

3. **Cooldown Enforcement:** Cooldown is per-bark-key, not per-type. A bark on cooldown simply does not fire (the audio may still play for atmosphere, but no BarkEvent is emitted to the Questbook).

4. **Failsafe Guarantee:** Every bark MUST have a defined failsafe for when its target condition is not met. No bark may produce an error state or undefined behavior.

5. **Scope Enforcement:** "Current room" means the visible screen area. "Current map" means the loaded map file. No bark effect persists beyond map transition except explicitly story-locked events (party name registration in slice).

6. **Quest Pressure Caps:** Pressure cannot exceed HIGH. At HIGH, PRESSURE_BARKs produce only flavor text. Pressure resets on map transition.

7. **Slice Coverage:** The vertical slice uses 7 unique bark keys across all three characters, covering SAFE_BARK, TRIGGER_BARK, PRESSURE_BARK, and UTILITY_BARK types. DANGER_BARK is defined but not required for slice completion.
