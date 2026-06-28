# Result: Step 11 — Render-Gerüst: Doodle-Figuren @ 1440p + Bild/Grid-Kopplung

**Brief:** `briefs/2026-06-28-korge-step11-doodle-render-scaffold.md`
**Branch:** `kiro/korge-step11-doodle-render-scaffold` (PR#49)
**PR:** https://github.com/Lootziffer666/GAIME/pull/49
**Datum:** 2026-06-28 · Integration-Review: Claude
**Status:** ✅ Fundament + Grid-Kopplung · ⚠️ Doodle-*Look* vorläufig (Art-Direction-Tuning offen)

---

## Acceptance

| Check | Ergebnis |
|---|---|
| `:core:desktopTest` | ✅ BUILD SUCCESSFUL |
| `:game:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:composeApp:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:game:screenshot` | ✅ inkl. 2 neue @2560×1440 |

---

## Geliefert
- **`DoodleLineFilter`** (neu, `fixedLocation=13`, in `ShaderEffects` registriert + time-driven):
  Luminanz-Gradient-Kantenerkennung (4 Nachbarn) → Kanten abdunkeln; `u_Time`-Boil-Jitter.
  Anime4K-Konzept neu implementiert, kein Fremdcode (Donor-Policy). Per-Layer angewandt
  (`charLayer.filter`), Hintergrund ungefiltert. **Code korrekt, sauber strukturiert.**
- **`grid_overlay_debug.png`** ✅ **klarer Erfolg:** CollisionGrid aus `tavern_interior.tmx`
  halbtransparent über das gemalte Bild — rot = blockiert (Möbel/Wände/Rand), grün = begehbar
  (Boden/Teppich). Beweist „Bild = Haut, Grid = Logik": das unsichtbare Raster deckt sich exakt
  mit der Malerei.
- **`doodle_1440p.png`**: hi-res Taverne (scharf) + große Figur mit angewandtem Filter.
- Docs (`SHADER_VISION`, `KORGE_MIGRATION_PLAN`) aktualisiert.

## Integration-Anpassung (Claude)
Kiros Capture platzierte die Figuren grid-abgeleitet bei ~92px → auf 1440p winzig, Doodle-Linien
nicht erkennbar. Auf Proof-Closeup (charScale 7.5, große Figuren) umgestellt; Koordinaten korrigiert
(erst lagen sie off-screen).

## ⚠️ Offene Schwachstelle: der Doodle-*Look* trägt noch nicht
Auf den 7.5× hochskalierten 64px-Sprites werden die Kanten **weich** statt feine Cartoon-Linien —
der Effekt liest im Screenshot nicht überzeugend. Ursachen/Tuning-Hebel (Art-Direction, Owner-Auge):
1. **Upscale-Methode:** weiches Hochskalieren glättet die Gradienten, die der Filter braucht.
   Evtl. nearest-Upscale + Filter, oder Filter VOR dem Upscale.
2. **Linienstärke/-dicke:** aktuell nur Abdunkeln (`*0.85`); echtes „Verdicken" der Linie
   (Dilation) fehlt.
3. **Boil-Amplitude** im Standbild nicht beurteilbar (braucht Animation).
Das ist bewusst ein **eigener Tuning-Schritt mit dem Owner** — nicht per Brute-Force-Render zu
erraten. Das **Fundament** (Filter-Pipeline, Per-Layer, 1440p, Grid-Kopplung) steht.

## DO_NOT_TOUCH — eingehalten ✅
B007 `localCurrentDirVfs` intakt (8.×); bestehende Shader nur konsumiert; core/composeApp/mapbuilder
unberührt.

## Offen / Folge
- **Doodle-Look art-dirigieren** (Linien-Dilation, Upscale-Methode, Boil am Bewegtbild) — mit Owner.
- **Harness-Sprite-Skalierung:** 64px-Sprites 7.5× = weich; für echte Tests evtl. größere Quell-Sprites.
- Echte 1440p-Fenster-Ausgabe (`:game:run`), Anime4K HQ A+B als Shader-Austausch, spielbare Szene
  (WorldScene mit Bild-Hintergrund + Grid, Bewegung live).

## Stand nach Merge
main aktualisiert. Render-Gerüst steht: hi-res gemalter Hintergrund + per-Layer-Filter + 1440p +
nachgewiesene Bild/Grid-Kopplung. Der Doodle-Look selbst ist der nächste, owner-geführte Tuning-Schritt.
