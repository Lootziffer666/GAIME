# TypeScript Best Practices for Game Development

Type-safe patterns for robust, maintainable game code. Covers generics, type guards, utility types, discriminated unions, and performance-conscious patterns.

---

## Core Type Patterns for Games

### Component Type Registry

```typescript
// Type-safe component system using a registry pattern
interface ComponentRegistry {
  transform: { position: THREE.Vector3; rotation: THREE.Quaternion; scale: THREE.Vector3 };
  velocity: { linear: THREE.Vector3; angular: THREE.Vector3 };
  health: { current: number; max: number; armor: number };
  render: { mesh: THREE.Mesh; visible: boolean; layer: number };
  collider: { shape: ColliderShape; isTrigger: boolean };
  ai: { behaviorTree: BTNode; target: Entity | null };
  inventory: { slots: InventorySlot[]; maxSlots: number };
  weapon: { config: WeaponConfig; ammo: number; cooldown: number };
}

type ComponentName = keyof ComponentRegistry;


// Type-safe entity queries
class TypedWorld {
  private components = new Map<ComponentName, Map<Entity, any>>();

  getComponent<K extends ComponentName>(entity: Entity, name: K): ComponentRegistry[K] | undefined {
    return this.components.get(name)?.get(entity) as ComponentRegistry[K] | undefined;
  }

  addComponent<K extends ComponentName>(entity: Entity, name: K, data: ComponentRegistry[K]): void {
    if (!this.components.has(name)) this.components.set(name, new Map());
    this.components.get(name)!.set(entity, data);
  }

  // Query with full type inference
  query<K extends ComponentName>(...names: K[]): Array<{ entity: Entity } & Pick<ComponentRegistry, K>> {
    // Implementation returns typed results
    return [] as any;
  }
}

// Usage - fully type-safe
const world = new TypedWorld();
const results = world.query('transform', 'velocity');
// results[0].transform.position is THREE.Vector3 - typed!
// results[0].velocity.linear is THREE.Vector3 - typed!
```

### Discriminated Unions for Game Events

```typescript
// Type-safe event system using discriminated unions
type GameEvent =
  | { type: 'damage'; target: Entity; amount: number; source: Entity; damageType: DamageType }
  | { type: 'heal'; target: Entity; amount: number }
  | { type: 'death'; entity: Entity; killer: Entity | null }
  | { type: 'spawn'; entity: Entity; position: THREE.Vector3 }
  | { type: 'levelUp'; entity: Entity; newLevel: number }
  | { type: 'itemPickup'; entity: Entity; item: Item; quantity: number }
  | { type: 'weaponFire'; entity: Entity; weapon: WeaponConfig; direction: THREE.Vector3 }
  | { type: 'collision'; entityA: Entity; entityB: Entity; point: THREE.Vector3; force: number };

type DamageType = 'physical' | 'fire' | 'ice' | 'electric' | 'poison';

// Type-safe event handler
type EventHandler<T extends GameEvent['type']> = (
  event: Extract<GameEvent, { type: T }>
) => void;

class EventBus {
  private handlers = new Map<string, Set<Function>>();

  on<T extends GameEvent['type']>(type: T, handler: EventHandler<T>): () => void {
    if (!this.handlers.has(type)) this.handlers.set(type, new Set());
    this.handlers.get(type)!.add(handler);
    return () => this.handlers.get(type)?.delete(handler);
  }

  emit(event: GameEvent): void {
    const handlers = this.handlers.get(event.type);
    if (handlers) {
      for (const handler of handlers) {
        handler(event);
      }
    }
  }
}

// Usage - TypeScript infers event shape
const bus = new EventBus();
bus.on('damage', (event) => {
  // event.target, event.amount, event.source, event.damageType - all typed!
  console.log(`${event.target} took ${event.amount} ${event.damageType} damage`);
});
```

### State Machine Types

```typescript
// Type-safe state machine with transitions
type PlayerState = 'idle' | 'running' | 'jumping' | 'falling' | 'attacking' | 'dead';

// Define valid transitions
type StateTransitions = {
  idle: 'running' | 'jumping' | 'attacking' | 'dead';
  running: 'idle' | 'jumping' | 'attacking' | 'dead';
  jumping: 'falling' | 'dead';
  falling: 'idle' | 'dead';
  attacking: 'idle' | 'dead';
  dead: 'idle'; // respawn
};

class TypedStateMachine<S extends string, T extends Record<S, S>> {
  private _state: S;
  private handlers = new Map<S, {
    enter?: () => void;
    update?: (dt: number) => void;
    exit?: () => void;
  }>();

  constructor(initial: S) {
    this._state = initial;
  }

  get state(): S { return this._state; }

  registerState(state: S, handlers: {
    enter?: () => void;
    update?: (dt: number) => void;
    exit?: () => void;
  }): void {
    this.handlers.set(state, handlers);
  }

  // Only allows valid transitions
  transition<From extends S>(
    from: From,
    to: T[From]
  ): boolean {
    if (this._state !== from) return false;
    this.handlers.get(this._state)?.exit?.();
    this._state = to as S;
    this.handlers.get(this._state)?.enter?.();
    return true;
  }

  update(dt: number): void {
    this.handlers.get(this._state)?.update?.(dt);
  }
}

// Usage
const fsm = new TypedStateMachine<PlayerState, StateTransitions>('idle');
fsm.transition('idle', 'running');    // OK
// fsm.transition('idle', 'falling'); // TypeScript ERROR - invalid transition
```

---

## Generic Patterns

### Object Pool with Type Safety

```typescript
class TypedPool<T> {
  private available: T[] = [];
  private active = new Set<T>();
  private factory: () => T;
  private reset: (obj: T) => void;

  constructor(config: {
    factory: () => T;
    reset: (obj: T) => void;
    initialSize?: number;
  }) {
    this.factory = config.factory;
    this.reset = config.reset;
    const size = config.initialSize ?? 20;
    for (let i = 0; i < size; i++) {
      this.available.push(this.factory());
    }
  }

  acquire(): T {
    const obj = this.available.pop() ?? this.factory();
    this.active.add(obj);
    return obj;
  }

  release(obj: T): void {
    if (!this.active.has(obj)) return;
    this.active.delete(obj);
    this.reset(obj);
    this.available.push(obj);
  }

  releaseAll(): void {
    for (const obj of this.active) {
      this.reset(obj);
      this.available.push(obj);
    }
    this.active.clear();
  }

  get activeCount(): number { return this.active.size; }
  get availableCount(): number { return this.available.length; }
}

// Usage
const bulletPool = new TypedPool({
  factory: () => ({
    mesh: new THREE.Mesh(bulletGeo, bulletMat),
    velocity: new THREE.Vector3(),
    lifetime: 0,
    damage: 10,
  }),
  reset: (bullet) => {
    bullet.mesh.visible = false;
    bullet.velocity.set(0, 0, 0);
    bullet.lifetime = 0;
  },
  initialSize: 100,
});
```

### Typed Asset Loader

```typescript
// Type-safe asset management
interface AssetManifest {
  textures: Record<string, string>;  // name -> path
  models: Record<string, string>;
  sounds: Record<string, string>;
  animations: Record<string, string>;
}

type AssetType = keyof AssetManifest;

interface LoadedAssets<M extends AssetManifest> {
  textures: { [K in keyof M['textures']]: THREE.Texture };
  models: { [K in keyof M['models']]: THREE.Group };
  sounds: { [K in keyof M['sounds']]: AudioBuffer };
  animations: { [K in keyof M['animations']]: THREE.AnimationClip };
}

class AssetManager<M extends AssetManifest> {
  private assets: Partial<LoadedAssets<M>> = {};

  async loadAll(manifest: M): Promise<LoadedAssets<M>> {
    // Load all assets with proper typing
    const textures = await this.loadTextures(manifest.textures);
    const models = await this.loadModels(manifest.models);
    // ... load others

    this.assets = { textures, models } as any;
    return this.assets as LoadedAssets<M>;
  }

  getTexture<K extends keyof M['textures']>(name: K): THREE.Texture {
    return (this.assets.textures as any)[name];
  }

  getModel<K extends keyof M['models']>(name: K): THREE.Group {
    return (this.assets.models as any)[name].clone();
  }

  private async loadTextures(manifest: Record<string, string>) {
    const loader = new THREE.TextureLoader();
    const results: Record<string, THREE.Texture> = {};
    for (const [name, path] of Object.entries(manifest)) {
      results[name] = await loader.loadAsync(path);
    }
    return results;
  }

  private async loadModels(manifest: Record<string, string>) {
    const loader = new GLTFLoader();
    const results: Record<string, THREE.Group> = {};
    for (const [name, path] of Object.entries(manifest)) {
      const gltf = await loader.loadAsync(path);
      results[name] = gltf.scene;
    }
    return results;
  }
}

// Usage with full type inference
const manifest = {
  textures: { grass: '/tex/grass.jpg', stone: '/tex/stone.jpg' },
  models: { tree: '/models/tree.glb', rock: '/models/rock.glb' },
  sounds: { shoot: '/audio/shoot.mp3', hit: '/audio/hit.mp3' },
  animations: {},
} as const satisfies AssetManifest;

const assetManager = new AssetManager<typeof manifest>();
const assets = await assetManager.loadAll(manifest);
const grassTex = assetManager.getTexture('grass'); // typed!
```

---

## Performance-Conscious Patterns

### Avoiding Allocations in Hot Paths

```typescript
// BAD: allocates new vectors every frame
function updateBad(entities: Entity[], dt: number) {
  for (const entity of entities) {
    const direction = new THREE.Vector3(); // GC pressure!
    direction.subVectors(entity.target, entity.position).normalize();
    entity.position.add(direction.multiplyScalar(entity.speed * dt));
  }
}

// GOOD: reuse pre-allocated vectors
const _direction = new THREE.Vector3();
const _tempVec = new THREE.Vector3();
const _tempQuat = new THREE.Quaternion();
const _tempMatrix = new THREE.Matrix4();

function updateGood(entities: Entity[], dt: number) {
  for (const entity of entities) {
    _direction.subVectors(entity.target, entity.position).normalize();
    entity.position.addScaledVector(_direction, entity.speed * dt);
  }
}

// Pattern: module-level temp variables with underscore prefix
// This signals "reusable temp, don't store references to this"
```

### Typed Ring Buffer (for history/replay)

```typescript
class RingBuffer<T> {
  private buffer: T[];
  private head = 0;
  private _size = 0;
  private capacity: number;

  constructor(capacity: number, factory: () => T) {
    this.capacity = capacity;
    this.buffer = Array.from({ length: capacity }, factory);
  }

  push(item: T): void {
    this.buffer[this.head] = item;
    this.head = (this.head + 1) % this.capacity;
    if (this._size < this.capacity) this._size++;
  }

  get(index: number): T | undefined {
    if (index >= this._size) return undefined;
    const i = (this.head - this._size + index + this.capacity) % this.capacity;
    return this.buffer[i];
  }

  get size(): number { return this._size; }
  get latest(): T | undefined { return this.get(this._size - 1); }
}

// Usage: input history for replays
interface InputSnapshot {
  timestamp: number;
  keys: Set<string>;
  mouseX: number;
  mouseY: number;
}

const inputHistory = new RingBuffer<InputSnapshot>(600, () => ({
  timestamp: 0, keys: new Set(), mouseX: 0, mouseY: 0
}));
```

### Const Enums for Zero-Cost Abstractions

```typescript
// const enums are inlined at compile time - zero runtime cost
const enum Layer {
  Default = 0,
  Player = 1,
  Enemy = 2,
  Projectile = 3,
  Terrain = 4,
  UI = 5,
  Trigger = 6,
}

const enum CollisionGroup {
  None     = 0,
  Player   = 1 << 0,
  Enemy    = 1 << 1,
  Bullet   = 1 << 2,
  World    = 1 << 3,
  Pickup   = 1 << 4,
  Trigger  = 1 << 5,
  All      = 0xFFFF,
}

// Collision mask example
const playerCollisionMask = CollisionGroup.Enemy | CollisionGroup.World | CollisionGroup.Pickup;

const enum GamePhase {
  Loading,
  Menu,
  Playing,
  Paused,
  GameOver,
}
```

### Branded Types for ID Safety

```typescript
// Prevent mixing up different ID types
type Brand<T, B> = T & { __brand: B };

type EntityId = Brand<number, 'EntityId'>;
type PlayerId = Brand<string, 'PlayerId'>;
type ItemId = Brand<string, 'ItemId'>;
type ChunkId = Brand<string, 'ChunkId'>;

// Constructor functions
function entityId(id: number): EntityId { return id as EntityId; }
function playerId(id: string): PlayerId { return id as PlayerId; }
function itemId(id: string): ItemId { return id as ItemId; }

// TypeScript prevents mixing IDs
function damage(target: EntityId, amount: number): void { /* ... */ }
function giveItem(player: PlayerId, item: ItemId): void { /* ... */ }

const enemy = entityId(42);
const player = playerId('player-1');
const sword = itemId('iron-sword');

damage(enemy, 10);     // OK
// damage(player, 10); // TypeScript ERROR - PlayerId is not EntityId
giveItem(player, sword); // OK
// giveItem(sword, player); // TypeScript ERROR - types swapped
```

### Utility Types for Game Config

```typescript
// Deep readonly for immutable configs
type DeepReadonly<T> = {
  readonly [K in keyof T]: T[K] extends object ? DeepReadonly<T[K]> : T[K];
};

// Game balance configuration (immutable at runtime)
const GAME_CONFIG = {
  player: {
    maxHealth: 100,
    moveSpeed: 5,
    jumpForce: 8,
    gravity: -25,
  },
  weapons: {
    pistol: { damage: 15, fireRate: 5, magazineSize: 12 },
    rifle: { damage: 25, fireRate: 10, magazineSize: 30 },
    shotgun: { damage: 80, fireRate: 1.5, magazineSize: 8 },
  },
  enemies: {
    grunt: { health: 50, speed: 3, damage: 10 },
    heavy: { health: 200, speed: 1.5, damage: 30 },
    sniper: { health: 30, speed: 2, damage: 50 },
  },
} as const satisfies DeepReadonly<GameConfig>;

// Type is deeply readonly - cannot be mutated
// GAME_CONFIG.player.maxHealth = 200; // TypeScript ERROR

// Extract types from config
type WeaponName = keyof typeof GAME_CONFIG.weapons;
type EnemyName = keyof typeof GAME_CONFIG.enemies;
```

---

## R3F-Specific TypeScript Patterns

### Typed Refs and ForwardRef

```typescript
import { forwardRef, useRef, useImperativeHandle } from 'react';
import { useFrame } from '@react-three/fiber';

// Expose typed API from a game component
interface PlayerAPI {
  takeDamage(amount: number): void;
  heal(amount: number): void;
  teleport(position: THREE.Vector3): void;
  getPosition(): THREE.Vector3;
}

const Player = forwardRef<PlayerAPI, { initialPosition?: [number, number, number] }>(
  ({ initialPosition = [0, 0, 0] }, ref) => {
    const meshRef = useRef<THREE.Mesh>(null!);
    const health = useRef(100);

    useImperativeHandle(ref, () => ({
      takeDamage(amount: number) {
        health.current = Math.max(0, health.current - amount);
      },
      heal(amount: number) {
        health.current = Math.min(100, health.current + amount);
      },
      teleport(position: THREE.Vector3) {
        meshRef.current.position.copy(position);
      },
      getPosition() {
        return meshRef.current.position.clone();
      },
    }));

    return (
      <mesh ref={meshRef} position={initialPosition}>
        <capsuleGeometry args={[0.5, 1]} />
        <meshStandardMaterial color="blue" />
      </mesh>
    );
  }
);

// Usage
function Game() {
  const playerRef = useRef<PlayerAPI>(null!);

  const handleHit = () => {
    playerRef.current.takeDamage(10); // fully typed
  };

  return <Player ref={playerRef} />;
}
```

### Custom Hook Types

```typescript
// Typed custom hook for game input
interface GameInput {
  movement: THREE.Vector2;
  look: THREE.Vector2;
  actions: {
    jump: boolean;
    fire: boolean;
    reload: boolean;
    interact: boolean;
    sprint: boolean;
  };
}

function useGameInput(): GameInput {
  const input = useRef<GameInput>({
    movement: new THREE.Vector2(),
    look: new THREE.Vector2(),
    actions: { jump: false, fire: false, reload: false, interact: false, sprint: false },
  });

  // ... setup listeners

  return input.current;
}

// Typed useFrame with priority
function useFixedUpdate(callback: (dt: number) => void, priority = 0) {
  const accumulator = useRef(0);
  const FIXED_DT = 1 / 60;

  useFrame((_, delta) => {
    accumulator.current += Math.min(delta, 0.1);
    while (accumulator.current >= FIXED_DT) {
      callback(FIXED_DT);
      accumulator.current -= FIXED_DT;
    }
  }, priority);
}
```
