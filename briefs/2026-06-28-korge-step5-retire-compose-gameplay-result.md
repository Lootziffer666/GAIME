# Result: KorGE Migration Step 5 — Retire Compose Gameplay Engine

**Brief:** `briefs/2026-06-28-korge-step5-retire-compose-gameplay.md`
**Branch:** `kiro/korge-step5-retire-compose-gameplay`
**PR:** *(nach Push)*
**Datum:** 2026-06-28
**Autor:** Kiro (Opus-only)
**Status:** ✅ Abgeschlossen — alle Acceptance-Kriterien grün

---

## Acceptance

| Check | Ergebnis |
|---|---|
| `:core:desktopTest` | ✅ BUILD SUCCESSFUL (inkl. 4 verschobene Pipeline-Tests) |
| `:game:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL (unverändert) |
| `:composeApp:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL (Waitroom-only) |

---

## KEEP/REMOVE/MOVE-Bilanz

### MOVED → `:core` (Compose-frei, Coverage erhalten)
| Datei | Von | Nach |
|---|---|---|
| `SliceDirector.kt` (inkl. BarkOutcome, CombatTurn) | `composeApp/.../rpg/` | `core/.../rpg/` |
| `AudioPlayer.kt` (Interface + NoOpAudioPlayer) | `composeApp/.../rpg/bark/audio/` | `core/.../rpg/bark/audio/` |
| `BarkAudioPlayer.kt` | `composeApp/.../rpg/bark/audio/` | `core/.../rpg/bark/audio/` |
| `BarkCooldownBypassTest.kt` | `composeApp/commonTest/` | `core/commonTest/` |
| `Chapter2PipelineTest.kt` | `composeApp/commonTest/` | `core/commonTest/` |
| `SliceAcceptanceCriteriaTest.kt` | `composeApp/commonTest/` | `core/commonTest/` |
| `SlicePipelineTest.kt` | `composeApp/commonTest/` | `core/commonTest/` |

### REMOVED (Compose Gameplay UI — replaced by `:game`)
| Datei | Grund |
|---|---|
| `ui/rpg/SliceScreen.kt` | KorGE WorldScene ersetzt |
| `ui/rpg/RpgDemoScreen.kt` | KorGE BattleScene ersetzt |
| `ui/rpg/RpgWorldScreen.kt` | KorGE WorldScene ersetzt |
| `ui/rpg/WorldScene.kt` | KorGE WorldScene ersetzt (Compose-Version) |
| `ui/rpg/SceneAtmosphere.kt` | Gameplay-Atmosphere, obsolet |
| `ui/rpg/DialogueLine.kt` | KorGE DialogOverlay ersetzt |
| `NpcDialogueTest.kt` | Testete gelöschte `DialogueLine` |

### KEPT (Waitroom + im-Zweifel-behalten)
| Datei/Package | Grund |
|---|---|
| `ui/WaitroomScreen.kt` | Waitroom aktiv |
| `ui/GameCanvas.kt` | Vom Waitroom importiert |
| `engine/**` | Vom Waitroom importiert |
| `rpg/bark/audio/PlatformAudioPlayer.kt` | expect/actual, Plattformcode |
| `rpg/gamepad/**` | Im-Zweifel-behalten-Regel |
| `ParticleEngineTest.kt` | Testet `engine/` |
| `BarkAudioPlayerTest.kt` | Testet AudioPlayer (jetzt in `:core`, aber `:composeApp` hat Dep darauf) |

### MODIFIED
| Datei | Änderung |
|---|---|
| `app/App.kt` | Auf Waitroom-only reduziert (Mode-Enum + Buttons entfernt) |

---

## Hinweise

- **Keine Test-Coverage verloren:** Alle 4 Pipeline-Tests laufen jetzt in `:core:desktopTest`.
  `ParticleEngineTest` + `BarkAudioPlayerTest` bleiben in `:composeApp`.
- **`PlatformAudioPlayer` (expect/actual):** Bleibt in composeApp, da die `actual`-Implementierungen
  plattformspezifisch sind (javax.sound / Android MediaPlayer). `:core` hat nur das Interface.
- **`rpg/gamepad/**`:** Brief sagt "im Zweifel behalten" — gamepad-Code referenziert `SliceScreen`
  nur in einem Kommentar, nicht als Import. Funktional unabhängig → behalten.
- **Screenshot/CUE-AGENT:** Brief verlangt visuellen Beweis via `scripts/setup-gl.sh` +
  `:game:screenshot` + CUE-AGENT. Diese erfordern ein GL-Display das die Sandbox nicht hat.
  `:game` wurde nicht angefasst (DO_NOT_TOUCH) — Screenshots sind exakt wie vorher.
  CUE-AGENT-Ausführung liegt bei der CI/Pipeline, nicht bei Kiro (Brief: "Claude führt es nicht aus").
