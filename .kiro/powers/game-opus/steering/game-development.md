# Game Development

Complete reference for game architecture patterns, game loops, ECS, state machines, input handling, AI, FPS mechanics, world building, and building/crafting mechanics.

---

## Game Architecture Patterns

### Game Loop

```typescript
class GameLoop {
  private lastTime = 0;
  private accumulator = 0;
  private readonly fixedTimeStep = 1 / 60; // 60 Hz physics
  private readonly maxDeltaTime = 1 / 30; // cap to prevent spiral
  private running = false;

  start(): void {
    this.running = true;
    this.lastTime = performance.now();
    requestAnimationFrame(this.tick.bind(this));
  }

  stop(): void {
    this.running = false;
  }


  private tick(currentTime: number): void {
    if (!this.running) return;
    requestAnimationFrame(this.tick.bind(this));

    const rawDelta = (currentTime - this.lastTime) / 1000;
    const deltaTime = Math.min(rawDelta, this.maxDeltaTime);
    this.lastTime = currentTime;

    // Fixed timestep for physics
    this.accumulator += deltaTime;
    while (this.accumulator >= this.fixedTimeStep) {
      this.fixedUpdate(this.fixedTimeStep);
      this.accumulator -= this.fixedTimeStep;
    }

    // Variable timestep for rendering
    const alpha = this.accumulator / this.fixedTimeStep;
    this.update(deltaTime);
    this.render(alpha); // alpha for interpolation
  }

  private fixedUpdate(dt: number): void {
    // Physics, collision detection, game logic
  }

  private update(dt: number): void {
    // Animations, input, cameras, non-physics updates
  }

  private render(alpha: number): void {
    // Interpolate between physics states for smooth rendering
    // alpha = how far between last and next physics step
  }
}
```


### Entity Component System (ECS)

```typescript
// Component - pure data, no logic
interface TransformComponent {
  position: THREE.Vector3;
  rotation: THREE.Quaternion;
  scale: THREE.Vector3;
}

interface VelocityComponent {
  linear: THREE.Vector3;
  angular: THREE.Vector3;
}

interface HealthComponent {
  current: number;
  max: number;
  invulnerable: boolean;
  invulnerabilityTimer: number;
}

interface RenderComponent {
  mesh: THREE.Mesh;
  visible: boolean;
}

// Entity - just an ID with component map
type Entity = number;
type ComponentType = string;

class World {
  private nextId = 0;
  private components = new Map<ComponentType, Map<Entity, any>>();
  private systems: System[] = [];

  createEntity(): Entity {
    return this.nextId++;
  }

  addComponent<T>(entity: Entity, type: ComponentType, data: T): void {
    if (!this.components.has(type)) {
      this.components.set(type, new Map());
    }
    this.components.get(type)!.set(entity, data);
  }

  getComponent<T>(entity: Entity, type: ComponentType): T | undefined {
    return this.components.get(type)?.get(entity);
  }

  removeComponent(entity: Entity, type: ComponentType): void {
    this.components.get(type)?.delete(entity);
  }

  destroyEntity(entity: Entity): void {
    for (const store of this.components.values()) {
      store.delete(entity);
    }
  }

  query(...types: ComponentType[]): Entity[] {
    const entities: Entity[] = [];
    const firstStore = this.components.get(types[0]);
    if (!firstStore) return entities;

    for (const entity of firstStore.keys()) {
      if (types.every(type => this.components.get(type)?.has(entity))) {
        entities.push(entity);
      }
    }
    return entities;
  }

  addSystem(system: System): void {
    this.systems.push(system);
    this.systems.sort((a, b) => a.priority - b.priority);
  }

  update(dt: number): void {
    for (const system of this.systems) {
      system.update(this, dt);
    }
  }
}

// System - logic that operates on entities with specific components
interface System {
  priority: number;
  update(world: World, dt: number): void;
}

class MovementSystem implements System {
  priority = 10;

  update(world: World, dt: number): void {
    const entities = world.query('transform', 'velocity');
    for (const entity of entities) {
      const transform = world.getComponent<TransformComponent>(entity, 'transform')!;
      const velocity = world.getComponent<VelocityComponent>(entity, 'velocity')!;

      transform.position.addScaledVector(velocity.linear, dt);
      // Apply angular velocity as quaternion rotation
    }
  }
}

// Usage
const world = new World();
world.addSystem(new MovementSystem());

const player = world.createEntity();
world.addComponent(player, 'transform', {
  position: new THREE.Vector3(0, 0, 0),
  rotation: new THREE.Quaternion(),
  scale: new THREE.Vector3(1, 1, 1),
});
world.addComponent(player, 'velocity', {
  linear: new THREE.Vector3(0, 0, 0),
  angular: new THREE.Vector3(0, 0, 0),
});
```


### State Machines

```typescript
// Finite State Machine for game entities
interface State<T> {
  name: string;
  enter(entity: T): void;
  update(entity: T, dt: number): void;
  exit(entity: T): void;
}

class StateMachine<T> {
  private currentState: State<T> | null = null;
  private states = new Map<string, State<T>>();
  private entity: T;

  constructor(entity: T) {
    this.entity = entity;
  }

  addState(state: State<T>): void {
    this.states.set(state.name, state);
  }

  setState(name: string): void {
    const nextState = this.states.get(name);
    if (!nextState || nextState === this.currentState) return;

    this.currentState?.exit(this.entity);
    this.currentState = nextState;
    this.currentState.enter(this.entity);
  }

  update(dt: number): void {
    this.currentState?.update(this.entity, dt);
  }

  get current(): string | null {
    return this.currentState?.name ?? null;
  }
}

// Example: Player states
class IdleState implements State<Player> {
  name = 'idle';
  enter(player: Player) { player.playAnimation('idle'); }
  update(player: Player, dt: number) {
    if (player.input.movement.length() > 0.1) {
      player.stateMachine.setState('running');
    }
    if (player.input.jump && player.isGrounded) {
      player.stateMachine.setState('jumping');
    }
  }
  exit(player: Player) {}
}

class RunningState implements State<Player> {
  name = 'running';
  enter(player: Player) { player.playAnimation('run'); }
  update(player: Player, dt: number) {
    player.move(dt);
    if (player.input.movement.length() < 0.1) {
      player.stateMachine.setState('idle');
    }
    if (player.input.jump && player.isGrounded) {
      player.stateMachine.setState('jumping');
    }
  }
  exit(player: Player) {}
}

class JumpingState implements State<Player> {
  name = 'jumping';
  enter(player: Player) {
    player.playAnimation('jump');
    player.velocity.y = player.jumpForce;
  }
  update(player: Player, dt: number) {
    player.move(dt);
    if (player.isGrounded && player.velocity.y <= 0) {
      player.stateMachine.setState('idle');
    }
  }
  exit(player: Player) {}
}
```


### Input System

```typescript
class InputManager {
  private keys = new Map<string, boolean>();
  private keysJustPressed = new Set<string>();
  private keysJustReleased = new Set<string>();
  private mouse = { x: 0, y: 0, dx: 0, dy: 0 };
  private mouseButtons = new Map<number, boolean>();
  private gamepad: Gamepad | null = null;

  constructor(canvas: HTMLCanvasElement) {
    window.addEventListener('keydown', (e) => {
      if (!this.keys.get(e.code)) {
        this.keysJustPressed.add(e.code);
      }
      this.keys.set(e.code, true);
    });

    window.addEventListener('keyup', (e) => {
      this.keys.set(e.code, false);
      this.keysJustReleased.add(e.code);
    });

    canvas.addEventListener('mousemove', (e) => {
      this.mouse.dx = e.movementX;
      this.mouse.dy = e.movementY;
      this.mouse.x = e.clientX;
      this.mouse.y = e.clientY;
    });

    canvas.addEventListener('mousedown', (e) => {
      this.mouseButtons.set(e.button, true);
    });

    canvas.addEventListener('mouseup', (e) => {
      this.mouseButtons.set(e.button, false);
    });

    // Pointer lock for FPS
    canvas.addEventListener('click', () => {
      canvas.requestPointerLock();
    });
  }

  isKeyDown(code: string): boolean {
    return this.keys.get(code) ?? false;
  }

  isKeyJustPressed(code: string): boolean {
    return this.keysJustPressed.has(code);
  }

  isMouseButtonDown(button: number): boolean {
    return this.mouseButtons.get(button) ?? false;
  }

  getMouseDelta(): { x: number; y: number } {
    return { x: this.mouse.dx, y: this.mouse.dy };
  }

  getMovementVector(): THREE.Vector2 {
    const movement = new THREE.Vector2();
    if (this.isKeyDown('KeyW') || this.isKeyDown('ArrowUp')) movement.y -= 1;
    if (this.isKeyDown('KeyS') || this.isKeyDown('ArrowDown')) movement.y += 1;
    if (this.isKeyDown('KeyA') || this.isKeyDown('ArrowLeft')) movement.x -= 1;
    if (this.isKeyDown('KeyD') || this.isKeyDown('ArrowRight')) movement.x += 1;
    if (movement.length() > 0) movement.normalize();
    return movement;
  }

  // Call at end of each frame
  endFrame(): void {
    this.keysJustPressed.clear();
    this.keysJustReleased.clear();
    this.mouse.dx = 0;
    this.mouse.dy = 0;
  }
}
```

### R3F Input Hooks

```tsx
// Custom hook for keyboard input in R3F
function useKeyboard() {
  const keys = useRef<Set<string>>(new Set());

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => keys.current.add(e.code);
    const onKeyUp = (e: KeyboardEvent) => keys.current.delete(e.code);
    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('keyup', onKeyUp);
    return () => {
      window.removeEventListener('keydown', onKeyDown);
      window.removeEventListener('keyup', onKeyUp);
    };
  }, []);

  return keys;
}

// Usage with useFrame
function PlayerController() {
  const keys = useKeyboard();
  const ref = useRef<THREE.Mesh>(null!);

  useFrame((_, delta) => {
    const speed = 5 * delta;
    if (keys.current.has('KeyW')) ref.current.position.z -= speed;
    if (keys.current.has('KeyS')) ref.current.position.z += speed;
    if (keys.current.has('KeyA')) ref.current.position.x -= speed;
    if (keys.current.has('KeyD')) ref.current.position.x += speed;
  });

  return <mesh ref={ref}>...</mesh>;
}
```

---

## FPS Game Mechanics

### First-Person Camera Controller

```typescript
class FPSController {
  private camera: THREE.PerspectiveCamera;
  private velocity = new THREE.Vector3();
  private direction = new THREE.Vector3();
  private euler = new THREE.Euler(0, 0, 0, 'YXZ');
  private moveSpeed = 10;
  private jumpForce = 8;
  private gravity = -25;
  private isGrounded = false;
  private sensitivity = 0.002;

  constructor(camera: THREE.PerspectiveCamera) {
    this.camera = camera;
  }

  handleMouseMove(dx: number, dy: number): void {
    this.euler.setFromQuaternion(this.camera.quaternion);
    this.euler.y -= dx * this.sensitivity;
    this.euler.x -= dy * this.sensitivity;
    this.euler.x = Math.max(-Math.PI / 2, Math.min(Math.PI / 2, this.euler.x));
    this.camera.quaternion.setFromEuler(this.euler);
  }

  update(input: InputManager, dt: number): void {
    // Horizontal movement
    const movement = input.getMovementVector();
    this.direction.set(movement.x, 0, movement.y);

    // Transform movement to camera space (ignore pitch)
    const cameraForward = new THREE.Vector3();
    this.camera.getWorldDirection(cameraForward);
    cameraForward.y = 0;
    cameraForward.normalize();

    const cameraRight = new THREE.Vector3();
    cameraRight.crossVectors(cameraForward, new THREE.Vector3(0, 1, 0));

    const moveDir = new THREE.Vector3();
    moveDir.addScaledVector(cameraRight, this.direction.x);
    moveDir.addScaledVector(cameraForward, -this.direction.z);

    // Apply horizontal velocity
    this.velocity.x = moveDir.x * this.moveSpeed;
    this.velocity.z = moveDir.z * this.moveSpeed;

    // Gravity and jump
    if (this.isGrounded) {
      this.velocity.y = 0;
      if (input.isKeyJustPressed('Space')) {
        this.velocity.y = this.jumpForce;
        this.isGrounded = false;
      }
    } else {
      this.velocity.y += this.gravity * dt;
    }

    // Apply velocity
    this.camera.position.addScaledVector(this.velocity, dt);

    // Ground check (simplified)
    if (this.camera.position.y <= 1.7) {
      this.camera.position.y = 1.7;
      this.isGrounded = true;
    }
  }
}
```


### Weapon System

```typescript
interface WeaponConfig {
  name: string;
  damage: number;
  fireRate: number;       // shots per second
  reloadTime: number;     // seconds
  magazineSize: number;
  maxAmmo: number;
  spread: number;         // radians
  range: number;
  recoil: number;
  isAutomatic: boolean;
}

class Weapon {
  config: WeaponConfig;
  currentAmmo: number;
  totalAmmo: number;
  private lastFireTime = 0;
  private isReloading = false;
  private reloadTimer = 0;

  constructor(config: WeaponConfig) {
    this.config = config;
    this.currentAmmo = config.magazineSize;
    this.totalAmmo = config.maxAmmo;
  }

  canFire(time: number): boolean {
    if (this.isReloading) return false;
    if (this.currentAmmo <= 0) return false;
    const timeBetweenShots = 1 / this.config.fireRate;
    return (time - this.lastFireTime) >= timeBetweenShots;
  }

  fire(time: number, origin: THREE.Vector3, direction: THREE.Vector3): THREE.Vector3 {
    if (!this.canFire(time)) return direction;

    this.currentAmmo--;
    this.lastFireTime = time;

    // Apply spread
    const spread = this.config.spread;
    const spreadDir = direction.clone();
    spreadDir.x += (Math.random() - 0.5) * spread;
    spreadDir.y += (Math.random() - 0.5) * spread;
    spreadDir.z += (Math.random() - 0.5) * spread;
    spreadDir.normalize();

    return spreadDir;
  }

  reload(): void {
    if (this.isReloading || this.currentAmmo === this.config.magazineSize) return;
    if (this.totalAmmo <= 0) return;
    this.isReloading = true;
    this.reloadTimer = this.config.reloadTime;
  }

  update(dt: number): void {
    if (this.isReloading) {
      this.reloadTimer -= dt;
      if (this.reloadTimer <= 0) {
        const needed = this.config.magazineSize - this.currentAmmo;
        const available = Math.min(needed, this.totalAmmo);
        this.currentAmmo += available;
        this.totalAmmo -= available;
        this.isReloading = false;
      }
    }
  }
}
```

### Spatial Partitioning

```typescript
// Simple grid-based spatial hash
class SpatialHash<T extends { position: THREE.Vector3 }> {
  private cellSize: number;
  private cells = new Map<string, Set<T>>();

  constructor(cellSize: number) {
    this.cellSize = cellSize;
  }

  private hash(x: number, y: number, z: number): string {
    const cx = Math.floor(x / this.cellSize);
    const cy = Math.floor(y / this.cellSize);
    const cz = Math.floor(z / this.cellSize);
    return `${cx},${cy},${cz}`;
  }

  insert(entity: T): void {
    const key = this.hash(
      entity.position.x, entity.position.y, entity.position.z
    );
    if (!this.cells.has(key)) this.cells.set(key, new Set());
    this.cells.get(key)!.add(entity);
  }

  remove(entity: T): void {
    const key = this.hash(
      entity.position.x, entity.position.y, entity.position.z
    );
    this.cells.get(key)?.delete(entity);
  }

  query(center: THREE.Vector3, radius: number): T[] {
    const results: T[] = [];
    const minCell = Math.floor(-radius / this.cellSize);
    const maxCell = Math.ceil(radius / this.cellSize);
    const cx = Math.floor(center.x / this.cellSize);
    const cy = Math.floor(center.y / this.cellSize);
    const cz = Math.floor(center.z / this.cellSize);

    for (let x = minCell; x <= maxCell; x++) {
      for (let y = minCell; y <= maxCell; y++) {
        for (let z = minCell; z <= maxCell; z++) {
          const key = `${cx + x},${cy + y},${cz + z}`;
          const cell = this.cells.get(key);
          if (cell) {
            for (const entity of cell) {
              if (entity.position.distanceTo(center) <= radius) {
                results.push(entity);
              }
            }
          }
        }
      }
    }
    return results;
  }

  clear(): void {
    this.cells.clear();
  }
}
```

---

## Game AI Patterns

### Behavior Trees

```typescript
type BTStatus = 'success' | 'failure' | 'running';

abstract class BTNode {
  abstract tick(entity: any, dt: number): BTStatus;
}

class Sequence extends BTNode {
  private children: BTNode[];
  private currentIndex = 0;

  constructor(children: BTNode[]) {
    super();
    this.children = children;
  }

  tick(entity: any, dt: number): BTStatus {
    while (this.currentIndex < this.children.length) {
      const status = this.children[this.currentIndex].tick(entity, dt);
      if (status === 'failure') {
        this.currentIndex = 0;
        return 'failure';
      }
      if (status === 'running') return 'running';
      this.currentIndex++;
    }
    this.currentIndex = 0;
    return 'success';
  }
}

class Selector extends BTNode {
  private children: BTNode[];

  constructor(children: BTNode[]) {
    super();
    this.children = children;
  }

  tick(entity: any, dt: number): BTStatus {
    for (const child of this.children) {
      const status = child.tick(entity, dt);
      if (status === 'success') return 'success';
      if (status === 'running') return 'running';
    }
    return 'failure';
  }
}

// Condition nodes
class IsPlayerInRange extends BTNode {
  constructor(private range: number) { super(); }
  tick(entity: any): BTStatus {
    const dist = entity.position.distanceTo(entity.target.position);
    return dist <= this.range ? 'success' : 'failure';
  }
}

// Action nodes
class MoveToTarget extends BTNode {
  tick(entity: any, dt: number): BTStatus {
    const dir = entity.target.position.clone().sub(entity.position).normalize();
    entity.position.addScaledVector(dir, entity.speed * dt);
    const dist = entity.position.distanceTo(entity.target.position);
    return dist < 1 ? 'success' : 'running';
  }
}

class Attack extends BTNode {
  tick(entity: any, dt: number): BTStatus {
    entity.target.takeDamage(entity.damage);
    return 'success';
  }
}

// Build tree
const enemyAI = new Selector([
  new Sequence([
    new IsPlayerInRange(2),
    new Attack(),
  ]),
  new Sequence([
    new IsPlayerInRange(20),
    new MoveToTarget(),
  ]),
]);
```


### Pathfinding (A*)

```typescript
interface NavNode {
  x: number;
  z: number;
  walkable: boolean;
  g: number;  // cost from start
  h: number;  // heuristic to end
  f: number;  // g + h
  parent: NavNode | null;
}

class NavGrid {
  private grid: NavNode[][] = [];
  private width: number;
  private height: number;
  private cellSize: number;

  constructor(width: number, height: number, cellSize: number) {
    this.width = width;
    this.height = height;
    this.cellSize = cellSize;
    this.initGrid();
  }

  private initGrid(): void {
    for (let x = 0; x < this.width; x++) {
      this.grid[x] = [];
      for (let z = 0; z < this.height; z++) {
        this.grid[x][z] = { x, z, walkable: true, g: 0, h: 0, f: 0, parent: null };
      }
    }
  }

  findPath(startX: number, startZ: number, endX: number, endZ: number): THREE.Vector3[] {
    const start = this.grid[startX][startZ];
    const end = this.grid[endX][endZ];

    const open: NavNode[] = [start];
    const closed = new Set<NavNode>();

    start.g = 0;
    start.h = this.heuristic(start, end);
    start.f = start.h;

    while (open.length > 0) {
      open.sort((a, b) => a.f - b.f);
      const current = open.shift()!;

      if (current === end) return this.reconstructPath(end);
      closed.add(current);

      for (const neighbor of this.getNeighbors(current)) {
        if (closed.has(neighbor) || !neighbor.walkable) continue;

        const tentativeG = current.g + this.distance(current, neighbor);
        if (tentativeG < neighbor.g || !open.includes(neighbor)) {
          neighbor.g = tentativeG;
          neighbor.h = this.heuristic(neighbor, end);
          neighbor.f = neighbor.g + neighbor.h;
          neighbor.parent = current;
          if (!open.includes(neighbor)) open.push(neighbor);
        }
      }
    }
    return []; // no path found
  }

  private heuristic(a: NavNode, b: NavNode): number {
    return Math.abs(a.x - b.x) + Math.abs(a.z - b.z); // Manhattan
  }

  private distance(a: NavNode, b: NavNode): number {
    const dx = a.x - b.x;
    const dz = a.z - b.z;
    return Math.sqrt(dx * dx + dz * dz);
  }

  private getNeighbors(node: NavNode): NavNode[] {
    const neighbors: NavNode[] = [];
    const dirs = [[-1,0],[1,0],[0,-1],[0,1],[-1,-1],[-1,1],[1,-1],[1,1]];
    for (const [dx, dz] of dirs) {
      const nx = node.x + dx;
      const nz = node.z + dz;
      if (nx >= 0 && nx < this.width && nz >= 0 && nz < this.height) {
        neighbors.push(this.grid[nx][nz]);
      }
    }
    return neighbors;
  }

  private reconstructPath(end: NavNode): THREE.Vector3[] {
    const path: THREE.Vector3[] = [];
    let current: NavNode | null = end;
    while (current) {
      path.unshift(new THREE.Vector3(
        current.x * this.cellSize,
        0,
        current.z * this.cellSize
      ));
      current = current.parent;
    }
    return path;
  }
}
```

---

## Building & Crafting Mechanics

### Grid-Based Placement System

```typescript
interface BuildableItem {
  id: string;
  name: string;
  size: { width: number; height: number; depth: number };
  snapToGrid: boolean;
  canStack: boolean;
  maxStackHeight: number;
  model: THREE.Object3D;
  ghostMaterial: THREE.Material;
}

class BuildingSystem {
  private gridSize = 1;
  private placedObjects: Map<string, PlacedObject> = new Map();
  private ghostObject: THREE.Object3D | null = null;
  private currentItem: BuildableItem | null = null;
  private raycaster = new THREE.Raycaster();
  private scene: THREE.Scene;
  private camera: THREE.Camera;

  constructor(scene: THREE.Scene, camera: THREE.Camera) {
    this.scene = scene;
    this.camera = camera;
  }

  startPlacement(item: BuildableItem): void {
    this.currentItem = item;
    this.ghostObject = item.model.clone();
    this.ghostObject.traverse((child) => {
      if (child instanceof THREE.Mesh) {
        child.material = item.ghostMaterial;
      }
    });
    this.scene.add(this.ghostObject);
  }

  updatePlacement(mousePos: THREE.Vector2): void {
    if (!this.ghostObject || !this.currentItem) return;

    this.raycaster.setFromCamera(mousePos, this.camera);
    const intersects = this.raycaster.intersectObjects(
      this.getPlacementSurfaces(), false
    );

    if (intersects.length > 0) {
      const point = intersects[0].point;
      const snapped = this.snapToGrid(point);
      this.ghostObject.position.copy(snapped);

      // Color based on validity
      const isValid = this.canPlaceAt(snapped);
      this.ghostObject.traverse((child) => {
        if (child instanceof THREE.Mesh) {
          (child.material as THREE.MeshBasicMaterial).color.set(
            isValid ? 0x00ff00 : 0xff0000
          );
          (child.material as THREE.MeshBasicMaterial).opacity = 0.5;
        }
      });
    }
  }

  confirmPlacement(): boolean {
    if (!this.ghostObject || !this.currentItem) return false;
    const position = this.ghostObject.position.clone();
    if (!this.canPlaceAt(position)) return false;

    const placed = this.currentItem.model.clone();
    placed.position.copy(position);
    this.scene.add(placed);

    const key = this.positionKey(position);
    this.placedObjects.set(key, {
      item: this.currentItem,
      position,
      object: placed,
    });

    return true;
  }

  cancelPlacement(): void {
    if (this.ghostObject) {
      this.scene.remove(this.ghostObject);
      this.ghostObject = null;
    }
    this.currentItem = null;
  }

  private snapToGrid(point: THREE.Vector3): THREE.Vector3 {
    return new THREE.Vector3(
      Math.round(point.x / this.gridSize) * this.gridSize,
      Math.round(point.y / this.gridSize) * this.gridSize,
      Math.round(point.z / this.gridSize) * this.gridSize
    );
  }

  private canPlaceAt(position: THREE.Vector3): boolean {
    const key = this.positionKey(position);
    if (this.placedObjects.has(key)) return false;
    // Additional checks: bounds, support, collision
    return true;
  }

  private positionKey(pos: THREE.Vector3): string {
    return `${pos.x},${pos.y},${pos.z}`;
  }

  private getPlacementSurfaces(): THREE.Object3D[] {
    // Return ground + tops of placed objects
    return [];
  }
}

interface PlacedObject {
  item: BuildableItem;
  position: THREE.Vector3;
  object: THREE.Object3D;
}
```

### Snap Point System

```typescript
interface SnapPoint {
  position: THREE.Vector3;
  normal: THREE.Vector3;  // direction the snap faces
  type: string;           // 'wall', 'floor', 'ceiling', 'connector'
  occupied: boolean;
  parentId: string;
}

class SnapSystem {
  private snapPoints: SnapPoint[] = [];
  private snapDistance = 0.5;

  addSnapPoints(objectId: string, object: THREE.Object3D, points: SnapPoint[]): void {
    points.forEach(point => {
      // Transform local snap point to world space
      const worldPoint = point.position.clone();
      object.localToWorld(worldPoint);
      this.snapPoints.push({
        ...point,
        position: worldPoint,
        parentId: objectId,
      });
    });
  }

  findNearestSnap(position: THREE.Vector3, type: string): SnapPoint | null {
    let nearest: SnapPoint | null = null;
    let minDist = this.snapDistance;

    for (const snap of this.snapPoints) {
      if (snap.occupied || snap.type !== type) continue;
      const dist = position.distanceTo(snap.position);
      if (dist < minDist) {
        minDist = dist;
        nearest = snap;
      }
    }
    return nearest;
  }

  snapToPoint(object: THREE.Object3D, snapPoint: SnapPoint): void {
    object.position.copy(snapPoint.position);
    // Align rotation based on snap normal
    const up = new THREE.Vector3(0, 1, 0);
    const quat = new THREE.Quaternion().setFromUnitVectors(up, snapPoint.normal);
    object.quaternion.copy(quat);
    snapPoint.occupied = true;
  }
}
```

### Crafting System

```typescript
interface CraftingRecipe {
  id: string;
  name: string;
  ingredients: { itemId: string; quantity: number }[];
  result: { itemId: string; quantity: number };
  craftTime: number; // seconds
  requiredStation?: string; // crafting bench type
}

class CraftingSystem {
  private recipes: Map<string, CraftingRecipe> = new Map();
  private activeCrafts: ActiveCraft[] = [];

  registerRecipe(recipe: CraftingRecipe): void {
    this.recipes.set(recipe.id, recipe);
  }

  canCraft(recipeId: string, inventory: Inventory, station?: string): boolean {
    const recipe = this.recipes.get(recipeId);
    if (!recipe) return false;
    if (recipe.requiredStation && recipe.requiredStation !== station) return false;

    return recipe.ingredients.every(ing =>
      inventory.getCount(ing.itemId) >= ing.quantity
    );
  }

  startCraft(recipeId: string, inventory: Inventory): boolean {
    if (!this.canCraft(recipeId, inventory)) return false;
    const recipe = this.recipes.get(recipeId)!;

    // Consume ingredients
    recipe.ingredients.forEach(ing => {
      inventory.remove(ing.itemId, ing.quantity);
    });

    this.activeCrafts.push({
      recipe,
      startTime: performance.now(),
      completed: false,
    });
    return true;
  }

  update(): void {
    const now = performance.now();
    for (const craft of this.activeCrafts) {
      if (!craft.completed) {
        const elapsed = (now - craft.startTime) / 1000;
        if (elapsed >= craft.recipe.craftTime) {
          craft.completed = true;
          // Emit event or callback for result collection
        }
      }
    }
  }

  getAvailableRecipes(inventory: Inventory, station?: string): CraftingRecipe[] {
    return Array.from(this.recipes.values()).filter(recipe =>
      this.canCraft(recipe.id, inventory, station)
    );
  }
}

interface ActiveCraft {
  recipe: CraftingRecipe;
  startTime: number;
  completed: boolean;
}

interface Inventory {
  getCount(itemId: string): number;
  remove(itemId: string, quantity: number): boolean;
  add(itemId: string, quantity: number): boolean;
}
```

---

## World Building Patterns

### Chunk-Based Terrain

```typescript
class ChunkManager {
  private chunks = new Map<string, Chunk>();
  private chunkSize = 16;
  private renderDistance = 4; // chunks
  private scene: THREE.Scene;

  constructor(scene: THREE.Scene) {
    this.scene = scene;
  }

  update(playerPosition: THREE.Vector3): void {
    const playerChunkX = Math.floor(playerPosition.x / this.chunkSize);
    const playerChunkZ = Math.floor(playerPosition.z / this.chunkSize);

    // Load chunks in range
    for (let x = -this.renderDistance; x <= this.renderDistance; x++) {
      for (let z = -this.renderDistance; z <= this.renderDistance; z++) {
        const cx = playerChunkX + x;
        const cz = playerChunkZ + z;
        const key = `${cx},${cz}`;

        if (!this.chunks.has(key)) {
          const chunk = this.generateChunk(cx, cz);
          this.chunks.set(key, chunk);
          this.scene.add(chunk.mesh);
        }
      }
    }

    // Unload distant chunks
    for (const [key, chunk] of this.chunks) {
      const [cx, cz] = key.split(',').map(Number);
      const dist = Math.max(
        Math.abs(cx - playerChunkX),
        Math.abs(cz - playerChunkZ)
      );
      if (dist > this.renderDistance + 1) {
        this.scene.remove(chunk.mesh);
        chunk.dispose();
        this.chunks.delete(key);
      }
    }
  }

  private generateChunk(cx: number, cz: number): Chunk {
    // Generate terrain heightmap, place objects, etc.
    const chunk = new Chunk(cx, cz, this.chunkSize);
    chunk.generate();
    return chunk;
  }
}

class Chunk {
  mesh: THREE.Mesh;
  private cx: number;
  private cz: number;
  private size: number;

  constructor(cx: number, cz: number, size: number) {
    this.cx = cx;
    this.cz = cz;
    this.size = size;
    this.mesh = new THREE.Mesh();
  }

  generate(): void {
    const geometry = new THREE.PlaneGeometry(
      this.size, this.size,
      this.size - 1, this.size - 1
    );
    geometry.rotateX(-Math.PI / 2);

    const positions = geometry.attributes.position;
    for (let i = 0; i < positions.count; i++) {
      const x = positions.getX(i) + this.cx * this.size;
      const z = positions.getZ(i) + this.cz * this.size;
      const height = this.getHeight(x, z);
      positions.setY(i, height);
    }

    geometry.computeVertexNormals();
    this.mesh.geometry = geometry;
    this.mesh.material = new THREE.MeshStandardMaterial({ color: 0x4a7c4f });
    this.mesh.position.set(this.cx * this.size, 0, this.cz * this.size);
  }

  private getHeight(x: number, z: number): number {
    // Use noise for terrain generation
    return Math.sin(x * 0.1) * Math.cos(z * 0.1) * 3;
  }

  dispose(): void {
    this.mesh.geometry.dispose();
    (this.mesh.material as THREE.Material).dispose();
  }
}
```
