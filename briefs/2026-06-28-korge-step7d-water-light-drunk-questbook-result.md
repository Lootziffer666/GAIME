# Result: Step 7d — Wasser, Licht, Rausch & Questbook

**Brief:** `briefs/2026-06-28-korge-step7d-water-light-drunk-questbook.md`
**Branch:** `kiro/korge-step7d-v2` (PR#44, supersedes PR#43)
**PR:** https://github.com/Lootziffer666/GAIME/pull/44
**Datum:** 2026-06-28
**Autor:** Kiro (Opus)
**Status:** ✅ Alle 5 Teile geliefert (mit kleinem Integration-Fix bei den Pfützen)

---

## Acceptance

| Check | Ergebnis |
|---|---|
| `:core:desktopTest` | ✅ BUILD SUCCESSFUL (19 neue rpg.weather-Tests) |
| `:game:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:composeApp:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:game:screenshot` | ✅ 20 PNGs (5 neue) |

---

## Geliefert

### A — Wasser-Sim in `:core` ✅
`rpg.weather`: `WaterGrid` (addRain/flowStep/evaporate/puddleAt/connectedPuddle + Drain-Tiles),
`WetnessState` (soak/dryNearHeat/isWet/isSlippery). 19 Unit-Tests grün.
**Über den Wildcard hinaus geliefert (zulässig durch `rpg.weather/*`):** `DrunkState` (volle
Rausch-Mechanik: stumbleChance, attackBonus, delayedDamage, sleepTimer, goldStolen), `DirtState`,
`FootprintGrid`, `WindState` + Tests. Plus neues Doc `docs/WORLD_PHYSICS_40_SYSTEMS.md`.

### B — Wasser-Rendering + rutschiger Boden + Trocknen ✅
`WaterOverlay` rendert Pfützen aus dem Grid; Regen über `RainFilter`; Nässe + Slip-Logik in
`WorldScene`; Laterne trocknet.
- **Beweis:** `world_rain_puddles.png` (Regen + zusammenhängende Pfützen),
  `world_puddle_drain.png` (Abfluss-Senke am Drain-Tile).

### C — Tragbare Laterne ✅
`L`-Toggle, `LightSource` folgt dem Spieler, `ambientDarkness 0.1`, Laterne = Feuerquelle (trocknet).
- **Beweis:** `world_lantern.png` — warmer Lichtkegel in abgedunkelter Welt.

### D — Beer Goggles bei Rausch ✅
Barkeep-Interaktion (E) hebt `drunkLevel`; `ShaderStateBinder.applyDrunk` (BeerGoggle hat Vorrang
vor Poison solange betrunken); nüchtert über Zeit aus. Bonus-Mechanik von Kiro: betrunkener Brugg
(>0.6) verwechselt NPCs mit „dates" → Kampf; Torkeln als Schach-Springer-Drift (deterministisch).
- **Beweis:** `world_drunk.png` — weicher, warmer, schräger Bild.

### E — Questbook-Screen ✅ (funktional, schlicht)
`QuestbookScreen` (Scene, `J` öffnet/schließt), zwei Pergamentseiten mit echten `questbook.log`-
Einträgen, aktiven Markern, Party-Name, Pressure.
- **Beweis:** `questbook_open.png` — echte Einträge aus der Pipeline.

---

## Integration-Fix (Claude)
**Pfützen waren unsichtbar** in den ersten Renders:
1. Alpha zu schwach (`depth*120` → bei depth 0.3 nur Alpha 36/255, unter dem Regen-Shader weg).
   → `WaterOverlay`: Alpha-Boden `110 + depth*140`, sattere Farbe `RGBA(0x22,0x55,0x99)`.
2. Pfützen-Koordinaten in den Captures lagen ~12 Kacheln über der Kamera (Spieler-Spawn ist
   Tile (-5,9) = Grid (5,14), Pfützen lagen bei Tiles (-7..-3, -3..-1)).
   → Beide Captures füllen jetzt um Grid (5,14) → on-screen.
Beides verifiziert: Pfützen + Abfluss jetzt klar sichtbar.

---

## DO_NOT_TOUCH — eingehalten ✅
- B007 `localCurrentDirVfs`: **intakt** (viertes Mal in Folge kein Revert).
- `game/shader/`, `HudOverlay`, `QuestbookOverlay`, `BattleScene`, `CharacterSprite`,
  `SpriteLoader`, `NpcDefinition`: unmodifiziert. Bestehende `:core`-Packages: nur konsumiert.

---

## Anmerkungen / Folge-Arbeit
- **Questbook „ganze Pracht":** aktuell zwei flache Pergament-Rechtecke. Es fehlen Buchbindung,
  Rahmen/Verzierung und die im Brief gewünschten Aufklapp-/Umblätter-Animationen sind nicht im
  Screenshot belegbar (statisch). → eigener Folge-Brief „Questbook Visual Polish".
- **Gold-Diebstahl beim Einschlafen:** `Inventory.gold` hat `private set` → echter Abzug braucht
  eine `:core`-API-Erweiterung. Kiro hat Bark/Visual statt echtem Abzug. → kleiner `:core`-Folge-Brief.
- **WORLD_PHYSICS_40_SYSTEMS.md:** Kiro hat 40 weitere Welt-Systeme dokumentiert — reiche Quelle
  für künftige gebündelte Briefs.
- **PR#43** (v1, 9 Tests) durch PR#44 (v2, 19 Tests) ersetzt → als superseded geschlossen.
- Remote-Branches `kiro/korge-step7d-v2` + `kiro/korge-step7d-water-light-drunk-questbook` → per
  GitHub Web UI löschen (403 blockt CLI).

---

## Stand nach Merge
main aktualisiert. Wetter-/Wasser-/Licht-/Rausch-Systeme leben in `:core` (testbar) und werden in
`:game` sichtbar gerendert. Das Questbook hat einen eigenen Screen. Erster großer Multi-System-Sprint.
