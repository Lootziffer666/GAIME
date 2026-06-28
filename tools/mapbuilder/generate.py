#!/usr/bin/env python3
"""
GAIME Map Generator — Agent-ready CLI.

Zero-interaction map generation. Feed it your project maps, get new ones.
Designed to be called by Codex, Claude Code, Cursor, or any coding agent.

Usage:
    # Generate one map (default 32x24):
    python3 generate.py --output assets/HD/locations/generated/tavern.tmx

    # Custom size:
    python3 generate.py --width 40 --height 30 --output new_map.tmx

    # Generate 5 maps at once:
    python3 generate.py --count 5 --output-dir assets/HD/locations/generated/

    # Use a specific seed (reproducible):
    python3 generate.py --seed 42 --output test.tmx

    # Only learn (save rules without generating):
    python3 generate.py --learn-only

    # Generate from previously saved rules (skip re-analysis):
    python3 generate.py --skip-learn --output fast.tmx

    # With Vision LLM annotation (needs OPENAI_API_KEY or ANTHROPIC_API_KEY):
    python3 generate.py --vision --output smart.tmx
"""

import argparse
import sys
import time
from pathlib import Path

# Add this directory to path for local imports
sys.path.insert(0, str(Path(__file__).parent))

from learn import MapLearner, MapGenerator
from tmx_loader import parse_tmx_to_layers
from export_tmx import export_tmx

REPO_ROOT = Path(__file__).parent.parent.parent
LOCATIONS_DIR = REPO_ROOT / "assets" / "HD" / "locations"
RULES_PATH = Path(__file__).parent / "learned_rules.json"


def find_project_maps() -> list[Path]:
    """Find all TMX maps in the project."""
    if not LOCATIONS_DIR.exists():
        return []
    return sorted(LOCATIONS_DIR.rglob("*.tmx"))


def learn_from_maps(maps: list[Path], use_vision: bool = False) -> MapLearner:
    """Parse all maps and extract rules."""
    learner = MapLearner()
    
    for tmx_path in maps:
        try:
            content = tmx_path.read_text()
            layers, w, h = parse_tmx_to_layers(content)
            
            # Vision annotation if requested and available
            if use_vision and w <= 64 and h <= 64:
                try:
                    from vision_annotate import annotate_with_vision
                    annotations = annotate_with_vision(layers, mode="full", backend=_detect_backend())
                    for key, data in annotations.items():
                        layers[key] = data
                except Exception:
                    pass
            else:
                # Heuristic hydrology
                gen = MapGenerator(MapLearner())
                layers["weather"] = gen._auto_hydrology(layers["ground"], w, h)
            
            learner.learn(layers)
            print(f"  OK: {tmx_path.relative_to(REPO_ROOT)} ({w}x{h})")
        except Exception as e:
            print(f"  SKIP: {tmx_path.name} ({e})")
    
    return learner


def _detect_backend() -> str:
    import os
    if os.environ.get("OPENAI_API_KEY"):
        return "openai"
    if os.environ.get("ANTHROPIC_API_KEY"):
        return "anthropic"
    try:
        import urllib.request
        urllib.request.urlopen("http://localhost:11434/api/tags", timeout=2)
        return "ollama"
    except Exception:
        return ""


def main():
    parser = argparse.ArgumentParser(
        description="GAIME Map Generator — agent-ready, zero-interaction.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument("--output", "-o", type=str, help="Output TMX path (single map)")
    parser.add_argument("--output-dir", type=str, help="Output directory (for --count > 1)")
    parser.add_argument("--width", "-W", type=int, default=32, help="Map width in tiles (default: 32)")
    parser.add_argument("--height", "-H", type=int, default=24, help="Map height in tiles (default: 24)")
    parser.add_argument("--count", "-n", type=int, default=1, help="Number of maps to generate")
    parser.add_argument("--seed", "-s", type=int, default=None, help="Random seed (reproducible)")
    parser.add_argument("--vision", action="store_true", help="Use Vision LLM for annotation")
    parser.add_argument("--learn-only", action="store_true", help="Only learn rules, don't generate")
    parser.add_argument("--skip-learn", action="store_true", help="Use saved rules, skip re-analysis")
    parser.add_argument("--quiet", "-q", action="store_true", help="Minimal output")
    args = parser.parse_args()
    
    # Default output
    if not args.output and not args.output_dir and not args.learn_only:
        args.output = "generated_map.tmx"
    
    log = (lambda msg: None) if args.quiet else print
    
    # --- LEARN ---
    if args.skip_learn and RULES_PATH.exists():
        log(f"Loading saved rules from {RULES_PATH.name}...")
        learner = MapLearner()
        learner.load(str(RULES_PATH))
    else:
        maps = find_project_maps()
        if not maps:
            print("ERROR: No TMX maps found in assets/HD/locations/", file=sys.stderr)
            sys.exit(1)
        
        log(f"Analyzing {len(maps)} maps...")
        t0 = time.time()
        learner = learn_from_maps(maps, use_vision=args.vision)
        elapsed = time.time() - t0
        
        rules = sum(
            sum(len(n) for n in dirs.values())
            for tiles in learner.adjacency.values()
            for tile, dirs in tiles.items()
        )
        log(f"Learned: {len(learner.frequency.get('ground', {}))} tile types, {rules} rules ({elapsed:.1f}s)")
        
        # Save rules
        learner.save(str(RULES_PATH))
        log(f"Rules saved to {RULES_PATH.name}")
    
    if args.learn_only:
        log("Done (learn only).")
        return
    
    # --- GENERATE ---
    if args.count > 1:
        out_dir = Path(args.output_dir or "generated")
        out_dir.mkdir(parents=True, exist_ok=True)
        
        for i in range(args.count):
            seed = (args.seed + i) if args.seed is not None else None
            gen = MapGenerator(learner, seed=seed)
            layers = gen.generate(width=args.width, height=args.height)
            
            out_path = out_dir / f"map_{i+1:03d}.tmx"
            export_tmx(layers, str(out_path))
            log(f"  [{i+1}/{args.count}] {out_path}")
        
        log(f"Generated {args.count} maps in {out_dir}/")
    else:
        gen = MapGenerator(learner, seed=args.seed)
        layers = gen.generate(width=args.width, height=args.height)
        
        out_path = Path(args.output)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        export_tmx(layers, str(out_path))
        log(f"Generated: {out_path} ({args.width}x{args.height})")


if __name__ == "__main__":
    main()
