# Production Workflow & QA

Complete reference for game production pipeline: project scaffolding, phase orchestration, QA verification, debug/profiling, release preparation, and AI asset generation workflows.

Based on knowledge from the threejs-game-skills collection (majidmanzarpour/threejs-game-skills).

---

## Game Director: Phase Orchestration

### Development Phases (in order)

1. **Gameplay Systems** - First playable slice, architecture, mechanics, input, camera
2. **External Asset Sourcing** - AI generation decisions, credential probing, asset pipeline
3. **AAA Graphics** - Model building, materials, VFX, lighting, visual scorecard
4. **UI Design** - HUDs, menus, overlays, responsive layout, touch controls
5. **Debug/Profile** - Bug fixing, performance profiling, optimization
6. **QA/Release** - Browser testing, screenshots, production build, deployment

### Phase Completion Requirements

Each phase requires:
- Implementation evidence (code changes)
- Verification evidence (build passes, screenshots, console clean)
- Reference checklist completion
- Remaining blockers documented

---

## Project Scaffold

### Recommended Vite + TypeScript + Three.js Setup

```bash
npm create vite@latest my-game -- --template vanilla-ts
cd my-game
npm install three @types/three
npm install -D vite
```

### Recommended Project Structure

```text
my-game/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
├── public/
│   └── assets/
│       ├── models/
│       ├── textures/
│       ├── audio/
│       └── fonts/
├── src/
│   ├── main.ts              - Entry point, renderer, game loop
│   ├── core/
│   │   ├── Engine.ts        - Renderer, resize, clock, RAF
│   │   ├── InputManager.ts  - Keyboard, mouse, touch, gamepad
│   │   ├── AudioManager.ts  - Web Audio context, SFX, music
│   │   └── AssetLoader.ts   - GLTF, textures, audio preloading
│   ├── game/
│   │   ├── GameState.ts     - State machine (menu, play, pause, gameover)
│   │   ├── GameLoop.ts      - Fixed/variable timestep, update order
│   │   └── ScoreManager.ts  - Points, combos, high scores
│   ├── entities/
│   │   ├── Player.ts        - Player controller, state, abilities
│   │   ├── Enemy.ts         - Enemy types, AI, spawning
│   │   └── Projectile.ts    - Bullets, abilities, pools
│   ├── systems/
│   │   ├── PhysicsWorld.ts  - Rapier/custom collision setup
│   │   ├── CollisionSystem.ts - Collision events, damage
│   │   ├── SpawnSystem.ts   - Entity spawning, waves, pools
│   │   └── CameraSystem.ts  - Follow, shake, transitions
│   ├── assets/
│   │   ├── MaterialLibrary.ts
│   │   ├── modelFactories/
│   │   └── ProceduralTextures.ts
│   ├── ui/
│   │   ├── HUD.ts           - In-game UI overlay
│   │   ├── Menu.ts          - Main menu, settings
│   │   └── GameOverScreen.ts
│   └── utils/
│       ├── ObjectPool.ts
│       ├── MathUtils.ts
│       └── Constants.ts
└── scripts/
    └── inspect-canvas.mjs   - Canvas pixel verification
```

### Vite Config for Games

```typescript
// vite.config.ts
import { defineConfig } from 'vite';

export default defineConfig({
  base: './', // relative for static hosting
  build: {
    target: 'es2020',
    outDir: 'dist',
    assetsInlineLimit: 0, // don't inline assets
    rollupOptions: {
      output: {
        manualChunks: {
          three: ['three'],
        },
      },
    },
  },
  server: {
    port: 5188,
  },
});
```

---

## Gameplay Systems Workflow

### One-Sentence Playable Loop Formula

**[VERB] + [OBJECTIVE] + [FEEDBACK] + [FAIL/RETRY]**

Examples:
- **Dodge** obstacles to **survive** as long as possible, **scoring** points for distance, **restart** on collision
- **Shoot** enemies to **clear** waves, **collecting** power-ups, **game over** when health depletes
- **Build** structures to **defend** your base, **earning** resources, **lose** when base is destroyed

### Update Order (Critical)

```typescript
function gameUpdate(dt: number): void {
  // 1. Input intents (what the player wants)
  inputManager.update();

  // 2. Fixed physics (deterministic simulation)
  physicsWorld.step(dt);

  // 3. Game state/collisions (rules, damage, scoring)
  collisionSystem.processEvents();
  gameState.update(dt);

  // 4. Entity updates (AI, spawning, abilities)
  entityManager.update(dt);

  // 5. VFX/Camera/UI (visual response to state)
  vfxSystem.update(dt);
  cameraSystem.update(dt);
  hud.update();

  // 6. Render
  renderer.render(scene, camera);
}
```

### Game Feel Tuning Checklist

- [ ] Movement acceleration/deceleration curves (not instant)
- [ ] Camera follow with damping and look-ahead
- [ ] Input buffering for actions (jump, attack)
- [ ] Hit pause (brief freeze on impact, 50-100ms)
- [ ] Camera shake on important events (with max clamp)
- [ ] Screen flash on damage
- [ ] Particle burst on collect/destroy
- [ ] Sound pitch variation on repeated actions
- [ ] Coyote time for platformer jumps
- [ ] Generous hitboxes for player-friendly actions

---

## Physics Engine Selection

### Decision Ladder

1. **Custom collision** - Arcade triggers, pickups, lanes, simple overlap
2. **Rapier** (default) - Serious rigid bodies, sensors, CCD, balls, vehicles
3. **cannon-es** - Small JS-only demos, low complexity
4. **Matter.js** - 2D only physics rendered with Three.js

### When to Use Each

| Game Type | Engine |
|-----------|--------|
| Endless runner, lane dodger | Custom |
| Mini golf, pool, pinball | Rapier |
| Platformer with slopes/ramps | Rapier |
| Physics puzzles, stacking | Rapier |
| Simple arcade shooter | Custom |
| Vehicle with collisions | Rapier |
| 2D platformer in 3D | Matter.js or Custom |

### Rapier Quick Setup

```typescript
import RAPIER from '@dimforge/rapier3d-compat';

await RAPIER.init();
const world = new RAPIER.World({ x: 0, y: -9.81, z: 0 });

// Fixed timestep
const FIXED_DT = 1 / 60;
let accumulator = 0;

function physicsUpdate(delta: number) {
  accumulator += Math.min(delta, 0.1);
  while (accumulator >= FIXED_DT) {
    world.timestep = FIXED_DT;
    world.step();
    accumulator -= FIXED_DT;
  }
}
```

---

## Debug & Profiling

### Blank Canvas Debugging Checklist

1. Check `renderer.domElement` is in DOM
2. Check canvas has non-zero dimensions
3. Check camera has correct aspect ratio
4. Check camera is looking at scene content
5. Check objects are within camera frustum (near/far planes)
6. Check materials aren't fully transparent
7. Check lights exist in the scene
8. Check `renderer.render(scene, camera)` is being called
9. Check for WebGL context loss errors
10. Check browser console for errors

### Performance Profiling Workflow

```typescript
// 1. Add stats.js for FPS monitoring
import Stats from 'stats.js';
const stats = new Stats();
document.body.appendChild(stats.dom);

// In render loop:
stats.begin();
renderer.render(scene, camera);
stats.end();

// 2. Log renderer info periodically
function logRendererInfo() {
  const info = renderer.info;
  console.table({
    'Draw calls': info.render.calls,
    'Triangles': info.render.triangles,
    'Geometries': info.memory.geometries,
    'Textures': info.memory.textures,
    'Programs': info.programs?.length,
  });
}

// 3. Use Chrome DevTools Performance tab
// - Record during gameplay
// - Look for long frames (>16ms)
// - Check for GC spikes
// - Identify CPU vs GPU bottleneck
```

### Common Performance Fixes

| Problem | Solution |
|---------|----------|
| Too many draw calls | InstancedMesh, merged geometry, shared materials |
| Too many triangles | LOD, simpler models, frustum culling |
| GC spikes | Object pooling, pre-allocated vectors |
| Shader compilation stutter | Warm up shaders on load |
| Large textures | Compress (KTX2), smaller dimensions, atlas |
| Shadow cost | Fewer casters, smaller maps, baked shadows |
| Post-processing cost | Reduce passes, lower resolution effects |
| Physics cost | Simpler colliders, sleep bodies, reduce CCD |

---

## QA Verification Workflow

### Pre-Release Checklist

1. **Build/Typecheck**
   ```bash
   npx tsc --noEmit
   npm run build
   ```

2. **Development Server Test**
   ```bash
   npm run dev  # Start dev server
   # Open http://localhost:5188
   ```

3. **Console/Error Check**
   - No JavaScript errors in console
   - No 404 asset loading errors
   - No WebGL warnings
   - No unhandled promise rejections

4. **Canvas Pixel Verification**
   - Canvas is not blank (has non-zero pixels)
   - Content renders at correct resolution
   - No black/white flash on load

5. **Input Path Testing**
   - Primary action works (move, shoot, jump)
   - Objective can be reached
   - Fail state triggers correctly
   - Restart/retry works cleanly
   - Pause/resume works

6. **Responsive/Mobile**
   - Test at 375x667 (mobile portrait)
   - Test at 1920x1080 (desktop)
   - Touch controls work (if applicable)
   - Text doesn't overflow
   - UI doesn't cover gameplay

7. **Performance Spot Check**
   - Stable 60 FPS on target device
   - No memory leaks (heap doesn't grow indefinitely)
   - No stuttering during gameplay

### Production Build Verification

```bash
# Build
npm run build

# Preview production build
npm run preview
# OR
npx serve dist

# Verify:
# - All assets load (no 404s)
# - Base path is correct for deployment target
# - Debug UI/logging is removed
# - Bundle size is reasonable
```

---

## AI Asset Generation (External Tools)

### 3D Generation (Tripo API)

For premium game assets that need high-fidelity 3D models:

- **Text-to-3D** - Describe the asset, get a GLB model
- **Image-to-3D** - Provide a concept image, get a 3D model
- **Texturing** - Re-texture existing models
- **Rigging** - Auto-rig characters for animation
- **Animation** - Retarget preset animations to rigged models
- **Stylization** - Convert to voxel/LEGO/low-poly styles

### Image Generation (Gemini API)

For 2D assets and references:

- **Concept art** - Character/vehicle/prop references
- **Textures** - Seamless material references
- **Sky/backgrounds** - Environment plates
- **UI art** - Logos, icons, badges, GUI panels
- **Image-to-3D input** - Clean references for 3D generation

### Audio Generation (ElevenLabs API)

For game audio:

- **SFX** - Impacts, pickups, UI sounds, weapons
- **Ambience** - Looping environmental audio
- **Voice/TTS** - Announcer lines, dialogue
- **Voice conversion** - Transform scratch performances

### Integration Rules

- Never call generation APIs from client-side game code
- Download generated assets immediately (URLs expire)
- Store API keys in environment variables only
- Commit generated assets to the project
- Use GLTFLoader for imported 3D models
- Use AnimationMixer for rigged/animated models
- Create collision proxies separate from generated meshes
- Track triangle/material/texture counts for performance budget

---

## UI Design Patterns

### Game UI State Inventory

Every game needs these UI states designed:

- **Gameplay HUD** - Health, score, ammo, minimap, objectives
- **Pause menu** - Resume, settings, quit
- **Settings** - Audio, graphics, controls, accessibility
- **Game over** - Score, stats, retry/menu buttons
- **Win/milestone** - Celebration, next level, rewards
- **Loading** - Progress bar, tips, loading animation
- **Touch controls** (mobile) - Virtual joystick, action buttons

### HUD Hierarchy

1. **Survival/status** (most important) - Health, shields, danger indicators
2. **Objective** - Score, progress, timer, wave counter
3. **Feedback** - Combo counter, kill feed, achievement popups
4. **Flavor** - Minimap, speedometer, compass, ammo type

### Responsive UI Rules

```css
/* Safe area padding for mobile */
.game-ui {
  padding-top: env(safe-area-inset-top);
  padding-bottom: env(safe-area-inset-bottom);
  padding-left: env(safe-area-inset-left);
  padding-right: env(safe-area-inset-right);
}

/* Touch targets minimum 44x44px */
.touch-button {
  min-width: 44px;
  min-height: 44px;
}

/* Text that fits without overflow */
.hud-text {
  font-size: clamp(12px, 2vw, 24px);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
```

### HTML Overlay Pattern (Three.js)

```typescript
// Separate HTML layer on top of canvas for UI
const uiLayer = document.createElement('div');
uiLayer.style.position = 'absolute';
uiLayer.style.top = '0';
uiLayer.style.left = '0';
uiLayer.style.width = '100%';
uiLayer.style.height = '100%';
uiLayer.style.pointerEvents = 'none';
document.body.appendChild(uiLayer);

// UI elements that need interaction:
const pauseButton = document.createElement('button');
pauseButton.style.pointerEvents = 'auto';
uiLayer.appendChild(pauseButton);
```

---

## Endless Runner Checklist

For the common endless runner genre, verify:

- [ ] Procedural/infinite level generation
- [ ] Increasing difficulty over time
- [ ] At least 3 obstacle types with unique silhouettes
- [ ] At least 2 collectible types
- [ ] Score system with distance + pickups
- [ ] High score persistence (localStorage)
- [ ] Speed increase over time
- [ ] Lane system OR free movement with boundaries
- [ ] Death/fail state with clear feedback
- [ ] Restart without page reload
- [ ] Mobile touch controls (swipe/tap)
- [ ] Camera follows player smoothly
- [ ] Background parallax for depth
- [ ] Audio: at minimum pickup and death sounds
