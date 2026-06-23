# Variant C — True HD-2D with Three.js (web)

A fully self-contained, runnable HD-2D demo: pixel-art **sprites billboarded in a
real 3D world**, lit by 3D lights, framed by a tilted camera, and finished with a
post-processing stack — the genuine Octopath-Traveler "HD-2D" approach.

Everything is **procedural** — the pixel art (heroes, Questbook, trees, ground,
dust) is painted to `<canvas>` at runtime, so there are **no asset downloads**.
Three.js loads from a CDN via an import map (no build step).

## Run it

Any static file server works (ES modules require `http://`, not `file://`):

```bash
cd demos/web-hd2d

# pick one:
python3 -m http.server 8099
# or
npx serve .
```

Then open <http://localhost:8099>.

## Controls

- **WASD / Arrow keys** — move Nib (the player sprite)
- **Mouse drag** — orbit the camera
- **Top-right panel** — toggle Bloom / Depth-of-Field / Vignette / Pixelation

## What makes it "true" HD-2D (vs. Variants A & B)

| Feature | How it's done |
|---------|---------------|
| 2D sprites in 3D space | `PlaneGeometry` meshes, Y-axis billboarded toward the camera every frame |
| Crisp pixel art | `CanvasTexture` + `NearestFilter`, `generateMipmaps = false` |
| Real depth | perspective camera at a high angle, fog, ground plane, scattered trees |
| Dynamic lighting + shadows | `DirectionalLight` (moonlight, shadow-casting) + `PointLight` lantern + `PointLight` book glow |
| Bloom | `UnrealBloomPass` — only emissive/bright pixels (Questbook, motes, lantern) glow |
| Depth-of-field (tilt-shift) | `BokehPass` focused on the party, blurring far/near |
| Vignette | custom `ShaderPass` |
| Optional pixelation | custom `ShaderPass` (off by default) |

## Files

- `index.html` — page shell, HUD, FX toggle panel, Three.js import map
- `hd2d.js` — the entire scene (sprite painter, world, lights, post-FX, loop)

## Verification status

- ✅ `node --check` — valid ES-module syntax
- ✅ All 8 `three@0.160.0` CDN module paths return HTTP 200
- ⚠️ **Not** render-tested headlessly in this environment (sandbox limitation).
  Run locally in any modern browser with WebGL2 to view.
