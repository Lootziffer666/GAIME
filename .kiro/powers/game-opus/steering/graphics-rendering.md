# Graphics & Rendering

Complete reference for WebGL rendering pipeline, WebGPU with Three.js Shading Language (TSL), custom shader effects, post-processing, and visual FX.

---

## WebGL Fundamentals

### Rendering Pipeline Overview

1. **Vertex Shader** - Transforms vertex positions (model -> world -> view -> clip space)
2. **Primitive Assembly** - Assembles vertices into triangles
3. **Rasterization** - Converts triangles to fragments (pixels)
4. **Fragment Shader** - Computes color for each fragment
5. **Depth/Stencil Test** - Determines visibility
6. **Blending** - Combines with framebuffer

### Custom ShaderMaterial

```typescript
import * as THREE from 'three';

const vertexShader = `
  uniform float uTime;
  varying vec2 vUv;
  varying vec3 vNormal;
  varying vec3 vWorldPosition;

  void main() {
    vUv = uv;
    vNormal = normalize(normalMatrix * normal);

    vec3 pos = position;
    // Vertex displacement
    pos.y += sin(pos.x * 2.0 + uTime) * 0.3;

    vec4 worldPos = modelMatrix * vec4(pos, 1.0);
    vWorldPosition = worldPos.xyz;

    gl_Position = projectionMatrix * viewMatrix * worldPos;
  }
`;

const fragmentShader = `
  uniform float uTime;
  uniform vec3 uColor;
  uniform sampler2D uTexture;

  varying vec2 vUv;
  varying vec3 vNormal;
  varying vec3 vWorldPosition;

  void main() {
    vec4 texColor = texture2D(uTexture, vUv);
    vec3 light = normalize(vec3(1.0, 1.0, 1.0));
    float diffuse = max(dot(vNormal, light), 0.0);

    vec3 color = uColor * texColor.rgb * (0.3 + 0.7 * diffuse);
    gl_FragColor = vec4(color, 1.0);
  }
`;

const material = new THREE.ShaderMaterial({
  vertexShader,
  fragmentShader,
  uniforms: {
    uTime: { value: 0.0 },
    uColor: { value: new THREE.Color(0.2, 0.5, 1.0) },
    uTexture: { value: texture },
  },
  transparent: true,
  side: THREE.DoubleSide,
});

// Update in render loop
material.uniforms.uTime.value = elapsedTime;
```

### GLSL Common Patterns

```glsl
// Noise functions (include in shader)
float random(vec2 st) {
  return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

// Simplex-like noise
float noise(vec2 st) {
  vec2 i = floor(st);
  vec2 f = fract(st);
  float a = random(i);
  float b = random(i + vec2(1.0, 0.0));
  float c = random(i + vec2(0.0, 1.0));
  float d = random(i + vec2(1.0, 1.0));
  vec2 u = f * f * (3.0 - 2.0 * f); // smoothstep
  return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

// FBM (Fractal Brownian Motion)
float fbm(vec2 st) {
  float value = 0.0;
  float amplitude = 0.5;
  float frequency = 1.0;
  for (int i = 0; i < 6; i++) {
    value += amplitude * noise(st * frequency);
    frequency *= 2.0;
    amplitude *= 0.5;
  }
  return value;
}

// SDF (Signed Distance Functions)
float sdSphere(vec3 p, float r) { return length(p) - r; }
float sdBox(vec3 p, vec3 b) {
  vec3 d = abs(p) - b;
  return min(max(d.x, max(d.y, d.z)), 0.0) + length(max(d, 0.0));
}

// Smooth operations
float smoothMin(float a, float b, float k) {
  float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
  return mix(b, a, h) - k * h * (1.0 - h);
}

// Color utilities
vec3 palette(float t, vec3 a, vec3 b, vec3 c, vec3 d) {
  return a + b * cos(6.28318 * (c * t + d));
}
```

### RawShaderMaterial (Full Control)

```typescript
// No Three.js built-in uniforms/attributes - you define everything
const rawMaterial = new THREE.RawShaderMaterial({
  vertexShader: `#version 300 es
    in vec3 position;
    in vec2 uv;
    uniform mat4 projectionMatrix;
    uniform mat4 modelViewMatrix;
    out vec2 vUv;
    void main() {
      vUv = uv;
      gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
    }
  `,
  fragmentShader: `#version 300 es
    precision highp float;
    in vec2 vUv;
    out vec4 fragColor;
    void main() {
      fragColor = vec4(vUv, 0.5, 1.0);
    }
  `,
  uniforms: {},
});
```

---

## WebGPU & Three.js Shading Language (TSL)

### TSL Overview

Three.js Shading Language (TSL) is a node-based shader system for WebGPU (and WebGL fallback). Write shaders in JavaScript instead of GLSL.

```typescript
import * as THREE from 'three/webgpu';
import {
  color, float, vec2, vec3, vec4,
  uniform, attribute, varying,
  sin, cos, mix, smoothstep, step, clamp, fract, floor,
  dot, normalize, length, distance, reflect,
  texture, uv, position, normal, time,
  MeshStandardNodeMaterial, MeshBasicNodeMaterial,
} from 'three/tsl';

// WebGPU Renderer
const renderer = new THREE.WebGPURenderer({ antialias: true });
await renderer.init();
renderer.setSize(window.innerWidth, window.innerHeight);
```

### TSL Material Examples

```typescript
// Animated color material
const material = new MeshStandardNodeMaterial();
material.colorNode = mix(
  color(0xff0000),
  color(0x0000ff),
  sin(time.mul(2.0)).mul(0.5).add(0.5)
);

// UV-based pattern
const checkerMat = new MeshBasicNodeMaterial();
const checker = step(0.5, fract(uv().mul(10.0)));
checkerMat.colorNode = mix(color(0x000000), color(0xffffff), checker.x.add(checker.y).mod(2.0));

// Fresnel effect
const fresnelMat = new MeshStandardNodeMaterial();
const viewDir = normalize(cameraPosition.sub(positionWorld));
const fresnel = float(1.0).sub(dot(viewDir, normalWorld)).pow(3.0);
fresnelMat.emissiveNode = color(0x00ffff).mul(fresnel);

// Displacement
const dispMat = new MeshStandardNodeMaterial();
dispMat.positionNode = position.add(
  normal.mul(sin(position.x.mul(5.0).add(time)).mul(0.2))
);
```

### TSL Noise & Patterns

```typescript
import { hash, mx_noise_float, mx_fractal_noise_float } from 'three/tsl';

// Perlin-like noise
const noiseValue = mx_noise_float(position.mul(2.0).add(time.mul(0.5)));

// Fractal noise
const fbmValue = mx_fractal_noise_float(
  position.mul(3.0), // position
  4,                  // octaves
  2.0,               // lacunarity
  0.5,               // diminish
  float(0.0)         // offset (unused for float)
);

// Apply to material
material.colorNode = mix(color(0x001133), color(0x00aaff), fbmValue);
```

---

## Post-Processing Effects

### Three.js EffectComposer

```typescript
import { EffectComposer } from 'three/examples/jsm/postprocessing/EffectComposer';
import { RenderPass } from 'three/examples/jsm/postprocessing/RenderPass';
import { UnrealBloomPass } from 'three/examples/jsm/postprocessing/UnrealBloomPass';
import { ShaderPass } from 'three/examples/jsm/postprocessing/ShaderPass';
import { SMAAPass } from 'three/examples/jsm/postprocessing/SMAAPass';

const composer = new EffectComposer(renderer);

// Base render
composer.addPass(new RenderPass(scene, camera));

// Bloom
const bloomPass = new UnrealBloomPass(
  new THREE.Vector2(window.innerWidth, window.innerHeight),
  1.5,  // strength
  0.4,  // radius
  0.85  // threshold
);
composer.addPass(bloomPass);

// Anti-aliasing
const smaaPass = new SMAAPass(window.innerWidth, window.innerHeight);
composer.addPass(smaaPass);

// Custom shader pass
const customPass = new ShaderPass({
  uniforms: {
    tDiffuse: { value: null },
    uTime: { value: 0 },
  },
  vertexShader: `
    varying vec2 vUv;
    void main() {
      vUv = uv;
      gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
    }
  `,
  fragmentShader: `
    uniform sampler2D tDiffuse;
    uniform float uTime;
    varying vec2 vUv;
    void main() {
      vec2 uv = vUv;
      // Chromatic aberration
      float offset = 0.003;
      float r = texture2D(tDiffuse, uv + vec2(offset, 0.0)).r;
      float g = texture2D(tDiffuse, uv).g;
      float b = texture2D(tDiffuse, uv - vec2(offset, 0.0)).b;
      gl_FragColor = vec4(r, g, b, 1.0);
    }
  `,
});
composer.addPass(customPass);

// In render loop
composer.render(); // instead of renderer.render(scene, camera)
```

### R3F Post-Processing (@react-three/postprocessing)

```tsx
import { EffectComposer, Bloom, ChromaticAberration, Vignette, SSAO, ToneMapping } from '@react-three/postprocessing';
import { BlendFunction } from 'postprocessing';

<EffectComposer>
  <Bloom
    luminanceThreshold={0.9}
    luminanceSmoothing={0.025}
    intensity={1.5}
    mipmapBlur
  />
  <ChromaticAberration
    offset={new THREE.Vector2(0.002, 0.002)}
    blendFunction={BlendFunction.NORMAL}
  />
  <Vignette offset={0.3} darkness={0.7} />
  <SSAO
    radius={0.1}
    intensity={30}
    luminanceInfluence={0.5}
    color={new THREE.Color(0x000000)}
  />
  <ToneMapping mode={THREE.ACESFilmicToneMapping} />
</EffectComposer>
```

---

## Custom Shader Effects

### Dissolve Effect

```glsl
// Fragment shader
uniform float uProgress; // 0 to 1
uniform sampler2D uNoiseTexture;
uniform vec3 uEdgeColor;
uniform float uEdgeWidth;

varying vec2 vUv;

void main() {
  float noise = texture2D(uNoiseTexture, vUv).r;

  // Discard dissolved pixels
  if (noise < uProgress) discard;

  // Edge glow
  float edge = smoothstep(uProgress, uProgress + uEdgeWidth, noise);
  vec3 color = mix(uEdgeColor, vec3(1.0), edge);

  gl_FragColor = vec4(color, 1.0);
}
```

### Outline Effect

```glsl
// Two-pass approach:
// Pass 1: Render objects with solid color to a render target
// Pass 2: Detect edges via Sobel filter on the render target

// Fragment shader (Pass 2)
uniform sampler2D tDiffuse;
uniform sampler2D tOutline;
uniform vec2 uResolution;
uniform vec3 uOutlineColor;
uniform float uOutlineThickness;

varying vec2 vUv;

void main() {
  vec4 sceneColor = texture2D(tDiffuse, vUv);

  // Sobel edge detection on outline buffer
  vec2 texel = vec2(1.0) / uResolution * uOutlineThickness;
  float tl = texture2D(tOutline, vUv + vec2(-texel.x, texel.y)).r;
  float t  = texture2D(tOutline, vUv + vec2(0.0, texel.y)).r;
  float tr = texture2D(tOutline, vUv + vec2(texel.x, texel.y)).r;
  float l  = texture2D(tOutline, vUv + vec2(-texel.x, 0.0)).r;
  float r  = texture2D(tOutline, vUv + vec2(texel.x, 0.0)).r;
  float bl = texture2D(tOutline, vUv + vec2(-texel.x, -texel.y)).r;
  float b  = texture2D(tOutline, vUv + vec2(0.0, -texel.y)).r;
  float br = texture2D(tOutline, vUv + vec2(texel.x, -texel.y)).r;

  float sobelX = tl + 2.0*l + bl - tr - 2.0*r - br;
  float sobelY = tl + 2.0*t + tr - bl - 2.0*b - br;
  float edge = sqrt(sobelX * sobelX + sobelY * sobelY);

  vec3 finalColor = mix(sceneColor.rgb, uOutlineColor, edge);
  gl_FragColor = vec4(finalColor, 1.0);
}
```

### Hologram Effect

```glsl
// Vertex shader
varying vec3 vNormal;
varying vec3 vViewDir;
varying float vY;

void main() {
  vNormal = normalize(normalMatrix * normal);
  vec4 viewPos = modelViewMatrix * vec4(position, 1.0);
  vViewDir = normalize(-viewPos.xyz);
  vY = position.y;
  gl_Position = projectionMatrix * viewPos;
}

// Fragment shader
uniform float uTime;
uniform vec3 uColor;

varying vec3 vNormal;
varying vec3 vViewDir;
varying float vY;

void main() {
  // Fresnel
  float fresnel = pow(1.0 - dot(vNormal, vViewDir), 3.0);

  // Scanlines
  float scanline = sin(vY * 50.0 + uTime * 5.0) * 0.5 + 0.5;
  scanline = pow(scanline, 4.0);

  // Flicker
  float flicker = sin(uTime * 30.0) * 0.03 + 0.97;

  // Combine
  float alpha = (fresnel * 0.7 + scanline * 0.3) * flicker;
  vec3 color = uColor * (1.0 + fresnel * 0.5);

  gl_FragColor = vec4(color, alpha * 0.8);
}
```

### Water Shader

```glsl
// Vertex shader
uniform float uTime;
uniform float uWaveHeight;
uniform float uWaveFrequency;

varying vec2 vUv;
varying vec3 vWorldPosition;
varying vec3 vNormal;

void main() {
  vUv = uv;
  vec3 pos = position;

  // Gerstner-like wave displacement
  float wave1 = sin(pos.x * uWaveFrequency + uTime * 2.0) * uWaveHeight;
  float wave2 = sin(pos.z * uWaveFrequency * 0.7 + uTime * 1.5) * uWaveHeight * 0.5;
  float wave3 = cos((pos.x + pos.z) * uWaveFrequency * 0.5 + uTime) * uWaveHeight * 0.3;
  pos.y += wave1 + wave2 + wave3;

  // Compute normal from partial derivatives
  float dx = cos(pos.x * uWaveFrequency + uTime * 2.0) * uWaveHeight * uWaveFrequency;
  float dz = cos(pos.z * uWaveFrequency * 0.7 + uTime * 1.5) * uWaveHeight * 0.5 * uWaveFrequency * 0.7;
  vNormal = normalize(vec3(-dx, 1.0, -dz));

  vec4 worldPos = modelMatrix * vec4(pos, 1.0);
  vWorldPosition = worldPos.xyz;
  gl_Position = projectionMatrix * viewMatrix * worldPos;
}

// Fragment shader
uniform float uTime;
uniform vec3 uWaterColor;
uniform vec3 uFoamColor;
uniform samplerCube uEnvMap;
uniform vec3 uCameraPosition;

varying vec2 vUv;
varying vec3 vWorldPosition;
varying vec3 vNormal;

void main() {
  vec3 viewDir = normalize(uCameraPosition - vWorldPosition);
  float fresnel = pow(1.0 - max(dot(viewDir, vNormal), 0.0), 4.0);

  // Reflection
  vec3 reflectDir = reflect(-viewDir, vNormal);
  vec3 envColor = textureCube(uEnvMap, reflectDir).rgb;

  // Deep vs shallow color
  vec3 waterColor = mix(uWaterColor, uWaterColor * 0.3, fresnel);

  // Combine
  vec3 color = mix(waterColor, envColor, fresnel * 0.6);

  gl_FragColor = vec4(color, 0.85);
}
```

### Particle Systems with Custom Shaders

```typescript
// GPU particles using BufferGeometry + ShaderMaterial
const COUNT = 10000;
const geometry = new THREE.BufferGeometry();
const positions = new Float32Array(COUNT * 3);
const randoms = new Float32Array(COUNT);
const sizes = new Float32Array(COUNT);

for (let i = 0; i < COUNT; i++) {
  positions[i * 3] = (Math.random() - 0.5) * 20;
  positions[i * 3 + 1] = Math.random() * 10;
  positions[i * 3 + 2] = (Math.random() - 0.5) * 20;
  randoms[i] = Math.random();
  sizes[i] = Math.random() * 2 + 0.5;
}

geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
geometry.setAttribute('aRandom', new THREE.BufferAttribute(randoms, 1));
geometry.setAttribute('aSize', new THREE.BufferAttribute(sizes, 1));

const particleMaterial = new THREE.ShaderMaterial({
  vertexShader: `
    attribute float aRandom;
    attribute float aSize;
    uniform float uTime;
    varying float vAlpha;

    void main() {
      vec3 pos = position;
      pos.y = mod(pos.y + uTime * (0.5 + aRandom), 10.0);
      pos.x += sin(uTime * aRandom) * 0.5;

      vec4 mvPosition = modelViewMatrix * vec4(pos, 1.0);
      gl_PointSize = aSize * (300.0 / -mvPosition.z);
      gl_Position = projectionMatrix * mvPosition;

      vAlpha = smoothstep(0.0, 2.0, pos.y) * smoothstep(10.0, 8.0, pos.y);
    }
  `,
  fragmentShader: `
    varying float vAlpha;
    void main() {
      float dist = length(gl_PointCoord - vec2(0.5));
      if (dist > 0.5) discard;
      float alpha = smoothstep(0.5, 0.0, dist) * vAlpha;
      gl_FragColor = vec4(1.0, 0.8, 0.4, alpha);
    }
  `,
  uniforms: { uTime: { value: 0 } },
  transparent: true,
  depthWrite: false,
  blending: THREE.AdditiveBlending,
});

const particles = new THREE.Points(geometry, particleMaterial);
```

---

## Render Targets & Multi-Pass

```typescript
// Off-screen rendering
const renderTarget = new THREE.WebGLRenderTarget(
  window.innerWidth, window.innerHeight,
  {
    minFilter: THREE.LinearFilter,
    magFilter: THREE.LinearFilter,
    format: THREE.RGBAFormat,
    type: THREE.HalfFloatType,
  }
);

// Render scene to texture
renderer.setRenderTarget(renderTarget);
renderer.render(scene, camera);
renderer.setRenderTarget(null);

// Use as texture in another material
quadMaterial.uniforms.tScene.value = renderTarget.texture;

// Multiple render targets (MRT) for deferred rendering
const mrt = new THREE.WebGLMultipleRenderTargets(width, height, 3);
// mrt.texture[0] = albedo
// mrt.texture[1] = normals
// mrt.texture[2] = position/depth
```
