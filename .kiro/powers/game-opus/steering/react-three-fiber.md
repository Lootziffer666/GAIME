# React Three Fiber & Physics

Complete reference for React Three Fiber (R3F) fundamentals, component patterns, drei helpers, and physics engine integration with Rapier and Cannon.

---

## R3F Fundamentals

### Canvas Setup

```tsx
import { Canvas } from '@react-three/fiber';

function App() {
  return (
    <Canvas
      camera={{ position: [0, 5, 10], fov: 75, near: 0.1, far: 1000 }}
      shadows
      dpr={[1, 2]}
      gl={{
        antialias: true,
        toneMapping: THREE.ACESFilmicToneMapping,
        outputColorSpace: THREE.SRGBColorSpace,
      }}
      onCreated={({ gl, scene }) => {
        gl.setClearColor(0x000000);
      }}
    >
      <SceneContent />
    </Canvas>
  );
}
```

### Core Hooks

```tsx
import { useFrame, useThree, useLoader } from '@react-three/fiber';
import * as THREE from 'three';


// useFrame - runs every frame (game loop)
function AnimatedBox() {
  const meshRef = useRef<THREE.Mesh>(null!);

  useFrame((state, delta) => {
    meshRef.current.rotation.y += delta;
    // state.clock, state.camera, state.scene, state.gl available
  });

  return <mesh ref={meshRef}><boxGeometry /><meshStandardMaterial /></mesh>;
}

// useThree - access renderer state
function CameraController() {
  const { camera, gl, scene, size, viewport } = useThree();
  // viewport gives world-space dimensions at z=0
  return null;
}

// useLoader - load assets with suspense
function Model() {
  const gltf = useLoader(GLTFLoader, '/model.glb');
  return <primitive object={gltf.scene} />;
}
```

### Declarative 3D Objects

```tsx
// Mesh with geometry and material
<mesh position={[0, 1, 0]} rotation={[0, Math.PI / 4, 0]} scale={1.5}>
  <boxGeometry args={[1, 1, 1]} />
  <meshStandardMaterial color="hotpink" metalness={0.5} roughness={0.2} />
</mesh>

// Lights
<ambientLight intensity={0.5} />
<directionalLight position={[10, 10, 5]} intensity={1} castShadow />
<pointLight position={[0, 5, 0]} intensity={1} distance={50} />
<spotLight position={[0, 10, 0]} angle={Math.PI / 6} penumbra={0.5} castShadow />

// Groups
<group position={[0, 0, 0]} rotation={[0, 0, 0]}>
  <mesh>...</mesh>
  <mesh>...</mesh>
</group>

// Instanced Mesh
<instancedMesh ref={ref} args={[geometry, material, count]}>
  <boxGeometry />
  <meshStandardMaterial />
</instancedMesh>
```

### Event Handling

```tsx
function InteractiveMesh() {
  const [hovered, setHovered] = useState(false);
  const [clicked, setClicked] = useState(false);

  return (
    <mesh
      onClick={(e) => { e.stopPropagation(); setClicked(!clicked); }}
      onPointerOver={(e) => { e.stopPropagation(); setHovered(true); }}
      onPointerOut={() => setHovered(false)}
      onPointerMove={(e) => { /* e.point, e.face, e.distance */ }}
      onDoubleClick={(e) => { /* double click */ }}
      onContextMenu={(e) => { /* right click */ }}
      scale={clicked ? 1.5 : 1}
    >
      <boxGeometry />
      <meshStandardMaterial color={hovered ? 'orange' : 'white'} />
    </mesh>
  );
}
```

---

## Drei Helpers

```tsx
import {
  OrbitControls, TransformControls, PerspectiveCamera,
  Environment, Sky, Stars, Cloud,
  Text, Text3D, Html, Billboard,
  useGLTF, useAnimations, useTexture,
  Float, MeshDistortMaterial, MeshWobbleMaterial,
  Sparkles, Trail, useHelper,
  Bvh, Instances, Instance,
} from '@react-three/drei';

// Camera controls
<OrbitControls makeDefault enableDamping dampingFactor={0.05} />

// Environment lighting
<Environment preset="sunset" background blur={0.5} />
// Presets: apartment, city, dawn, forest, lobby, night, park, studio, sunset, warehouse

// Sky
<Sky sunPosition={[100, 20, 100]} turbidity={8} rayleigh={2} />

// Preloaded GLTF (with static preload for caching)
function Character() {
  const { scene, animations } = useGLTF('/character.glb');
  const { actions } = useAnimations(animations, scene);
  useEffect(() => { actions['Idle']?.play(); }, []);
  return <primitive object={scene} />;
}
Character.preload = () => useGLTF.preload('/character.glb');

// Texture hook
function Ground() {
  const [diffuse, normal, rough] = useTexture([
    '/ground-diffuse.jpg', '/ground-normal.jpg', '/ground-rough.jpg'
  ]);
  return (
    <mesh rotation-x={-Math.PI / 2}>
      <planeGeometry args={[100, 100]} />
      <meshStandardMaterial map={diffuse} normalMap={normal} roughnessMap={rough} />
    </mesh>
  );
}

// HTML overlay in 3D space
<Html position={[0, 2, 0]} center distanceFactor={10} occlude>
  <div className="health-bar">HP: 100</div>
</Html>

// Text rendering
<Text fontSize={0.5} color="white" anchorX="center" anchorY="middle">
  Hello World
</Text>

// Performance: BVH for raycasting acceleration
<Bvh>
  <mesh>...</mesh>
</Bvh>

// Instancing with Drei
<Instances limit={1000}>
  <boxGeometry />
  <meshStandardMaterial />
  {positions.map((pos, i) => (
    <Instance key={i} position={pos} color="red" />
  ))}
</Instances>
```

---

## R3F Performance Patterns

### Avoiding Re-renders

```tsx
// BAD: inline objects cause re-renders
<mesh position={[0, 1, 0]} /> // new array every render!

// GOOD: stable references
const position = useMemo(() => new THREE.Vector3(0, 1, 0), []);
<mesh position={position} />

// GOOD: for static values, use args or set
<mesh position-y={1} />

// BAD: state in parent re-renders all children
function Parent() {
  const [count, setCount] = useState(0); // triggers full re-render
  return <><ExpensiveScene /><HUD count={count} /></>;
}

// GOOD: isolate state with zustand or separate components
import { create } from 'zustand';
const useStore = create((set) => ({
  count: 0,
  increment: () => set((s) => ({ count: s.count + 1 })),
}));
```

### useFrame Best Practices

```tsx
// GOOD: direct mutation in useFrame (no state, no re-render)
useFrame((state, delta) => {
  ref.current.position.x = Math.sin(state.clock.elapsedTime);
  ref.current.material.opacity = 0.5 + Math.sin(state.clock.elapsedTime) * 0.5;
});

// BAD: setting state in useFrame (60 re-renders per second!)
useFrame(() => {
  setPosition(new THREE.Vector3(...)); // NEVER do this
});

// Priority system for render order
useFrame(callback, priority); // lower = earlier, default 0
// Use negative priority for pre-render logic
// Use positive priority for post-render (e.g., post-processing)
```

### Suspense & Loading

```tsx
import { Suspense } from 'react';
import { Loader } from '@react-three/drei';

function App() {
  return (
    <>
      <Canvas>
        <Suspense fallback={<LoadingBox />}>
          <HeavyScene />
        </Suspense>
      </Canvas>
      <Loader /> {/* HTML loading overlay */}
    </>
  );
}

function LoadingBox() {
  return (
    <mesh>
      <boxGeometry />
      <meshBasicMaterial color="gray" wireframe />
    </mesh>
  );
}
```

---

## Physics with Rapier (@react-three/rapier)

### Setup

```tsx
import { Physics, RigidBody, CuboidCollider, BallCollider } from '@react-three/rapier';

function PhysicsScene() {
  return (
    <Physics gravity={[0, -9.81, 0]} debug={false} timeStep="vary">
      <Ground />
      <Player />
      <Obstacles />
    </Physics>
  );
}
```

### RigidBody Types

```tsx
// Dynamic - affected by forces and gravity
<RigidBody type="dynamic" mass={1} restitution={0.5} friction={0.7}>
  <mesh>
    <sphereGeometry args={[0.5]} />
    <meshStandardMaterial />
  </mesh>
</RigidBody>

// Fixed - immovable (walls, ground)
<RigidBody type="fixed">
  <mesh rotation-x={-Math.PI / 2}>
    <planeGeometry args={[100, 100]} />
    <meshStandardMaterial />
  </mesh>
</RigidBody>

// KinematicPosition - moved by code, not physics
<RigidBody type="kinematicPosition" ref={platformRef}>
  <mesh>
    <boxGeometry args={[4, 0.5, 4]} />
    <meshStandardMaterial />
  </mesh>
</RigidBody>

// KinematicVelocity - moved by velocity
<RigidBody type="kinematicVelocity" ref={ref}>
  <mesh>...</mesh>
</RigidBody>
```

### Collider Shapes

```tsx
// Auto colliders from geometry
<RigidBody colliders="cuboid">...</RigidBody>  // box
<RigidBody colliders="ball">...</RigidBody>     // sphere
<RigidBody colliders="hull">...</RigidBody>     // convex hull
<RigidBody colliders="trimesh">...</RigidBody>  // exact mesh (expensive!)
<RigidBody colliders={false}>                    // manual colliders
  <mesh>...</mesh>
  <CuboidCollider args={[0.5, 0.5, 0.5]} position={[0, 0, 0]} />
  <BallCollider args={[0.5]} position={[0, 1, 0]} />
  <CapsuleCollider args={[0.5, 1]} /> {/* halfHeight, radius */}
</RigidBody>

// Sensor colliders (trigger zones, no physical collision)
<CuboidCollider
  args={[2, 2, 2]}
  sensor
  onIntersectionEnter={(payload) => console.log('entered', payload.other)}
  onIntersectionExit={(payload) => console.log('exited', payload.other)}
/>
```

### Forces and Impulses

```tsx
function Player() {
  const rigidBodyRef = useRef(null);

  const jump = () => {
    rigidBodyRef.current?.applyImpulse({ x: 0, y: 5, z: 0 }, true);
  };

  const move = (direction: THREE.Vector3) => {
    rigidBodyRef.current?.applyForce(
      { x: direction.x * 10, y: 0, z: direction.z * 10 },
      true
    );
  };

  // Set velocity directly
  const setVelocity = () => {
    rigidBodyRef.current?.setLinvel({ x: 0, y: 10, z: 0 }, true);
  };

  // Set angular velocity
  const spin = () => {
    rigidBodyRef.current?.setAngvel({ x: 0, y: 5, z: 0 }, true);
  };

  // Teleport (kinematic)
  const teleport = () => {
    rigidBodyRef.current?.setTranslation({ x: 0, y: 10, z: 0 }, true);
    rigidBodyRef.current?.setRotation({ x: 0, y: 0, z: 0, w: 1 }, true);
  };

  return (
    <RigidBody ref={rigidBodyRef} type="dynamic" lockRotations>
      <mesh>
        <capsuleGeometry args={[0.5, 1]} />
        <meshStandardMaterial />
      </mesh>
    </RigidBody>
  );
}
```

### Collision Events

```tsx
<RigidBody
  onCollisionEnter={(payload) => {
    // payload.other - the other rigidbody
    // payload.manifold - contact info
    // payload.target - this rigidbody
    console.log('Collided with', payload.other.rigidBodyObject?.name);
    const contactForce = payload.totalForceMagnitude();
    if (contactForce > 10) playSound('impact');
  }}
  onCollisionExit={(payload) => {
    console.log('Separation from', payload.other.rigidBodyObject?.name);
  }}
  onContactForce={(payload) => {
    // Continuous contact force info
    const force = payload.totalForceMagnitude();
  }}
>
  <mesh name="player">...</mesh>
</RigidBody>
```

### Character Controller (Rapier)

```tsx
import { CapsuleCollider, RigidBody, useRapier } from '@react-three/rapier';

function CharacterController() {
  const rigidBody = useRef(null);
  const { rapier, world } = useRapier();

  useFrame((state, delta) => {
    if (!rigidBody.current) return;

    const velocity = rigidBody.current.linvel();
    const position = rigidBody.current.translation();

    // Ground check via raycast
    const ray = new rapier.Ray(
      { x: position.x, y: position.y, z: position.z },
      { x: 0, y: -1, z: 0 }
    );
    const hit = world.castRay(ray, 1.5, true);
    const grounded = hit && hit.timeOfImpact < 1.1;

    // Movement
    const moveSpeed = 5;
    const movement = { x: 0, y: velocity.y, z: 0 };

    if (keys.forward) movement.z -= moveSpeed;
    if (keys.backward) movement.z += moveSpeed;
    if (keys.left) movement.x -= moveSpeed;
    if (keys.right) movement.x += moveSpeed;

    // Jump
    if (keys.jump && grounded) {
      movement.y = 8;
    }

    rigidBody.current.setLinvel(movement, true);
  });

  return (
    <RigidBody
      ref={rigidBody}
      type="dynamic"
      colliders={false}
      lockRotations
      linearDamping={0.5}
    >
      <CapsuleCollider args={[0.75, 0.5]} position={[0, 1.25, 0]} />
      <mesh>
        <capsuleGeometry args={[0.5, 1.5]} />
        <meshStandardMaterial color="blue" />
      </mesh>
    </RigidBody>
  );
}
```

### Joint Constraints

```tsx
import { useSphericalJoint, useRevoluteJoint, usePrismaticJoint } from '@react-three/rapier';

function RopeSegment() {
  const joint = useSphericalJoint(bodyA, bodyB, [
    [0, -0.5, 0], // anchor on body A (local space)
    [0, 0.5, 0],  // anchor on body B (local space)
  ]);
  return <>{/* render segments */}</>;
}

function Door() {
  const joint = useRevoluteJoint(frameBody, doorBody, [
    [0.5, 0, 0],  // anchor on frame
    [-0.5, 0, 0], // anchor on door
    [0, 1, 0],    // axis of rotation
  ]);
  return <>{/* render door */}</>;
}
```

---

## State Management for R3F Games

### Zustand (Recommended)

```tsx
import { create } from 'zustand';

interface GameState {
  health: number;
  score: number;
  isPaused: boolean;
  inventory: string[];
  takeDamage: (amount: number) => void;
  addScore: (points: number) => void;
  togglePause: () => void;
  addItem: (item: string) => void;
}

const useGameStore = create<GameState>((set) => ({
  health: 100,
  score: 0,
  isPaused: false,
  inventory: [],
  takeDamage: (amount) => set((s) => ({ health: Math.max(0, s.health - amount) })),
  addScore: (points) => set((s) => ({ score: s.score + points })),
  togglePause: () => set((s) => ({ isPaused: !s.isPaused })),
  addItem: (item) => set((s) => ({ inventory: [...s.inventory, item] })),
}));

// In components - subscribe to specific slices (prevents unnecessary re-renders)
function HUD() {
  const health = useGameStore((s) => s.health);
  const score = useGameStore((s) => s.score);
  return <Html><div>HP: {health} | Score: {score}</div></Html>;
}

// In useFrame - direct access without subscription
useFrame(() => {
  const { health, isPaused } = useGameStore.getState();
  if (isPaused) return;
  // game logic...
});
```
