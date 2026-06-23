// =============================================================================
// GAIME - Variant C: TRUE HD-2D with Three.js
// -----------------------------------------------------------------------------
// Octopath-Traveler style: pixel-art SPRITES (2D) placed as billboarded planes
// inside a real 3D world, lit by 3D lights, framed by a tilted camera, and
// finished with a post-processing stack (bloom + depth-of-field + vignette).
//
// Everything here is PROCEDURAL - the pixel art is painted to <canvas> at runtime
// so the demo is fully self-contained (no external image assets needed).
//
// Built using the knowledge in the `game-opus` Kiro Power:
//   steering/aaa-graphics-pipeline.md  -> HD-2D / 2.5D techniques
//   steering/pixel-art-2d-assets.md    -> NearestFilter, palettes, sprite sheets
//   steering/graphics-rendering.md     -> EffectComposer, bloom, DoF
// =============================================================================

import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { EffectComposer } from 'three/addons/postprocessing/EffectComposer.js';
import { RenderPass } from 'three/addons/postprocessing/RenderPass.js';
import { UnrealBloomPass } from 'three/addons/postprocessing/UnrealBloomPass.js';
import { BokehPass } from 'three/addons/postprocessing/BokehPass.js';
import { ShaderPass } from 'three/addons/postprocessing/ShaderPass.js';
import { OutputPass } from 'three/addons/postprocessing/OutputPass.js';

// -----------------------------------------------------------------------------
// Pixel-art palette (DB16-ish) used by the procedural sprite painter
// -----------------------------------------------------------------------------
const PALETTE = {
  skin: '#e0a868', skinDark: '#a8703b',
  cloak: '#7e2553', cloakDark: '#4a1531',
  leather: '#8f563b', leatherDark: '#5a3322',
  hair: '#3a2417', steel: '#c2c3c7', steelDark: '#6d7278',
  gold: '#ffd98a', book: '#7be0ff', bookDark: '#2a6fae',
  grass: '#4b6f2f', grassDark: '#33501f', grassLite: '#6aa13a',
  dirt: '#5a3f2a', stone: '#6b6661', stoneDark: '#43403c',
  wood: '#6e4a2c', woodDark: '#4a2f1a', leaf: '#37663a', trunk: '#3a2716',
};

// Paint a tiny pixel grid to an offscreen canvas, return a NEAREST-filtered texture.
// `grid` is an array of strings; each char maps to a palette color via `map`.
function pixelTexture(grid, map, scale = 8) {
  const h = grid.length;
  const w = grid[0].length;
  const cv = document.createElement('canvas');
  cv.width = w * scale;
  cv.height = h * scale;
  const ctx = cv.getContext('2d');
  ctx.imageSmoothingEnabled = false;
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const c = grid[y][x];
      if (c === '.' || c === ' ') continue;
      ctx.fillStyle = map[c] || '#ff00ff';
      ctx.fillRect(x * scale, y * scale, scale, scale);
    }
  }
  const tex = new THREE.CanvasTexture(cv);
  tex.magFilter = THREE.NearestFilter;
  tex.minFilter = THREE.NearestFilter;
  tex.colorSpace = THREE.SRGBColorSpace;
  tex.generateMipmaps = false;
  return { tex, w, h };
}

// --- Sprite definitions (mini 16x18 heroes) ----------------------------------
const NIB = [
  '....hhhh....',
  '...hhhhhh...',
  '...sSSSSs...',
  '...sSooSs...',  // eyes
  '...sSSSSs...',
  '..cCCCCCCc..',
  '.cCCCCCCCCc.',
  '.cCgGGGgCc..',  // belt buckle
  '.cCCCCCCCCc.',
  '..lLLLLLLl..',
  '..lL.ss.Ll..',
  '..lL.ss.Ll..',
  '...k....k...',
];
const NIB_MAP = {
  h: PALETTE.hair, s: PALETTE.skin, S: PALETTE.skin, o: '#1a1a1a',
  c: PALETTE.cloak, C: PALETTE.cloak, g: PALETTE.gold, G: PALETTE.gold,
  l: PALETTE.leather, L: PALETTE.leatherDark, k: PALETTE.leatherDark,
};

const BRUGG = [
  '...MMMMMM...',
  '..MMMMMMMM..',
  '..sSSSSSSs..',
  '..sSooSoSs..',
  '..sSSSSSSs..',
  '.tTTTTTTTTt.',
  'tTTTTTTTTTTt',
  'tTTTTTTTTTTt',
  'tTTTggTTTTTt',
  '.tTTTTTTTTt.',
  '..LLLLLLLL..',
  '..LL.ss.LL..',
  '...k....k...',
];
const BRUGG_MAP = {
  M: PALETTE.steel, s: PALETTE.skinDark, S: PALETTE.skinDark, o: '#1a1a1a',
  t: PALETTE.steelDark, T: PALETTE.steel, g: PALETTE.gold,
  L: PALETTE.leatherDark, k: PALETTE.leatherDark,
};

const VELLUM = [
  '....BBBB....',
  '...BBBBBB...',  // pointed hat
  '..BBBBBBBB..',
  '...sSSSSs...',
  '...sSooSs...',
  '...sSSSSs...',
  '..vVVVVVVv..',
  '.vVVVgGVVVv.',  // robe with gold trim
  '.vVVVVVVVVv.',
  '.vVVVVVVVVv.',
  '..vVVVVVVv..',
  '..vV.ss.Vv..',
  '...k....k...',
];
const VELLUM_MAP = {
  B: '#2a3a8a', s: PALETTE.skin, S: PALETTE.skin, o: '#1a1a1a',
  v: '#3a3f74', V: '#5b6ee1', g: PALETTE.gold, G: PALETTE.gold,
  k: PALETTE.leatherDark,
};

// The glowing Questbook (emissive)
const BOOK = [
  'bBBBBBBBBb',
  'BllllllllB',
  'Bl.GG.GG.B',
  'Bl.GG.GG.B',
  'BllllllllB',
  'Bl.GGGG..B',
  'BllllllllB',
  'bBBBBBBBBb',
];
const BOOK_MAP = { b: PALETTE.bookDark, B: PALETTE.book, l: '#bfe9ff', G: PALETTE.gold };

const TREE = [
  '...LLLLL...',
  '..LLLLLLL..',
  '.LLLLLLLLL.',
  'LLLLLLLLLLL',
  '.LLLLLLLLL.',
  '..LLLLLLL..',
  '....ttt....',
  '....ttt....',
  '....ttt....',
];
const TREE_MAP = { L: PALETTE.leaf, t: PALETTE.trunk };

// =============================================================================
// Scene setup
// =============================================================================
const app = document.getElementById('app');

const scene = new THREE.Scene();
scene.background = new THREE.Color(0x0a0714);
scene.fog = new THREE.Fog(0x1a0f28, 14, 42);

// Camera: high angle looking down ~ HD-2D signature framing
const camera = new THREE.PerspectiveCamera(34, window.innerWidth / window.innerHeight, 0.1, 200);
camera.position.set(0, 9, 11);
camera.lookAt(0, 0.5, 0);

const renderer = new THREE.WebGLRenderer({ antialias: true });
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 1.15;
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
app.appendChild(renderer.domElement);

const controls = new OrbitControls(camera, renderer.domElement);
controls.target.set(0, 0.6, 0);
controls.enableDamping = true;
controls.dampingFactor = 0.08;
controls.minPolarAngle = 0.35;
controls.maxPolarAngle = 1.15;
controls.minDistance = 6;
controls.maxDistance = 20;

// --- Lighting (3D!) - this is what sprites in a 2D engine cannot get for free
const ambient = new THREE.AmbientLight(0x4a3f6b, 0.6);
scene.add(ambient);

const moon = new THREE.DirectionalLight(0x9fb4ff, 0.9);
moon.position.set(-6, 12, 4);
moon.castShadow = true;
moon.shadow.mapSize.set(2048, 2048);
moon.shadow.camera.near = 1;
moon.shadow.camera.far = 40;
moon.shadow.camera.left = -16;
moon.shadow.camera.right = 16;
moon.shadow.camera.top = 16;
moon.shadow.camera.bottom = -16;
moon.shadow.bias = -0.0008;
scene.add(moon);

// Warm lantern that follows nothing - pure atmosphere
const lantern = new THREE.PointLight(0xffb35e, 14, 16, 2);
lantern.position.set(3.5, 2.4, 2);
scene.add(lantern);
const lanternMesh = new THREE.Mesh(
  new THREE.SphereGeometry(0.12, 12, 12),
  new THREE.MeshBasicMaterial({ color: 0xffd98a })
);
lanternMesh.position.copy(lantern.position);
scene.add(lanternMesh);

// =============================================================================
// Ground - 3D plane with a pixel-art tile texture (repeated)
// =============================================================================
function makeGroundTexture() {
  const size = 32;
  const cv = document.createElement('canvas');
  cv.width = cv.height = size;
  const ctx = cv.getContext('2d');
  // base grass
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const r = Math.random();
      ctx.fillStyle = r > 0.85 ? PALETTE.grassLite : (r > 0.4 ? PALETTE.grass : PALETTE.grassDark);
      ctx.fillRect(x, y, 1, 1);
    }
  }
  // a stone path down the middle
  ctx.fillStyle = PALETTE.stone;
  for (let y = 0; y < size; y++) {
    for (let x = 12; x < 20; x++) {
      ctx.fillStyle = Math.random() > 0.5 ? PALETTE.stone : PALETTE.stoneDark;
      ctx.fillRect(x, y, 1, 1);
    }
  }
  const tex = new THREE.CanvasTexture(cv);
  tex.magFilter = THREE.NearestFilter;
  tex.minFilter = THREE.NearestFilter;
  tex.wrapS = tex.wrapT = THREE.RepeatWrapping;
  tex.repeat.set(10, 10);
  tex.colorSpace = THREE.SRGBColorSpace;
  return tex;
}

const ground = new THREE.Mesh(
  new THREE.PlaneGeometry(40, 40),
  new THREE.MeshStandardMaterial({ map: makeGroundTexture(), roughness: 1, metalness: 0 })
);
ground.rotation.x = -Math.PI / 2;
ground.receiveShadow = true;
scene.add(ground);

// =============================================================================
// Billboarded pixel sprite factory
// =============================================================================
const billboards = [];

function makeSprite(def, map, { x = 0, z = 0, height = 2.2, emissive = false, scale = 8 } = {}) {
  const { tex, w, h } = pixelTexture(def, map, scale);
  const aspect = w / h;
  const mat = new THREE.MeshStandardMaterial({
    map: tex,
    transparent: true,
    alphaTest: 0.5,
    roughness: 1,
    metalness: 0,
    side: THREE.DoubleSide,
    emissive: emissive ? new THREE.Color(0x3a8fe0) : new THREE.Color(0x000000),
    emissiveMap: emissive ? tex : null,
    emissiveIntensity: emissive ? 1.6 : 0,
  });
  const mesh = new THREE.Mesh(new THREE.PlaneGeometry(height * aspect, height), mat);
  mesh.position.set(x, height / 2, z);
  mesh.castShadow = true;
  scene.add(mesh);
  billboards.push(mesh);
  return mesh;
}

// Party
const nib = makeSprite(NIB, NIB_MAP, { x: 0, z: 0, height: 2.0 });
makeSprite(BRUGG, BRUGG_MAP, { x: -1.8, z: 0.6, height: 2.4 });
makeSprite(VELLUM, VELLUM_MAP, { x: 1.8, z: 0.6, height: 2.2 });

// Glowing Questbook on a little pedestal
const book = makeSprite(BOOK, BOOK_MAP, { x: 0, z: -2.4, height: 1.1, emissive: true, scale: 10 });
const pedestal = new THREE.Mesh(
  new THREE.CylinderGeometry(0.5, 0.6, 0.5, 8),
  new THREE.MeshStandardMaterial({ color: 0x43403c, roughness: 0.9 })
);
pedestal.position.set(0, 0.25, -2.4);
pedestal.castShadow = true;
pedestal.receiveShadow = true;
scene.add(pedestal);
const bookGlow = new THREE.PointLight(0x7be0ff, 6, 8, 2);
bookGlow.position.set(0, 1.2, -2.4);
scene.add(bookGlow);

// Trees scattered for parallax depth
const treePositions = [
  [-7, -6], [6, -7], [-9, 2], [9, -2], [-5, 6], [7, 5], [0, -9], [-11, -4], [11, 4],
];
for (const [tx, tz] of treePositions) {
  makeSprite(TREE, TREE_MAP, { x: tx, z: tz, height: 3.4 + Math.random() });
}

// Floating dust motes (instanced points) - catch the bloom
const moteGeo = new THREE.BufferGeometry();
const MOTES = 220;
const mpos = new Float32Array(MOTES * 3);
for (let i = 0; i < MOTES; i++) {
  mpos[i * 3] = (Math.random() - 0.5) * 30;
  mpos[i * 3 + 1] = Math.random() * 6 + 0.3;
  mpos[i * 3 + 2] = (Math.random() - 0.5) * 30;
}
moteGeo.setAttribute('position', new THREE.BufferAttribute(mpos, 3));
const moteMat = new THREE.PointsMaterial({
  color: 0xffe9b0, size: 0.07, transparent: true, opacity: 0.8,
  blending: THREE.AdditiveBlending, depthWrite: false,
});
const motes = new THREE.Points(moteGeo, moteMat);
scene.add(motes);

// =============================================================================
// Post-processing stack (the HD-2D "magic")
// =============================================================================
const composer = new EffectComposer(renderer);
composer.addPass(new RenderPass(scene, camera));

const bloomPass = new UnrealBloomPass(
  new THREE.Vector2(window.innerWidth, window.innerHeight),
  0.9,   // strength
  0.5,   // radius
  0.78   // threshold - only bright/emissive pixels bloom
);
composer.addPass(bloomPass);

const bokehPass = new BokehPass(scene, camera, {
  focus: 11.0,     // focus on the party
  aperture: 0.0016, // tilt-shift miniaturisation
  maxblur: 0.012,
});
composer.addPass(bokehPass);

// Simple vignette shader pass
const VignetteShader = {
  uniforms: { tDiffuse: { value: null }, offset: { value: 1.05 }, darkness: { value: 1.25 } },
  vertexShader: `varying vec2 vUv; void main(){ vUv=uv; gl_Position=projectionMatrix*modelViewMatrix*vec4(position,1.0); }`,
  fragmentShader: `
    uniform sampler2D tDiffuse; uniform float offset; uniform float darkness; varying vec2 vUv;
    void main(){
      vec4 tex = texture2D(tDiffuse, vUv);
      vec2 uv = (vUv - 0.5) * vec2(offset);
      float v = clamp(1.0 - dot(uv, uv) * darkness, 0.0, 1.0);
      gl_FragColor = vec4(tex.rgb * v, tex.a);
    }`,
};
const vignettePass = new ShaderPass(VignetteShader);
composer.addPass(vignettePass);

// Optional pixelation pass (off by default)
const PixelShader = {
  uniforms: {
    tDiffuse: { value: null },
    resolution: { value: new THREE.Vector2(window.innerWidth, window.innerHeight) },
    pixelSize: { value: 4.0 },
  },
  vertexShader: `varying vec2 vUv; void main(){ vUv=uv; gl_Position=projectionMatrix*modelViewMatrix*vec4(position,1.0); }`,
  fragmentShader: `
    uniform sampler2D tDiffuse; uniform vec2 resolution; uniform float pixelSize; varying vec2 vUv;
    void main(){
      vec2 d = pixelSize / resolution;
      vec2 uv = d * floor(vUv / d);
      gl_FragColor = texture2D(tDiffuse, uv);
    }`,
};
const pixelPass = new ShaderPass(PixelShader);
pixelPass.enabled = false;
composer.addPass(pixelPass);

composer.addPass(new OutputPass());

// --- FX toggles
document.getElementById('fx-bloom').addEventListener('change', (e) => bloomPass.enabled = e.target.checked);
document.getElementById('fx-dof').addEventListener('change', (e) => bokehPass.enabled = e.target.checked);
document.getElementById('fx-vignette').addEventListener('change', (e) => vignettePass.enabled = e.target.checked);
document.getElementById('fx-pixel').addEventListener('change', (e) => pixelPass.enabled = e.target.checked);

// =============================================================================
// Input - move Nib (the player)
// =============================================================================
const keys = {};
window.addEventListener('keydown', (e) => keys[e.key.toLowerCase()] = true);
window.addEventListener('keyup', (e) => keys[e.key.toLowerCase()] = false);

// =============================================================================
// Animation loop
// =============================================================================
const clock = new THREE.Clock();

function billboardAll() {
  // Y-axis billboard: sprites face the camera horizontally (HD-2D style)
  for (const b of billboards) {
    b.rotation.y = Math.atan2(
      camera.position.x - b.position.x,
      camera.position.z - b.position.z
    );
  }
}

function animate() {
  requestAnimationFrame(animate);
  const dt = Math.min(clock.getDelta(), 0.05);
  const t = clock.elapsedTime;

  // Move player
  const speed = 4 * dt;
  let dx = 0, dz = 0;
  if (keys['w'] || keys['arrowup']) dz -= 1;
  if (keys['s'] || keys['arrowdown']) dz += 1;
  if (keys['a'] || keys['arrowleft']) dx -= 1;
  if (keys['d'] || keys['arrowright']) dx += 1;
  if (dx || dz) {
    const len = Math.hypot(dx, dz);
    nib.position.x += (dx / len) * speed;
    nib.position.z += (dz / len) * speed;
    // little hop bob while walking
    nib.position.y = 1.0 + Math.abs(Math.sin(t * 12)) * 0.12;
  } else {
    nib.position.y = 1.0 + Math.sin(t * 2) * 0.03; // idle breathe
  }

  // Questbook bob + glow pulse
  book.position.y = 0.9 + Math.sin(t * 2) * 0.08;
  bookGlow.intensity = 5 + Math.sin(t * 2.5) * 2;

  // Lantern flicker
  lantern.intensity = 12 + Math.sin(t * 9) * Math.sin(t * 3.3) * 3;

  // Drift motes upward
  const arr = moteGeo.attributes.position.array;
  for (let i = 0; i < MOTES; i++) {
    arr[i * 3 + 1] += dt * (0.2 + (i % 5) * 0.06);
    if (arr[i * 3 + 1] > 6.5) arr[i * 3 + 1] = 0.2;
  }
  moteGeo.attributes.position.needsUpdate = true;

  billboardAll();
  controls.update();
  composer.render();
}

// =============================================================================
// Resize + boot
// =============================================================================
window.addEventListener('resize', () => {
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);
  composer.setSize(window.innerWidth, window.innerHeight);
  pixelPass.uniforms.resolution.value.set(window.innerWidth, window.innerHeight);
});

document.getElementById('loading').style.display = 'none';
animate();
