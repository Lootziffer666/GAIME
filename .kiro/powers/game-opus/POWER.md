---
name: "game-opus"
displayName: "Game Opus"
description: "Ultimate game development meta-skill for JavaScript/TypeScript 3D games. Combines 15 specialized knowledge areas: Three.js, React Three Fiber, WebGL, WebGPU, shaders, physics, game architecture, building mechanics, and TypeScript best practices."
keywords: ["threejs", "react-three-fiber", "r3f", "webgl", "webgpu", "game-development", "3d", "shaders", "physics", "game-engine"]
author: "Game Opus"
---

# Game Opus - Ultimate Game Development Intelligence

The most comprehensive game development knowledge base for JavaScript/TypeScript 3D games. This power combines 15 specialized knowledge areas into a single unified resource for building games, 3D scenes, physics simulations, shaders, and interactive Three.js/R3F projects.

## Overview

Game Opus provides deep expertise across the entire game development stack:

- **3D Engine Fundamentals** - Scene graphs, cameras, renderers, geometry, materials, lighting
- **React Three Fiber** - Declarative 3D in React with hooks, physics, and component patterns
- **GPU Programming** - WebGL pipelines, WebGPU with TSL, custom shader effects
- **Game Architecture** - Game loops, ECS, state machines, input handling, AI patterns
- **Performance Engineering** - 60 FPS optimization, draw call reduction, memory management
- **Physics Simulation** - Rapier/Cannon integration, rigidbodies, colliders, raycasting
- **Building Mechanics** - Crafting systems, placement, snapping, grid-based construction
- **Type Safety** - TypeScript patterns for robust, maintainable game code

## Available Steering Files

This power organizes its knowledge into focused steering files for progressive loading:

- **threejs-core** - Three.js fundamentals: scene, camera, renderer, geometry, materials, lighting, scene graph construction, object management, animation, and optimization (instancing, LOD, object pooling, memory management)
- **react-three-fiber** - React Three Fiber: Canvas setup, hooks, declarative 3D, drei helpers, physics engines (Rapier/Cannon), colliders, rigidbodies, character controllers, joints, state management with Zustand
- **graphics-rendering** - WebGL pipeline, WebGPU with Three.js Shading Language (TSL), custom shader effects (dissolve, outline, hologram, water), post-processing, particle systems, render targets, and multi-pass rendering
- **game-development** - Game architecture patterns: game loop, ECS, state machines, input handling, AI (behavior trees, A* pathfinding), FPS mechanics, weapon systems, spatial partitioning, building/crafting systems, grid placement, snap points, chunk-based terrain
- **typescript-patterns** - TypeScript best practices for game code: component registries, discriminated unions for events, typed state machines, object pools, asset loaders, branded types, const enums, allocation-free hot paths, R3F typed refs
- **aaa-graphics-pipeline** - Production graphics: visual scorecard (10-category scoring), procedural model recipes (hero vehicles, obstacle families, world prop kits), material libraries, render/lighting recipes, VFX systems, hybrid AI asset pipeline, and 2.5D/HD-2D techniques (Octopath Traveler style)
- **production-workflow** - Game production pipeline: project scaffolding (Vite+TS+Three.js), phase orchestration, gameplay systems workflow, physics engine selection, debug/profiling, QA verification, release preparation, AI asset generation (3D/image/audio), UI design patterns, and endless runner checklist
- **pixel-art-2d-assets** - Pixel art and 2D game asset generation: AI-powered sprite/tileset/animation pipelines, palette systems (PICO-8, DB16, DB32, NES, AAP-64), post-processing (nearest-neighbor + quantize), Tiled TSX/TMJ export, SpriteCook workflows, QA metrics, prompt engineering, and game engine integration (Three.js/Godot)

Call action "readSteering" to access specific topics as needed.

## When to Use This Power

Activate Game Opus when:
- Building any 3D game or interactive experience with Three.js or R3F
- Creating 2.5D/HD-2D games (Octopath Traveler style pixel art in 3D scenes)
- Generating pixel art sprites, tilesets, or sprite-sheet animations
- Building 2D games needing AI-generated art assets (characters, tiles, animations)
- Creating custom shaders or post-processing effects
- Implementing physics simulations
- Optimizing rendering performance
- Designing game architecture (ECS, state machines, game loops)
- Building crafting/placement/building mechanics
- Working with WebGL or WebGPU directly
- Upgrading prototype visuals to premium/AAA quality
- Setting up AI asset generation pipelines (3D models, images, audio, pixel art)
- Integrating Tiled maps, spritesheets, or pixel-art assets into game engines
- Reviewing 3D/game code for best practices
- Planning production workflow and QA for browser games

## How to Apply Knowledge

After reading the relevant steering files, apply the combined knowledge following this priority:

1. **Architecture First** - Use game-developer patterns for game loop, ECS, state machines
2. **3D Scene Setup** - Apply Three.js fundamentals for scene graph construction
3. **Rendering Pipeline** - Follow best practices for optimal draw calls, LOD, instancing
4. **Physics Integration** - Use Rapier/Cannon for realistic collisions and rigidbodies
5. **Shader Effects** - Apply custom shaders and WebGPU TSL for unique visual effects
6. **Game Mechanics** - Implement FPS controls, interactions, building systems
7. **React Integration** - Use R3F for declarative 3D with React component patterns
8. **Performance** - Combine all optimization knowledge: frustum culling, instanced meshes, texture atlases, object pooling
9. **Type Safety** - Use TypeScript patterns for robust game code

## Game Development Philosophy

- **60 FPS or bust** - Every frame counts, profile and optimize ruthlessly
- **Physics feels real** - Proper mass, friction, restitution for believable interactions
- **Draw calls minimal** - Batch, instance, merge geometries, use texture atlases
- **Memory conscious** - Pool objects, dispose textures/geometries, avoid GC spikes
- **Shaders for everything visual** - Custom materials > built-in for unique look
- **Input responsive** - Sub-frame input handling, no input lag
- **Mobile capable** - Fallback renderers, quality settings, touch controls

## Performance Checklist

Use this checklist when reviewing or building any 3D game project:

- [ ] Use `InstancedMesh` for repeated objects (trees, bullets, particles)
- [ ] Implement frustum culling and LOD (Level of Detail)
- [ ] Texture atlases instead of many small textures
- [ ] Object pooling for frequently created/destroyed objects
- [ ] Use `BufferGeometry` always, never legacy `Geometry`
- [ ] Minimize state changes (material switches, shader changes)
- [ ] Use Web Workers for physics/AI computation
- [ ] Profile with `stats.js` and Chrome DevTools Performance tab
- [ ] Dispose unused resources (`geometry.dispose()`, `texture.dispose()`)
- [ ] Use compressed textures (KTX2/Basis) for GPU-friendly formats
- [ ] Batch draw calls with merged geometries where possible
- [ ] Use `requestAnimationFrame` correctly (single loop, delta time)
- [ ] Implement spatial partitioning (octree/BVH) for large scenes
- [ ] Reduce overdraw with proper depth sorting and early-Z
- [ ] Use GPU instancing for particle systems

## Quick Reference: Project Setup

### Three.js Vanilla

```bash
npm install three @types/three
```

```typescript
import * as THREE from 'three';

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000);
const renderer = new THREE.WebGLRenderer({ antialias: true });
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
document.body.appendChild(renderer.domElement);

function animate() {
  requestAnimationFrame(animate);
  renderer.render(scene, camera);
}
animate();
```

### React Three Fiber

```bash
npm install @react-three/fiber @react-three/drei @react-three/rapier three @types/three
```

```tsx
import { Canvas } from '@react-three/fiber';
import { OrbitControls } from '@react-three/drei';

function App() {
  return (
    <Canvas camera={{ position: [0, 5, 10], fov: 75 }}>
      <ambientLight intensity={0.5} />
      <directionalLight position={[10, 10, 5]} />
      <OrbitControls />
      {/* Your 3D content here */}
    </Canvas>
  );
}
```

## Best Practices Summary

### Scene Management
- Use scene graph hierarchy for logical grouping
- Dispose resources when removing objects
- Use layers for selective rendering
- Implement proper camera controls with damping

### Material Optimization
- Share materials between meshes where possible
- Use `MeshStandardMaterial` for PBR, `MeshBasicMaterial` for unlit
- Prefer texture-based detail over geometry complexity
- Use normal maps instead of high-poly meshes

### Physics Best Practices
- Use simple collider shapes (box, sphere, capsule) over trimesh
- Sleep inactive rigidbodies
- Use fixed timestep for physics simulation
- Separate physics world from render world

### Game Loop Patterns
- Use delta time for frame-rate independent movement
- Separate update logic from render logic
- Implement fixed timestep for physics
- Use interpolation for smooth rendering between physics steps

## Knowledge Sources

This power synthesizes knowledge from multiple game development skill collections:

- **threejs-game-skills** (majidmanzarpour/threejs-game-skills) - Comprehensive Three.js game development skill suite including game direction, gameplay systems, AAA graphics building, UI design, debug/profiling, QA/release, 3D generation (Tripo), image generation (Gemini), and audio generation (ElevenLabs)
- **ai-pixel-art-image-generation** (ianlintner/ai-pixel-art-image-generation) - AI pixel art pipeline: text-to-image generation via OpenAI/Azure/fal.ai, pixelization post-processing, palette quantization, sprite/tileset/animation generation with QA metrics, and Tiled TSX/TMJ export
- **SpriteCook skills** (SpriteCook/skills) - MCP-based pixel art and game asset workflows: sprite generation, tileset autotile generation, animation from still images, character animation presets, preset management, asset upload/management, and Godot integration
- **Three.js official documentation** - Core engine fundamentals, rendering pipeline, geometry, materials, lighting
- **React Three Fiber ecosystem** - @react-three/fiber, @react-three/drei, @react-three/rapier, @react-three/postprocessing
- **Rapier physics engine** - @dimforge/rapier3d-compat, rigid bodies, colliders, sensors, CCD
- **WebGPU and TSL** - Three.js Shading Language for next-gen GPU programming
- **Game development patterns** - ECS, behavior trees, A* pathfinding, state machines, object pooling
