#!/usr/bin/env node
/**
 * GAIME Shader Editor — Iterative Visual Tuning
 * Loads village scene + material map into editor, renders shader output,
 * captures canvas screenshot, analyzes with Python (histogram, delta to target),
 * adjusts sliders, and repeats until visual quality converges.
 */
import puppeteer from 'puppeteer';
import { readFile, writeFile, mkdir } from 'fs/promises';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';
import { execSync } from 'child_process';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '../..');
const OUT = resolve(__dirname, 'iterations');

// Asset paths (relative to repo root)
const SCENE = resolve(ROOT, '1782823262240.png');
const TARGET = resolve(ROOT, '1782823374309.png');
const MATMAP = resolve(ROOT, '1782824829119.png');
const FLOWMAP = resolve(ROOT, '1782824578280.png');

async function run() {
  await mkdir(OUT, { recursive: true });

  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox',
           '--use-gl=angle', '--use-angle=swiftshader',
           '--enable-webgl', '--ignore-gpu-blocklist',
           '--disable-dev-shm-usage'],
  });

  const results = [];

  // Test v4.7 with asset set
  console.log('\n========== v4.7 CODEX ==========');
  const r47 = await testEditor(browser, 'v4.7_codex.html', 'v47', {
    hasAssetSets: true,
    sliderSets: [
      { name: 'dry', sliders: {'s-w':'0'} },
      { name: 'storm_preset', sliders: {'s-w':'76','s-wind':'38','s-shad':'24','s-pud':'84','s-rain':'8','s-riv':'46','s-fog':'24','s-age':'10','s-moss':'72','s-inv':'60'} },
      { name: 'heavy_storm', sliders: {'s-w':'95','s-wind':'80','s-pud':'100','s-rain':'90','s-riv':'80','s-fog':'70'} },
      { name: 'light_mist', sliders: {'s-w':'30','s-wind':'15','s-pud':'20','s-rain':'5','s-fog':'40','s-age':'0'} },
    ]
  });
  results.push(...r47);

  // Test v4.8 with time-of-day scenarios
  console.log('\n========== v4.8 DAY/NIGHT ==========');
  const r48 = await testEditor(browser, 'v4.8_tag_nacht.html', 'v48', {
    hasAssetSets: false,
    sliderSets: [
      { name: 'noon_clear', sliders: {'s-timeofday':'1300','s-rain':'0','s-lightning':'0','s-rollingfog':'0','s-wind':'30','s-season':'50'} },
      { name: 'midnight', sliders: {'s-timeofday':'0','s-rain':'0','s-lightning':'0','s-rollingfog':'0'} },
      { name: 'dawn_fog', sliders: {'s-timeofday':'600','s-rollingfog':'70','s-rain':'0','s-season':'0'} },
      { name: 'dusk_storm', sliders: {'s-timeofday':'1950','s-rain':'80','s-lightning':'70','s-wind':'60','s-rollingfog':'30'} },
      { name: 'winter_noon', sliders: {'s-timeofday':'1200','s-season':'100','s-cold':'80','s-rain':'0'} },
      { name: 'autumn_dusk', sliders: {'s-timeofday':'1800','s-season':'100','s-rollingfog':'50','s-rain':'10'} },
    ]
  });
  results.push(...r48);

  // Test v5.0 with atomic controls
  console.log('\n========== v5.0 ATOMIC ==========');
  const r50 = await testEditor(browser, 'v5.0_weather_atomic.html', 'v50', {
    hasAssetSets: false,
    sliderSets: [
      { name: 'dry_noon', sliders: {'s-timeofday':'1300','s-pudDepth':'0','s-wetness':'0','s-rain':'0'} },
      { name: 'puddles_only', sliders: {'s-timeofday':'1300','s-pudDepth':'80','s-pudSpread':'70','s-pudRipple':'60','s-wetness':'0','s-rain':'0'} },
      { name: 'wet_no_puddles', sliders: {'s-timeofday':'1300','s-pudDepth':'0','s-wetness':'90','s-wetGloss':'70','s-wetDarken':'50','s-rain':'0'} },
      { name: 'rain_no_wet', sliders: {'s-timeofday':'1300','s-pudDepth':'0','s-wetness':'0','s-rain':'80','s-wind':'50'} },
      { name: 'full_storm', sliders: {'s-timeofday':'0','s-pudDepth':'90','s-pudSpread':'80','s-wetness':'80','s-rain':'90','s-wind':'70','s-lightning':'60','s-rollingfog':'40'} },
      { name: 'spring_dawn', sliders: {'s-timeofday':'600','s-season':'0','s-wetness':'20','s-rollingfog':'30'} },
      { name: 'match_target', sliders: {'s-timeofday':'1300','s-pudDepth':'70','s-pudSpread':'60','s-wetness':'50','s-wetGloss':'60','s-rain':'30','s-wind':'30','s-rollingfog':'15','s-season':'50'} },
    ]
  });
  results.push(...r50);

  await browser.close();

  // Now analyze all iterations with Python and generate comparison
  console.log('\n========== ANALYZING WITH PYTHON ==========');
  analyzeAll(results);
}

async function testEditor(browser, file, prefix, opts) {
  const page = await browser.newPage();
  await page.setViewport({ width: 1400, height: 900 });
  const filePath = `file://${resolve(__dirname, file)}`;
  await page.goto(filePath, { waitUntil: 'domcontentloaded' });
  await new Promise(r => setTimeout(r, 300));

  // Load images via file input simulation
  const sceneInput = await page.$('#f-scene');
  const matInput = await page.$('#f-mat');

  if (sceneInput) await sceneInput.uploadFile(SCENE);
  await new Promise(r => setTimeout(r, 500));
  if (matInput) await matInput.uploadFile(MATMAP);
  await new Promise(r => setTimeout(r, 500));

  // For v4.7, also load phys1
  if (opts.hasAssetSets) {
    const phys1Input = await page.$('#f-phys1');
    if (phys1Input) await phys1Input.uploadFile(FLOWMAP);
    await new Promise(r => setTimeout(r, 300));
  }

  // Wait for render
  await new Promise(r => setTimeout(r, 1000));

  const results = [];

  for (const scenario of opts.sliderSets) {
    // Set all sliders
    await page.evaluate((sliders) => {
      Object.entries(sliders).forEach(([id, val]) => {
        const el = document.getElementById(id);
        if (el) {
          el.value = val;
          el.dispatchEvent(new Event('input', { bubbles: true }));
        }
      });
    }, scenario.sliders);

    // Wait for 2 render frames
    await new Promise(r => setTimeout(r, 400));

    // Screenshot the canvas only
    const canvasBox = await page.evaluate(() => {
      const c = document.getElementById('gl');
      if (!c) return null;
      const rect = c.getBoundingClientRect();
      return { x: rect.x, y: rect.y, width: rect.width, height: rect.height };
    });

    const fname = `${prefix}_${scenario.name}.png`;
    const fpath = resolve(OUT, fname);

    if (canvasBox && canvasBox.width > 10) {
      await page.screenshot({
        path: fpath,
        clip: { x: canvasBox.x, y: canvasBox.y, width: canvasBox.width, height: canvasBox.height }
      });
      console.log(`  📸 ${fname} (${canvasBox.width}x${canvasBox.height})`);
      results.push({ file: fname, path: fpath, editor: prefix, scenario: scenario.name, sliders: scenario.sliders });
    } else {
      // Fallback: full page screenshot
      await page.screenshot({ path: fpath });
      console.log(`  📸 ${fname} (fullpage fallback)`);
      results.push({ file: fname, path: fpath, editor: prefix, scenario: scenario.name, sliders: scenario.sliders });
    }
  }

  await page.close();
  return results;
}

function analyzeAll(results) {
  // Write analysis script
  const pyScript = `
import sys, json, os
from PIL import Image
import numpy as np

OUT = "${OUT}"
TARGET = "${TARGET}"

target = np.array(Image.open(TARGET).convert('RGB')).astype(np.float32)
th, tw = target.shape[:2]

report = []

for item in json.loads(sys.stdin.read()):
    fpath = item['path']
    if not os.path.exists(fpath):
        report.append({**item, 'error': 'file not found'})
        continue
    
    img = Image.open(fpath).convert('RGB')
    arr = np.array(img).astype(np.float32)
    h, w = arr.shape[:2]
    
    # Basic stats
    mean_rgb = arr.mean(axis=(0,1)).tolist()
    std_rgb = arr.std(axis=(0,1)).tolist()
    luminance = (arr[:,:,0]*0.299 + arr[:,:,1]*0.587 + arr[:,:,2]*0.114)
    mean_lum = float(luminance.mean())
    contrast = float(luminance.std())
    
    # Dark/bright ratio
    dark_pct = float((luminance < 50).sum() / luminance.size * 100)
    bright_pct = float((luminance > 200).sum() / luminance.size * 100)
    
    # Compare to target (resize if needed)
    if h > 10 and w > 10 and h != th:
        tgt_resized = np.array(Image.open(TARGET).convert('RGB').resize((w, h))).astype(np.float32)
    elif h == th and w == tw:
        tgt_resized = target
    else:
        tgt_resized = None
    
    delta_e = None
    if tgt_resized is not None:
        diff = arr - tgt_resized
        delta_e = float(np.sqrt((diff**2).sum(axis=2)).mean())
    
    # Color distribution
    r_dom = float((arr[:,:,0] > arr[:,:,1]).sum() / arr[:,:,0].size * 100)
    g_dom = float((arr[:,:,1] > arr[:,:,0]).sum() / arr[:,:,1].size * 100)
    b_heavy = float((arr[:,:,2] > 150).sum() / arr[:,:,2].size * 100)
    
    # Is it all black/grey?
    is_blank = mean_lum < 5 or contrast < 2
    is_grey = abs(mean_rgb[0] - mean_rgb[1]) < 3 and abs(mean_rgb[1] - mean_rgb[2]) < 3 and contrast < 10
    
    report.append({
        **item,
        'size': f'{w}x{h}',
        'mean_rgb': [round(x,1) for x in mean_rgb],
        'mean_lum': round(mean_lum, 1),
        'contrast': round(contrast, 1),
        'dark_pct': round(dark_pct, 1),
        'bright_pct': round(bright_pct, 1),
        'delta_to_target': round(delta_e, 2) if delta_e else None,
        'r_dominant_pct': round(r_dom, 1),
        'g_dominant_pct': round(g_dom, 1),
        'blue_heavy_pct': round(b_heavy, 1),
        'is_blank': is_blank,
        'is_grey': is_grey,
    })

print(json.dumps(report, indent=2))
`;

  const { writeFileSync, readFileSync } = await import('fs');
  const pyPath = resolve(OUT, 'analyze.py');
  writeFileSync(pyPath, pyScript);

  const input = JSON.stringify(results);
  // Write input to file to avoid shell escaping issues
  const inputPath = resolve(OUT, 'input.json');
  writeFileSync(inputPath, input);
  const output = execSync(`python3 ${pyPath} < ${inputPath}`, {
    encoding: 'utf-8', maxBuffer: 10 * 1024 * 1024
  });

  const analysis = JSON.parse(output);

  // Generate markdown report
  let md = `# Shader Editor — Iterative Rendering Report\n`;
  md += `Generated: ${new Date().toISOString()}\n\n`;
  md += `Target image: \`1782823374309.png\` (1365x768)\n\n`;

  md += `## Rendering Results\n\n`;
  md += `| Editor | Scenario | Size | Mean Lum | Contrast | Delta-E to Target | Status |\n`;
  md += `|--------|----------|------|----------|----------|-------------------|--------|\n`;

  for (const r of analysis) {
    const status = r.is_blank ? '🔴 BLANK' : r.is_grey ? '⚠️ GREY' : r.delta_to_target && r.delta_to_target < 30 ? '✅ CLOSE' : '📊 OK';
    md += `| ${r.editor} | ${r.scenario} | ${r.size || '?'} | ${r.mean_lum || '?'} | ${r.contrast || '?'} | ${r.delta_to_target || 'N/A'} | ${status} |\n`;
  }

  md += `\n## Detailed Analysis\n\n`;
  for (const r of analysis) {
    md += `### ${r.editor} / ${r.scenario}\n`;
    md += `- **File:** \`${r.file}\`\n`;
    md += `- **Mean RGB:** [${r.mean_rgb?.join(', ') || '?'}]\n`;
    md += `- **Luminance:** ${r.mean_lum} (contrast: ${r.contrast})\n`;
    md += `- **Dark pixels:** ${r.dark_pct}% | **Bright:** ${r.bright_pct}%\n`;
    md += `- **Delta-E to target:** ${r.delta_to_target || 'N/A'}\n`;
    if (r.is_blank) md += `- ⚠️ **BLANK RENDER — shader may have failed**\n`;
    if (r.is_grey) md += `- ⚠️ **ALL GREY — no color variation, likely no image loaded**\n`;
    md += `- **Sliders:** \`${JSON.stringify(r.sliders)}\`\n\n`;
  }

  // Find best match to target
  const withDelta = analysis.filter(r => r.delta_to_target && !r.is_blank && !r.is_grey);
  if (withDelta.length) {
    withDelta.sort((a, b) => a.delta_to_target - b.delta_to_target);
    md += `## Best Match to Target\n\n`;
    md += `**${withDelta[0].editor} / ${withDelta[0].scenario}** — Delta-E: ${withDelta[0].delta_to_target}\n`;
    md += `Sliders: \`${JSON.stringify(withDelta[0].sliders)}\`\n\n`;
    md += `### Ranking (closest to target)\n\n`;
    for (let i = 0; i < Math.min(5, withDelta.length); i++) {
      md += `${i+1}. **${withDelta[i].editor}/${withDelta[i].scenario}** — Δ=${withDelta[i].delta_to_target}\n`;
    }
  }

  // Findings
  md += `\n## Issues Found\n\n`;
  const blanks = analysis.filter(r => r.is_blank);
  const greys = analysis.filter(r => r.is_grey);
  if (blanks.length) {
    md += `### 🔴 Blank Renders (shader crash/no output)\n`;
    blanks.forEach(b => md += `- ${b.editor}/${b.scenario}\n`);
    md += `\n`;
  }
  if (greys.length) {
    md += `### ⚠️ Grey/Flat Renders (no image loaded or uniform fallback)\n`;
    greys.forEach(b => md += `- ${b.editor}/${b.scenario}\n`);
    md += `\n`;
  }
  if (!blanks.length && !greys.length) {
    md += `✅ All renders produced meaningful color output.\n\n`;
  }

  writeFileSync(resolve(__dirname, 'ITERATION_REPORT.md'), md);
  console.log(`\n✅ Report: ITERATION_REPORT.md`);
  console.log(`✅ ${analysis.length} screenshots in ./iterations/`);

  if (blanks.length) console.log(`🔴 ${blanks.length} BLANK renders`);
  if (greys.length) console.log(`⚠️  ${greys.length} GREY renders`);
  console.log(`📊 Delta-E range: ${withDelta.length ? withDelta[0].delta_to_target + ' — ' + withDelta[withDelta.length-1].delta_to_target : 'N/A'}`);
}

run().catch(e => { console.error(e); process.exit(1); });
