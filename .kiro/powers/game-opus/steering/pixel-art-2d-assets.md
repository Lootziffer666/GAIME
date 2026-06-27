# Pixel Art & 2D Game Asset Generation

Complete reference for generating pixel-art sprites, tilesets, sprite-sheet animations, and integrating 2D game assets. Covers AI-powered generation pipelines, palette systems, post-processing, Tiled integration, and SpriteCook workflows.

Based on knowledge from ianlintner/ai-pixel-art-image-generation and SpriteCook/skills.

---

## Pixel Art Generation Pipeline

### Core Pipeline (Generate + Pixelize)

The standard workflow for creating true pixel art from AI image generators:

1. **Generate** - Text-to-image via OpenAI/Azure/Gemini/fal.ai
2. **Downscale** - Lanczos to 2x target, then nearest-neighbor to final size
3. **Palette Quantize** - Snap every color to the chosen palette
4. **Alpha Threshold** - Kill semi-transparent edges (threshold at 128)
5. **Optional Outline** - 1px dark ring via dilate-XOR-darkest-neighbour

Result: RGBA PNG with exactly the target palette's colors, hard edges, crisp alpha.

### Why Post-Processing Matters

AI models do NOT emit true grid-aligned pixel art. They produce "pixel-art-styled" rasterized images with:
- Anti-aliased edges (sub-pixel blending)
- Colors outside any specific palette
- Soft gradients instead of hard transitions

The pixelization pipeline converts this into real pixel art.

---

## Prompt Engineering for Pixel Art

### Prompt Structure

```text
{subject}, {view angle}, {pose/action},
{style qualifier}, {background constraint},
{negative constraints}
```

### Building Blocks

| Element | Recommended Values |
|---------|-------------------|
| Style qualifier | "pixel art sprite", "flat pixel art", "16-bit RPG sprite", "retro console style" |
| View angles | "side view profile", "top-down view", "3/4 isometric view", "front view" |
| Background | "plain flat background", "solid magenta background", "transparent background" |
| Negatives | "no text, no watermark, no logo, no anti-aliasing, hard edges, no gradients" |

### Strong Prompt Examples

```text
# Single sprite
orange tabby cat, front view, idle standing pose.
Pixel art sprite, clean hard edges, limited palette,
plain flat background, no text, no watermark, no anti-aliasing.

# Tileset tile
top-down game tile, seamless, centered, grass terrain.
Single tile, no border, no frame, no UI chrome, tileable edges.
Pixel art, hard edges, limited palette.

# Character for rigging/animation
knight with blue cape, side view profile, standing still.
Pixel art sprite, clean hard edges, DB16 palette,
plain flat background, no text, no watermark.
```

### What to Avoid

- "photorealistic" - fights pixel-art style
- "smooth shading" / "gradients" - adds AA the quantizer fights
- Real-person names - content filter rejection
- Copyrighted character names - rejected and counterproductive

---

## Palette System

### Available Palettes

| Name | Colors | Vibe | Best at size |
|------|--------|------|--------------|
| `gameboy` | 4 | Classic GameBoy greens, high contrast | 16, 32 |
| `pico8` | 16 | PICO-8 fantasy console | 16, 32 |
| `db16` | 16 | Dawnbringer, warm/broad hues | 32, 64 |
| `db32` | 32 | Dawnbringer, strong saturation range | 32, 64 |
| `nes` | 54 | NES approximation | 32, 64 |
| `aap64` | 64 | Adigun Polack wide-gamut | 64, 128 |

### Palette Selection Guide

| Keyword Pattern | Palette | Why |
|-----------------|---------|-----|
| metal, steel, armor, stone, dungeon, rock | `db32` | Mid-greys present |
| tropical, beach, coral, jungle, underwater | `aap64` | Wide warm/cool range |
| gameboy, monochrome, green only | `gameboy` | 4-shade monochrome |
| arcade, nes, 8-bit | `nes` | Canonical NES palette |
| knight, fantasy, rpg, character, warrior | `db16` | Canonical RPG palette |
| terrain, tile, overworld, map, landscape | `db32` | Earth/foliage/water tones |
| detailed, portrait, hi-detail | `aap64` | 64 colours for subtle shading |
| (no match) | `db32` | Default safe choice |

### Color Values

**PICO-8 (16 colors):**
```
#000000 #1d2b53 #7e2553 #008751
#ab5236 #5f574f #c2c3c7 #fff1e8
#ff004d #ffa300 #ffec27 #00e436
#29adff #83769c #ff77a8 #ffccaa
```

**DB32 (32 colors):**
```
#000000 #222034 #45283c #663931
#8f563b #df7126 #d9a066 #eec39a
#fbf236 #99e550 #6abe30 #37946e
#4b692f #524b24 #323c39 #3f3f74
#306082 #5b6ee1 #639bff #5fcde4
#cbdbfc #ffffff #9badb7 #847e87
#696a6a #595652 #76428a #ac3232
#d95763 #d77bba #8f974a #8a6f30
```

---

## Sprite Generation

### Size Recommendations

| Target Size | Use Case | Quality Setting |
|-------------|----------|-----------------|
| 16x16 | Tiny sprites, item icons | medium, `pico8`/`gameboy` |
| 32x32 | Standard top-down sprites | medium, `db16`/`db32` |
| 64x64 | Detailed hero sprites | medium/high, `db32`/`aap64` |
| 128x128 | Portraits, high-detail props | high, `aap64` |

### SpriteCook Generation

```
generate_game_art:
  prompt: "armored knight, side view, idle stance"
  width: 64
  height: 64
  pixel: true
  bg_mode: "transparent"
  theme: "dark fantasy medieval"
  style: "16-bit SNES style"
  smart_crop: true
  smart_crop_mode: "tightest"
```

### Character Perspectives (SpriteCook)

| Perspective | Default Animations | Presets Available |
|-------------|-------------------|-------------------|
| `platformer` | idle, walk, jump | idle, walk, jump, run, attack, hurt, death |
| `isometric` | idle, walk_down, walk_right | idle, idle_back, walk_down, walk_right, jump, run, attack, hurt, death |
| `topdown` | idle, walk_up, walk_right, walk_down | idle, idle_back, idle_right, walk_up, walk_down, walk_right, attack, hurt, death |

---

## Sprite Sheet Animations

### Walk Cycle (4 Frames)

Standard walk cycle uses the contact/passing pose pattern:
- Frame 0: Contact (right foot forward)
- Frame 1: Passing (left foot passing)
- Frame 2: Contact mirror (left foot forward)
- Frame 3: Passing mirror (right foot passing)

### Animation Generation Approaches

**Approach 1: AI Frame Consistency (Gemini reference)**
- Generate Frame 0 as the canonical silhouette
- Use Frame 0 as reference for Frames 1-N with Gemini
- Post-process all frames with same palette
- Apply shared bounding box crop across all frames

**Approach 2: SpriteCook Animate**
- Generate base character still image
- Use `animate_game_art` with motion prompt
- Get back spritesheet with consistent frames

### Animation Prompt Writing

Good animation prompts describe:
- What moves and how it moves
- What stays stable
- How visible props/weapons are used
- Keep motion grounded in the existing character

**Example idle:**
```text
The armored soldier stands in a steady combat stance, subtly bobbing up
and down in a rhythmic breathing idle. His rifle shifts slightly in his
grip, maintaining a high state of readiness.
```

**Example attack:**
```text
The armored soldier raises his rifle and fires several shots, with muzzle
flashes appearing at the barrel tip. His body recoils with each shot while
his head remains focused forward.
```

### Frame Rate Guidelines

| Animation Type | Frames | FPS | Duration per frame |
|---------------|--------|-----|-------------------|
| Idle | 2-4 | 4-6 | 167-250ms |
| Walk | 4-6 | 8-10 | 100-125ms |
| Run | 4-8 | 10-12 | 83-100ms |
| Attack | 4-6 | 10-12 | 83-100ms |
| Death | 4-8 | 8 | 125ms |

---

## Tileset Generation

### Tileset Types

| Piece Set | Tiles | Use Case |
|-----------|-------|----------|
| `15-piece` | 15 | Standard autotile top-down |
| `17-piece-base` | 17 | Inner-corner base tiles |
| `autotile-16-set` | 16 | Side-view platformer |

### Tileset Prompts

Keep prompts short and material-focused:
```text
# Good
"mossy dungeon floor"
"snowy stone path"
"volcanic rock and lava"   (for two_surfaces)
"clean wooden floor"

# Bad (too complex)
"a dungeon with monsters and treasure chests and a hero"
```

### Tiled Export Format (TSX/TMJ)

**TSX (Tileset XML):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<tileset version="1.10" tiledversion="1.10.2"
         name="overworld" tilewidth="32" tileheight="32"
         tilecount="16" columns="4">
  <image source="overworld.png" width="128" height="128"/>
  <tile id="0"><properties><property name="name" value="grass"/></properties></tile>
  <tile id="1"><properties><property name="name" value="dirt"/></properties></tile>
</tileset>
```

**TMJ (Map JSON):**
```json
{
  "width": 10, "height": 10,
  "tilewidth": 32, "tileheight": 32,
  "layers": [{
    "type": "tilelayer",
    "data": [1, 1, 1, 2, 2, ...]
  }],
  "tilesets": [{
    "firstgid": 1,
    "source": "overworld.tsx"
  }]
}
```

**Key rules:**
- TSX `<image source>` is relative to TSX file - keep PNG + TSX together
- TMJ `data` uses global IDs: tile 0 in TSX = gid 1 in TMJ
- Animated tiles use `<animation>` block inside `<tile>`

### Seamless Tiling

Post-process approach for seamless tiles:
1. Generate source at 3x tile size
2. Center-crop for edge consistency
3. If seam_diff >= 12.0, apply torus-wrap feather blend
4. Final pixelization to target size

---

## QA Metrics for Pixel Art

### Hard Gates (must pass)

| Metric | Threshold | Description |
|--------|-----------|-------------|
| `palette_fidelity` | == 1.0 | All pixels match palette exactly |
| `alpha_crispness` | >= 0.999 | No semi-transparent pixels |
| `tile_seam_diff_mean` | <= 12.0 | Tile edges match (tilesets) |
| `silhouette_iou_f0_f2` | >= 0.85 | Shape consistency in animation |
| `bbox_drift_x` | <= 6px | Horizontal stability (animation) |
| `bbox_drift_y` | <= 3px | Vertical stability (animation) |
| `baseline_alignment` | >= 3 contiguous opaque in lowest row | Character grounded |

### Soft Gates (warnings)

| Metric | Threshold | Description |
|--------|-----------|-------------|
| `outline_coverage` | >= 0.85 | 1px outline present |
| `palette_coverage` | 0.15-0.60 | Not mono, not too busy |

### QA-Driven Iteration Loop

1. First pass: low quality, no QA - eyeball shape and pose
2. Second pass: medium quality + QA - read metrics
3. On soft fails: adjust prompt or palette
4. On hard fails: regenerate with different seed/prompt (don't tune)

---

## Integration with Game Engines

### Three.js / R3F Integration

```typescript
// Load pixel art sprite as texture
const texture = new THREE.TextureLoader().load('/sprites/character.png');
texture.magFilter = THREE.NearestFilter;  // CRITICAL for pixel art
texture.minFilter = THREE.NearestFilter;
texture.generateMipmaps = false;
texture.colorSpace = THREE.SRGBColorSpace;

// Create sprite plane
const material = new THREE.MeshBasicMaterial({
  map: texture,
  transparent: true,
  alphaTest: 0.5,
  side: THREE.DoubleSide,
});
const sprite = new THREE.Mesh(
  new THREE.PlaneGeometry(1, 1),
  material
);
```

### Sprite Sheet Animation in Three.js

```typescript
class PixelSpriteAnimator {
  private texture: THREE.Texture;
  private columns: number;
  private rows: number;
  private currentFrame = 0;
  private elapsed = 0;
  private fps: number;

  constructor(texture: THREE.Texture, columns: number, rows: number, fps: number) {
    this.texture = texture;
    this.columns = columns;
    this.rows = rows;
    this.fps = fps;

    // Pixel-perfect filtering
    texture.magFilter = THREE.NearestFilter;
    texture.minFilter = THREE.NearestFilter;
    texture.generateMipmaps = false;

    // Set UV repeat for single frame
    texture.repeat.set(1 / columns, 1 / rows);
  }

  update(dt: number, startFrame: number, endFrame: number): void {
    this.elapsed += dt;
    if (this.elapsed >= 1 / this.fps) {
      this.elapsed = 0;
      this.currentFrame++;
      if (this.currentFrame > endFrame) this.currentFrame = startFrame;

      const col = this.currentFrame % this.columns;
      const row = Math.floor(this.currentFrame / this.columns);
      this.texture.offset.set(
        col / this.columns,
        1 - (row + 1) / this.rows
      );
    }
  }
}
```

### Godot Integration (SpriteFrames)

For importing pixel art spritesheets into Godot:

1. Create `SpriteFrames` resource with `AtlasTexture` sub-resources per frame
2. Each frame: `region = Rect2(i * frame_width, 0, frame_width, frame_height)`
3. Add `AnimatedSprite2D` node referencing the SpriteFrames
4. Set `autoplay` for idle animation
5. Import settings: Filter = Nearest, Mipmaps = Off

### Tilemap Integration (Tiled -> Game Engine)

```typescript
// Load TMJ map in Three.js
async function loadTiledMap(tmjPath: string): Promise<TileMap> {
  const response = await fetch(tmjPath);
  const mapData = await response.json();

  const tileWidth = mapData.tilewidth;
  const tileHeight = mapData.tileheight;
  const mapWidth = mapData.width;
  const mapHeight = mapData.height;

  // Load tileset texture
  const tilesetPath = mapData.tilesets[0].source;
  const texture = await new THREE.TextureLoader().loadAsync(
    tilesetPath.replace('.tsx', '.png')
  );
  texture.magFilter = THREE.NearestFilter;
  texture.minFilter = THREE.NearestFilter;

  // Create instanced mesh for each tile type
  // or use a single plane with UV offsets per tile
  // ...
}
```

---

## Available AI Image Models

### For Pixel Art Sprites

| Model | Provider | Best For |
|-------|----------|----------|
| `gpt-image-2` | OpenAI/Azure | Highest quality silhouettes |
| `gemini-2.5-flash-image` | Google | Multi-frame animation consistency |
| `fal-ai/recraft/v3/text-to-image` | fal.ai | SOTA pixel art tiles/sprites |
| `fal-ai/flux/schnell` | fal.ai | Fast iteration, cheap batches |

### For Style-Consistent Sets

| Model | Provider | Best For |
|-------|----------|----------|
| `gemini-3.1-flash-image-preview` | Google/SpriteCook | Recommended default |
| `gemini-3-pro-image-preview` | Google/SpriteCook | Highest quality |
| `fal-ai/flux-pro/kontext` | fal.ai | Reference + targeted edits |

---

## Pixel Art Tools Summary

### ianlintner/ai-pixel-art-image-generation

| Script | Purpose |
|--------|---------|
| `generate_image.py` | General-purpose text-to-image |
| `generate_sprite.py` | Single pixel-art sprite with palette |
| `generate_tileset.py` | N tiles packed into sheet + TSX + TMJ |
| `generate_animation.py` | 2-8 frame sprite-sheet with TSX animation |
| `pixelize.py` | Post-process existing image to pixel art |
| `qa_report.py` | QA metrics on existing pixel-art PNG |

### SpriteCook MCP Tools

| Tool | Purpose |
|------|---------|
| `generate_game_art` | Still image generation (pixel or detailed) |
| `generate_tileset` | Autotile tileset generation |
| `animate_game_art` | Animate existing asset into spritesheet |
| `generate_character` | Base character with perspective settings |
| `generate_character_animations` | Preset/custom animations for character |
| `export_godot_character_package` | Package for Godot import |
