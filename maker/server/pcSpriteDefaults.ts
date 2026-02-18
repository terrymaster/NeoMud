import fs from 'fs'
import path from 'path'
import type { PrismaClient } from './generated/prisma/client.js'

export const RACES = ['HUMAN', 'DWARF', 'ELF', 'HALFLING', 'GNOME', 'HALF_ORC'] as const
export const GENDERS = ['male', 'female', 'neutral'] as const
export const CLASSES = [
  'WARRIOR', 'PALADIN', 'WITCHHUNTER', 'CLERIC', 'PRIEST', 'MISSIONARY',
  'MAGE', 'WARLOCK', 'DRUID', 'RANGER', 'THIEF', 'NINJA', 'MYSTIC', 'BARD', 'GYPSY',
] as const

export interface PcSpriteCombo {
  id: string
  race: string
  gender: string
  characterClass: string
  imagePrompt: string
  imageStyle: string
  imageNegativePrompt: string
}

export function buildPcSpriteCombos(): PcSpriteCombo[] {
  const combos: PcSpriteCombo[] = []
  for (const race of RACES) {
    for (const gender of GENDERS) {
      for (const cls of CLASSES) {
        const id = `${race.toLowerCase()}_${gender}_${cls.toLowerCase()}`
        combos.push({
          id,
          race,
          gender,
          characterClass: cls,
          imagePrompt: `A ${gender} ${race.toLowerCase().replace('_', ' ')} ${cls.toLowerCase()}, fantasy RPG character portrait, full body, facing forward`,
          imageStyle: 'pixel art, 16-bit JRPG style, transparent background',
          imageNegativePrompt: 'blurry, modern clothing, realistic photo',
        })
      }
    }
  }
  return combos
}

export async function seedPcSprites(prisma: PrismaClient): Promise<void> {
  const combos = buildPcSpriteCombos()
  for (const combo of combos) {
    await prisma.pcSprite.create({
      data: {
        id: combo.id,
        race: combo.race,
        gender: combo.gender,
        characterClass: combo.characterClass,
        imagePrompt: combo.imagePrompt,
        imageStyle: combo.imageStyle,
        imageNegativePrompt: combo.imageNegativePrompt,
      },
    })
  }
}

/** Minimal valid WebP file (1x1 pixel, lossy) - 44 bytes */
const WEBP_PLACEHOLDER = Buffer.from([
  0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00,
  0x57, 0x45, 0x42, 0x50, 0x56, 0x50, 0x38, 0x20,
  0x18, 0x00, 0x00, 0x00, 0x30, 0x01, 0x00, 0x9D,
  0x01, 0x2A, 0x01, 0x00, 0x01, 0x00, 0x01, 0x40,
  0x25, 0xA4, 0x00, 0x03, 0x70, 0x00, 0xFE, 0xFB,
  0x94, 0x00, 0x00,
])

export function generatePlaceholderSprites(assetsDir: string): void {
  const playersDir = path.join(assetsDir, 'assets', 'images', 'players')
  fs.mkdirSync(playersDir, { recursive: true })
  const combos = buildPcSpriteCombos()
  for (const combo of combos) {
    fs.writeFileSync(path.join(playersDir, `${combo.id}.webp`), WEBP_PLACEHOLDER)
  }
}
