# Three.js Core

Complete reference for Three.js fundamentals, scene building, best practices, and optimization techniques. Covers scene graph, cameras, renderers, geometry, materials, lighting, and complex scene management.

---

## Three.js Fundamentals

### Scene Setup

The Three.js rendering pipeline consists of three core objects:

```typescript
import * as THREE from 'three';

// Scene - container for all 3D objects
const scene = new THREE.Scene();
scene.background = new THREE.Color(0x000000);
scene.fog = new THREE.Fog(0x000000, 10, 100); // color, near, far

// Camera - defines viewpoint
const camera = new THREE.PerspectiveCamera(
  75,                                    // FOV in degrees
  window.innerWidth / window.innerHeight, // Aspect ratio
  0.1,                                   // Near clipping plane
  1000                                   // Far clipping plane
);
camera.position.set(0, 5, 10);
camera.lookAt(0, 0, 0);

// Renderer - draws the scene
const renderer = new THREE.WebGLRenderer({
  antialias: true,
  alpha: false,
  powerPreference: 'high-performance',
});
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 1.0;
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
document.body.appendChild(renderer.domElement);
```

### Camera Types

```typescript
// Perspective Camera - realistic 3D perspective
const perspCamera = new THREE.PerspectiveCamera(fov, aspect, near, far);

// Orthographic Camera - no perspective distortion (2D/isometric)
const orthoCamera = new THREE.OrthographicCamera(
  left, right, top, bottom, near, far
);

// Useful for UI overlays, minimaps, shadow cameras
```

### Geometry

```typescript
// Built-in geometries
const box = new THREE.BoxGeometry(width, height, depth, widthSegments, heightSegments, depthSegments);
const sphere = new THREE.SphereGeometry(radius, widthSegments, heightSegments);
const plane = new THREE.PlaneGeometry(width, height, widthSegments, heightSegments);
const cylinder = new THREE.CylinderGeometry(radiusTop, radiusBottom, height, radialSegments);
const torus = new THREE.TorusGeometry(radius, tube, radialSegments, tubularSegments);
const cone = new THREE.ConeGeometry(radius, height, radialSegments);

// Custom BufferGeometry
const geometry = new THREE.BufferGeometry();
const vertices = new Float32Array([
  -1, -1, 0,
   1, -1, 0,
   1,  1, 0,
  -1,  1, 0,
]);
const indices = new Uint16Array([0, 1, 2, 0, 2, 3]);
geometry.setAttribute('position', new THREE.BufferAttribute(vertices, 3));
geometry.setIndex(new THREE.BufferAttribute(indices, 1));
geometry.computeVertexNormals();
```

### Materials

```typescript
// Basic - unlit, no shadows
const basic = new THREE.MeshBasicMaterial({ color: 0xff0000 });

// Standard - PBR metallic-roughness workflow
const standard = new THREE.MeshStandardMaterial({
  color: 0xffffff,
  metalness: 0.5,
  roughness: 0.5,
  map: diffuseTexture,
  normalMap: normalTexture,
  roughnessMap: roughnessTexture,
  metalnessMap: metalnessTexture,
  aoMap: aoTexture,
  envMap: environmentMap,
});

// Physical - extends Standard with clearcoat, transmission, etc.
const physical = new THREE.MeshPhysicalMaterial({
  ...standard,
  clearcoat: 1.0,
  clearcoatRoughness: 0.1,
  transmission: 0.9,  // glass-like
  thickness: 0.5,
  ior: 1.5,
});

// Lambert - non-physically correct but cheap
const lambert = new THREE.MeshLambertMaterial({ color: 0x00ff00 });

// Phong - specular highlights, cheap
const phong = new THREE.MeshPhongMaterial({
  color: 0x0000ff,
  specular: 0xffffff,
  shininess: 100,
});
```

### Lighting

```typescript
// Ambient - uniform light everywhere
const ambient = new THREE.AmbientLight(0xffffff, 0.3);

// Directional - sun-like parallel rays
const directional = new THREE.DirectionalLight(0xffffff, 1.0);
directional.position.set(10, 20, 10);
directional.castShadow = true;
directional.shadow.mapSize.set(2048, 2048);
directional.shadow.camera.left = -20;
directional.shadow.camera.right = 20;
directional.shadow.camera.top = 20;
directional.shadow.camera.bottom = -20;
directional.shadow.camera.near = 0.1;
directional.shadow.camera.far = 100;

// Point - light bulb, emits in all directions
const point = new THREE.PointLight(0xffaa00, 1.0, 50, 2);
point.position.set(0, 5, 0);
point.castShadow = true;

// Spot - cone-shaped light
const spot = new THREE.SpotLight(0xffffff, 1.0, 50, Math.PI / 6, 0.5, 2);
spot.position.set(0, 10, 0);
spot.target.position.set(0, 0, 0);
spot.castShadow = true;

// Hemisphere - sky/ground gradient
const hemi = new THREE.HemisphereLight(0x87ceeb, 0x362907, 0.5);

// RectArea - rectangular light (like a window or screen)
const rectArea = new THREE.RectAreaLight(0xffffff, 5, 4, 4);
rectArea.position.set(0, 5, 0);
rectArea.lookAt(0, 0, 0);
```

### Textures

```typescript
const textureLoader = new THREE.TextureLoader();

// Basic texture loading
const texture = textureLoader.load('/textures/diffuse.jpg');
texture.colorSpace = THREE.SRGBColorSpace; // for color/diffuse maps
texture.wrapS = THREE.RepeatWrapping;
texture.wrapT = THREE.RepeatWrapping;
texture.repeat.set(2, 2);
texture.magFilter = THREE.LinearFilter;
texture.minFilter = THREE.LinearMipmapLinearFilter;
texture.anisotropy = renderer.capabilities.getMaxAnisotropy();

// KTX2 compressed textures (GPU-friendly)
import { KTX2Loader } from 'three/examples/jsm/loaders/KTX2Loader';
const ktx2Loader = new KTX2Loader()
  .setTranscoderPath('/libs/basis/')
  .detectSupport(renderer);
const compressedTexture = await ktx2Loader.loadAsync('/textures/diffuse.ktx2');

// Environment maps with HDR
import { RGBELoader } from 'three/examples/jsm/loaders/RGBELoader';
const rgbeLoader = new RGBELoader();
const hdrTexture = await rgbeLoader.loadAsync('/textures/environment.hdr');
hdrTexture.mapping = THREE.EquirectangularReflectionMapping;
scene.environment = hdrTexture;
scene.background = hdrTexture;
```

---

## Scene Graph & Object Management

### Hierarchy and Groups

```typescript
// Groups for logical organization
const world = new THREE.Group();
const buildings = new THREE.Group();
const vehicles = new THREE.Group();

world.add(buildings);
world.add(vehicles);
scene.add(world);

// Object3D as empty containers
const pivotPoint = new THREE.Object3D();
pivotPoint.position.set(0, 2, 0);
const orbitingMesh = new THREE.Mesh(geometry, material);
orbitingMesh.position.set(5, 0, 0);
pivotPoint.add(orbitingMesh);
scene.add(pivotPoint);

// Rotating the pivot rotates child around the pivot
pivotPoint.rotation.y += deltaTime;
```

### Transform Operations

```typescript
// Position
mesh.position.set(x, y, z);
mesh.position.copy(targetVector);
mesh.position.lerp(targetPosition, alpha); // smooth movement

// Rotation (Euler angles - beware of gimbal lock)
mesh.rotation.set(x, y, z);
mesh.rotation.order = 'YXZ'; // FPS camera order

// Quaternion (no gimbal lock)
mesh.quaternion.setFromAxisAngle(new THREE.Vector3(0, 1, 0), angle);
mesh.quaternion.slerp(targetQuat, alpha); // smooth rotation

// Scale
mesh.scale.set(sx, sy, sz);
mesh.scale.setScalar(2); // uniform scale

// Matrix (manual control)
mesh.matrixAutoUpdate = false;
mesh.matrix.compose(position, quaternion, scale);
mesh.updateMatrixWorld(true);

// Look at target
mesh.lookAt(targetPosition);

// World transforms
const worldPos = new THREE.Vector3();
mesh.getWorldPosition(worldPos);
const worldQuat = new THREE.Quaternion();
mesh.getWorldQuaternion(worldQuat);
```

### Raycasting

```typescript
const raycaster = new THREE.Raycaster();
const mouse = new THREE.Vector2();

function onMouseClick(event: MouseEvent) {
  mouse.x = (event.clientX / window.innerWidth) * 2 - 1;
  mouse.y = -(event.clientY / window.innerHeight) * 2 + 1;

  raycaster.setFromCamera(mouse, camera);
  const intersects = raycaster.intersectObjects(scene.children, true);

  if (intersects.length > 0) {
    const hit = intersects[0];
    console.log('Hit:', hit.object.name);
    console.log('Point:', hit.point);
    console.log('Normal:', hit.face?.normal);
    console.log('Distance:', hit.distance);
  }
}

// Directional raycasting (e.g., from player forward)
raycaster.set(origin, direction.normalize());
raycaster.far = 100; // max distance
raycaster.near = 0;
```

### Loading 3D Models

```typescript
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';
import { DRACOLoader } from 'three/examples/jsm/loaders/DRACOLoader';

const dracoLoader = new DRACOLoader();
dracoLoader.setDecoderPath('/libs/draco/');

const gltfLoader = new GLTFLoader();
gltfLoader.setDRACOLoader(dracoLoader);

const gltf = await gltfLoader.loadAsync('/models/character.glb');
const model = gltf.scene;

// Traverse and configure
model.traverse((child) => {
  if (child instanceof THREE.Mesh) {
    child.castShadow = true;
    child.receiveShadow = true;
    // Fix materials if needed
    if (child.material.map) {
      child.material.map.colorSpace = THREE.SRGBColorSpace;
    }
  }
});

scene.add(model);

// Animations
const mixer = new THREE.AnimationMixer(model);
const clips = gltf.animations;
const action = mixer.clipAction(clips[0]);
action.play();

// Update in game loop
mixer.update(deltaTime);
```

---

## Three.js Best Practices & Optimization

### Draw Call Reduction

```typescript
// INSTANCED MESH - for many identical objects
const count = 10000;
const instancedMesh = new THREE.InstancedMesh(geometry, material, count);
const matrix = new THREE.Matrix4();
const color = new THREE.Color();

for (let i = 0; i < count; i++) {
  matrix.setPosition(
    Math.random() * 100 - 50,
    Math.random() * 10,
    Math.random() * 100 - 50
  );
  instancedMesh.setMatrixAt(i, matrix);
  instancedMesh.setColorAt(i, color.setHex(Math.random() * 0xffffff));
}
instancedMesh.instanceMatrix.needsUpdate = true;
scene.add(instancedMesh);

// MERGED GEOMETRY - for static objects that never move individually
import { mergeGeometries } from 'three/examples/jsm/utils/BufferGeometryUtils';

const geometries: THREE.BufferGeometry[] = [];
for (let i = 0; i < 100; i++) {
  const geo = new THREE.BoxGeometry(1, 1, 1);
  geo.translate(Math.random() * 50, 0, Math.random() * 50);
  geometries.push(geo);
}
const mergedGeo = mergeGeometries(geometries);
const mergedMesh = new THREE.Mesh(mergedGeo, sharedMaterial);
scene.add(mergedMesh);

// BATCH MESH (Three.js r160+) - dynamic batching
import { BatchedMesh } from 'three';
const batchedMesh = new BatchedMesh(maxGeometryCount, maxVertexCount, maxIndexCount, material);
```

### Level of Detail (LOD)

```typescript
const lod = new THREE.LOD();

// High detail - close
const highDetail = new THREE.Mesh(highPolyGeometry, material);
lod.addLevel(highDetail, 0);

// Medium detail
const medDetail = new THREE.Mesh(medPolyGeometry, material);
lod.addLevel(medDetail, 50);

// Low detail - far away
const lowDetail = new THREE.Mesh(lowPolyGeometry, material);
lod.addLevel(lowDetail, 100);

// Billboard/sprite - very far
const sprite = new THREE.Sprite(spriteMaterial);
lod.addLevel(sprite, 200);

scene.add(lod);

// LOD updates automatically based on camera distance
```

### Frustum Culling

```typescript
// Enabled by default on all Object3D
mesh.frustumCulled = true; // default

// Custom bounding sphere for better culling
geometry.computeBoundingSphere();

// Manual frustum check for custom systems
const frustum = new THREE.Frustum();
const projScreenMatrix = new THREE.Matrix4();
projScreenMatrix.multiplyMatrices(camera.projectionMatrix, camera.matrixWorldInverse);
frustum.setFromProjectionMatrix(projScreenMatrix);

if (frustum.containsPoint(object.position)) {
  // Object is visible
}
```

### Object Pooling

```typescript
class ObjectPool<T extends THREE.Object3D> {
  private pool: T[] = [];
  private active: Set<T> = new Set();
  private factory: () => T;

  constructor(factory: () => T, initialSize: number = 50) {
    this.factory = factory;
    for (let i = 0; i < initialSize; i++) {
      const obj = factory();
      obj.visible = false;
      this.pool.push(obj);
    }
  }

  acquire(): T {
    let obj = this.pool.pop();
    if (!obj) {
      obj = this.factory();
    }
    obj.visible = true;
    this.active.add(obj);
    return obj;
  }

  release(obj: T): void {
    obj.visible = false;
    this.active.delete(obj);
    this.pool.push(obj);
  }

  get activeCount(): number {
    return this.active.size;
  }
}

// Usage
const bulletPool = new ObjectPool(() => {
  const bullet = new THREE.Mesh(bulletGeo, bulletMat);
  scene.add(bullet);
  return bullet;
}, 100);
```

### Memory Management

```typescript
// ALWAYS dispose resources when done
function disposeObject(obj: THREE.Object3D): void {
  obj.traverse((child) => {
    if (child instanceof THREE.Mesh) {
      child.geometry.dispose();
      if (Array.isArray(child.material)) {
        child.material.forEach(disposeMaterial);
      } else {
        disposeMaterial(child.material);
      }
    }
  });
  obj.parent?.remove(obj);
}

function disposeMaterial(material: THREE.Material): void {
  material.dispose();
  // Dispose all texture properties
  for (const key of Object.keys(material)) {
    const value = (material as any)[key];
    if (value instanceof THREE.Texture) {
      value.dispose();
    }
  }
}

// Dispose render targets
renderTarget.dispose();

// Dispose renderer on cleanup
renderer.dispose();
renderer.forceContextLoss();
```

### Render Loop Best Practices

```typescript
const clock = new THREE.Clock();
let previousTime = 0;

function animate(): void {
  requestAnimationFrame(animate);

  const elapsedTime = clock.getElapsedTime();
  const deltaTime = elapsedTime - previousTime;
  previousTime = elapsedTime;

  // Cap delta time to prevent spiral of death
  const dt = Math.min(deltaTime, 1 / 30);

  // Update game logic
  update(dt);

  // Render
  renderer.render(scene, camera);
}

// Handle resize
function onResize(): void {
  const width = window.innerWidth;
  const height = window.innerHeight;

  camera.aspect = width / height;
  camera.updateProjectionMatrix();

  renderer.setSize(width, height);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
}
window.addEventListener('resize', onResize);
```

### Texture Optimization

```typescript
// Use power-of-two dimensions (256, 512, 1024, 2048)
// Smaller is better - don't use 4096 unless needed

// Texture atlas - combine many textures into one
// Reduces texture binding state changes

// Compressed textures - smaller GPU memory, faster loading
// KTX2 with Basis Universal - best cross-platform

// Mipmap generation
texture.generateMipmaps = true; // default for power-of-two
texture.minFilter = THREE.LinearMipmapLinearFilter;

// Limit anisotropy for performance
texture.anisotropy = Math.min(4, renderer.capabilities.getMaxAnisotropy());

// Flip Y for loaded textures (GLTF handles this automatically)
texture.flipY = false; // for GLTF textures
```

### Shadow Optimization

```typescript
// Limit shadow-casting lights (expensive!)
// Prefer 1 directional light with shadows

// Optimize shadow map size
directionalLight.shadow.mapSize.set(1024, 1024); // not always 4096

// Tight shadow camera frustum
const d = 20;
directionalLight.shadow.camera.left = -d;
directionalLight.shadow.camera.right = d;
directionalLight.shadow.camera.top = d;
directionalLight.shadow.camera.bottom = -d;
directionalLight.shadow.camera.near = 0.1;
directionalLight.shadow.camera.far = 50;

// Shadow bias to prevent artifacts
directionalLight.shadow.bias = -0.001;
directionalLight.shadow.normalBias = 0.02;

// Selective shadow casting/receiving
importantMesh.castShadow = true;
ground.receiveShadow = true;
distantDecoration.castShadow = false; // save performance
```

### Animation System

```typescript
// AnimationMixer for skeletal/keyframe animations
const mixer = new THREE.AnimationMixer(model);

// Crossfade between animations
function crossfade(fromAction: THREE.AnimationAction, toAction: THREE.AnimationAction, duration: number) {
  fromAction.fadeOut(duration);
  toAction.reset().fadeIn(duration).play();
}

// Animation weights for blending
walkAction.setEffectiveWeight(1.0);
runAction.setEffectiveWeight(0.0);

// Additive animations (e.g., breathing on top of idle)
breatheAction.blendMode = THREE.AdditiveAnimationBlendMode;

// Update all mixers
mixer.update(deltaTime);
```
