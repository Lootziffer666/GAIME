# QUEST ACCEPTED: UNFORTUNATELY

**The Completely Official Quest of Questionable Importance**

---

## Elevator Pitch

Three misfit adventurers in the town of Stokeport are declared an official hero party by a broken magical Questbook that interprets every spoken cliche, exaggeration, and throwaway comment as a binding bureaucratic quest. The player must survive the consequences of their own party's big mouths.

---

## Tone

- Dry British-bureaucratic humor meets fantasy nonsense
- 8/16-bit top-down pixel RPG aesthetic
- The world takes itself seriously; the comedy comes from the system, not from the characters being "random"
- Think: Terry Pratchett's Ankh-Morpork as a Game Boy game

---

## Core Mechanic

The **Questbook** is a magical artifact that listens to party members' spoken barks and interprets them as official quests.

**Critical distinction:**
- Barks are internal game events: `BarkEvent.X`
- When a bark plays, the game already knows the bark key
- The Questbook reacts to **BarkEvents**, not to audio analysis
- There is NO speech recognition, NO audio parsing, NO NLP

The Questbook receives a bark key (e.g., `BarkEvent.NIB_SMELL_TREASURE`) and produces a deterministic, bureaucratic misinterpretation as a new quest entry or quest pressure change.

---

## Party

### Nib
Small, quick, morally flexible. Thief/rogue archetype. Talks too much, especially about treasure, shortcuts, and how none of this is her fault. Her barks tend to create greed-based or escape-based misinterpretations.

### Brugg
Large, loyal, not complicated. Warrior archetype. Speaks in short, direct sentences about fighting, orders, and food. His barks tend to create combat-obligation or duty-based misinterpretations.

### Vellum
Scholarly, precise, condescending. Mage archetype. Quotes books, names spells aloud, and corrects everyone. His barks tend to create knowledge-based, elemental, or contractual misinterpretations.

---

## Questbook Logic

The Questbook operates on these rules:

1. **Input:** Receives a `BarkEvent` enum key
2. **Interpretation:** Applies a fixed, deterministic bureaucratic misreading
3. **Output:** Either a new quest marker, a quest pressure change, or a visible reaction (text, UI flash, map marker)
4. **Scope:** All effects are LOCAL to the current map
5. **Determinism:** Same bark in same context always produces same result

**The Questbook is NOT a random generator.**
It is always understandably wrong.
The humor comes from the system interpreting consistently, bureaucratically, and WRONG -- not from arbitrary things happening.

Example:
- Nib says: "I smell treasure!" (`BarkEvent.NIB_SMELL_TREASURE`)
- Questbook interprets: "Official Request: Locate and catalogue all olfactory-detectable valuables within municipal jurisdiction"
- Effect: A quest marker appears pointing to the nearest pile of garbage

---

## Quest Pressure

Quest Pressure is a single global meter with exactly three levels:

| Level | Effects |
|-------|---------|
| **LOW** | Harmless markers, small hints, flavor reactions |
| **MEDIUM** | Enemies spawn, traps activate, false quest markers appear |
| **HIGH** | Boss-level chaos reaction, but contained within current map |

- Quest Pressure rises when `PRESSURE_BARK` or `DANGER_BARK` events fire
- Quest Pressure only affects the current map
- Quest Pressure resets on map transition
- Quest Pressure never causes permanent world-state changes

---

## Core Message

"Being a hero is mostly paperwork, and the paperwork is wrong."

---

## What Will NOT Be Built

- No open-world simulation
- No real audio or speech recognition
- No branching dialogue trees
- No global quest tracking beyond current map
- No complex inventory system
- No procedural content generation
- No multiplayer
- No new chapters beyond what is already defined (Prologue + 3 Chapters)
- No new party members beyond Nib, Brugg, Vellum
- No new mechanics beyond BarkEvent-to-Questbook pipeline
- No random event system
- No AI-driven dialogue
- No narrative branching based on player choice
- No crafting system
- No skill trees
- No leveling system in the vertical slice

---

## Design Lock

This document represents the locked concept. No additions, expansions, or "wouldn't it be cool if" changes are permitted without explicit scope review. The concept is complete. The task is to build it small and prove it works.
