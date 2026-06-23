# VERTICAL SLICE: Quest Accepted: Unfortunately

**Target Duration:** 10-15 minutes of gameplay
**Scope:** Prologue + Chapter 1 only
**Goal:** Prove that a played voice bark can visibly and traceably trigger a Questbook reaction.

---

## Play Sequence

### 1. Tavern: The Limping Cockatrice

**Location:** Single-room tavern interior map

**Setup:**
- Party (Nib, Brugg, Vellum) is seated at a table
- The broken Questbook sits on the table, glowing faintly
- Barkeep NPC stands behind counter
- One patron NPC sits in corner

**Events:**
1. Short text-box intro establishes the party has been "officially registered" by accident
2. Player can move around the tavern, interact with Barkeep (1 dialogue) and Patron (1 dialogue)
3. Nib delivers a bark: `BarkEvent.NIB_SMELL_TREASURE` (automatic trigger on approaching the cellar door)
4. **QUESTBOOK REACTION:** The Questbook glows, text appears: "Official Quest Registered: Locate subterranean valuables (Priority: Mandatory)"
5. A quest marker appears on the cellar door
6. Player interacts with cellar door to proceed

**Questbook Moment:** First visible demonstration of BarkEvent triggering a Questbook reaction. UI overlay shows the Questbook text briefly.

---

### 2. Fall into the Sewers

**Location:** Transition event (uses sewer map entry point)

**Events:**
1. The cellar floor collapses (scripted, not player-controlled)
2. Party falls into the sewers below
3. Brugg delivers a bark: `BarkEvent.BRUGG_THAT_WASNT_SO_BAD` (automatic on landing)
4. **QUESTBOOK REACTION:** "Amendment Filed: Structural Assessment of Municipal Underground -- Status: Satisfactory"
5. Brief text-box: Vellum complains about the smell

**Purpose:** Transition into dungeon map. Second Questbook demonstration.

---

### 3. Sewers of Bad Decisions

**Location:** Sewer dungeon map -- linear corridor section

**Events:**
1. Player navigates a short linear sewer corridor
2. Two Sewer Rat enemies block the path (basic combat encounter)
3. After combat, Nib delivers: `BarkEvent.NIB_IT_WASNT_ME` (automatic post-combat)
4. **QUESTBOOK REACTION:** "Incident Report Filed: Denial of Involvement (Case #0001) -- Noted for Records"
5. Quest Pressure rises to MEDIUM
6. A false quest marker appears deeper in the sewer ("Investigate Reported Innocence")

**Combat:** Simple top-down action combat. Attack button, dodge. No combos, no specials in slice.

---

### 4. Mini-Dungeon

**Location:** Sewer dungeon map -- branching room section

**Events:**
1. Player enters a slightly larger room with two exits (one blocked by rubble)
2. Three Sewer Rat enemies + one Sludge Blob enemy
3. Vellum delivers: `BarkEvent.VELLUM_KNOWLEDGE_IS_THE_ANSWER` (automatic on seeing blocked path)
4. **QUESTBOOK REACTION:** "Academic Grant Approved: Research Into Obstruction Removal (Budget: 0 Gold)"
5. The rubble shakes but does not clear (flavor only at MEDIUM pressure)
6. Brugg delivers: `BarkEvent.BRUGG_ATTACK` (player-triggered utility bark to break rubble)
7. **QUESTBOOK REACTION:** "Demolition Permit Issued: Immediate Effect"
8. Rubble clears, path opens

**Purpose:** Demonstrate UTILITY_BARK usage (player intentionally triggers bark for progress).

---

### 5. Boss: The Rat Accountant

**Location:** Sewer dungeon map -- boss room (final room)

**Setup:**
- Large rat wearing tiny spectacles, sitting behind a desk made of garbage
- The Rat Accountant is NOT a combat boss in the traditional sense
- It is a bureaucratic boss: it files counter-quests

**Boss Phases:**

| Phase | HP Threshold | Behavior |
|-------|-------------|----------|
| Phase 1 | 100%-50% | Attacks with thrown papers. Standard dodge-and-hit pattern. Summons 2 Sewer Rats as adds. |
| Phase 2 | 50%-25% | Starts "filing objections" -- quest pressure rises to HIGH. False quest markers spam the room. Player must ignore false markers and keep attacking. |
| Phase 3 | 25%-0% | Desperate: throws its desk. Single large telegraphed attack. One hit to finish. |

**Barks during boss:**
- `BarkEvent.NIB_SMELL_TREASURE` fires when desk is thrown (Phase 3) -- Questbook notes "Valuables Located: Filing Cabinet (Contents: Rats)"
- `BarkEvent.VELLUM_CALLS_FOR_FLAME` can be player-triggered to burn paper adds (UTILITY_BARK)

---

### 6. Finding the First Questbook Page

**Location:** Sewer dungeon map -- boss room (post-combat)

**Events:**
1. Rat Accountant defeated, drops a glowing page
2. Player picks up the page
3. Questbook UI opens in full-screen overlay for the first time
4. The page reads: "Official Registry of Heroes, Page 1: [Party Name Pending]"
5. Vellum delivers: `BarkEvent.VELLUM_THIS_CHANGES_EVERYTHING` (automatic)
6. **QUESTBOOK REACTION:** "Name Registration Complete: 'Everything Changes' -- Official Party Name Locked"
7. Brief comedic beat as party reacts to their terrible official name

**Purpose:** Payoff moment. The Questbook's misinterpretation becomes permanent (party name). Sets up the premise for the full game.

---

### 7. Return / Conclusion

**Location:** Tavern map (return)

**Events:**
1. Fade-to-black transition back to tavern
2. Barkeep reacts to party's return: "Back already? Smells like sewer."
3. Questbook sits on table, now with one page visible
4. Final text box: "Quest Pressure: Reset. New quests pending. The Questbook is always listening."
5. **END OF SLICE**

---

## Required Assets

### Maps

| Map | Description | Dimensions |
|-----|-------------|-----------|
| `map_tavern_limping_cockatrice` | Single-room tavern interior. Counter, tables, cellar door. | Small (approx 20x15 tiles) |
| `map_sewer_bad_decisions` | Linear corridor + branching room + boss room. Connected as one map. | Medium (approx 40x30 tiles) |

### NPCs

| NPC | Location | Interaction |
|-----|----------|-------------|
| Barkeep | Tavern, behind counter | 1 dialogue (pre-sewer), 1 dialogue (post-sewer) |
| Patron | Tavern, corner table | 1 dialogue (flavor text only) |

### Enemies

| Enemy | Location | Behavior |
|-------|----------|----------|
| Sewer Rat | Sewer corridor + mini-dungeon + boss room | Basic melee, low HP, moves toward player |
| Sludge Blob | Mini-dungeon room | Slow, ranged spit attack, medium HP |
| The Rat Accountant (Boss) | Boss room | 3 phases (see above) |

### Items

| Item | Location | Purpose |
|------|----------|---------|
| Questbook Page 1 | Boss room (post-combat drop) | Story progression, triggers name registration |

No other items required. No potions, no equipment, no loot system in slice.

### Required Barks (Used in Slice)

| Bark Key | Trigger | Section |
|----------|---------|---------|
| `NIB_SMELL_TREASURE` | Auto: approaching cellar door / Phase 3 desk throw | 1, 5 |
| `BRUGG_THAT_WASNT_SO_BAD` | Auto: landing in sewer | 2 |
| `NIB_IT_WASNT_ME` | Auto: post-combat corridor | 3 |
| `VELLUM_KNOWLEDGE_IS_THE_ANSWER` | Auto: seeing blocked path | 4 |
| `BRUGG_ATTACK` | Player-triggered: break rubble | 4 |
| `VELLUM_CALLS_FOR_FLAME` | Player-triggered: burn papers in boss fight | 5 |
| `VELLUM_THIS_CHANGES_EVERYTHING` | Auto: picking up Questbook page | 6 |

### Required UI States

| State | Description |
|-------|-------------|
| `UI_QUESTBOOK_FLASH` | Brief overlay showing new Questbook text (1-2 seconds, auto-dismiss) |
| `UI_QUESTBOOK_FULL` | Full-screen Questbook page view (player-dismissable) |
| `UI_QUEST_MARKER` | Map marker icon (appears on minimap/screen edge) |
| `UI_FALSE_QUEST_MARKER` | Visually similar to quest marker but slightly glitchy/off-color |
| `UI_QUEST_PRESSURE_INDICATOR` | Small meter showing LOW/MEDIUM/HIGH |
| `UI_DIALOGUE_BOX` | Standard text box for NPC/party dialogue |
| `UI_BOSS_HP_BAR` | Simple HP bar for Rat Accountant |

### Required Questbook Reactions

| Trigger | Questbook Text | Visible Effect |
|---------|---------------|----------------|
| `NIB_SMELL_TREASURE` (tavern) | "Official Quest Registered: Locate subterranean valuables (Priority: Mandatory)" | Quest marker on cellar door |
| `BRUGG_THAT_WASNT_SO_BAD` | "Amendment Filed: Structural Assessment of Municipal Underground -- Status: Satisfactory" | Flavor text only |
| `NIB_IT_WASNT_ME` | "Incident Report Filed: Denial of Involvement (Case #0001) -- Noted for Records" | False quest marker + pressure rise |
| `VELLUM_KNOWLEDGE_IS_THE_ANSWER` | "Academic Grant Approved: Research Into Obstruction Removal (Budget: 0 Gold)" | Rubble shakes (flavor) |
| `BRUGG_ATTACK` (rubble) | "Demolition Permit Issued: Immediate Effect" | Rubble clears |
| `VELLUM_CALLS_FOR_FLAME` (boss) | "Controlled Burn Authorization: Filed" | Paper adds burn |
| `NIB_SMELL_TREASURE` (boss) | "Valuables Located: Filing Cabinet (Contents: Rats)" | Flavor text only |
| `VELLUM_THIS_CHANGES_EVERYTHING` | "Name Registration Complete: 'Everything Changes' -- Official Party Name Locked" | Full Questbook UI opens |

---

## Win Condition

The slice is complete when:
1. The player defeats The Rat Accountant
2. The player picks up Questbook Page 1
3. The party name is registered by the Questbook
4. The player returns to the tavern

---

## Fail / Game Over Condition

- All party members reach 0 HP during any combat encounter
- Game Over screen: "Quest Status: Unresolved. The Questbook notes your failure for administrative purposes."
- Restart from last section entry point (tavern or sewer entry)

---

## Acceptance Criteria

The vertical slice is considered PROVEN when:

1. A `BarkEvent` fires during gameplay (either automatic or player-triggered)
2. The Questbook visibly reacts with text output
3. The reaction produces a visible effect (marker, pressure change, or gameplay effect)
4. The full pipeline is traceable: Bark Key -> Questbook Logic -> Output Effect
5. At least one UTILITY_BARK demonstrates player-intentional usage (rubble breaking)
6. Quest Pressure visibly escalates across the slice (LOW -> MEDIUM -> HIGH)
7. All effects remain map-local (nothing persists beyond map boundaries except the party name)
8. The boss encounter demonstrates pressure-based escalation
9. The final Questbook moment (name registration) demonstrates permanent-feeling misinterpretation
10. Total playtime is 10-15 minutes at normal pace
