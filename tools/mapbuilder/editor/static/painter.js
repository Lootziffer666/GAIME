/**
 * GAIME Map Builder — Interactive Multi-Layer Tile Painter
 * 
 * Canvas-based editor with multiple layers:
 * - Ground (floor, wall, water, grass, stone)
 * - Weather Zones (rain, snow, leaves, wind areas)
 * - Exits/Entrances (spawn, exit markers with destinations)
 * - Decorative (props, details)
 *
 * Features: brush, fill, eraser, ghost image overlay, layer toggle,
 * tile picker (swap tiles), exit placement with destination config.
 */

const CELL_SIZE = 16;
let gridWidth = 32;
let gridHeight = 24;
let showGhost = true;
let ghostImage = null;
let painting = false;

// --- Multi-Layer System ---
const LAYERS = {
    ground: { name: "Ground", visible: true, data: null },
    weather: { name: "Hydrology (Pfützen/Abfluss)", visible: true, data: null },
    exits: { name: "Exits / Entrances", visible: true, data: null },
    npc_routes: { name: "NPC Routes", visible: true, data: null },
    interactable: { name: "Interactive Objects", visible: true, data: null },
    decorative: { name: "Decorative", visible: true, data: null },
};
let activeLayer = "ground";

// --- Tools ---
let selectedLabel = "floor";
let tool = "brush"; // brush | fill | eraser | exit

const canvas = document.getElementById("map-canvas");
const ctx = canvas.getContext("2d");

// --- Label Definitions per Layer ---
const GROUND_LABELS = [
    { id: "floor", name: "Floor", color: "#c8b48c" },
    { id: "wall", name: "Wall", color: "#3c3c3c" },
    { id: "water", name: "Water", color: "#2878b4" },
    { id: "grass", name: "Grass", color: "#50b450" },
    { id: "stone", name: "Stone", color: "#b4b4b4" },
    { id: "door", name: "Door", color: "#c83c3c" },
    { id: "empty", name: "Empty", color: "#000000" },
];

const WEATHER_LABELS = [
    { id: "puddle_zone", name: "Puddle Area", color: "#3366aa", icon: "\u25cf" },
    { id: "drain", name: "Drain/Abfluss", color: "#224488", icon: "\u2193" },
    { id: "flow_north", name: "Flow North", color: "#4499cc", icon: "\u2191" },
    { id: "flow_south", name: "Flow South", color: "#4499cc", icon: "\u2193" },
    { id: "flow_east", name: "Flow East", color: "#4499cc", icon: "\u2192" },
    { id: "flow_west", name: "Flow West", color: "#4499cc", icon: "\u2190" },
    { id: "sheltered", name: "Sheltered (kein Regen)", color: "#665544", icon: "\u2302" },
    { id: "exposed", name: "Exposed (Regen direkt)", color: "#5588bb", icon: "\u2602" },
    { id: "slope_low", name: "Low Point (sammelt)", color: "#223366", icon: "\u25bc" },
    { id: "slope_high", name: "High Point (flie\u00dft ab)", color: "#88bbdd", icon: "\u25b2" },
    { id: "none", name: "Normal", color: "transparent" },
];

const EXIT_LABELS = [
    { id: "spawn", name: "Spawn Point", color: "#00ff00", icon: "S" },
    { id: "exit_north", name: "Exit North", color: "#ffaa00", icon: "\u2191" },
    { id: "exit_south", name: "Exit South", color: "#ffaa00", icon: "\u2193" },
    { id: "exit_east", name: "Exit East", color: "#ffaa00", icon: "\u2192" },
    { id: "exit_west", name: "Exit West", color: "#ffaa00", icon: "\u2190" },
    { id: "none", name: "Clear", color: "transparent" },
];

const DECO_LABELS = [
    { id: "tree", name: "Tree", color: "#2d5a27", icon: "T" },
    { id: "rock", name: "Rock", color: "#777777", icon: "R" },
    { id: "flower", name: "Flowers", color: "#ff88cc", icon: "F" },
    { id: "torch", name: "Torch", color: "#ffcc00", icon: "\u{1f525}" },
    { id: "chest", name: "Chest", color: "#aa6600", icon: "C" },
    { id: "sign", name: "Sign", color: "#8b5e3c", icon: "!" },
    { id: "none", name: "Clear", color: "transparent" },
];

const NPC_ROUTE_LABELS = [
    { id: "npc_waypoint", name: "Waypoint", color: "#ff66ff", icon: "\u25cf" },
    { id: "npc_patrol_a", name: "Patrol A", color: "#ff33aa", icon: "A" },
    { id: "npc_patrol_b", name: "Patrol B", color: "#aa33ff", icon: "B" },
    { id: "npc_patrol_c", name: "Patrol C", color: "#3399ff", icon: "C" },
    { id: "npc_idle_spot", name: "Idle Spot", color: "#ffaa66", icon: "\u23f8" },
    { id: "npc_spawn", name: "NPC Spawn", color: "#66ffaa", icon: "N" },
    { id: "none", name: "Clear", color: "transparent" },
];

const INTERACT_LABELS = [
    { id: "lever", name: "Lever", color: "#cc8800", icon: "L" },
    { id: "button", name: "Button", color: "#cc4400", icon: "\u25a0" },
    { id: "gate", name: "Gate/Door", color: "#886644", icon: "\u2503" },
    { id: "npc_talk", name: "NPC (Talkable)", color: "#44ccff", icon: "\u{1f4ac}" },
    { id: "shop", name: "Shop", color: "#ffdd00", icon: "$" },
    { id: "save_point", name: "Save Point", color: "#88ff88", icon: "\u2605" },
    { id: "pickup", name: "Item Pickup", color: "#ffaa00", icon: "\u2b06" },
    { id: "trap", name: "Trap", color: "#ff2222", icon: "\u26a0" },
    { id: "none", name: "Clear", color: "transparent" },
];

function getLabelsForLayer(layer) {
    switch (layer) {
        case "ground": return GROUND_LABELS;
        case "weather": return WEATHER_LABELS;
        case "exits": return EXIT_LABELS;
        case "npc_routes": return NPC_ROUTE_LABELS;
        case "interactable": return INTERACT_LABELS;
        case "decorative": return DECO_LABELS;
        default: return GROUND_LABELS;
    }
}

// --- Init ---

function init() {
    buildLayerTabs();
    buildPalette();
    setupEvents();
    loadProjectMapList();
    
    // AUTO MODE BY DEFAULT: analyze all project maps and generate immediately.
    // The editor exists only for corrections.
    autoStart();
}

async function autoStart() {
    try {
        const res = await fetch("/api/project_maps");
        const maps = await res.json();
        if (maps.length === 0) return;
        
        // Show editor immediately with loading indicator
        document.getElementById("editor-section").style.display = "block";
        document.getElementById("upload-section").style.display = "none";
        canvas.width = 512; canvas.height = 384;
        ctx.fillStyle = "#16213e"; ctx.fillRect(0, 0, 512, 384);
        ctx.fillStyle = "#e94560"; ctx.font = "16px sans-serif"; ctx.textAlign = "center";
        ctx.fillText("Generating map from project data...", 256, 180);
        ctx.fillStyle = "#666"; ctx.font = "12px sans-serif";
        ctx.fillText(`Analyzing ${maps.length} locations`, 256, 210);
        
        // Full auto
        const autoRes = await fetch("/api/full_auto", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ maps: maps.map(m => m.path), generate_width: 32, generate_height: 24 }),
        });
        const data = await autoRes.json();
        
        if (data.error) {
            // Fallback to manual
            document.getElementById("upload-section").style.display = "block";
            return;
        }
        
        // Show generated result
        gridWidth = data.width; gridHeight = data.height;
        for (const [key, ld] of Object.entries(data.layers)) { if (LAYERS[key]) LAYERS[key].data = ld; }
        for (const key of Object.keys(LAYERS)) {
            if (!LAYERS[key].data) LAYERS[key].data = Array.from({length: gridHeight}, () => Array(gridWidth).fill("none"));
        }
        canvas.width = gridWidth * CELL_SIZE; canvas.height = gridHeight * CELL_SIZE;
        render();
        
        // Toast
        const t = document.createElement("div"); t.className = "auto-toast";
        t.textContent = `Auto-generated from ${data.maps_analyzed} maps. Edit if needed, then Export.`;
        document.body.appendChild(t); setTimeout(() => t.remove(), 6000);
    } catch (e) {
        document.getElementById("upload-section").style.display = "block";
    }
}

async function loadProjectMapList() {
    try {
        const res = await fetch("/api/project_maps");
        const maps = await res.json();
        const select = document.getElementById("project-maps");
        maps.forEach(m => {
            const opt = document.createElement("option");
            opt.value = m.path;
            opt.textContent = m.name;
            select.appendChild(opt);
        });
    } catch (e) { /* ignore */ }
}

function buildLayerTabs() {
    const tabs = document.getElementById("layer-tabs");
    tabs.innerHTML = "";
    for (const [key, layer] of Object.entries(LAYERS)) {
        const tab = document.createElement("button");
        tab.className = "layer-tab" + (key === activeLayer ? " active" : "");
        tab.textContent = layer.name;
        tab.dataset.layer = key;
        tab.onclick = () => switchLayer(key);
        tabs.appendChild(tab);

        // Visibility toggle
        const eye = document.createElement("span");
        eye.className = "layer-eye" + (layer.visible ? " visible" : "");
        eye.textContent = layer.visible ? "\u{1f441}" : "\u{1f441}\u200d\u{1f5e8}";
        eye.title = "Toggle visibility";
        eye.onclick = (e) => { e.stopPropagation(); toggleLayerVisibility(key); };
        tab.appendChild(eye);
    }
}

function switchLayer(key) {
    activeLayer = key;
    const labels = getLabelsForLayer(key);
    selectedLabel = labels[0].id;
    buildLayerTabs();
    buildPalette();
    render();
}

function toggleLayerVisibility(key) {
    LAYERS[key].visible = !LAYERS[key].visible;
    buildLayerTabs();
    render();
}

function buildPalette() {
    const palette = document.getElementById("palette");
    palette.innerHTML = "";
    const labels = getLabelsForLayer(activeLayer);
    labels.forEach(l => {
        const swatch = document.createElement("div");
        swatch.className = "swatch" + (l.id === selectedLabel ? " active" : "");
        swatch.style.background = l.color || "#333";
        if (l.color === "transparent") {
            swatch.style.background = "#111";
            swatch.style.border = "2px dashed #555";
        }
        swatch.title = l.name;
        swatch.dataset.label = l.id;
        swatch.textContent = l.icon || "";
        swatch.onclick = () => selectLabel(l.id);
        palette.appendChild(swatch);
    });
}

function selectLabel(id) {
    selectedLabel = id;
    document.querySelectorAll(".swatch").forEach(s => {
        s.classList.toggle("active", s.dataset.label === id);
    });
}

// --- Events ---

function setupEvents() {
    const dropZone = document.getElementById("drop-zone");
    const fileInput = document.getElementById("file-input");

    dropZone.onclick = () => fileInput.click();
    dropZone.ondragover = e => { e.preventDefault(); dropZone.classList.add("dragover"); };
    dropZone.ondragleave = () => dropZone.classList.remove("dragover");
    dropZone.ondrop = e => {
        e.preventDefault();
        dropZone.classList.remove("dragover");
        if (e.dataTransfer.files.length) handleFile(e.dataTransfer.files[0]);
    };
    fileInput.onchange = () => { if (fileInput.files.length) handleFile(fileInput.files[0]); };

    document.getElementById("btn-segment").onclick = doSegment;
    document.getElementById("btn-export").onclick = doExport;
    document.getElementById("btn-new").onclick = doNewBlank;
    document.getElementById("btn-teach").onclick = doTeachAI;
    document.getElementById("btn-generate").onclick = doGenerate;
    document.getElementById("btn-load-project").onclick = doLoadProjectMap;
    document.getElementById("btn-load-tmx").onclick = doLoadTmxFile;
    document.getElementById("btn-vision").onclick = doVisionAnnotate;
    document.getElementById("btn-auto").onclick = doFullAuto;
    
    const tmxInput = document.getElementById("tmx-input");
    tmxInput.onchange = () => { document.getElementById("btn-load-tmx").disabled = !tmxInput.files.length; };

    // Tools
    document.getElementById("btn-brush").onclick = () => setTool("brush");
    document.getElementById("btn-fill").onclick = () => setTool("fill");
    document.getElementById("btn-eraser").onclick = () => setTool("eraser");
    document.getElementById("toggle-ghost").onchange = e => { showGhost = e.target.checked; render(); };

    // Canvas painting
    canvas.onmousedown = e => { painting = true; paint(e); };
    canvas.onmousemove = e => { updateStatus(e); if (painting) paint(e); };
    canvas.onmouseup = () => { painting = false; saveGrid(); };
    canvas.onmouseleave = () => { painting = false; };

    // Keyboard shortcuts
    document.onkeydown = e => {
        if (e.target.tagName === "INPUT") return;
        if (e.key === "b") setTool("brush");
        if (e.key === "f") setTool("fill");
        if (e.key === "e") setTool("eraser");
        if (e.key === "1") switchLayer("ground");
        if (e.key === "2") switchLayer("weather");
        if (e.key === "3") switchLayer("exits");
        if (e.key === "4") switchLayer("npc_routes");
        if (e.key === "5") switchLayer("interactable");
        if (e.key === "6") switchLayer("decorative");
    };
}

let uploadedFile = null;

function handleFile(file) {
    uploadedFile = file;
    document.getElementById("btn-segment").disabled = false;
    document.querySelector(".upload-area p").textContent = file.name;
}

function setTool(t) {
    tool = t;
    document.querySelectorAll(".tool").forEach(b => b.classList.remove("active"));
    document.getElementById("btn-" + t).classList.add("active");
}

// --- New Blank Map ---

function doNewBlank() {
    gridWidth = parseInt(document.getElementById("grid-width").value) || 32;
    gridHeight = parseInt(document.getElementById("grid-height").value) || 24;

    // Initialize all layers
    LAYERS.ground.data = Array.from({ length: gridHeight }, () => Array(gridWidth).fill("floor"));
    LAYERS.weather.data = Array.from({ length: gridHeight }, () => Array(gridWidth).fill("none"));
    LAYERS.exits.data = Array.from({ length: gridHeight }, () => Array(gridWidth).fill("none"));
    LAYERS.npc_routes.data = Array.from({ length: gridHeight }, () => Array(gridWidth).fill("none"));
    LAYERS.interactable.data = Array.from({ length: gridHeight }, () => Array(gridWidth).fill("none"));
    LAYERS.decorative.data = Array.from({ length: gridHeight }, () => Array(gridWidth).fill("none"));

    ghostImage = null;
    document.getElementById("editor-section").style.display = "block";
    canvas.width = gridWidth * CELL_SIZE;
    canvas.height = gridHeight * CELL_SIZE;
    render();
}

// --- Segmentation ---

async function doSegment() {
    if (!uploadedFile) return;

    const btn = document.getElementById("btn-segment");
    btn.disabled = true;
    btn.textContent = "Processing...";

    gridWidth = parseInt(document.getElementById("grid-width").value) || 32;
    gridHeight = parseInt(document.getElementById("grid-height").value) || 24;

    const formData = new FormData();
    formData.append("image", uploadedFile);
    formData.append("grid_width", gridWidth);
    formData.append("grid_height", gridHeight);

    try {
        const res = await fetch("/api/upload", { method: "POST", body: formData });
        const data = await res.json();

        if (data.error) { alert("Error: " + data.error); return; }

        // Ground layer from segmentation
        LAYERS.ground.data = data.grid;
        // Other layers start empty
        LAYERS.weather.data = Array.from({ length: gridHeight }, () => Array(gridWidth).fill("none"));
        LAYERS.exits.data = Array.from({ length: gridHeight }, () => Array(gridWidth).fill("none"));
        LAYERS.npc_routes.data = Array.from({ length: gridHeight }, () => Array(gridWidth).fill("none"));
        LAYERS.interactable.data = Array.from({ length: gridHeight }, () => Array(gridWidth).fill("none"));
        LAYERS.decorative.data = Array.from({ length: gridHeight }, () => Array(gridWidth).fill("none"));

        gridWidth = data.width;
        gridHeight = data.height;

        // Load ghost image
        if (data.original) {
            ghostImage = new Image();
            ghostImage.src = "data:image/png;base64," + data.original;
        }

        document.getElementById("editor-section").style.display = "block";
        canvas.width = gridWidth * CELL_SIZE;
        canvas.height = gridHeight * CELL_SIZE;
        render();
    } catch (err) {
        alert("Segmentation failed: " + err.message);
    } finally {
        btn.disabled = false;
        btn.textContent = "Segment";
    }
}

// --- Painting ---

function paint(e) {
    const rect = canvas.getBoundingClientRect();
    const x = Math.floor((e.clientX - rect.left) / CELL_SIZE);
    const y = Math.floor((e.clientY - rect.top) / CELL_SIZE);

    if (x < 0 || x >= gridWidth || y < 0 || y >= gridHeight) return;

    const layerData = LAYERS[activeLayer].data;
    if (!layerData) return;

    if (tool === "fill") {
        floodFill(layerData, x, y, layerData[y][x], selectedLabel);
    } else if (tool === "eraser") {
        layerData[y][x] = activeLayer === "ground" ? "empty" : "none";
    } else {
        layerData[y][x] = selectedLabel;
    }

    render();
}

function floodFill(layerData, startX, startY, targetLabel, newLabel) {
    if (targetLabel === newLabel) return;
    const stack = [[startX, startY]];
    const visited = new Set();

    while (stack.length > 0) {
        const [x, y] = stack.pop();
        const key = `${x},${y}`;
        if (visited.has(key)) continue;
        if (x < 0 || x >= gridWidth || y < 0 || y >= gridHeight) continue;
        if (layerData[y][x] !== targetLabel) continue;

        visited.add(key);
        layerData[y][x] = newLabel;
        stack.push([x + 1, y], [x - 1, y], [x, y + 1], [x, y - 1]);
    }
}

// --- Rendering ---

const LABEL_COLORS = {
    floor: "#c8b48c", wall: "#3c3c3c", water: "#2878b4",
    grass: "#50b450", stone: "#b4b4b4", door: "#c83c3c", empty: "#000000",
    // Weather / Hydrology
    puddle_zone: "#3366aa", drain: "#224488",
    flow_north: "#4499cc", flow_south: "#4499cc",
    flow_east: "#4499cc", flow_west: "#4499cc",
    sheltered: "#665544", exposed: "#5588bb",
    slope_low: "#223366", slope_high: "#88bbdd",
    // Exits
    spawn: "#00ff00", exit_north: "#ffaa00", exit_south: "#ffaa00",
    exit_east: "#ffaa00", exit_west: "#ffaa00",
    // NPC Routes
    npc_waypoint: "#ff66ff", npc_patrol_a: "#ff33aa", npc_patrol_b: "#aa33ff",
    npc_patrol_c: "#3399ff", npc_idle_spot: "#ffaa66", npc_spawn: "#66ffaa",
    // Interactive
    lever: "#cc8800", button: "#cc4400", gate: "#886644",
    npc_talk: "#44ccff", shop: "#ffdd00", save_point: "#88ff88",
    pickup: "#ffaa00", trap: "#ff2222",
    // Deco
    tree: "#2d5a27", rock: "#777777", flower: "#ff88cc",
    torch: "#ffcc00", chest: "#aa6600", sign: "#8b5e3c",
};

const LABEL_ICONS = {
    spawn: "S", exit_north: "\u2191", exit_south: "\u2193",
    exit_east: "\u2192", exit_west: "\u2190",
    tree: "T", rock: "R", flower: "F", torch: "\u{1f525}", chest: "C", sign: "!",
    rain_zone: "~", snow_zone: "*", leaves_zone: "\u{1f342}", wind_zone: ">", fog_zone: "\u2601",
    puddle_zone: "\u25cf", drain: "\u2193",
    flow_north: "\u2191", flow_south: "\u2193", flow_east: "\u2192", flow_west: "\u2190",
    sheltered: "\u2302", exposed: "\u2602", slope_low: "\u25bc", slope_high: "\u25b2",
    // NPC Routes
    npc_waypoint: "\u25cf", npc_patrol_a: "A", npc_patrol_b: "B",
    npc_patrol_c: "C", npc_idle_spot: "\u23f8", npc_spawn: "N",
    // Interactive
    lever: "L", button: "\u25a0", gate: "\u2503",
    npc_talk: "\u{1f4ac}", shop: "$", save_point: "\u2605",
    pickup: "\u2b06", trap: "\u26a0",
};

function render() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Ghost image
    if (showGhost && ghostImage && ghostImage.complete) {
        ctx.globalAlpha = 0.25;
        ctx.drawImage(ghostImage, 0, 0, canvas.width, canvas.height);
        ctx.globalAlpha = 1.0;
    }

    // Render layers bottom to top
    const layerOrder = ["ground", "weather", "npc_routes", "interactable", "decorative", "exits"];
    for (const layerKey of layerOrder) {
        const layer = LAYERS[layerKey];
        if (!layer.visible || !layer.data) continue;

        for (let y = 0; y < gridHeight; y++) {
            for (let x = 0; x < gridWidth; x++) {
                const label = layer.data[y][x];
                if (label === "none" || label === "empty" && layerKey !== "ground") continue;

                const color = LABEL_COLORS[label];
                if (!color) continue;

                if (layerKey === "ground") {
                    ctx.fillStyle = color;
                    ctx.globalAlpha = showGhost ? 0.65 : 1.0;
                    ctx.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE - 1, CELL_SIZE - 1);
                } else {
                    // Overlay layers: semi-transparent with icon/pattern
                    ctx.fillStyle = color;
                    ctx.globalAlpha = 0.4;
                    ctx.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE - 1, CELL_SIZE - 1);
                    ctx.globalAlpha = 1.0;

                    // Draw icon
                    const icon = LABEL_ICONS[label];
                    if (icon) {
                        ctx.fillStyle = "#fff";
                        ctx.font = "bold 9px monospace";
                        ctx.textAlign = "center";
                        ctx.textBaseline = "middle";
                        ctx.fillText(icon, x * CELL_SIZE + CELL_SIZE / 2, y * CELL_SIZE + CELL_SIZE / 2);
                    }
                }
            }
        }
    }
    ctx.globalAlpha = 1.0;

    // Highlight active layer cells with border
    if (LAYERS[activeLayer].data && activeLayer !== "ground") {
        ctx.strokeStyle = "rgba(233, 69, 96, 0.3)";
        ctx.lineWidth = 1;
        for (let y = 0; y < gridHeight; y++) {
            for (let x = 0; x < gridWidth; x++) {
                const label = LAYERS[activeLayer].data[y][x];
                if (label !== "none") {
                    ctx.strokeRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE - 1, CELL_SIZE - 1);
                }
            }
        }
    }

    // Grid lines
    ctx.strokeStyle = "rgba(255,255,255,0.04)";
    ctx.lineWidth = 0.5;
    for (let x = 0; x <= gridWidth; x++) {
        ctx.beginPath(); ctx.moveTo(x * CELL_SIZE, 0); ctx.lineTo(x * CELL_SIZE, canvas.height); ctx.stroke();
    }
    for (let y = 0; y <= gridHeight; y++) {
        ctx.beginPath(); ctx.moveTo(0, y * CELL_SIZE); ctx.lineTo(canvas.width, y * CELL_SIZE); ctx.stroke();
    }
}

// --- Status ---

function updateStatus(e) {
    const rect = canvas.getBoundingClientRect();
    const x = Math.floor((e.clientX - rect.left) / CELL_SIZE);
    const y = Math.floor((e.clientY - rect.top) / CELL_SIZE);

    document.getElementById("status-pos").textContent = `Tile: (${x}, ${y})`;
    const layerData = LAYERS[activeLayer].data;
    if (layerData && y >= 0 && y < gridHeight && x >= 0 && x < gridWidth) {
        document.getElementById("status-label").textContent = `[${LAYERS[activeLayer].name}] ${layerData[y][x]}`;
    }
}

// --- Save / Export ---

async function saveGrid() {
    const payload = {};
    for (const [key, layer] of Object.entries(LAYERS)) {
        if (layer.data) payload[key] = layer.data;
    }
    await fetch("/api/update_grid", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
    });
}

async function doExport() {
    await saveGrid();
    const res = await fetch("/api/export_tmx", { method: "POST" });
    if (!res.ok) { alert("Export failed"); return; }
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "generated_map.tmx";
    a.click();
    URL.revokeObjectURL(url);
}

async function doTeachAI() {
    await saveGrid();
    const res = await fetch("/api/learn", { method: "POST" });
    const data = await res.json();
    if (data.error) { alert(data.error); return; }
    alert(data.message);
}

async function doGenerate() {
    const w = parseInt(document.getElementById("grid-width").value) || 32;
    const h = parseInt(document.getElementById("grid-height").value) || 24;
    
    const res = await fetch("/api/generate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ width: w, height: h }),
    });
    const data = await res.json();
    
    if (data.error) { alert(data.error); return; }
    
    // Load generated layers into editor
    gridWidth = data.width;
    gridHeight = data.height;
    
    for (const [key, layerData] of Object.entries(data.layers)) {
        if (LAYERS[key]) LAYERS[key].data = layerData;
    }
    
    ghostImage = null;
    document.getElementById("editor-section").style.display = "block";
    canvas.width = gridWidth * CELL_SIZE;
    canvas.height = gridHeight * CELL_SIZE;
    render();
}

// --- Load existing maps ---

async function doVisionAnnotate() {
    await saveGrid();
    
    const mode = confirm("Full annotation (hydrology + exits + NPCs + deco)?\n\nOK = Full\nCancel = Hydrology only") ? "full" : "hydrology";
    
    const btn = document.getElementById("btn-vision");
    btn.disabled = true;
    btn.textContent = "Analyzing...";
    
    try {
        const res = await fetch("/api/vision_annotate", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ mode }),
        });
        const data = await res.json();
        
        if (data.error) {
            alert("Vision AI: " + data.error);
            return;
        }
        
        // Merge annotations into current layers
        for (const [key, layerData] of Object.entries(data.annotations)) {
            if (LAYERS[key] && layerData) {
                LAYERS[key].data = layerData;
            }
        }
        
        render();
        alert(`Vision AI annotated ${Object.keys(data.annotations).length} layer(s). Review and correct as needed.`);
    } catch (err) {
        alert("Vision annotation failed: " + err.message);
    } finally {
        btn.disabled = false;
        btn.textContent = "Vision Annotate";
    }
}

async function doFullAuto() {
    const btn = document.getElementById("btn-auto");
    btn.disabled = true;
    const origText = btn.textContent;
    
    const log = (msg) => { btn.textContent = msg; };
    
    try {
        // Step 1: Load all project maps and auto-annotate each
        log("Loading maps...");
        const mapsRes = await fetch("/api/project_maps");
        const maps = await mapsRes.json();
        
        if (maps.length === 0) {
            alert("No project maps found!");
            return;
        }
        
        log(`Processing ${maps.length} maps...`);
        
        const res = await fetch("/api/full_auto", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ 
                maps: maps.map(m => m.path),
                generate_width: parseInt(document.getElementById("grid-width").value) || 32,
                generate_height: parseInt(document.getElementById("grid-height").value) || 24,
            }),
        });
        const data = await res.json();
        
        if (data.error) {
            alert("Full Auto: " + data.error);
            return;
        }
        
        // Load the generated map into the editor
        gridWidth = data.width;
        gridHeight = data.height;
        for (const [key, layerData] of Object.entries(data.layers)) {
            if (LAYERS[key]) LAYERS[key].data = layerData;
        }
        for (const key of Object.keys(LAYERS)) {
            if (!LAYERS[key].data) {
                LAYERS[key].data = Array.from({ length: gridHeight }, () => Array(gridWidth).fill("none"));
            }
        }
        
        ghostImage = null;
        document.getElementById("editor-section").style.display = "block";
        canvas.width = gridWidth * CELL_SIZE;
        canvas.height = gridHeight * CELL_SIZE;
        render();
        
        alert(`Done! Processed ${data.maps_analyzed} maps, learned ${data.rules_extracted} rules, generated a ${gridWidth}x${gridHeight} map.\n\nYou can export directly or tweak if you want.`);
    } catch (err) {
        alert("Full Auto failed: " + err.message);
    } finally {
        btn.disabled = false;
        btn.textContent = origText;
    }
}

async function doLoadProjectMap() {
    const select = document.getElementById("project-maps");
    const path = select.value;
    if (!path) { alert("Select a map first"); return; }
    
    const res = await fetch("/api/load_tmx", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ path }),
    });
    const data = await res.json();
    if (data.error) { alert(data.error); return; }
    
    loadLayersIntoEditor(data);
}

async function doLoadTmxFile() {
    const input = document.getElementById("tmx-input");
    if (!input.files.length) return;
    
    const formData = new FormData();
    formData.append("tmx_file", input.files[0]);
    
    const res = await fetch("/api/load_tmx", { method: "POST", body: formData });
    const data = await res.json();
    if (data.error) { alert(data.error); return; }
    
    loadLayersIntoEditor(data);
}

function loadLayersIntoEditor(data) {
    gridWidth = data.width;
    gridHeight = data.height;
    
    for (const [key, layerData] of Object.entries(data.layers)) {
        if (LAYERS[key]) LAYERS[key].data = layerData;
    }
    
    // Ensure all layers exist
    for (const key of Object.keys(LAYERS)) {
        if (!LAYERS[key].data) {
            LAYERS[key].data = Array.from({ length: gridHeight }, () => Array(gridWidth).fill("none"));
        }
    }
    
    ghostImage = null;
    document.getElementById("editor-section").style.display = "block";
    canvas.width = gridWidth * CELL_SIZE;
    canvas.height = gridHeight * CELL_SIZE;
    render();
}

// --- Boot ---
init();
