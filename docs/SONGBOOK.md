# QUEST ACCEPTED: UNFORTUNATELY — Official Songbook

Canonical lyrics for the game's chiptune soundtrack. One title theme plus one
boss theme per chapter. The code mapping lives in `:core`
(`rpg.music.MusicTrack`); playback is wired by the renderer (KorGE `:game`,
see `.kiro/steering/rendering-engine.md`).

> **Ch2 boss canon (resolved):** the canonical Chapter 2 boss is **The Guard
> Captain Who Cannot Legally Move** (this song, "The Warden's Mandate") — it
> embodies the self-arresting authority loop. The shipped **Tax Collector
> Badger** (PR #12) is **non-canon / implementation drift** and is to be
> reconciled in a future PR.

**Audio files:** drop the rendered tracks into `assets/audio/music/` using the
base names in the table below (`.ogg` preferred). NB: `.gitignore` currently
ignores `*.ogg`/`*.mp3`, so they must be force-added (like the bark WAVs) or the
ignore rule relaxed for `assets/audio/music/`.

| # | Track | Chapter | Boss | Genre | Base file |
|---|---|---|---|---|---|
| 1 | Quest Accepted: Unfortunately! | — (title/menu) | — | Comedy Chiptune / 16-Bit March | `title_quest_accepted` |
| 2 | The Rat Accountant (Form 8-B Denied) | Ch1 Sewers | The Rat Accountant | 16-Bit Bureaucracy Chiptune | `boss_rat_accountant` |
| 3 | The Warden's Mandate | Ch2 Market | The Guard Captain Who Cannot Legally Move (canon) | 16-Bit Symphonic Chiptune / Paradox Loop | `boss_wardens_mandate` |
| 4 | Fanfare For The Seeker | Ch3 Woods | The Helpful Tree | 16-Bit JRPG Chiptune / Baroque Pop | `boss_helpful_tree` |
| 5 | Plunder Permit | Ch4 Ship | Captain Formbeard | Chiptune Sea Shanty | `boss_formbeard` |
| 6 | The Administragon | Ch5 Dragon | The Administragon | 16-Bit Final Boss Symphonic Chiptune | `boss_administragon` |

---

## 1. Title Song — "Quest Accepted: Unfortunately!"
*Genre: Comedy Chiptune / 16-Bit March*

```
(Hey!) Another day, another decree,
Some self-important royal family tree,
Says a gallant hero they must now send,
To a quest with no redeeming end.
QUEST ACCEPTED: UNFORTUNATELY!
Now go get all that sweet, sweet loot for me!
QUEST ACCEPTED: UNFORTUNATELY!
QUEST ACCEPTED: UNFORTUNATELY! (Hooray!)
```

## 2. Chapter 1 Boss — "The Rat Accountant (Form 8-B Denied)"
*Genre: 16-Bit Hectic Bureaucracy Chiptune*

```
Hee hee hee! Welcome to my little office...
Let's review your Form 8-B
Addendum C, section three
Your heroism's quite compelling
But your tax returns are... DENIED!
YOUR QUEST LOG IS A TRAGIC MESS!
YOU FAILED TO FILE FORM B-25-YES!
Your lack of notarized compliance is a bore
So I'll just stamp this... DENIED!
Your provisional stipend has been DENIED!
And all future healing potions are... DENIED!
Your whole adventure is DENIED! (Hee hee hee!)
```

## 3. Chapter 2 Boss — "The Warden's Mandate"
*The Guard Captain Who Cannot Legally Move — Genre: 16-Bit Symphonic Chiptune / Paradox Loop*

```
By the mandate of Protocol Nine,
This precinct and post are legally mine.
But Clause Four initiates my detainment,
Pending a full command restatement!
I cannot void this paradoxical decree!
THE ONLY OFFICER AUTHORIZED IS ME!
THIS CUSTODY ORDER I CAN'T DISAVOW!
I CANNOT DISAVOW! (Cannot disavow!)
```

## 4. Chapter 3 Boss — "Fanfare For The Seeker"
*The Helpful Tree — Genre: 16-Bit JRPG Chiptune / Baroque Pop*

```
(Ooh... aah...) Welcome, seeker!
Your path to optimal efficiency begins now!
Consult your map for supplementary data-points.
Behold your journal! Sub-quest coordinates are logged!
Embrace the metrics! Every click improves the process!
More helpful hints are being generated...
(Hee hee hee! So many quests!)
```

## 5. Chapter 4 Boss — "Plunder Permit"
*Captain Formbeard — Genre: Chiptune Sea Shanty*

```
(Aaaarrrr-gh! Present your manifest!)
I've got my boarding permit, signed in ink,
I'll take your gold before you blink!
No messy raids or chaotic strife,
I'm living the regulated pirate life.
My looting application's filed in triplicate,
My cannons fired, my schedule's intricate!
PLUNDER PERMIT GRANTED!
PLUNDER PERMIT GRANTED!
```

## 6. Chapter 5 Endboss — "The Administragon"
*Genre: 16-Bit Final Boss Symphonic Chiptune*

```
(System initialization... Dragon detected...)
My scales are written in binary code,
A flaming obstacle, a procedural load.
The quest log cried, "I smell dragon breath!"
So I was spawned to drag you to death.
I have no story, no heart, no home,
Just a data-set where the heroes roam.
CLOSE THE TICKET! SEAL THE FILE!
I am the boss of the final trial!
```
