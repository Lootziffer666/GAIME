#Requires -Version 5.1
<#
.SYNOPSIS
  Collect exactly the assets GAIME needs from a large asset library into a tidy
  staging folder, ready to drop into the repo / upload.

.DESCRIPTION
  Walks the source drive/folder once and, using the name patterns confirmed in
  treekram.txt, copies only the relevant art/audio (HD character parts, RPG
  top-down characters, enemies, tiles, audio/music, UI, fonts; optionally FX,
  backgrounds) into <Dest>\<category>\... preserving the relative path so
  duplicates (e.g. several "Parts HD" folders) never collide. Irrelevant stuff
  (3D models, Unity/Construct projects, console button prompts, normal/UV maps,
  previews) is skipped. Writes a manifest and a size summary.

.EXAMPLE
  # Preview first (nothing is copied):
  powershell -ExecutionPolicy Bypass -File .\gather-assets.ps1 -Source 'W:\' -DryRun

.EXAMPLE
  # Do it (P0 set into your home folder):
  powershell -ExecutionPolicy Bypass -File .\gather-assets.ps1 -Source 'W:\'

.EXAMPLE
  # Include P1 (FX, backgrounds) too:
  powershell -ExecutionPolicy Bypass -File .\gather-assets.ps1 -Source 'W:\' -IncludeP1
#>

[CmdletBinding()]
param(
    # Root folder to search. Default: the W: drive seen in treekram.txt.
    [string]$Source = 'W:\',
    # Where to assemble the curated assets.
    [string]$Dest = (Join-Path $HOME 'gaime-assets'),
    # Show what would be copied without copying.
    [switch]$DryRun,
    # Also gather P1 (effect-storm FX, parallax backgrounds).
    [switch]$IncludeP1,
    # Skip any single file larger than this (MB) — guards against stray huge files.
    [int]$MaxFileMB = 25
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $Source)) {
    Write-Error "Source '$Source' not found. Pass -Source 'X:\path\to\assets'."
    return
}

# ----- Exclusions (folder/file path fragments we never want) -------------------
$exclude = @(
    'models', 'unity', 'construct', 'normal map', 'uv texture', 'vector',
    'previews', '\preview', 'samples', '\sample', 'nintendo', 'playstation',
    'xbox', 'steam deck', 'steam controller', 'steam frame', 'valve index',
    'meta quest', 'playdate', 'keyboard & mouse', 'gamecube', '\wii', 'touch',
    'spritesheets only', '.fbx', '.obj', '.blend', '.psd', '.aseprite'
)

# ----- Rules: first match wins. Each: a regex over the full path, allowed
#       extensions, and the destination category. -------------------------------
$rules = @(
    @{ Name = 'characters_hd';   Pattern = '(parts hd|poses hd)';                         Ext = @('.png');         Into = 'characters_hd' }
    @{ Name = 'party_chars';     Pattern = '(adventurer|\\characters?\\|character[-_])';  Ext = @('.png');         Into = 'characters' }
    @{ Name = 'enemies';         Pattern = '(enemy|monster|zombie|skeleton|\borc\b|ghost|slime|vampire)'; Ext = @('.png'); Into = 'enemies' }
    @{ Name = 'audio_music';     Pattern = '(\\audio\\|\\music\\|soundtrack)';            Ext = @('.ogg','.mp3','.wav'); Into = 'audio_music' }
    @{ Name = 'ui';              Pattern = '(\\ui\\|interface|\bhud\b|flair|overlay)';    Ext = @('.png');         Into = 'ui' }
    @{ Name = 'fonts';           Pattern = '(font)';                                       Ext = @('.ttf','.otf','.fnt','.png'); Into = 'fonts' }
    @{ Name = 'tiles';           Pattern = '(topdown|tilesheet|tileset|\\tiles?\\|rpg)';  Ext = @('.png','.tsx','.tmx','.tmj','.json'); Into = 'tiles' }
)
if ($IncludeP1) {
    $rules += @{ Name = 'fx';          Pattern = '(particle|effect|\bfx\b|smoke|spark|glow|flash|explos)'; Ext = @('.png'); Into = 'fx' }
    $rules += @{ Name = 'backgrounds'; Pattern = '(background|parallax|skybox|\bsky\b)';  Ext = @('.png'); Into = 'backgrounds' }
}

$srcFull = (Resolve-Path -LiteralPath $Source).Path.TrimEnd('\')
Write-Host "Scanning $srcFull ..." -ForegroundColor Cyan

$counts   = @{}
$bytes    = @{}
$manifest = New-Object System.Collections.Generic.List[string]
$maxBytes = $MaxFileMB * 1MB

Get-ChildItem -LiteralPath $srcFull -Recurse -File -ErrorAction SilentlyContinue | ForEach-Object {
    $file = $_
    $lower = $file.FullName.ToLowerInvariant()

    foreach ($x in $exclude) { if ($lower.Contains($x)) { return } }
    if ($file.Length -gt $maxBytes) { return }

    foreach ($rule in $rules) {
        if ($file.Extension.ToLowerInvariant() -notin $rule.Ext) { continue }
        if ($lower -notmatch $rule.Pattern) { continue }

        # relative path from source, placed under the category folder
        $rel = $file.FullName.Substring($srcFull.Length).TrimStart('\')
        $target = Join-Path (Join-Path $Dest $rule.Into) $rel

        $counts[$rule.Into] = ($counts[$rule.Into] + 1)
        $bytes[$rule.Into]  = ($bytes[$rule.Into]  + $file.Length)
        $manifest.Add("$($rule.Into)`t$($file.FullName)")

        if (-not $DryRun) {
            $dir = Split-Path -Parent $target
            if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
            Copy-Item -LiteralPath $file.FullName -Destination $target -Force
        }
        break  # first matching rule wins
    }
}

Write-Host ""
Write-Host ("=" * 60)
Write-Host ($(if ($DryRun) {"DRY RUN — would collect:"} else {"Collected into $Dest :"})) -ForegroundColor Green
$total = 0L
foreach ($k in ($counts.Keys | Sort-Object)) {
    $mb = [math]::Round($bytes[$k] / 1MB, 1)
    $total += $bytes[$k]
    "{0,-16} {1,6} files  {2,8} MB" -f $k, $counts[$k], $mb | Write-Host
}
Write-Host ("-" * 60)
"{0,-16} {1,6} files  {2,8} MB" -f 'TOTAL', ($counts.Values | Measure-Object -Sum).Sum, [math]::Round($total/1MB,1) | Write-Host
Write-Host ("=" * 60)

if (-not $DryRun) {
    if (-not (Test-Path -LiteralPath $Dest)) { New-Item -ItemType Directory -Path $Dest -Force | Out-Null }
    $manifest | Set-Content -LiteralPath (Join-Path $Dest 'MANIFEST.txt') -Encoding UTF8
    Write-Host "Manifest written to $Dest\MANIFEST.txt" -ForegroundColor Cyan
    Write-Host "Next: zip '$Dest' and upload it, or point me at it." -ForegroundColor Cyan
} else {
    Write-Host "Re-run without -DryRun to actually copy." -ForegroundColor Yellow
}
