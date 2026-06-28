/**
 * GAIME Map Builder — Interactive Tile Painter
 * 
 * Canvas-based editor for painting/correcting tile labels.
 * Supports: brush, fill (flood-fill), eraser, ghost image overlay.
 */

const CELL_SIZE = 16;
let grid = null;
let gridWidth = 32;
let gridHeight = 24;
let labels = [];
let selectedLabel = "floor";
let tool = "brush"; // brush | fill | eraser
let showGhost = true;
let ghostImage = null;
let painting = false;

const canvas = document.getElementById("map-canvas");
const ctx = canvas.getContext("2d");

// --- Init ---

async function init() {
    const res = await fetch("/api/labels");
    labels = await res.json();
    buildPalette();
    setupEvents();
}

function buildPalette() {
    const palette = document.getElementById("palette");
    palette.innerHTML = "";
    labels.forEach(l => {
        const swatch = document.createElement("div");
        swatch.className = "swatch" + (l.id === selectedLabel ? " active" : "");
        swatch.style.background = l.color;
        swatch.title = l.name;
        swatch.dataset.label = l.id;
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
    // File upload
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
    
    // Tools
    document.getElementById("btn-brush").onclick = () => setTool("brush");
    document.getElementById("btn-fill").onclick = () => setTool("fill");
    document.getElementById("btn-eraser").onclick = () => setTool("eraser");
    document.getElementById("toggle-ghost").onchange = e => {
        showGhost = e.target.checked;
        render();
    };
    
    // Canvas painting
    canvas.onmousedown = e => { painting = true; paint(e); };
    canvas.onmousemove = e => { updateStatus(e); if (painting) paint(e); };
    canvas.onmouseup = () => { painting = false; saveGrid(); };
    canvas.onmouseleave = () => { painting = false; };
    
    // Keyboard shortcuts
    document.onkeydown = e => {
        if (e.key === "b") setTool("brush");
        if (e.key === "f") setTool("fill");
        if (e.key === "e") setTool("eraser");
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
        
        if (data.error) {
            alert("Error: " + data.error);
            return;
        }
        
        grid = data.grid;
        gridWidth = data.width;
        gridHeight = data.height;
        
        // Load ghost image
        if (data.original) {
            ghostImage = new Image();
            ghostImage.src = "data:image/png;base64," + data.original;
        }
        
        // Show editor
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
    
    if (tool === "fill") {
        floodFill(x, y, grid[y][x], selectedLabel);
    } else if (tool === "eraser") {
        grid[y][x] = "empty";
    } else {
        grid[y][x] = selectedLabel;
    }
    
    render();
}

function floodFill(startX, startY, targetLabel, newLabel) {
    if (targetLabel === newLabel) return;
    
    const stack = [[startX, startY]];
    const visited = new Set();
    
    while (stack.length > 0) {
        const [x, y] = stack.pop();
        const key = `${x},${y}`;
        if (visited.has(key)) continue;
        if (x < 0 || x >= gridWidth || y < 0 || y >= gridHeight) continue;
        if (grid[y][x] !== targetLabel) continue;
        
        visited.add(key);
        grid[y][x] = newLabel;
        
        stack.push([x + 1, y], [x - 1, y], [x, y + 1], [x, y - 1]);
    }
}

// --- Rendering ---

const LABEL_COLORS = {
    floor: "#c8b48c",
    wall: "#3c3c3c",
    water: "#2878b4",
    grass: "#50b450",
    stone: "#b4b4b4",
    door: "#c83c3c",
    empty: "#000000",
};

function render() {
    if (!grid) return;
    
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    
    // Ghost image (photo underneath)
    if (showGhost && ghostImage && ghostImage.complete) {
        ctx.globalAlpha = 0.3;
        ctx.drawImage(ghostImage, 0, 0, canvas.width, canvas.height);
        ctx.globalAlpha = 1.0;
    }
    
    // Draw tiles
    for (let y = 0; y < gridHeight; y++) {
        for (let x = 0; x < gridWidth; x++) {
            const label = grid[y][x];
            const color = LABEL_COLORS[label] || "#808080";
            
            ctx.fillStyle = color;
            ctx.globalAlpha = showGhost ? 0.7 : 1.0;
            ctx.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE - 1, CELL_SIZE - 1);
        }
    }
    ctx.globalAlpha = 1.0;
    
    // Grid lines (subtle)
    ctx.strokeStyle = "rgba(255,255,255,0.05)";
    ctx.lineWidth = 0.5;
    for (let x = 0; x <= gridWidth; x++) {
        ctx.beginPath();
        ctx.moveTo(x * CELL_SIZE, 0);
        ctx.lineTo(x * CELL_SIZE, canvas.height);
        ctx.stroke();
    }
    for (let y = 0; y <= gridHeight; y++) {
        ctx.beginPath();
        ctx.moveTo(0, y * CELL_SIZE);
        ctx.lineTo(canvas.width, y * CELL_SIZE);
        ctx.stroke();
    }
}

// --- Status ---

function updateStatus(e) {
    const rect = canvas.getBoundingClientRect();
    const x = Math.floor((e.clientX - rect.left) / CELL_SIZE);
    const y = Math.floor((e.clientY - rect.top) / CELL_SIZE);
    
    document.getElementById("status-pos").textContent = `Tile: (${x}, ${y})`;
    if (grid && y >= 0 && y < gridHeight && x >= 0 && x < gridWidth) {
        document.getElementById("status-label").textContent = `Label: ${grid[y][x]}`;
    }
}

// --- Save / Export ---

async function saveGrid() {
    if (!grid) return;
    await fetch("/api/update_grid", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ grid }),
    });
}

async function doExport() {
    if (!grid) return;
    
    // Save current state first
    await saveGrid();
    
    // Trigger download
    const res = await fetch("/api/export_tmx", { method: "POST" });
    if (!res.ok) {
        alert("Export failed");
        return;
    }
    
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
