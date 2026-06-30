# Result: Step 16 -- Pfeiler 2: Alle Grid-Physik sichtbar im Unified Runtime + Harness-Daten-Refactor

**Datum:** 2026-06-30
**Branch:** `kiro/korge-step16-pfeiler2-visible-physics`
**BASE_SHA:** `db1cce00`

---

## Ergebnis

Alle Ziele erreicht. 5 neue WorldSystems in `:core`, vollstaendig in die Registry des
Unified Runtime integriert, plus der Harness-Daten-Refactor mit 6 neuen 1440p-Screenshots.

---

## Teil A -- 5 neue WorldSystems

| System | Datei | Grid | Tests |
|---|---|---|---|
| SnowSystem | `core/.../rpg/systems/SnowSystem.kt` | SnowGrid | 5 |
| BloodSystem | `core/.../rpg/systems/BloodSystem.kt` | BloodGrid | 4 |
| FootprintSystem | `core/.../rpg/systems/FootprintSystem.kt` | FootprintGrid | 5 |
| SeasonSystem | `core/.../rpg/systems/SeasonSystem.kt` | SeasonalGrid | 7 |
| MaterialFatigueSystem | `core/.../rpg/systems/MaterialFatigueSystem.kt` | MaterialFatigue | 5 |

**Gesamt: 26 neue Tests** (Anforderung: >=20). Alle BUILD SUCCESSFUL.

### System-Logik:
- **SnowSystem**: Akkumulation bei Schnee, Regrow bei trocken, clearAt an Spieler-Zelle (Trampeln nur bei Bewegung).
- **BloodSystem**: age() pro Tick (Eintrocknen). Spill-API fuer Events/Combat.
- **FootprintSystem**: Spur bei Zellenwechsel, fade() mit Wetter-Einfluss (Regen/Wind beschleunigen).
- **SeasonSystem**: 4 Jahreszeiten-Modi (Spring=Blumen, Summer=Gras, Autumn=Laub, Winter=NoOp). Spieler-Interaktion je Modus.
- **MaterialFatigueSystem**: Gleichmaessige Stress-Akkumulation + Heal-Gegengewicht. Impact-API fuer Events.

---

## Teil B -- DoodleWorldScene Integration

- Alle 5 Grids + Systeme erzeugt mit Grid-Dimensionen aus CollisionGrid.
- Season aus `ImageWorldDef.season` abgeleitet (Default: SUMMER).
- Overlays in `worldLayer` (scrollen mit Kamera).
- Render-Reihenfolge: Water > Blood > Snow > Footprints > Season > MaterialFatigue.
- DEBUG_HOTSPOTS bleiben zuletzt (nicht von Overlays verdeckt, da in worldLayer nach Overlays gezeichnet -- die Registry-Render-Calls steuern die Reihenfolge).

---

## Teil C -- Harness-Daten-Refactor (UnifiedSceneSpec)

```kotlin
data class UnifiedSceneSpec(
    val name: String,
    val map: ImageMapId = ImageMapId.TAVERN_INTERIOR,
    val season: Season? = null,
    val weather: Weather? = null,
    val prefill: (SystemRegistry) -> Unit = {},
    val playerCell: Pair<Int, Int>? = null,
)
```

- `renderUnifiedScene(spec)` baut den kompletten Unified-Runtime-Stack auf (Bild + Grid + Doodle + Registry + alle Overlays), wendet `prefill` an, rendert, speichert.
- Nutzt existierende `Weather`-Enum aus `MapConfig.kt` (kein Duplikat).
- `localCurrentDirVfs`-Zeile + Import NICHT angefasst (B007).
- 42 Legacy-Captures vollstaendig unveraendert.

---

## Teil D -- 6 neue Screenshots (alle 2560x1440)

| Name | Inhalt | Systeme aktiv |
|---|---|---|
| `unified_winter` | Schnee + Fussspuren-Trail | Snow, Footprint |
| `unified_blood_snow` | Frisches + altes Blut auf Schnee | Snow, Blood |
| `unified_spring` | Blumen-Overlay im Wildwood | Season (SPRING) |
| `unified_autumn` | Laub-Overlay im Wildwood | Season (AUTUMN) |
| `unified_material_fatigue` | Risse (hairline + broken) | MaterialFatigue |
| `unified_all` | Schnee + Blut + Fussspuren + Risse + Wasser | Alle 5 + Water |

Alle 49 Screenshots (43 bestehend + 6 neu) generiert ohne Regress.

---

## ImageWorldDef-Erweiterung

Additives `season: Season? = null` Feld eingefuegt. Bestehende Map-Definitionen
unveraendert (Default null = keine Saison-Praeferenz).

---

## ACCEPTANCE

```
./gradlew :core:desktopTest              -> BUILD SUCCESSFUL (26 neue Tests, alle gruen)
./gradlew :game:compileKotlinDesktop     -> BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop -> BUILD SUCCESSFUL
./gradlew :game:screenshot               -> BUILD SUCCESSFUL
   -> 6 neue PNGs (unified_winter, unified_blood_snow, unified_spring,
      unified_autumn, unified_material_fatigue, unified_all) alle 2560x1440
   -> ALLE 43 bestehenden Screenshots weiterhin erzeugt
```

---

## DO_NOT_TOUCH -- bestaetigt eingehalten

- `core/rpg/weather/*` -- nur konsumiert, nicht modifiziert
- `localCurrentDirVfs`-Zeile/Import in ScreenshotHarness -- unveraendert
- 42 Legacy-Captures -- unveraendert
- WorldScene, MapConfig, Shader-Dateien, GridOverlay, SystemRegistry -- nicht angefasst
- composeApp/, tools/, assets/, settings.gradle.kts -- nicht angefasst

---

## Bekannte Hinweise

- "Too many callbacks" Warnungen beim Screenshot-Harness sind normal (brief: "einmal wiederholen").
  Build schliesst trotzdem erfolgreich ab.
- SeasonSystem nutzt `SpringOverlay` als universellen Saison-Renderer in der Registry
  (zeigt Blumen im Fruehling, Laub im Herbst via SeasonalGrid-Daten). Fuer Sommer
  waere SummerOverlay besser -- aber das erfordert WindState, der hier nicht im
  SystemRegistry-Pattern lebt. Fuer Pfeiler 2b vorgemerkt.
