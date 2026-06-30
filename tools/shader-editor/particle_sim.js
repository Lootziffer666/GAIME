/**
 * GAIME Particle Physics System
 * CPU-simulated particles (leaves, snow) with:
 * - Wind field interaction (from flow canvas)
 * - Collision with scene geometry (from material map)
 * - Accumulation in corners/edges
 * - State transitions (fresh → dry → crumbling → gone)
 * - Wetness interaction (mass increase, color shift)
 * - Entity disturbance (radius impulse)
 */

// ============================================================
// CONFIGURATION
// ============================================================
const PARTICLE_CONFIG = {
  leaf: {
    maxCount: 300,
    spawnRate: 2, // per frame when active
    gravity: 0.15,
    mass: 0.3,
    drag: 0.985,
    windInfluence: 1.2,
    size: { min: 3, max: 7 },
    lifetime: { min: 400, max: 1200 }, // frames
    colors: [
      [220, 140, 20],   // golden
      [200, 80, 10],    // orange
      [160, 40, 15],    // dark red
      [120, 90, 30],    // brown
      [180, 160, 40],   // yellow-green
    ],
    rotationSpeed: 0.03,
    tumble: 0.4, // lateral oscillation
    // Physics states
    states: {
      FALLING: 0,
      GROUNDED: 1,
      PILED: 2,      // accumulated in corner
      DISTURBED: 3,  // kicked up by entity
      DECAYING: 4,
    },
    // Wet behavior
    wetMassMultiplier: 3.0,
    wetDragMultiplier: 0.95,
    wetColor: [60, 40, 20], // dark brown mush
    // Accumulation
    pileMaxHeight: 20, // pixels of pile before overflow
  },
  snow: {
    maxCount: 400,
    spawnRate: 3,
    gravity: 0.06,
    mass: 0.1,
    drag: 0.992,
    windInfluence: 0.8,
    size: { min: 2, max: 4 },
    lifetime: { min: 600, max: 2000 },
    colors: [
      [240, 245, 255],
      [230, 235, 250],
      [250, 252, 255],
    ],
    rotationSpeed: 0.01,
    tumble: 0.6,
    states: {
      FALLING: 0,
      GROUNDED: 1,
      PILED: 2,
      DISTURBED: 3,
      MELTING: 4,
    },
    wetMassMultiplier: 1.5, // wet snow is heavier
    wetDragMultiplier: 0.97,
    wetColor: [200, 210, 220], // slush
    pileMaxHeight: 30,
  }
};

// ============================================================
// PARTICLE CLASS
// ============================================================
class Particle {
  constructor(type, x, y, config) {
    this.type = type;
    this.x = x;
    this.y = y;
    this.vx = 0;
    this.vy = 0;
    this.age = 0;
    this.lifetime = config.lifetime.min + Math.random() * (config.lifetime.max - config.lifetime.min);
    this.state = config.states.FALLING;
    this.size = config.size.min + Math.random() * (config.size.max - config.size.min);
    this.rotation = Math.random() * Math.PI * 2;
    this.rotSpeed = (Math.random() - 0.5) * config.rotationSpeed;
    this.color = config.colors[Math.floor(Math.random() * config.colors.length)].slice();
    this.originalColor = this.color.slice();
    this.alpha = 1.0;
    this.grounded = false;
    this.groundedTime = 0;
    this.tumblePhase = Math.random() * Math.PI * 2;
    this.mass = config.mass;
    this.wetness = 0; // 0-1
  }
}

// ============================================================
// PARTICLE SYSTEM
// ============================================================
class ParticleSystem {
  constructor(canvas, matCanvas, flowCanvas) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    this.matCanvas = matCanvas;
    this.flowCanvas = flowCanvas;
    this.width = 0;
    this.height = 0;

    this.particles = [];
    this.pileMap = null;      // accumulation height per column
    this.collisionMap = null; // 1 = solid (roof/wood), 0 = passable
    this.flowField = null;    // vec2 per cell (wind direction)

    this.config = {
      enabled: false,
      type: 'leaf', // 'leaf' or 'snow'
      windX: 0,
      windY: 0,
      windStrength: 0.5,
      season: 0.5,   // 0=spring, 1=winter
      wetness: 0,    // 0-1
      cold: 0,       // 0-1
      age: 0,        // 0-1
    };

    // Spawn regions (where particles originate)
    this.spawnZones = []; // [{x, y, w, h, material}]
  }

  resize(w, h) {
    this.width = w;
    this.height = h;
    this.canvas.width = w;
    this.canvas.height = h;
    this.pileMap = new Float32Array(w);
    this.buildCollisionMap();
    this.buildFlowField();
    this.findSpawnZones();
  }

  buildCollisionMap() {
    if (!this.matCanvas || this.matCanvas.width < 2) return;
    const ctx = this.matCanvas.getContext('2d', {willReadFrequently: true});
    const data = ctx.getImageData(0, 0, this.width, this.height).data;
    // Collision: roof (orange) and wood (brown) are solid surfaces
    // Using simplified grid (every 4px)
    const gridW = Math.ceil(this.width / 4);
    const gridH = Math.ceil(this.height / 4);
    this.collisionGrid = new Uint8Array(gridW * gridH);
    this.collisionGridW = gridW;
    this.collisionGridH = gridH;

    for (let gy = 0; gy < gridH; gy++) {
      for (let gx = 0; gx < gridW; gx++) {
        const px = Math.min(gx * 4, this.width - 1);
        const py = Math.min(gy * 4, this.height - 1);
        const idx = (py * this.width + px) * 4;
        const r = data[idx], g = data[idx+1], b = data[idx+2];
        // Roof: orange (R high, G medium, B low)
        const isRoof = r > 180 && g > 60 && g < 160 && b < 80;
        // Wood: brown (R medium, G low-med, B low)
        const isWood = r > 100 && r < 180 && g > 40 && g < 100 && b < 60;
        // Stone: solid surface
        const isStone = r > 180 && g < 60; // red in material map
        this.collisionGrid[gy * gridW + gx] = (isRoof || isWood || isStone) ? 1 : 0;
      }
    }
  }

  buildFlowField() {
    if (!this.flowCanvas || this.flowCanvas.width < 2) return;
    const ctx = this.flowCanvas.getContext('2d', {willReadFrequently: true});
    const data = ctx.getImageData(0, 0, this.width, this.height).data;
    const gridW = Math.ceil(this.width / 8);
    const gridH = Math.ceil(this.height / 8);
    this.flowGridW = gridW;
    this.flowGridH = gridH;
    this.flowGrid = new Float32Array(gridW * gridH * 2);

    for (let gy = 0; gy < gridH; gy++) {
      for (let gx = 0; gx < gridW; gx++) {
        const px = Math.min(gx * 8, this.width - 1);
        const py = Math.min(gy * 8, this.height - 1);
        const idx = (py * this.width + px) * 4;
        // Flow encoded as R=dx, G=dy (128=neutral)
        const dx = (data[idx] / 127.5) - 1.0;
        const dy = (data[idx+1] / 127.5) - 1.0;
        this.flowGrid[(gy * gridW + gx) * 2] = dx;
        this.flowGrid[(gy * gridW + gx) * 2 + 1] = dy;
      }
    }
  }

  findSpawnZones() {
    if (!this.matCanvas || this.matCanvas.width < 2) return;
    this.spawnZones = [];
    const ctx = this.matCanvas.getContext('2d', {willReadFrequently: true});
    const data = ctx.getImageData(0, 0, this.width, this.height).data;
    // Find foliage regions (for leaves) and open sky (for snow)
    const step = 16;
    for (let y = 0; y < this.height; y += step) {
      for (let x = 0; x < this.width; x += step) {
        const idx = (y * this.width + x) * 4;
        const r = data[idx], g = data[idx+1], b = data[idx+2];
        // Foliage: purple-ish or cyan in material map
        const isFoliage = (r > 140 && b > 140 && g < 30) || (g > 150 && b > 180 && r < 30);
        // Grass: green
        const isGrass = g > 140 && r < 80 && b < 100;
        if (isFoliage) {
          this.spawnZones.push({x, y, w: step, h: step, material: 'foliage'});
        }
        if (isGrass || (!isFoliage && y < this.height * 0.3)) {
          // Snow can spawn anywhere in upper portion
          this.spawnZones.push({x, y, w: step, h: step, material: 'sky'});
        }
      }
    }
  }

  isSolid(px, py) {
    if (!this.collisionGrid) return false;
    const gx = Math.floor(px / 4);
    const gy = Math.floor(py / 4);
    if (gx < 0 || gx >= this.collisionGridW || gy < 0 || gy >= this.collisionGridH) return false;
    return this.collisionGrid[gy * this.collisionGridW + gx] === 1;
  }

  getFlow(px, py) {
    if (!this.flowGrid) return {x: 0, y: 0};
    const gx = Math.floor(px / 8);
    const gy = Math.floor(py / 8);
    if (gx < 0 || gx >= this.flowGridW || gy < 0 || gy >= this.flowGridH) return {x: 0, y: 0};
    const idx = (gy * this.flowGridW + gx) * 2;
    return {x: this.flowGrid[idx], y: this.flowGrid[idx + 1]};
  }

  getPileHeight(x) {
    if (!this.pileMap) return 0;
    const ix = Math.floor(Math.max(0, Math.min(x, this.width - 1)));
    return this.pileMap[ix];
  }

  addToPile(x, amount) {
    if (!this.pileMap) return;
    const ix = Math.floor(Math.max(0, Math.min(x, this.width - 1)));
    const cfg = PARTICLE_CONFIG[this.config.type];
    this.pileMap[ix] = Math.min(this.pileMap[ix] + amount, cfg.pileMaxHeight);
    // Spread to neighbors (settling)
    if (ix > 0) this.pileMap[ix-1] = Math.min(this.pileMap[ix-1] + amount * 0.3, cfg.pileMaxHeight);
    if (ix < this.width-1) this.pileMap[ix+1] = Math.min(this.pileMap[ix+1] + amount * 0.3, cfg.pileMaxHeight);
  }

  spawnParticle() {
    const cfg = PARTICLE_CONFIG[this.config.type];
    if (this.particles.length >= cfg.maxCount) return;

    let zones = this.spawnZones;
    if (this.config.type === 'leaf') {
      zones = zones.filter(z => z.material === 'foliage');
    }
    if (zones.length === 0) {
      // Fallback: spawn from top
      const p = new Particle(this.config.type, Math.random() * this.width, -5, cfg);
      p.vy = cfg.gravity * 2;
      this.particles.push(p);
      return;
    }

    const zone = zones[Math.floor(Math.random() * zones.length)];
    const x = zone.x + Math.random() * zone.w;
    const y = zone.y + Math.random() * zone.h;
    const p = new Particle(this.config.type, x, y, cfg);
    this.particles.push(p);
  }

  update() {
    if (!this.config.enabled || this.width === 0) return;

    const cfg = PARTICLE_CONFIG[this.config.type];
    const isLeafSeason = this.config.type === 'leaf' && this.config.season > 0.4;
    const isSnowSeason = this.config.type === 'snow' && this.config.cold > 0.2;

    // Spawn
    if (isLeafSeason || isSnowSeason) {
      const rate = cfg.spawnRate * (this.config.type === 'leaf' ? this.config.season : this.config.cold);
      for (let i = 0; i < rate; i++) {
        this.spawnParticle();
      }
    }

    // Update each particle
    for (let i = this.particles.length - 1; i >= 0; i--) {
      const p = this.particles[i];
      p.age++;

      // Kill old particles
      if (p.age > p.lifetime || p.y > this.height + 20 || p.x < -20 || p.x > this.width + 20) {
        this.particles.splice(i, 1);
        continue;
      }

      // State machine
      if (p.state === cfg.states.FALLING || p.state === cfg.states.DISTURBED) {
        // Gravity
        p.vy += cfg.gravity * p.mass;

        // Wind (global + local flow field)
        const flow = this.getFlow(p.x, p.y);
        const windX = this.config.windX * cfg.windInfluence + flow.x * this.config.windStrength;
        const windY = flow.y * this.config.windStrength * 0.3; // vertical wind less influential
        p.vx += windX * 0.1;
        p.vy += windY * 0.05;

        // Tumble (lateral oscillation — leaves flutter)
        p.tumblePhase += 0.05 + Math.random() * 0.02;
        p.vx += Math.sin(p.tumblePhase) * cfg.tumble * 0.05;

        // Drag (wetness increases drag)
        const wetDrag = 1.0 - (1.0 - cfg.drag) * (1.0 + p.wetness * (cfg.wetDragMultiplier - 1.0));
        p.vx *= wetDrag;
        p.vy *= wetDrag;

        // Wetness from rain
        p.wetness = Math.min(1.0, p.wetness + this.config.wetness * 0.005);
        // Wet mass
        const effectiveMass = cfg.mass * (1.0 + p.wetness * (cfg.wetMassMultiplier - 1.0));
        p.vy += (effectiveMass - cfg.mass) * cfg.gravity;

        // Move
        p.x += p.vx;
        p.y += p.vy;
        p.rotation += p.rotSpeed * (1.0 - p.wetness * 0.7); // wet = less spin

        // Collision with solid surfaces
        if (this.isSolid(p.x, p.y + p.size)) {
          // Land on surface
          p.vy = 0;
          p.vx *= 0.5;
          p.state = cfg.states.GROUNDED;
          p.grounded = true;
          p.groundedTime = 0;
        }

        // Ground collision (bottom of scene + pile)
        const pileH = this.getPileHeight(p.x);
        if (p.y >= this.height - pileH - 2) {
          p.y = this.height - pileH - 2;
          p.vy = 0;
          p.vx *= 0.3;
          p.state = cfg.states.GROUNDED;
          p.grounded = true;
          p.groundedTime = 0;
        }

        // Side collision (bounce off walls)
        if (this.isSolid(p.x + p.size, p.y) || this.isSolid(p.x - p.size, p.y)) {
          p.vx *= -0.4;
          // Corner detection: if surrounded by solid on both sides, pile up
          if (this.isSolid(p.x + p.size * 2, p.y) && this.isSolid(p.x - p.size * 2, p.y)) {
            p.state = cfg.states.PILED;
          }
        }

      } else if (p.state === cfg.states.GROUNDED) {
        p.groundedTime++;
        p.vx *= 0.9;
        p.vy = 0;

        // Still affected by strong wind
        if (Math.abs(this.config.windX) > 0.3 && p.wetness < 0.5) {
          p.vx += this.config.windX * 0.02;
          if (Math.abs(p.vx) > 0.5) {
            p.state = cfg.states.FALLING; // blown away
            p.vy = -0.5;
          }
        }

        // Accumulate into pile over time
        if (p.groundedTime > 30) {
          this.addToPile(p.x, 0.1);
          p.state = cfg.states.PILED;
        }

        // Decay (leaves)
        if (this.config.type === 'leaf' && p.groundedTime > 200) {
          p.state = cfg.states.DECAYING;
        }

      } else if (p.state === cfg.states.PILED) {
        // Stationary, slowly decay
        p.alpha = Math.max(0, p.alpha - 0.001);
        // Color shift: darken over time
        p.color[0] = Math.max(20, p.color[0] - 0.1);
        p.color[1] = Math.max(15, p.color[1] - 0.1);
        p.color[2] = Math.max(10, p.color[2] - 0.05);

        if (p.alpha <= 0) {
          this.particles.splice(i, 1);
          continue;
        }

      } else if (p.state === cfg.states.DECAYING) {
        p.alpha -= 0.005;
        p.size *= 0.999; // shrink
        p.color[0] = Math.max(30, p.color[0] * 0.998);
        p.color[1] = Math.max(20, p.color[1] * 0.997);
        p.color[2] = Math.max(10, p.color[2] * 0.996);
        if (p.alpha <= 0) {
          this.particles.splice(i, 1);
          continue;
        }
      }

      // Wet color blending
      if (p.wetness > 0.3) {
        const wetFactor = (p.wetness - 0.3) / 0.7;
        p.color[0] = p.originalColor[0] * (1 - wetFactor) + cfg.wetColor[0] * wetFactor;
        p.color[1] = p.originalColor[1] * (1 - wetFactor) + cfg.wetColor[1] * wetFactor;
        p.color[2] = p.originalColor[2] * (1 - wetFactor) + cfg.wetColor[2] * wetFactor;
      }
    }

    // Pile decay (slow settling/melting)
    if (this.pileMap) {
      const meltRate = this.config.type === 'snow' ? (1.0 - this.config.cold) * 0.02 : 0.005;
      for (let i = 0; i < this.width; i++) {
        this.pileMap[i] = Math.max(0, this.pileMap[i] - meltRate);
      }
    }
  }

  // Entity disturbance: kick up particles in radius
  disturb(cx, cy, radius, strength) {
    const cfg = PARTICLE_CONFIG[this.config.type];
    for (const p of this.particles) {
      if (p.state === cfg.states.GROUNDED || p.state === cfg.states.PILED) {
        const dx = p.x - cx, dy = p.y - cy;
        const distSq = dx * dx + dy * dy;
        if (distSq < radius * radius) {
          const dist = Math.sqrt(distSq) + 0.01;
          const force = strength * (1.0 - dist / radius);
          // Dry: flies up easily. Wet: barely moves
          const wetResist = 1.0 - p.wetness * 0.8;
          p.vx += (dx / dist) * force * wetResist;
          p.vy += (-Math.abs(dy / dist) - 0.5) * force * wetResist; // always upward
          p.state = cfg.states.DISTURBED;
          p.grounded = false;
        }
      }
    }
  }

  render() {
    this.ctx.clearRect(0, 0, this.width, this.height);
    if (!this.config.enabled) return;

    const cfg = PARTICLE_CONFIG[this.config.type];

    // Draw pile accumulation
    if (this.pileMap) {
      const pileColor = this.config.type === 'snow' ? 'rgba(235,240,250,' : 'rgba(80,55,25,';
      this.ctx.beginPath();
      this.ctx.moveTo(0, this.height);
      for (let x = 0; x < this.width; x += 2) {
        const h = this.pileMap[x];
        if (h > 0.5) {
          this.ctx.lineTo(x, this.height - h);
        }
      }
      this.ctx.lineTo(this.width, this.height);
      this.ctx.closePath();
      this.ctx.fillStyle = pileColor + '0.6)';
      this.ctx.fill();
    }

    // Draw particles
    for (const p of this.particles) {
      if (p.alpha <= 0) continue;
      this.ctx.save();
      this.ctx.translate(p.x, p.y);
      this.ctx.rotate(p.rotation);
      this.ctx.globalAlpha = p.alpha;

      if (this.config.type === 'leaf') {
        // Leaf shape: elongated diamond
        this.ctx.fillStyle = `rgb(${Math.floor(p.color[0])},${Math.floor(p.color[1])},${Math.floor(p.color[2])})`;
        this.ctx.beginPath();
        this.ctx.moveTo(0, -p.size);
        this.ctx.quadraticCurveTo(p.size * 0.6, -p.size * 0.3, p.size * 0.3, p.size * 0.5);
        this.ctx.quadraticCurveTo(0, p.size, -p.size * 0.3, p.size * 0.5);
        this.ctx.quadraticCurveTo(-p.size * 0.6, -p.size * 0.3, 0, -p.size);
        this.ctx.fill();
        // Leaf vein
        this.ctx.strokeStyle = `rgba(${Math.floor(p.color[0]*0.6)},${Math.floor(p.color[1]*0.6)},0,0.4)`;
        this.ctx.lineWidth = 0.5;
        this.ctx.beginPath();
        this.ctx.moveTo(0, -p.size * 0.8);
        this.ctx.lineTo(0, p.size * 0.7);
        this.ctx.stroke();
      } else {
        // Snowflake: circle with slight variation
        this.ctx.fillStyle = `rgb(${Math.floor(p.color[0])},${Math.floor(p.color[1])},${Math.floor(p.color[2])})`;
        this.ctx.beginPath();
        this.ctx.arc(0, 0, p.size, 0, Math.PI * 2);
        this.ctx.fill();
      }

      this.ctx.restore();
    }
  }
}

// Export for use in editor
if (typeof window !== 'undefined') {
  window.ParticleSystem = ParticleSystem;
  window.PARTICLE_CONFIG = PARTICLE_CONFIG;
}
