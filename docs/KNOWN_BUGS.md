# Known Bugs & Pitfalls

Inkrementell geführt von Claude Code und Kiro. Pflichtlektüre vor jedem Auftrag.
Einträge nie löschen — nur als FIXED markieren, damit der Fehler nicht wiederholt wird.

Format: `[ID] Kurzbeschreibung — gefunden von, Datum, betroffene Datei(en), Status`

---

## Offen

*(noch keine Einträge)*

---

## Behoben

| ID | Bug | Gefunden | Datei(en) | Behoben in |
|---|---|---|---|---|
| B001 | Kiro implementierte `rpg.items.*` mit inkompatiblem API (`displayName`, `attackBonus`, `PurchaseResult`, `Inventory(party, gold)`) — kollidierte mit bereits verdrahteter `SliceScreen.kt`-ShopView | Claude, 2026-06-28 | `rpg/items/Item.kt`, `Inventory.kt`, `ItemCatalog.kt` | Integration-Branch `claude/integration` (Merge PR#29) |
| B002 | Kiro öffnete PNG-Assets als Base64-Dateiinhalt → Kontext-Flood, Thread unbrauchbar | User, 2026-06-28 | `assets/HD/ui/fantasy-icons/PNG/Gui_icons2.png` | Protokoll-Regel (handoff-protocol.md) |
| B003 | `gruff/raspy/whiny .7z.part*` lagen im Repo-Root und wurden versehentlich getrackt | Claude, 2026-06-28 | Repo-Root | Commit 4ccfa781 |

---

## Pitfalls (kein Bug, aber Falle)

- **Kiro briefen während Branches offen sind**: führt zu 3-Way-Divergenz. Immer erst `git fetch --all --prune` + Branch-Audit, dann brief.
- **`settings.gradle.kts` anfassen ohne Auftrag**: Kiro hat dieses File mehrfach unaufgefordert geändert. Immer explizit in `DO_NOT_TOUCH` setzen wenn ein neues Modul im Auftrag ist.
- **Compose-UI-Features investieren**: `composeApp/` ist Throwaway (KorGE-Migration). Keinen Aufwand in SliceScreen/DialogueLine/BarkAudioPlayer stecken.
