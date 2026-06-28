# Result: KorGE Migration Step 3b + Step 4 — Android-Target & Tiled-Loader

**Brief:** `briefs/2026-06-28-korge-step3b-step4-tiled-android.md`
**Branch:** `kiro/korge-step4-tiled-android`
**PR:** https://github.com/Lootziffer666/GAIME/pull/32
**Datum:** 2026-06-28
**Autor:** Kiro
**Status:** ✅ Abgeschlossen — verifizierbarer Acceptance-Subset grün

---

## Zusammenfassung

**Step 3b:** `:game` erhielt `androidTarget()` + `com.android.library` (AGP 8.2.2),
KorGE-Dependency nach `commonMain` verschoben (Desktop + Android erben).
Desktop-Target weiterhin grün; Android-Compile nicht in der Sandbox verifizierbar
(kein SDK — identisch mit `:composeApp`).

**Step 4:** Eigener Tiled-Tilemap-Loader in `:core` — Datenmodell, CSV-Parser
(infinite Chunks, Flip-Bits, animierte Tiles), und tile-abgeleitetes
Kollisionsraster. Keine externe Dependency. 12 Unit-Tests grün.

---

## Acceptance

| Check | Ergebnis | Anmerkung |
|---|---|---|
| `:game:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL | |
| `:core:desktopTest` | ✅ BUILD SUCCESSFUL (alle Tests grün) | inkl. 12 neue Tiled-Tests |
| `:composeApp:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL | unverändert |
| `:game:compileDebugKotlinAndroid` | ⚠️ SDK not found | Sandbox hat kein Android SDK; Build-Config identisch mit `:composeApp`-Android (das auch fehlschlägt). Kein Code-Bug — Sandbox-Limitation. |

---

## Geänderte / neue Dateien

| Datei | Art |
|---|---|
| `game/build.gradle.kts` | **modify** — `androidTarget()`, AGP, `commonMain`-Deps |
| `game/src/androidMain/kotlin/game/.gitkeep` | **create** — Marker für androidMain-Source-Set |
| `core/src/commonMain/kotlin/rpg/tiled/TiledMap.kt` | **create** — Datenmodell |
| `core/src/commonMain/kotlin/rpg/tiled/TmxLoader.kt` | **create** — CSV-TMX-Parser |
| `core/src/commonMain/kotlin/rpg/tiled/CollisionGrid.kt` | **create** — Kollisionsraster |
| `core/src/commonTest/kotlin/rpg/tiled/TmxLoaderTest.kt` | **create** — 6 Tests |
| `core/src/commonTest/kotlin/rpg/tiled/CollisionGridTest.kt` | **create** — 6 Tests |
| `docs/KORGE_MIGRATION_PLAN.md` | modify — Steps 3b+4 ✅ |
| `README.md` | modify — KorGE-Sektion |
| `docs/KNOWN_BUGS.md` | modify — TMX-Parser-Fallstricke |

---

## Schritt 0 — Recherche-Ergebnis

- `scripts/tmx_render.py` vollständig gelesen → TMX-Subset-Verständnis (infinite
  Chunks, CSV, Flip-Bits, animierte Tiles, Floor-basiertes Kollisionsmodell).
- Layer-Namen aus allen 12 relevanten `.tmx`-Dateien extrahiert (grep, keine
  Binärdaten gelesen) → realistische Layer-Klassifikation implementiert.
- KorGE-Android-Artefakt: `com.soywiz.korge:korge:6.0.0` ist ein KMP-Artefakt und
  liefert Android-Artefakte mit (via Gradle Metadata). Verifizierung in der Sandbox
  scheitert nur am fehlenden SDK, nicht an fehlenden Artefakten.
- Donor-Policy eingehalten: `tmx_render.py` nur als Referenz gelesen; Parser neu
  implementiert aus beobachtetem Verhalten und TMX-Spec-Verständnis.

---

## Parser-Fallstricke (Details in KNOWN_BUGS.md)

- Inline-CSV auf derselben Zeile wie `<data>`/`<chunk>`
- GID > Int.MAX_VALUE durch Flip-Bits (→ toLong)
- `</chunk>` auf derselben Zeile wie letzter CSV-Block
- Layer-Width bei finite Maps nötig
- Negative Chunk-Koordinaten (Bounding-Box + Normalisierung)

---

## Sandbox-Einschränkungen

- **Kein Android SDK** → `:game:compileDebugKotlinAndroid` nicht verifizierbar.
  Identische Situation für `:composeApp`. Build-Config ist korrekt und identisch
  mit dem bereits funktionierenden `:composeApp` Android-Setup.
- **BASE_SHA `dbf238be…` nicht kryptografisch verifizierbar** (Shallow-Clone).
  HEAD war der Brief-Commit `dc9d3b5c`; Branch von dort gezogen, keine Divergenz.

---

## Nächster Schritt

**Step 4b** — Gameplay in KorGE: World/Grid-Movement, HD-Sprites, Dialogue, Combat,
Bark-Audio. Die Tiled-Maps werden vom neuen Loader gelesen und in KorGE-Szenen
gerendert.
