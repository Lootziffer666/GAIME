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
    weather: { name: "Weather Zones", visible: true, data: null },
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
    { id: "rain_zone", name: "Rain", color: "#4488cc", pattern: "///" },
    { id: "snow_zone", name: "Snow", color: "#ccddff", pattern: "***" },
    { id: "leaves_zone", name: "Leaves", color: "#cc8833", pattern: "~~~" },
    { id: "wind_zone", name: "Wind", color: "#88ccaa", pattern: ">>>" },
    { id: "fog_zone", name: "Fog", color: "#999999", pattern: "..." },
    { id: "none", name: "Clear", color: "transparent" },
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
    // Weather
    rain_zone: "#4488cc", snow_zone: "#ccddff", leaves_zone: "#cc8833",
    wind_zone: "#88ccaa", fog_zone: "#999999",
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

// --- Boot ---
init();
