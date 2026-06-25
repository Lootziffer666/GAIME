# CAMPAIGN: Quest Accepted: Unfortunately

**The Completely Official Quest of Questionable Importance**

This is the canonical full-game design. The vertical slice (`docs/VERTICAL_SLICE.md`)
proves the core pipeline (Prologue + Chapter 1). This document defines the
complete campaign the slice grows into: **Prologue + 5 Chapters + Finale**,
~60-90 minutes of play.

> This file is the source of truth for chapter structure, bosses, and the
> finale mechanic. If it conflicts with older notes, this file wins.

---

## Premise

In the harbour town of **Stokeport**, the **Bureau of Official Heroic Business**
registers, rewards and archives heroic deeds. Its heart is the **Official
Questbook**, an ancient artefact that recognises spoken intent and turns it into
official quests.

It used to work:

> "We must rescue the mayor." -> QUEST ACCEPTED: RESCUE THE MAYOR.

Then the Questbook got a municipal update. Now it can no longer tell the
difference between intent, irony, boasting, a sales pitch, panic, a tavern lie,
or Nib's normal breathing. The book no longer records a story. It **enforces**
one.

**Main quest:** FIND THE BROKEN QUESTBOOK BEFORE IT COMPLETES THE WRONG STORY.

The comedy is never random. The system is always *understandably wrong*:
consistent, bureaucratic, and certain.

---

## Tone

- Dry, British-bureaucratic fantasy humour.
- Not "haha, random dragon." Instead: "The dragon was duly generated because
  someone mentioned it within earshot of a defective public quest-management
  artefact."
- 8/16-bit SNES/Mega-Drive top-down pixel RPG. Small clear maps, three playable
  characters, short text boxes, many reusable voice barks, simple combat, clear
  chapter structure. Comedy comes from system logic, not chaos.

---

## Core Mechanic (recap)

The Questbook reacts to internal `BarkEvent` keys, never to real audio. When a
bark plays, the game already knows its key. Every trigger is:
**map-local, cooldown-limited, failsafe-protected, and visibly traceable.**

See `docs/GAME_CONCEPT_LOCK.md` and `docs/BARK_TRIGGER_TABLE.md`.

### Signature campaign reactions

| Spoken bark | Questbook reaction |
|---|---|
| "I smell treasure." | Treasure marker appears |
| "I smell sewage." | Sewer/drain path revealed |
| "This looks like a secret entrance." | Secret door opens |
| "It's a trap." | Trap becomes visible / arms |
| "I smell monsters." | Enemies spawn or are marked |
| "Where did I put that map?" | Map disappears / map-quest starts |
| "What are your orders?" | A false order may be adopted |
| "This calls for flame." | Flammable objects react |
| "This calls for ice." | Water / enemies freeze |
| "This calls for lightning." | Machines / glyphs activate |
| "I smell dragon." | Dragon quest is activated |

---

## Quest Pressure (recap)

A single visible meter with three levels (`docs/GAME_CONCEPT_LOCK.md`):

- **LOW** — small markers, optional chests, small enemies, harmless side effects.
- **MEDIUM** — doors change, NPCs take statements literally, merchant prices turn
  "quest-relevant", maps shift, false objectives appear.
- **HIGH** — boss encounters form, town logic breaks, the quest board overrides
  NPC dialogue, the book tries to force a final heroic story.

HIGH never affects other chapters, other maps, or global world state.

The final mission is **not** "do everything". It is to crash the system on
purpose with contradictory, banal quests.

---

## The Party

### Nib — whiny rogue / merchant / coward with a loot radar
Opens chests, finds secret entrances, spots traps, finds loot, runs merchant and
thief dialogue, and causes roughly 40% of all problems.
Running gag: Nib insists he can "almost certainly" open things. The Questbook
reads "almost certainly" as administratively sufficient competence.

### Brugg — gruff guard / ex-soldier / portable battering ram with a sense of duty
Tank. Protects the party, kicks in doors, blocks traps, talks to guards, solves
authority dialogue. Running gag: Brugg recognises nonsense but executes it
correctly the moment it sounds like an order.

### Vellum — raspy mage / cleric / walking footnote with memory problems
Fire/ice/lightning/light, healing, reads glyphs, disables magical barriers,
identifies Questbook fragments. Running gag: Vellum explains every problem until
the Questbook derives a bigger problem from it.

---

## Structure

Five short chapters plus a finale. Each chapter is built on one RPG cliche the
Questbook misreads bureaucratically. Each chapter ends with one recovered
**Questbook page**.

| # | Chapter | Setting | Boss | Page recovered |
|---|---|---|---|---|
| 0 | Prologue: The Four-Armed Bartender | Tavern *The Limping Cockatrice* | — | — |
| 1 | The Sewers of Bad Decisions | Sewers under Stokeport | The Rat Accountant | The Page of Beginnings |
| 2 | The Market of Mandatory Commerce | Stokeport market + forest trail | The Tax Collector Badger | The Page of Terms and Conditions |
| 3 | The Woods That Had Opinions | Woods outside Stokeport | The Helpful Tree | The Page of Directions |
| 4 | The Ship That Was Technically Seaworthy | Harbour, warehouse, ship, fog sea | Captain Formbeard | The Page of Claims and Rewards |
| 5 | The Dragon That Was Accidentally Summoned | Island cave, gold chamber | The Administragon | (final) |
| F | Finale: Done Enough | The broken Questbook | — | — |

---

## Prologue — The Four-Armed Bartender

Tavern *The Limping Cockatrice*. The party shares a table by accident. The
four-armed barkeep is conspicuously slow. A patron: "He sure is slow for a four
armed bartender." Nib boasts "I smell treasure." — the Questbook listens. An
official stamp falls from the sky into the table and turns a beer mat into an
official document:

> QUEST ACCEPTED: FIND THE TREASURE BENEATH STOKEPORT.

Brugg: "Are you out of your mind?" Vellum: "Hmm, I wonder what this could be."
The tavern floor opens. Nib: "It wasn't me." Everyone falls into the sewer.

**Teaches:** movement, party switching, chests, simple combat, Questbook
reactions.

---

## Chapter 1 — The Sewers of Bad Decisions

Cliche: every RPG starts in a cellar/well/sewer. The sewer was never built as a
dungeon; the Questbook merely insulted it enough to behave like one.

Map elements: slimes, rats, broken bridges, rusted grates, locked maintenance
doors, wet forms, chests nobody should have left here.

Mechanical hook: Nib finds secret maintenance passages, but every loot bark
raises the chance a chest is marked "quest-relevant". Some chests hold good
items, others hold forms. Some forms bite.

**Boss: The Rat Accountant** — a giant bespectacled rat accountant with a quill
and an unnaturally clean desk. He approves quests only when the matching forms
are dry; the fight is in a sewer. He summons mini-rats when the party checks
wrong boxes, skips objectives, takes an item without declaring it "found", or
lets Nib speak.

After victory the party finds **The Page of Beginnings**. Vellum realises the
Questbook does not belong to the king — it belongs to the city administration.
Brugg: "Objective complete." Vellum: "Hard-won knowledge." Nib: "Do we get paid
for knowing that?"

(See `docs/VERTICAL_SLICE.md` for the buildable detail of the Prologue + Ch1.)

---

## Chapter 2 — The Market of Mandatory Commerce

*(Shipped: PR #12. Buildable and playable on top of Chapter 1.)*

Cliche: town market, merchants, a quest board, and guards with unclear orders.
The party follows the Questbook's demands out of the tavern into Stokeport's
market square and the forest beyond.

Play beats (~10 min):
1. **Stokeport Market** — merchant and city-guard NPCs. Nib's "I smell gold!"
   opens a *Commercial Survey* quest; merchants reinterpret every bark as a
   price-relevant declaration.
2. **Forest Trail** — three wolf encounters, rising quest pressure.
3. **Shrine of the Fine Print** — a Vellum puzzle solved with the
   `VELLUM_CALLS_FOR_LIGHTNING` utility bark (like flame clears rubble in Ch1).
4. **Boss: The Tax Collector Badger** — three phases: wolf adds, then "OVERDUE"
   stamps, then a single heavy stamp. He audits the party's outstanding balance.
5. **The Page of Terms and Conditions** — "Outstanding Quest Balance: 47.
   Payment: Additional heroism."
6. **Return to Market** — chapter complete.

Running gag: the guard and the board take every line literally, and merchants
price "quest-relevant" junk accordingly. The heroic move is to stop answering.

---

## Chapter 3 — The Woods That Had Opinions

Cliche: magical woods with talking trees, kobolds, mushrooms, and paths that
don't stay put. Problem: the Questbook reads every orientation question as a new
navigation quest. Whoever asks where to go changes the direction.

Map: talking trees, kobold scouts, magical glyphs, mushroom rings, invisible
paths, secret entrances, a signpost with too many opinions.

Barks: "It's as though the trees have eyes.", "There are all manner of creatures
in these woods.", "Just keep to the trail.", "Which direction?", "Where did I
put that map?", "This looks like a map.", "This looks like a glyph.", "This looks
like a secret entrance.", "From the shadows.", "Show yourselves."

Mechanic: the map shifts every time someone says "Which direction?". A simple
puzzle results — sometimes let Nib speak to open a secret entrance; sometimes
Brugg must stay silent so the path is not recomputed as a military route; Vellum
must read glyphs but his element barks change the environment.

Element puzzles:
- Flame -> thorns burn away, but mushrooms turn aggressive.
- Ice -> the swamp freezes, but bridges turn slippery.
- Lightning -> glyphs activate, but quest wisps wake up.

**Boss: The Helpful Tree** — an ancient tree that genuinely wants to help. That
is the problem: it gives so many hints, sub-hints, context, alternative routes,
moral assessments, optional explanations and historical tree comparisons that
the party constantly receives new quests. **You win by ignoring hints** —
deliberately take the route it calls "technically inadvisable".

Ends with **The Page of Directions**. Vellum: not every quest leads somewhere;
some are just noise with a reward icon. Nib asks whether noise is stackable.

---

## Chapter 4 — The Ship That Was Technically Seaworthy

Cliche: pirates, barrels, crates, anchor, sails, a ship nobody would insure.
Problem: the fourth Questbook page is on an island off Stokeport. The only
available ship is "technically seaworthy" because it has not yet fully fallen
over at low tide.

Map: harbour, warehouse, pirates, fog, ship, barrels, crates, a barrel that
looks far too important, a crate that looks back.

Barks: "I love the fresh sea air.", "Is she seaworthy?", "Drop anchor.", "Hoist
anchor.", "Raise the sail.", "The sea is an angry mistress.", "I've never been
fond of deep water.", "Let's be underway.", "We're out maneuvered.", "Fall
back.", "Retreat."

Comedy set-piece: Nib finds something in every barrel. First coins, an apple,
rope, a healing potion. Then a bill, a wet kobold, a smaller barrel, a Barrel
Mimic, a tax notice, and a sign reading "Please Stop Searching Barrels." Nib:
"Ooo, another barrel." Questbook: OPTIONAL QUEST ACCEPTED: INSPECT EVERY BARREL.
Brugg: "Fall back." Questbook: TACTICAL RETREAT ACCEPTED. The ship reverses out
of the harbour.

**Boss: Captain Formbeard** — a pirate captain whose beard is folded plunder
applications. He attacks only when his paperwork is complete; being a pirate, he
forges it mid-fight. Three actions:
1. **Raid Application** — summons Pirate Clerks.
2. **Incomplete Boarding Action** — the attack fails if Brugg interrupts with
   "Drop your weapons and surrender."
3. **Loot Declaration** — marks Nib's inventory as "piratically redistributable".

On defeat, Formbeard shouts "Avenge me." Nobody is dead. He means his form. The
party finds **The Page of Claims and Rewards** and sails to the island. The ship
does not sink — it thereby exactly meets the minimum standard.

---

## Chapter 5 — The Dragon That Was Accidentally Summoned

Cliche: final dungeon, gold, bones, dragon, dark prophecy. Problem: there was no
dragon left. Nib says anyway: "I smell dragon." The Questbook does not treat this
as an opinion but as a **defect report**.

> URGENT QUEST ACCEPTED: DEFEAT THE DRAGON.

With no dragon present, the Questbook builds one from available materials: gold,
dragon bones, old hero songs, tax forms, Nib's greed, Brugg's sense of duty, and
Vellum's mispronunciation of "draconic residue".

**Boss: The Administragon** — a bureaucratic dragon of fantasy cliche and public
procurement. It does not breathe fire. It breathes *jurisdiction*.

Barks: "I smell dragon.", "This place reeks of death.", "Darkness take you.",
"Have at thee.", "I smite you.", "Summon your strength.", "The deeper we go, the
darker it gets.", "That drew blood.", "Low on health.", "I don't have much left
in me.", "I didn't think it would end like this."

### Final boss structure

- **Phase 1 — Greed.** The Administragon lures Nib with treasure markers. Each
  collected treasure slightly heals the boss, because it keeps the quest
  "dramatically appropriate". Nib must learn to leave loot. He objects.
- **Phase 2 — Duty.** The dragon issues false orders ("Hold the line.", "Fall
  back.", "Attack.", "Protect the asset.", "Surrender immediately."). Brugg must
  ignore orders that look official. He objects, quietly.
- **Phase 3 — Magic chaos.** The dragon mirrors Vellum's elements: fire makes ice
  barriers, ice makes lightning fields, lightning activates forms, forms deal
  damage over time. Vellum must pick the **least appropriate** spell, not the
  strongest. This wounds him academically.
- **Phase 4 — System Overload.** The party realises the Questbook cannot be beaten
  by completing its quests (that only confirms its jurisdiction). So they overload
  it with simultaneous, contradictory, utterly banal quests, deliberately firing
  silly barks ("Where's the privvy?", "Is that roast I smell?", "Hey, that's not
  a horse.", "This looks like gold.", "This looks like trouble.", "This looks like
  treasure.", "This chest is almost certainly unlocked.", "Who runs this city?",
  "Where did I put that map?", "I smell dragon."). The Questbook tries to
  prioritise everything at once:

      QUEST ACCEPTED: FIND THE PRIVVY.
      QUEST ACCEPTED: IDENTIFY THE HORSE.
      QUEST ACCEPTED: ROAST CONFIRMATION.
      QUEST ACCEPTED: MUNICIPAL AUTHORITY REVIEW.
      QUEST ACCEPTED: OPEN THE CHEST.
      QUEST ACCEPTED: DEFEAT THE DRAGON.
      QUEST ACCEPTED: LOCATE THE MAP REQUIRED TO LOCATE THE MAP.

  Then: **GAME OVER.** This time the Questbook says it about itself. The
  Administragon collapses into gold dust, bones, smoke, and a very small form
  reading "PLEASE RESTART THE STORY CORRECTLY." Nobody does.

---

## Finale — Done Enough

The Questbook lies smoking on the ground. Vellum does not fully repair it (that
would need a certificate, three moons and someone with better handwriting). He
writes one new rule on the first page:

> A quest only begins when someone means it.

Nib: "So that's how it is then?" Brugg: "Agreed." Vellum: "Knowledge is the
answer."

---

## Implementation status

- **Built (logic + tests):** Prologue + Chapter 1 pipeline, Rat Accountant boss,
  Questbook reactions for slice barks, three-level pressure, bark cooldown bus,
  bark audio playback.
- **This document adds the canon for:** Chapters 2-5 + Finale, their bosses, and
  the **Finale "System Overload"** mechanic (overload the Questbook with
  contradictory quests at HIGH pressure to trigger a self-inflicted GAME OVER).
- **Next build steps:** chapter model in code, new campaign bark events, new boss
  archetypes/controllers, per-chapter Questbook reactions, and the overload
  mechanic, each with tests. Maps/UI per chapter follow the slice's logic-first
  approach.
