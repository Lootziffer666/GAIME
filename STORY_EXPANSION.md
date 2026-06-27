# Story Expansion Guide

Dieses Dokument ist das Drehbuch-Template für neue Dialogszenen.
Trage dein Skript direkt hier ein — jede Szene hat eine fest codierte Stelle im Code.

---

## Wie Dialogue funktioniert

```kotlin
DialogueLine(speaker, text, audioPath?)
```

- `speaker` — Name des Sprechers (erscheint fett über dem Text). Leer `""` = Erzählertext.
- `text` — Der angezeigte Satz.
- `audioPath` — Optional. Pfad relativ zu `composeResources/files/`. Leer = kein Ton.

**Verfügbare NPC-Stimmen (Voice Jam / Gruff, pitch-verschoben pro Charakter):**

| Ordner | Charakter | Pitch | Verfügbare Zeilen |
|--------|-----------|-------|-------------------|
| `bark/barkeep/` | Barkeep | –2.3 st (tief, rau) | greetings_stranger, hmm_i_wonder_what_this_could_be, spend_some_coin_or_get_out, been_playing_in_the_sewers_have_we, stew_again, what_do_you_have_for_me, thats_nothing_a_flagon_of_ale_wont_fix, youve_gotta_try_this_roast_cockatrice |
| `bark/guard/` | Guard | +0.8 st (scharf, militärisch) | who_goes_there, greetings_traveler, drop_your_weapons_and_surrender, been_playing_in_the_sewers_have_we, nothing_to_see_here, there_are_all_manner_of_creatures_within_these_woods |
| `bark/merchant/` | Merchant | +1.9 st (hoch, quirlig) | greetings_friend, see_if_any_of_this_strikes_your_fancy, make_me_an_offer, might_i_interest_you_in_this_bauble, name_your_price, ive_got_some_goods_to_unload |
| `bark/guildmaster/` | Guildmaster | –3.4 st (sehr tief, befehlend) | greetings_friends, experience_is_how_we_grow, knowledge_is_the_answer, grab_your_torch_theres_work_to_be_done, well_met |
| `bark/citizen/` | Citizen / Devotee | +1.3 st (leichter, ängstlich) | hmm_i_wonder_what_this_could_be, what_dark_dealings_await_here, our_prayers_will_be_answered, this_is_unusual, greetings_stranger, i_need_to_speak_to_the_town_guard |

**Helden-Stimmen** (`bark/brugg/`, `bark/nib/`, `bark/vellum/`) — 187 Zeilen pro Held.
Relevante Zeilen: `knowledge_is_the_answer`, `greetings_traveler`, `just_keep_to_the_trail`,
`so_thats_how_it_is_then`, `what_do_we_have_here`, `i_smell_dragon`, `this_place_reeks_of_death`, uvm.

**Neue NPC-Stimmen hinzufügen:**
Neue Zeile aus `assets/audio/voices/Voice Jam/Gruff/` konvertieren:
```bash
python3 scripts/add_npc_bark.py "source line name" barkeep my_new_line
```
*(Script noch zu erstellen — oder Konversion manuell via `convert_npc_barks_v2.py` erweitern.)*

---

## Szenen mit offenen Dialogslots

Jede Szene hat eine `private val *_LINES`-Konstante in
`composeApp/src/commonMain/kotlin/ui/rpg/SliceScreen.kt`.

---

### Szene 1 — Kapitel 1 Intro (INTRO_CUTSCENE)

**Datei:** `SliceScreen.kt` → `INTRO_LINES`
**Trigger:** Spielstart
**Aktuelle Zeilen:**

```
Barkeep: "You've been officially registered as a Hero Party. Don't ask how."
Nib:     "...by who?"
Barkeep: "The Questbook. It fell on the desk and opened to the right page. Fate, probably."
```

**Dein Skript (Ersetze oder ergänze):**

```
[DEIN TEXT HIER]
```

---

### Szene 2 — Barkeep (Taverne, vor Keller)

**Datei:** `SliceScreen.kt` → `BARKEEP_PRE_SEWER_LINES`
**Trigger:** E-Taste am Barkeep-NPC in der Taverne (vor dem Keller)
**Aktuelle Zeilen:**

```
Barkeep: "Spend some coin or get out."
Brugg:   "Barkeep! A flagon of ale!"
```

**Dein Skript:**

```
[DEIN TEXT HIER]
```

---

### Szene 3 — Patron (Taverne)

**Datei:** `SliceScreen.kt` → `PATRON_LINES`
**Trigger:** E-Taste am Patron-NPC
**Aktuelle Zeilen:**

```
Patron: "He sure is slow for a four-armed bartender."
```

**Dein Skript:**

```
[DEIN TEXT HIER]
```

---

### Szene 4 — Barkeep (Taverne, nach Rückkehr aus Keller)

**Datei:** `SliceScreen.kt` → `BARKEEP_POST_SEWER_LINES`
**Trigger:** E-Taste am Barkeep nach dem Keller
**Aktuelle Zeilen:**

```
Barkeep: "Been playing in the sewers, have we?"
```

**Dein Skript:**

```
[DEIN TEXT HIER]
```

---

### Szene 5 — Heroes Home Ext (Dorfeingang)

**Datei:** `SliceScreen.kt` → `HEROES_HOME_EXT_LINES`
**Trigger:** Eintritt in `HEROES_HOME_EXT` Phase
**Aktuelle Zeilen:**

```
[Erzähler]: "The party steps outside into the morning air."
Nib:        "The guild hall is just down the road. And the tavern's right behind us."
Brugg:      "Let's not dawdle."
```

**Dein Skript (Dorfbewohner, Händler, Ambiente):**

```
[DEIN TEXT HIER]
```

---

### Szene 6 — Guildmaster (Gildengebäude-Exterior)

**Datei:** `SliceScreen.kt` → `GUILDMASTER_LINES`
**Trigger:** E-Taste am Guildmaster-NPC in `CHAPTER2_GUILDHALL`
**Aktuelle Zeilen:**

```
Guildmaster: "Registered heroes may pick up contracts at the board inside."
Guildmaster: "Non-registered adventurers are asked to leave or be fined."
Nib:         "We're registered. The Questbook said so."
```

**Dein Skript (offizielle Exposition, Questbook-Lore):**

```
[DEIN TEXT HIER]
```

---

### Szene 7 — Kapitel 2 Market Intro

**Datei:** `SliceScreen.kt` → `CHAPTER2_MARKET_INTRO_LINES`
**Trigger:** Eintritt in `CHAPTER2_MARKET`
**Aktuelle Zeilen:**

```
[Erzähler]: "The party exits into the market square of Stokeport."
Nib:        "Fresh air! And fresh pockets to pick."
Vellum:     "The Questbook is restless. It demands a new page."
```

**Dein Skript:**

```
[DEIN TEXT HIER]
```

---

### Szene 8 — Market Merchant

**Datei:** `SliceScreen.kt` → `CHAPTER2_MERCHANT_LINES`
**Trigger:** E-Taste am Merchant-NPC im Markt
**Aktuelle Zeilen:**

```
Merchant: "See if any of this strikes your fancy."
Merchant: "Make me an offer. I won't bite."
Nib:      "How much do you want for this?"
```

**Dein Skript (Händler-Exposition, Waren, Stadtlore):**

```
[DEIN TEXT HIER]
```

---

### Szene 9 — Market Guard

**Datei:** `SliceScreen.kt` → `CHAPTER2_GUARD_LINES`
**Trigger:** E-Taste am Guard-NPC im Markt
**Aktuelle Zeilen:**

```
Guard: "The forest trail east of here has been overrun by wolves."
Guard: "If you're looking for trouble, you'll find it there."
Brugg: "Just keep to the trail."
```

**Dein Skript (Warnung vor dem Wald, Questgeber-Funktion):**

```
[DEIN TEXT HIER]
```

---

### Szene 10 — Chapel Devotees (Kapellen-Exterior)

**Datei:** `SliceScreen.kt` → `CHAPEL_DEVOTEE_LINES`
**Trigger:** Eintritt in `CHAPTER2_CHAPEL_EXT`
**Aktuelle Zeilen:**

```
Citizen: "The chapel has been... quiet lately. Too quiet."
Citizen: "Something moved the pews. Something large."
Vellum:  "Perfect. Let's go in."
```

**Dein Skript (Mysterium, Horror-Ansatz, Lead-in zum Boss):**

```
[DEIN TEXT HIER]
```

---

### Szene 11 — Temple Exterior

**Datei:** `SliceScreen.kt` → `TEMPLE_EXT_INTRO_LINES`
**Trigger:** Eintritt in `CHAPTER2_TEMPLE_EXT`
**Aktuelle Zeilen:**

```
[Erzähler]: "The ruined temple exterior. Overgrown. Unsettled."
Brugg:      "Wolves."
Nib:        "Lots of wolves."
```

**Dein Skript (Ruinen-Atmosphäre, Gefahr):**

```
[DEIN TEXT HIER]
```

---

## Neue Szenen (noch nicht implementiert)

Folgende Assets und Charaktere existieren bereits, haben aber noch keine Dialogszene.
Füge neue `SlicePhase`-Einträge in `core/src/commonMain/kotlin/rpg/SlicePhase.kt` hinzu
und neue `*_LINES`-Konstanten + `when(phase)`-Blöcke in `SliceScreen.kt`.

---

### NEU: Priest / Mönche (Chapel-Interior)

**Assets verfügbar:**
- Sprites: `assets/HD/locations/chapel/PNG/Animation/Mon1k-4k/` (Mönche)
- Sprites: `assets/HD/locations/chapel/PNG/Animation/Priest/` (Priester)
- Sprites: `assets/HD/locations/chapel/PNG/Animation/Parishioners 1-11/` (Gemeinde)
- Hintergrund: Kapellen-Interior TMX nicht nutzbar (broken) — Exterior bereits als `world_chapel_ext`

**Vorgeschlagene SlicePhase:** `CHAPEL_INTERIOR`

**Dein Skript (Priester-Dialog, religiöse Lore, Questbook-Seite-Bezug):**

```
Priest:  ""
Priest:  ""
[Held]:  ""
```

**Für Priest-Stimme:** Die Gruff-Zeile `"we must live by the teachings of holy wisdom"`
und `"blessings upon you"` sind gute Kandidaten. Pitch empfohlen: –5% (würdevoll).
Neue Stimme erstellen: Füge `("blessings upon you", "priest", "blessings_upon_you")` in
`convert_npc_barks_v2.py` ein und ergänze `CHARACTER_PITCH["priest"] = 0.95`.

---

### NEU: Mage (Gildenhalle-Interior)

**Assets verfügbar:**
- Sprites: `assets/HD/locations/guild-hall/PNG/Mage1-4/` (4 Mage-Varianten)
- Sprites: `assets/HD/locations/guild-hall/PNG/Guildmaster/` (eigener Guildmaster-Sprite)
- Sprites: `assets/HD/locations/guild-hall/PNG/Reader1-2/` (NSCs)
- Hintergrund: `assets/HD/locations/guild-hall/PNG/` (Interior TMX broken, nur Exterior nutzbar)

**Vorgeschlagene SlicePhase:** `CHAPTER2_GUILDHALL_INTERIOR`

**Dein Skript (Magie-Lore, Questbook-Deutung, Auftrag):**

```
Mage:    ""
Mage:    ""
[Held]:  ""
```

**Für Mage-Stimme:** Gruff-Zeilen `"now what was that incantation"`, `"of all the arcane lore"`,
`"the elements are mine to command"` passen gut. Pitch empfohlen: +15% (exzentrisch hoch).

---

### NEU: Medusa Boss (neue Arena)

**Assets verfügbar:**
- Boss-Sprite: `assets/HD/enemies/rpg-monsters/PNG/medusa/` (vollständig)
- Weitere Bosse: `demon/`, `dragon/`, `jinn/`, `lizard/`, `small_dragon/`

**Vorgeschlagene SlicePhase:** `CHAPTER3_BOSS_MEDUSA`

**Dein Skript (Boss-Intro + Post-Boss):**

```
Medusa:  "" (Intro)
Vellum:  "" (Reaktion)

[nach Kampf]
[Erzähler]: ""
```

---

### NEU: Village Elder (Dorf-Interior oder Heroes Home Ext)

**Assets verfügbar:**
- Sprites: `assets/HD/locations/guild-hall/PNG/Guildmaster/` (als Ältester nutzbar)
- Alternativ: `assets/HD/locations/heroes-home/PNG/` (Innen-Assets)

**Vorgeschlagene SlicePhase:** Neue Phase oder E-Taste-Trigger in `HEROES_HOME_EXT`

**Dein Skript (Dorfchronik, Backstory der Helden, Questbook-Ursprung):**

```
Elder:   ""
Elder:   ""
Nib:     ""
```

---

### NEU: Bridge-Szene (optionale Begegnung)

**Assets verfügbar:**
- Hintergrund: `world_bridge` (bereits gerendert, 1360×1104 Brücke)
- NPCs: Citizen1/2 als Reisende
- Feinde: Kein spezifisches Brücken-Monster — `enemy_wolf` oder zukünftiger `demon` nutzbar

**Vorgeschlagene SlicePhase:** `CHAPTER2_BRIDGE` (bereits in `SlicePhase.kt` vorhanden)
**Trigger:** Brücken-Überquerung (east/west exits)

**Dein Skript (Reisende, Gerüchte, Weltbuilding):**

```
Citizen: ""
Citizen: ""
[Held]:  ""
```

---

## Freie Gruff-Zeilen (noch nicht zugewiesen)

Folgende Zeilen aus dem Gruff-Pack sind thematisch interessant und noch keinem NPC zugewiesen.
Sie können als neue Bark-Dateien konvertiert werden:

| Zeile | Passender Kontext |
|-------|-------------------|
| `"blessings upon you"` | Priester, Devotee |
| `"we must live by the teachings of holy wisdom"` | Priester |
| `"light will scour this place clean"` | Paladin, Priester |
| `"now what was that incantation"` | Mage |
| `"of all the arcane lore"` | Mage |
| `"the elements are mine to command"` | Mage |
| `"the balance of life and death sits on a knifes edge"` | Boss-Intro, Priester |
| `"i smell dragon"` | Wildnis-Begegnung |
| `"what dark dealings await here"` | Dungeon-Einstieg |
| `"this place reeks of death"` | Boss-Raum |
| `"by my blade youll not see tomorrow"` | Feind/Boss-Intro |
| `"from the shadows"` | Überraschungsangriff |
| `"i need a healer"` | Post-Kampf |
| `"the deeper we go the darker it gets"` | Dungeon-Übergang |
| `"i smell sewage"` | Kanalübergang |
| `"its as though the trees have eyes"` | Wald-Atmosphäre |
| `"are you out of your mind"` | Reaktion auf Helden |
| `"i hear the king likes to wear evening gowns"` | Taverne-Klatsch |
| `"wheres the nearest inn"` | Reisender |
| `"greetings your lordship"` | formelle Begrüßung |
| `"greetings my lady"` | formelle Begrüßung |

---

## Code-Struktur für neue Dialogue-Szenen

```kotlin
// 1. Neue Konstante in SliceScreen.kt
private val MEINE_NEUEN_LINES = listOf(
    DialogueLine("Priest", "Blessings upon you, travelers.", "bark/priest/blessings_upon_you.wav"),
    DialogueLine("Brugg", "Much obliged.", "bark/brugg/by_your_leave.wav"),
    DialogueLine("Priest", "The Questbook... I have seen one before.", "bark/priest/we_must_live_by_the_teachings_of_holy_wisdom.wav")
)

// 2. Neue SlicePhase in SlicePhase.kt
CHAPEL_INTERIOR,   // Chapel interior — priest encounter

// 3. Neues when-Branch in SliceScreen.kt (im Render-Block)
SlicePhase.CHAPEL_INTERIOR -> ExploreView(
    world = chapelInteriorWorld,
    scene = chapelInteriorScene,
    onTrigger = { trigger ->
        // Trigger-Handling
    },
    ...
)
```

---

*Zuletzt aktualisiert: Phase 3.5 — NPC Voice Audio*
