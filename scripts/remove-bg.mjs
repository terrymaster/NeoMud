#!/usr/bin/env node
/**
 * Removes fake "checkerboard transparency" backgrounds from AI-generated sprites.
 *
 * AI models render a visual checkerboard instead of actual alpha. This script:
 * 1. Samples corner regions to learn the actual background color(s)
 * 2. Flood-fills from edges, matching only pixels close to those sampled colors
 * 3. Applies a small alpha erosion to clean up fringing
 *
 * Usage: node scripts/remove-bg.mjs <input.webp> [output.webp]
 *        node scripts/remove-bg.mjs --batch <directory> [--ext webp]
 */

import sharp from 'sharp';
import { readdir, readFile, writeFile } from 'fs/promises';
import { join, extname, basename } from 'path';

const COLOR_TOLERANCE = 30;   // max distance from sampled bg colors to count as bg
const CORNER_SAMPLE = 8;      // sample NxN pixels from each corner
const FRINGE_PASSES = 1;      // erosion passes to clean edges
const FRINGE_THRESHOLD = 220; // only erode fringe pixels lighter than this
const ALPHA_CLAMP_THRESHOLD = 10; // pixels with alpha <= this are clamped to fully transparent

async function removeBackground(inputPath, outputPath) {
  // Read file into buffer first to release file handle (Windows lock issue)
  const inputBuffer = await readFile(inputPath);
  const image = sharp(inputBuffer);

  // Get raw RGBA pixel data
  const { data, info } = await image
    .ensureAlpha()
    .raw()
    .toBuffer({ resolveWithObject: true });

  const w = info.width;
  const h = info.height;
  const pixels = new Uint8Array(data.buffer, data.byteOffset, data.length);

  // Pre-pass: clamp near-transparent pixels to fully transparent.
  // AI models often produce "ghost" pixels with alpha 1-10 that still show
  // checkerboard RGB values — these need to be zeroed out.
  let clampedCount = 0;
  for (let i = 0; i < w * h; i++) {
    const a = pixels[i * 4 + 3];
    if (a > 0 && a <= ALPHA_CLAMP_THRESHOLD) {
      pixels[i * 4] = 0;
      pixels[i * 4 + 1] = 0;
      pixels[i * 4 + 2] = 0;
      pixels[i * 4 + 3] = 0;
      clampedCount++;
    }
  }

  // Sample corner regions to determine background colors
  const bgColors = sampleBackgroundColors(pixels, w, h);

  if (bgColors.length === 0) {
    if (clampedCount > 0) {
      // No flood-fill needed, but we did clamp ghost pixels — still write output
      const out = outputPath || inputPath;
      const ext = extname(out).toLowerCase();
      let pipeline = sharp(Buffer.from(pixels.buffer), {
        raw: { width: w, height: h, channels: 4 }
      });
      let outBuffer;
      if (ext === '.webp') {
        outBuffer = await pipeline.webp({ lossless: true }).toBuffer();
      } else if (ext === '.png') {
        outBuffer = await pipeline.png().toBuffer();
      } else {
        outBuffer = await pipeline.webp({ lossless: true }).toBuffer();
      }
      await writeFile(out, outBuffer);
      const pct = ((clampedCount / (w * h)) * 100).toFixed(1);
      console.log(`  ✓ ${basename(out)} (${pct}% ghost pixels clamped to transparent)`);
      return;
    }
    console.log(`  - ${basename(inputPath)} (no clear background detected, skipped)`);
    return;
  }

  // Check if pixel matches any sampled background color within tolerance
  function isBackground(idx) {
    const r = pixels[idx];
    const g = pixels[idx + 1];
    const b = pixels[idx + 2];
    for (const bg of bgColors) {
      const dist = Math.abs(r - bg.r) + Math.abs(g - bg.g) + Math.abs(b - bg.b);
      if (dist <= COLOR_TOLERANCE) return true;
    }
    return false;
  }

  // Flood fill from edges to find connected background regions
  const visited = new Uint8Array(w * h);
  const bgMask = new Uint8Array(w * h);
  const queue = [];

  // Seed from all edge pixels that match background
  for (let x = 0; x < w; x++) {
    const topIdx = x * 4;
    if (isBackground(topIdx)) { queue.push(x); visited[x] = 1; }
    const botPos = (h - 1) * w + x;
    if (isBackground(botPos * 4) && !visited[botPos]) { queue.push(botPos); visited[botPos] = 1; }
  }
  for (let y = 0; y < h; y++) {
    const leftPos = y * w;
    if (isBackground(leftPos * 4) && !visited[leftPos]) { queue.push(leftPos); visited[leftPos] = 1; }
    const rightPos = y * w + (w - 1);
    if (isBackground(rightPos * 4) && !visited[rightPos]) { queue.push(rightPos); visited[rightPos] = 1; }
  }

  // BFS flood fill
  while (queue.length > 0) {
    const pos = queue.shift();
    bgMask[pos] = 1;

    const x = pos % w;
    const y = Math.floor(pos / w);

    if (x > 0 && !visited[pos - 1] && isBackground((pos - 1) * 4)) { visited[pos - 1] = 1; queue.push(pos - 1); }
    if (x < w - 1 && !visited[pos + 1] && isBackground((pos + 1) * 4)) { visited[pos + 1] = 1; queue.push(pos + 1); }
    if (y > 0 && !visited[pos - w] && isBackground((pos - w) * 4)) { visited[pos - w] = 1; queue.push(pos - w); }
    if (y < h - 1 && !visited[pos + w] && isBackground((pos + w) * 4)) { visited[pos + w] = 1; queue.push(pos + w); }
  }

  // Apply background mask
  let removedCount = 0;
  for (let i = 0; i < w * h; i++) {
    if (bgMask[i]) {
      const idx = i * 4;
      pixels[idx] = 0;
      pixels[idx + 1] = 0;
      pixels[idx + 2] = 0;
      pixels[idx + 3] = 0;
      removedCount++;
    }
  }

  // Alpha erosion to clean up fringing at edges
  for (let pass = 0; pass < FRINGE_PASSES; pass++) {
    const alphaSnapshot = new Uint8Array(w * h);
    for (let i = 0; i < w * h; i++) {
      alphaSnapshot[i] = pixels[i * 4 + 3];
    }

    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        const pos = y * w + x;
        if (alphaSnapshot[pos] === 0) continue;

        let bordersTrans = false;
        if (x > 0 && alphaSnapshot[pos - 1] === 0) bordersTrans = true;
        if (x < w - 1 && alphaSnapshot[pos + 1] === 0) bordersTrans = true;
        if (y > 0 && alphaSnapshot[pos - w] === 0) bordersTrans = true;
        if (y < h - 1 && alphaSnapshot[pos + w] === 0) bordersTrans = true;

        if (bordersTrans) {
          const idx = pos * 4;
          const r = pixels[idx], g = pixels[idx + 1], b = pixels[idx + 2];
          if (r >= FRINGE_THRESHOLD && g >= FRINGE_THRESHOLD && b >= FRINGE_THRESHOLD) {
            pixels[idx + 3] = 0;
          }
        }
      }
    }
  }

  // Write output
  const out = outputPath || inputPath;
  const ext = extname(out).toLowerCase();

  let pipeline = sharp(Buffer.from(pixels.buffer), {
    raw: { width: w, height: h, channels: 4 }
  });

  let outBuffer;
  if (ext === '.webp') {
    outBuffer = await pipeline.webp({ lossless: true }).toBuffer();
  } else if (ext === '.png') {
    outBuffer = await pipeline.png().toBuffer();
  } else {
    outBuffer = await pipeline.webp({ lossless: true }).toBuffer();
  }

  await writeFile(out, outBuffer);

  const totalCleaned = removedCount + clampedCount;
  const pct = ((totalCleaned / (w * h)) * 100).toFixed(1);
  console.log(`  ✓ ${basename(out)} (${pct}% background removed${clampedCount > 0 ? `, ${clampedCount} ghost pixels clamped` : ''})`);
}

/**
 * Sample corner regions to find the dominant background color(s).
 * Returns an array of {r,g,b} colors that represent the background.
 * For checkerboard patterns, this typically returns two colors.
 */
function sampleBackgroundColors(pixels, w, h) {
  const n = CORNER_SAMPLE;
  const colorCounts = new Map(); // "r,g,b" -> count

  // Sample from all four corners
  const corners = [
    { x0: 0, y0: 0 },                     // top-left
    { x0: w - n, y0: 0 },                  // top-right
    { x0: 0, y0: h - n },                  // bottom-left
    { x0: w - n, y0: h - n },              // bottom-right
  ];

  for (const { x0, y0 } of corners) {
    for (let dy = 0; dy < n; dy++) {
      for (let dx = 0; dx < n; dx++) {
        const x = x0 + dx;
        const y = y0 + dy;
        if (x >= w || y >= h) continue;
        const idx = (y * w + x) * 4;
        // Quantize to reduce noise (round to nearest 4)
        const r = (pixels[idx] >> 2) << 2;
        const g = (pixels[idx + 1] >> 2) << 2;
        const b = (pixels[idx + 2] >> 2) << 2;
        const key = `${r},${g},${b}`;
        colorCounts.set(key, (colorCounts.get(key) || 0) + 1);
      }
    }
  }

  // Sort by frequency, take top colors that account for most corner pixels
  const sorted = [...colorCounts.entries()].sort((a, b) => b[1] - a[1]);
  const totalSampled = n * n * 4;
  const bgColors = [];
  let accounted = 0;

  for (const [key, count] of sorted) {
    if (accounted >= totalSampled * 0.8) break; // stop once we've accounted for 80%
    if (bgColors.length >= 3) break; // at most 3 bg colors
    const [r, g, b] = key.split(',').map(Number);
    // Only consider light colors as potential background (R,G,B all >= 180)
    if (r >= 180 && g >= 180 && b >= 180) {
      bgColors.push({ r, g, b });
      accounted += count;
    } else {
      // If corners contain dark pixels, the background isn't light
      // Only skip if this dark color is dominant
      if (count > totalSampled * 0.3) {
        return []; // dark background — don't process
      }
    }
  }

  return bgColors;
}

async function processDirectory(dir, ext = 'webp') {
  const files = await readdir(dir);
  const targets = files.filter(f => f.endsWith(`.${ext}`) && !f.startsWith('.'));

  console.log(`Processing ${targets.length} .${ext} files in ${dir}...`);

  for (const file of targets) {
    const fullPath = join(dir, file);
    try {
      await removeBackground(fullPath, fullPath);
    } catch (err) {
      console.error(`  ✗ ${file}: ${err.message}`);
    }
  }
}

// CLI
const args = process.argv.slice(2);

if (args[0] === '--batch') {
  const dir = args[1];
  const ext = args[2] === '--ext' ? args[3] : 'webp';
  await processDirectory(dir, ext);
} else if (args.length >= 1) {
  await removeBackground(args[0], args[1] || args[0]);
} else {
  console.log('Usage:');
  console.log('  node scripts/remove-bg.mjs <input> [output]');
  console.log('  node scripts/remove-bg.mjs --batch <directory> [--ext webp]');
}
