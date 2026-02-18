#!/usr/bin/env node
// Generate 270 placeholder PC sprite WebP files
// These are minimal valid 1x1 WebP images with a colored pixel

const fs = require('fs');
const path = require('path');

const RACES = ['human', 'dwarf', 'elf', 'halfling', 'gnome', 'half_orc'];
const GENDERS = ['male', 'female', 'neutral'];
const CLASSES = [
  'warrior', 'paladin', 'witchhunter', 'cleric', 'priest', 'missionary',
  'mage', 'warlock', 'druid', 'ranger', 'thief', 'ninja', 'mystic', 'bard', 'gypsy'
];

// Minimal valid WebP file (1x1 pixel, lossy) - 44 bytes
// This is a valid RIFF/WebP container with a VP8 lossy bitstream
const WEBP_HEADER = Buffer.from([
  0x52, 0x49, 0x46, 0x46, // "RIFF"
  0x24, 0x00, 0x00, 0x00, // File size - 8
  0x57, 0x45, 0x42, 0x50, // "WEBP"
  0x56, 0x50, 0x38, 0x20, // "VP8 "
  0x18, 0x00, 0x00, 0x00, // Chunk size
  0x30, 0x01, 0x00, 0x9D, // VP8 bitstream header
  0x01, 0x2A, 0x01, 0x00, // Width=1
  0x01, 0x00, 0x01, 0x40, // Height=1
  0x25, 0xA4, 0x00, 0x03, // Quantization params
  0x70, 0x00, 0xFE, 0xFB, // Color data
  0x94, 0x00, 0x00       // Padding
]);

const outDir = path.join(__dirname, '..', 'server', 'src', 'main', 'resources', 'assets', 'images', 'players');
fs.mkdirSync(outDir, { recursive: true });

let count = 0;
for (const race of RACES) {
  for (const gender of GENDERS) {
    for (const cls of CLASSES) {
      const filename = `${race}_${gender}_${cls}.webp`;
      fs.writeFileSync(path.join(outDir, filename), WEBP_HEADER);
      count++;
    }
  }
}

console.log(`Generated ${count} placeholder sprites in ${outDir}`);
