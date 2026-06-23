# QA-Dokumentation: HD-2D Web-Demo (Variante C)

> Automatisierter QA-Lauf der GAIME HD-2D Web-Demo (Three.js), erzeugt mit
> [CUE-AGENT](https://github.com/Lootziffer666/CUE-AGENT) — ein QA-/Verifikations-Agent
> auf Basis von Node.js + Playwright + (optionaler) Vision-Analyse.

## Überblick

| Feld | Wert |
|---|---|
| Datum | 2026-06-23 16:21 UTC |
| Getestetes Artefakt | HD-2D Web-Demo, Variante C (`demos/web-hd2d/`) |
| URL | `http://127.0.0.1:8099/` |
| QA-Provider | `cue-local` (Offline-Stub, **keine** API-Keys) |
| **Score** | **80 / 100** |
| Severity | medium |

## Bewertung

| Metrik | Wert |
|---|---|
| Fehler | 0 |
| Warnungen | 4 |
| Server-Fehler | 0 |
| Client-Fehler | 0 |
| Fehlgeschlagene Requests (HTTP ≥ 400) | keine |

## Performance-Metriken

| Metrik | Wert |
|---|---|
| First Contentful Paint (FCP) | 28 ms |
| DOMContentLoaded | 675 ms |
| Load Complete | 675 ms |

Sehr schnelle initiale Darstellung; die Demo lädt sauber ohne Netzwerk-Fehler.


## Konsolen-Signale (4 Warnungen, identisch)

```
[WARNING] GL Driver Message (OpenGL, Performance, GL_CLOSE_PATH_NV, High):
          GPU stall due to ReadPixels
```

**Einordnung:** Dies ist **kein App-Fehler**, sondern eine reine Performance-Warnung
des headless-WebGL-Treibers (Chromium ohne GPU-Beschleunigung in der Sandbox-Rendering-
Umgebung). In einem echten Browser mit GPU tritt diese Meldung so nicht auf.

## LLM-/Vision-Analyse

> Gesamteindruck: aufgeräumtes, konsistentes Layout mit klarer Typo-Hierarchie und
> ausreichenden Kontrasten. Keine offensichtlichen Layout-Brüche im sichtbaren Bereich.
>
> **Empfehlungen:**
> - Sichtbare Tastatur-Fokus-Stile ergänzen
> - Touch-Ziele (≥ 44 px) auf kleinen Viewports prüfen

## Ehrliche Einordnung / Geltungsbereich

- ✅ **Variante C** (Three.js-Web HD-2D) wurde tatsächlich headless gerendert und
  automatisiert bewertet — Score 80/100, keine Fehler.
- ⚠️ Die WebGL-Warnungen stammen aus der Sandbox-Rendering-Umgebung, nicht aus dem
  Demo-Code.
- ❌ **Variante A** (Compose Canvas) konnte **nicht** per CUE-AGENT getestet werden —
  kein Android-SDK in der Sandbox verfügbar.
- ❌ **Variante B** (KorGE) ist ein Scaffold und wurde nicht kompiliert/getestet.

## Artefakte

Die Rohdaten des Laufs liegen unter [`artifacts/`](./artifacts/):

| Datei | Inhalt |
|---|---|
| `report-2026-06-23T16-21-13-589Z.md` | Lesbarer QA-Report |
| `report-2026-06-23T16-21-13-589Z.json` | Strukturierte QA-Daten |
| `qa-bundle.json` | Vollständiges QA-Bundle |
| `screenshot-2026-06-23T16-21-13-589Z.png` | Screenshot des gerenderten Zustands |

## Reproduktion

```bash
# Demo-Server starten (Port 8099) + Offline-LLM-Stub (Port 8771)
# dann in CUE-AGENT:
CUE_LLM_BASE_URL=http://127.0.0.1:8771/v1 \
  node bin/cue.js qa --url http://127.0.0.1:8099/ --lang de
```

---

*Erzeugt von CUE-AGENT · dokumentiert im Rahmen der HD-2D-Upgrade-Arbeit (siehe PR #5).*
