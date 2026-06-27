# AAA Graphics Pipeline

Production graphics architecture for upgrading Three.js games from basic/prototype visuals to premium quality. Covers visual scorecard, procedural model recipes, render recipes, material libraries, and the hybrid AI asset pipeline.

Based on knowledge from the threejs-game-skills collection (majidmanzarpour/threejs-game-skills).

---

## Visual Scorecard

Score active-play screenshots (not idle title screens). Use this to evaluate and track visual quality.

### Scoring Scale

- **0: Placeholder** - Default primitives, sparse world, debug UI, no evidence
- **1: Basic styled** - Playable and themed, but obvious prototype assets
- **2: Premium stylized** - Authored silhouettes, material systems, cohesive UI/world
- **3: Showcase** - Strong art direction, memorable hero, dense authored detail

### 10 Categories

1. **Art direction** - Theme consistency across forms, materials, UI, world
2. **Hero/player** - Authored silhouette, state cues, collision proxy
3. **Obstacles/enemies** - Readable variants with telegraphs and material cues
4. **Rewards/interactables** - Authored forms with idle/collect states
5. **World/environment** - Layered prop kit with depth and scale cues
6. **Materials/textures** - Shared material roles, procedural detail
7. **Lighting/render** - Intentional tone mapping, key/fill/rim, depth
8. **VFX/motion** - Event-driven effects that clarify gameplay
9. **UI/HUD** - Genre-specific states, responsive text fit
10. **Performance evidence** - Renderer counts, build/browser QA

### Premium Thresholds

- Every category at least 2, average at least 2.3
- Desktop and mobile screenshots captured
- Renderer diagnostics reported

### Automatic Failures (prevent premium claims)

- Active screenshot is primitive-dominant
- World is mostly stretched boxes or flat planes
- Hero is mostly default primitives plus glow
- Fog/darkness/bloom hides missing geometry
- Game is not playable through real input

---

## Production Graphics Architecture

### Recommended File Structure

```text
src/assets/MaterialLibrary.ts      - Named material roles
src/assets/ProceduralTextures.ts   - Canvas/procedural detail
src/assets/DecalShapes.ts          - Panel lines, marks, icons
src/assets/ModelDiagnostics.ts     - Mesh/material/triangle counts
src/assets/modelFactories/
  HeroFactory.ts                   - Player/vehicle model
  ObstacleFactory.ts               - Hazard family
  RewardFactory.ts                 - Pickup/interactable family
  WorldPropKit.ts                  - Modular environment pieces
src/systems/LightingRig.ts         - Camera-aware lighting
src/systems/RenderPipeline.ts      - Renderer setup, post-processing
src/systems/VfxSystem.ts           - Pooled event-driven effects
src/systems/WorldArtDirector.ts    - Layer composition
src/systems/QualityDiagnostics.ts  - Performance monitoring
```

### Material Library Pattern

Create named material roles instead of one-off colors:

```typescript
const MaterialLibrary = {
  bodyPrimary: new THREE.MeshStandardMaterial({
    color: 0x2a4a7f, roughness: 0.4, metalness: 0.6
  }),
  bodySecondary: new THREE.MeshStandardMaterial({
    color: 0x1a1a2e, roughness: 0.6, metalness: 0.3
  }),
  trim: new THREE.MeshStandardMaterial({
    color: 0xcccccc, roughness: 0.2, metalness: 0.9
  }),
  hazard: new THREE.MeshStandardMaterial({
    color: 0xff3300, roughness: 0.5, metalness: 0.4, emissive: 0xff1100, emissiveIntensity: 0.3
  }),
  reward: new THREE.MeshStandardMaterial({
    color: 0xffcc00, roughness: 0.3, metalness: 0.7, emissive: 0xffaa00, emissiveIntensity: 0.2
  }),
  glass: new THREE.MeshPhysicalMaterial({
    color: 0x88ccff, roughness: 0.1, metalness: 0.0, transmission: 0.8, thickness: 0.3
  }),
  emissiveSignal: new THREE.MeshStandardMaterial({
    color: 0x00ffcc, emissive: 0x00ffcc, emissiveIntensity: 1.5
  }),
  groundContact: new THREE.MeshStandardMaterial({
    color: 0x111111, roughness: 0.9, metalness: 0.0
  }),
};
```

---

## Procedural Model Recipes

### Modeling Principles

- **Start with silhouette** - Model must be recognizable as a dark shape
- **Combine primitives with authored geometry** - Extrusions, bevels, curves, tubes, lathes
- **Use asymmetry and functional parts** - Hinges, fins, vents, handles, rails, brackets
- **Detail where camera sees it** - Spend triangles on player-facing surfaces
- **State variants through materials** - Material swaps, animated children, emissive strips
- **Collision proxy separate from visuals** - Simple shapes for physics

### Hero Vehicle Recipe

```typescript
function createHeroVehicle(): THREE.Group {
  const vehicle = new THREE.Group();

  // Core hull - NOT a box
  const hullShape = new THREE.Shape();
  // Define tapered, aerodynamic cross-section
  hullShape.moveTo(-0.8, 0);
  hullShape.quadraticCurveTo(-1, 0.3, -0.6, 0.5);
  hullShape.lineTo(0.6, 0.5);
  hullShape.quadraticCurveTo(1, 0.3, 0.8, 0);
  hullShape.lineTo(-0.8, 0);

  const hullGeo = new THREE.ExtrudeGeometry(hullShape, {
    depth: 3, bevelEnabled: true, bevelThickness: 0.05, bevelSize: 0.05
  });
  const hull = new THREE.Mesh(hullGeo, MaterialLibrary.bodyPrimary);
  vehicle.add(hull);

  // Cockpit glass
  const cockpit = new THREE.Mesh(
    new THREE.SphereGeometry(0.4, 16, 12, 0, Math.PI * 2, 0, Math.PI / 2),
    MaterialLibrary.glass
  );
  cockpit.position.set(0, 0.5, 0.5);
  cockpit.name = 'cockpitGlass';
  vehicle.add(cockpit);

  // Engines with nozzle rings
  const engineGeo = new THREE.CylinderGeometry(0.15, 0.25, 0.8, 12);
  [-0.5, 0.5].forEach((x, i) => {
    const engine = new THREE.Mesh(engineGeo, MaterialLibrary.bodySecondary);
    engine.position.set(x, 0.1, -1.5);
    engine.rotation.x = Math.PI / 2;
    engine.name = `engine${i}`;
    vehicle.add(engine);

    // Nozzle ring
    const ring = new THREE.Mesh(
      new THREE.TorusGeometry(0.2, 0.03, 8, 16),
      MaterialLibrary.trim
    );
    ring.position.set(x, 0.1, -1.9);
    vehicle.add(ring);

    // Engine glow
    const glow = new THREE.Mesh(
      new THREE.CircleGeometry(0.15, 12),
      MaterialLibrary.emissiveSignal
    );
    glow.position.set(x, 0.1, -1.91);
    glow.name = `engineGlow${i}`;
    vehicle.add(glow);
  });

  // Collision proxy
  const proxy = new THREE.Mesh(
    new THREE.BoxGeometry(1.6, 0.6, 3.2),
    new THREE.MeshBasicMaterial({ visible: false })
  );
  proxy.name = 'collisionProxy';
  vehicle.add(proxy);

  return vehicle;
}
```

### Obstacle Family (3 Variants)

```typescript
function createObstacleFamily(): THREE.Group[] {
  return [
    createBarrierObstacle(),    // Low ground hazard
    createGateObstacle(),       // Overhead frame with shutters
    createDroneObstacle(),      // Flying hazard with telegraph
  ];
}

// Each variant needs:
// - Unique silhouette
// - Material cue for danger (hazard material)
// - Telegraph from distance (warning lights, anticipation animation)
// - Animation or state change
// - Collision proxy
```

### World Prop Kit (Layered)

```typescript
interface WorldPropKit {
  // Near layer - speed and scale
  railSegment: THREE.InstancedMesh;
  lampPost: THREE.InstancedMesh;
  barrier: THREE.InstancedMesh;

  // Mid layer - playable corridor definition
  building: THREE.InstancedMesh;
  bridge: THREE.InstancedMesh;
  platform: THREE.InstancedMesh;

  // Far layer - depth without draw calls
  skylineSilhouette: THREE.Mesh;
  mountainRange: THREE.Mesh;
  cloudCards: THREE.InstancedMesh;
}
```

---

## Render & Lighting Recipes

### Lighting Stack

```typescript
function createLightingRig(scene: THREE.Scene): void {
  // Key light - defines form and direction
  const key = new THREE.DirectionalLight(0xffeedd, 1.2);
  key.position.set(5, 10, 7);
  key.castShadow = true;
  key.shadow.mapSize.set(1024, 1024);
  scene.add(key);

  // Fill light - keeps gameplay objects legible
  const fill = new THREE.HemisphereLight(0x87ceeb, 0x362907, 0.4);
  scene.add(fill);

  // Rim/back light - separates player from background
  const rim = new THREE.DirectionalLight(0x88aaff, 0.6);
  rim.position.set(-3, 5, -5);
  scene.add(rim);

  // Ambient base
  const ambient = new THREE.AmbientLight(0x222244, 0.2);
  scene.add(ambient);
}
```

### Renderer Setup

```typescript
function setupRenderer(renderer: THREE.WebGLRenderer): void {
  renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.1;
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFSoftShadowMap;
}
```

### Event-Driven VFX Patterns

```typescript
interface VFXEvent {
  type: 'boost' | 'pickup' | 'hit' | 'nearMiss' | 'shield' | 'spawn';
  position: THREE.Vector3;
  intensity?: number;
}

class VFXSystem {
  private pools: Map<string, ObjectPool<THREE.Object3D>> = new Map();

  trigger(event: VFXEvent): void {
    switch (event.type) {
      case 'boost':
        // Engine cone flares, trail ribbons, FOV ease, side streaks
        break;
      case 'pickup':
        // Ring contraction, shard burst, score trail to HUD
        break;
      case 'hit':
        // Impact ring, debris particles, camera impulse, brief hit-pause
        break;
      case 'nearMiss':
        // Edge spark, badge pulse, streak counter animation
        break;
      case 'shield':
        // Refractive shell, rim pulse, absorbed impact ripple
        break;
      case 'spawn':
        // Anticipation pulse, telegraph circle, scale snap
        break;
    }
  }
}
```

---

## Hybrid AI Asset Pipeline

For premium games, choose asset source per surface:

| Surface | Procedural Three.js | Image Generator | 3D Generator |
|---------|---------------------|-----------------|--------------|
| Repeated small props | Yes | - | - |
| Hero/player model | Fallback only | Reference concept | Preferred |
| Characters/creatures | - | T-pose reference | Preferred |
| Vehicles/weapons | Simple version | Reference | Preferred |
| Textures/materials | Canvas-based | Reference images | - |
| Sky/backgrounds | Gradient only | Generated plates | - |
| Logos/icons/GUI | Shape geometry | Generated art | - |
| World kit pieces | Instanced | - | Hero pieces only |

### Asset Pipeline Flow

1. **Concept** - Image generator creates 2D reference
2. **Generation** - 3D generator creates model from concept
3. **Integration** - GLTFLoader imports, scale/pivot adjusted
4. **Collision** - Add simple physics proxy
5. **Animation** - AnimationMixer for rigged models
6. **Diagnostics** - Triangle/material/texture counts verified

---

## 2.5D / HD-2D Techniques (Octopath Traveler Style)

### What is 2.5D / HD-2D?

A visual style combining 2D pixel art sprites/textures with 3D depth, lighting, and camera effects. Made famous by Octopath Traveler, Triangle Strategy, and Live A Live remake.

### Core Technique: Billboarded Sprites in 3D World

```typescript
// HD-2D: Pixel art sprites rendered as planes in a 3D scene
function createHD2DCharacter(spriteTexture: THREE.Texture): THREE.Mesh {
  // Pixel-perfect texture settings
  spriteTexture.magFilter = THREE.NearestFilter;
  spriteTexture.minFilter = THREE.NearestFilter;
  spriteTexture.generateMipmaps = false;
  spriteTexture.colorSpace = THREE.SRGBColorSpace;

  const material = new THREE.MeshStandardMaterial({
    map: spriteTexture,
    transparent: true,
    alphaTest: 0.5,
    side: THREE.DoubleSide,
    // Accept 3D lighting on the sprite
    roughness: 1.0,
    metalness: 0.0,
  });

  const plane = new THREE.Mesh(
    new THREE.PlaneGeometry(1, 1.5),
    material
  );
  plane.castShadow = true;
  return plane;
}

// Billboard toward camera (Y-axis only for HD-2D)
function billboardToCamera(sprite: THREE.Mesh, camera: THREE.Camera): void {
  const cameraDir = new THREE.Vector3();
  camera.getWorldDirection(cameraDir);
  cameraDir.y = 0; // Only rotate on Y axis
  cameraDir.normalize();

  sprite.lookAt(
    sprite.position.x + cameraDir.x,
    sprite.position.y,
    sprite.position.z + cameraDir.z
  );
}
```

### HD-2D Environment: 3D Geometry with Pixel Textures

```typescript
function createHD2DWorld(): THREE.Group {
  const world = new THREE.Group();

  // Ground tiles - 3D geometry with pixel art textures
  const tileTexture = textureLoader.load('/textures/grass-tile.png');
  tileTexture.magFilter = THREE.NearestFilter;
  tileTexture.minFilter = THREE.NearestFilter;
  tileTexture.wrapS = THREE.RepeatWrapping;
  tileTexture.wrapT = THREE.RepeatWrapping;
  tileTexture.repeat.set(16, 16);

  const ground = new THREE.Mesh(
    new THREE.PlaneGeometry(16, 16),
    new THREE.MeshStandardMaterial({ map: tileTexture })
  );
  ground.rotation.x = -Math.PI / 2;
  ground.receiveShadow = true;
  world.add(ground);

  // 3D buildings with pixel textures
  const buildingTexture = textureLoader.load('/textures/building-facade.png');
  buildingTexture.magFilter = THREE.NearestFilter;
  buildingTexture.minFilter = THREE.NearestFilter;

  const building = new THREE.Mesh(
    new THREE.BoxGeometry(2, 3, 2),
    new THREE.MeshStandardMaterial({ map: buildingTexture })
  );
  building.position.set(3, 1.5, -2);
  building.castShadow = true;
  world.add(building);

  // Depth-of-field and volumetric lighting sell the 3D
  return world;
}
```

### HD-2D Camera Setup

```typescript
// Isometric-like perspective with tilt-shift feel
function createHD2DCamera(): THREE.PerspectiveCamera {
  const camera = new THREE.PerspectiveCamera(30, aspect, 0.1, 100);
  // High angle, looking down at ~45 degrees
  camera.position.set(0, 8, 8);
  camera.lookAt(0, 0, 0);
  return camera;
}

// OR use orthographic for pure isometric
function createIsometricCamera(): THREE.OrthographicCamera {
  const frustumSize = 10;
  const camera = new THREE.OrthographicCamera(
    -frustumSize * aspect / 2, frustumSize * aspect / 2,
    frustumSize / 2, -frustumSize / 2,
    0.1, 100
  );
  camera.position.set(10, 10, 10);
  camera.lookAt(0, 0, 0);
  return camera;
}
```

### HD-2D Post-Processing Stack

```typescript
// Key effects for HD-2D look:
// 1. Depth of field (tilt-shift) - blurs foreground/background
// 2. Bloom on light sources - volumetric god rays
// 3. Vignette - frames the pixel art scene
// 4. Film grain (subtle) - adds warmth

// Using @react-three/postprocessing:
<EffectComposer>
  <DepthOfField
    focusDistance={0.02}
    focalLength={0.05}
    bokehScale={4}
  />
  <Bloom
    luminanceThreshold={0.8}
    luminanceSmoothing={0.3}
    intensity={0.8}
    mipmapBlur
  />
  <Vignette offset={0.3} darkness={0.5} />
</EffectComposer>
```

### Sprite Animation System for HD-2D

```typescript
class SpriteAnimator {
  private currentFrame = 0;
  private elapsed = 0;
  private frameRate = 8; // FPS for pixel animation
  private spriteSheet: THREE.Texture;
  private columns: number;
  private rows: number;

  constructor(spriteSheet: THREE.Texture, columns: number, rows: number) {
    this.spriteSheet = spriteSheet;
    this.columns = columns;
    this.rows = rows;

    // Pixel-perfect settings
    spriteSheet.magFilter = THREE.NearestFilter;
    spriteSheet.minFilter = THREE.NearestFilter;
    spriteSheet.generateMipmaps = false;

    // Set initial UV offset
    spriteSheet.repeat.set(1 / columns, 1 / rows);
  }

  update(dt: number, startFrame: number, endFrame: number): void {
    this.elapsed += dt;
    if (this.elapsed >= 1 / this.frameRate) {
      this.elapsed = 0;
      this.currentFrame++;
      if (this.currentFrame > endFrame) this.currentFrame = startFrame;

      const col = this.currentFrame % this.columns;
      const row = Math.floor(this.currentFrame / this.columns);
      this.spriteSheet.offset.set(col / this.columns, 1 - (row + 1) / this.rows);
    }
  }
}
```

### Isometric Grid System

```typescript
// Convert grid coordinates to isometric world position
function gridToWorld(gridX: number, gridZ: number, tileSize: number): THREE.Vector3 {
  return new THREE.Vector3(
    gridX * tileSize,
    0,
    gridZ * tileSize
  );
}

// For diamond isometric (rotated 45 degrees):
function gridToDiamondIso(gridX: number, gridZ: number, tileSize: number): THREE.Vector3 {
  return new THREE.Vector3(
    (gridX - gridZ) * tileSize * 0.5,
    0,
    (gridX + gridZ) * tileSize * 0.5
  );
}

// Tile-based map with height layers
class IsometricMap {
  private tiles: Map<string, { height: number; type: string }> = new Map();

  setTile(x: number, z: number, height: number, type: string): void {
    this.tiles.set(`${x},${z}`, { height, type });
  }

  buildMesh(scene: THREE.Scene): void {
    for (const [key, tile] of this.tiles) {
      const [x, z] = key.split(',').map(Number);
      const worldPos = gridToWorld(x, z, 1);
      worldPos.y = tile.height * 0.5;

      const mesh = this.createTileMesh(tile.type);
      mesh.position.copy(worldPos);
      scene.add(mesh);
    }
  }

  private createTileMesh(type: string): THREE.Mesh {
    // Return appropriately textured tile based on type
    const geo = new THREE.BoxGeometry(1, 0.5, 1);
    const mat = new THREE.MeshStandardMaterial({
      map: this.getTextureForType(type),
    });
    mat.map!.magFilter = THREE.NearestFilter;
    mat.map!.minFilter = THREE.NearestFilter;
    return new THREE.Mesh(geo, mat);
  }

  private getTextureForType(type: string): THREE.Texture {
    // Load pixel art texture for tile type
    return new THREE.Texture();
  }
}
```
