# COMEDY BIBLE — Quest Accepted: Unfortunately

The humor is **architecture, not a moral filter**. The engine of every joke:

```
sacred concept + banal administrative process + an over-serious Questbook = hit
```
or:
```
pop-culture myth + bureaucratese + physically embarrassing minor damage = easter egg
```

The target is never "haha, person X." It is: *humanity turned X into an oracle,
a religion, a stock price, a salvation plan, or a lore-covered toilet wall.* We
stamp every sacred cow, file it, drop it in the sewer, and let Nib ask if it
drops loot.

> Not: "we're edgy because we know taboo words."
> Instead: "we're edgy because we process every holy thing through paperwork."

---

## The five hardness levels

| Level | Vibe | Where | Example |
|---|---|---|---|
| 1 | Subtly cool (only nerds catch it) | item/sign/bookshelf text | *Self-Driving Horse — still requires a rider, a lawyer, and ideal weather.* |
| 2 | Dumb pun, fast in/out | NPC one-liners | "This barrel is empty." / "Then why did you open it?" / "Because hope is a disease." |
| 3 | Below the belt but **systemic** (dirty, never about a real person's origin/body/sexuality) | dungeon flavor + items | *Blessed Loincloth of Moral Certainty — protects from doubt. Not from smell.* |
| 4 | Shockingly honest, short, true | party lines | "Orders are comforting. That is why cowards write so many of them." |
| 5 | Massively too much pathos, then stabbed with a dumb line | **bosses & finale only** | Administragon's litany → Nib: "So... no treasure?" |

Rule for L5: inflate the pathos, then puncture it with one stupid sentence.

---

## Placement by map

- **Tavern** — meme biotope: pop-culture, fandoms, false prophets, dad jokes.
  The joke is the *fan exegesis*, not the star. (Bard-fandom, tech-messiah,
  social-media tome, slow-motion beach knight...)
- **Ch1 Sewers** — ideologies as sewage: communism/capitalism/church apparatus/
  cults/fundamentalism, each as litter holding a form. *People's Chest — belongs
  to everyone. Opens only for the committee.*
- **Ch2 Guardhouse** — executive power, authoritarianism, strongman kitsch,
  law-and-order fetish. Fascism is always ridiculous/petty, never cool. Strongman
  cults are interchangeable kitsch (no name parade). Also home of the **Insult
  Duel** (below).
- **Ch3 Woods** — fandom, lore wars, internet brain-rot. The forest is the
  comment section of humanity (Star Wars / LOTR / Avatar / South Park / Rule 34
  by implication only / lore purists). *Canon Goblin — immune to joy, weak to
  release delays.*
- **Ch4 Ship** — corporations, tech, compliance, IP, pirate-capitalism.
  *Founder's Compass — always points toward valuation. Never toward land.*
- **Ch5 Administragon** — everything becomes a myth-machine: the algorithm of
  salvation + market + leader-cult + fanbase + religion + bureaucracy + quest log.

---

## Per-map mixing rule (do not exceed)

Per map, ship a mix — more than this becomes wallpaper of cynicism:

- 3 item descriptions
- 3 signs / bookshelves
- 2 NPC one-liners
- 2 Questbook popups
- 1 genuinely cruel-but-true line
- 1 completely dumb dad joke

---

## Recurring gag formats

- **A. Questbook as a dumb extremist** — every harmless need is radicalised.
  *"I want fairness" → QUEST ACCEPTED: CENTRALIZE ALL OUTCOMES.* This is the core
  joke in pure form. Implemented as data: `rpg.humor.SatiricalQuestbook`.
- **B. Dad jokes as an instrument of office** — intentionally awful
  ("arrest / a-rest", "rat-ification", "man-date").
- **C. The game mocking itself** — *QUEST ACCEPTED: ADD MEANINGFUL CONTENT →
  OBJECTIVE UPDATED: PLACE THREE BARRELS.*
- **D. The wrong-grammar-case NPC** ("Kasus-Kevin" / The Accusative Prophet):
  German case confusion as a quest ("FIND MICH", "LOCATE MIR").
- **E. The too-honest item description** — dry: *Sacred Flag — +5 unity, -20
  nuance, extremely flammable.*
- **F. Pop-culture as broken in-world names** — *Laser Monks of the Copyright
  Moon; The Walking Ring Problem; Archive Thirty-Four.*

## Below-the-belt techniques that work

1. High concept, low body ("The soul seeks transcendence." / "Mine seeks a toilet.").
2. Salvation promise with an invoice ("Salvation is free." / "Then why is there a counter?").
3. Power apparatus with one small embarrassing detail (lifts painted into the Eternal Commander's boots) — **status hatred, not body hatred**.
4. Fanatic speaking support-desk language ("Your disagreement has been received. It will be processed as hostility.").
5. Corporation as cult ("We are a family." / "Families do not require quarterly alignment rituals.").

---

## The Officially Sanctioned Insult Duel (Chapter 2)

A small, fixed, enum-based duel that *looks* like dialogue variety but stays
technically cheap. Implemented in `:core` (`rpg.duel`).

**Why Chapter 2:** the guard cannot legally move (it arrested itself), but
contempt is "fully mobile", so hostility is resolved verbally under *Verbal
Engagement Protocol 12-B: Non-Physical Hostility Resolution*. The best gag:

> The guard may not strike anyone because it has arrested itself.
> So it insults you officially.

**Mechanic (no free text, no AI):**
- The opponent throws a typed `Insult` (`InsultType`: AUTHORITY, COWARDICE,
  INTELLIGENCE, SMELL, LOOT, GRAMMAR, HEROISM, BUREAUCRACY).
- The player picks one of a few `Counter`s. It lands iff
  `counter.counters == insult.type`. Exactly one option is correct; the others
  are funny party barks.
- Health is replaced by **OFFICIAL DIGNITY**. Each landed counter drops it by
  one; at zero: **MANDATE DESTABILIZED**. Missteps are counted (the renderer may
  raise quest pressure).
- Ambiguity is the Questbook's friend: *order* (command / shop order / medal /
  tidiness), *charge*, *fine*, *case*, *appeal*, *sentence*, *bar* — it
  interprets wrongly but understandably.

**Content (`rpg.duel.DuelLibrary`):**
- `guardClerkDuel()` — 3-round tutorial at the door; reward: access.
- `guardCaptainDuel()` — a phase of the Guard Captain boss; landing all counters
  destabilises the mandate and makes the boss vulnerable.
- (Planned: a 2-round merchant haggling duel; reward: discount / page hint.)

Keep it to 3-4 duels of 2-4 rounds — more would be a game inside the game.

---

## Dramatic Entrances (staging system)

Over-the-top reveals as a scripted, renderer-agnostic sequence (`:core`
`rpg.staging`). The renderer (KorGE) plays the beats: looming shadows, effect
storms, close-ups (extra sprites), pathos proclamations, the reveal, and a
deflating punchline.

**Ridiculousness with system** is measurable: every entrance has a
`buildupIntensity` (1..10) and an `actualThreat` (1..10). The `ironyGap`
(buildup minus threat) is the joke — the bigger the gap, the funnier. The engine
*enforces* that an overhyped entrance (gap >= 4) ends on a `DEFLATE` beat, so
pathos is always punctured.

Beat types: `SHADOW_LOOM`, `EFFECT_STORM` (effects + intensity), `CLOSE_UP`
(extra sprite), `TITLE_CARD`, `PROCLAMATION` (the self-important speech),
`REVEAL` (often a tiny mook), `DEFLATE` (a party member's dumb, honest line).

Authored entrances (`EntranceLibrary`):
- **The Dread Shadow of the Deep** — the signature *missed expectation*: a single
  Sewer Rat staged as the apocalypse (buildup 10 / threat 1). Huge shadow +
  full effect storm + title card + "I AM THE END OF ALL THINGS" → reveal: a rat
  → Nib: "It's a rat."
- **The Administragon** — earned, maximal pathos (10/9). The litany, then Nib:
  "So... no treasure?" (Not overhyped — but still punctured.)
- **Rat Accountant**, **Captain Formbeard**, **The Helpful Tree** (9/4,
  far-too-celebratory), **Guard Captain** (8/3, maximum martial buildup for
  someone who then cannot legally move → Brugg: "He cannot move.").

`EntranceLibrary.forBoss(boss)` returns the entrance to play before each boss.
Sprite keys (`administragon_eye`, `formbeard_beard`, ...) name the extra art the
renderer must supply.

---



Platt, böse, klug, dumm, systemisch — in that mix. Stamp the holy thing, file
it, sewer it, and let Nib ask if it drops loot.
