import type { PrismaClient } from './generated/prisma/client.js'

export interface DefaultSfxEntry {
  id: string
  label: string
  category: string
  description: string
  prompt: string
  duration: number
}

export const DEFAULT_SFX: DefaultSfxEntry[] = [
  { id: 'dodge', label: 'Dodge', category: 'combat', description: 'Player/NPC dodges an attack', prompt: 'short whoosh dodge evasion sound effect, fantasy RPG', duration: 2 },
  { id: 'parry', label: 'Parry', category: 'combat', description: 'Player parries an attack', prompt: 'short metallic sword parry deflection clang sound effect', duration: 2 },
  { id: 'miss', label: 'Miss', category: 'combat', description: 'Generic miss fallback', prompt: 'short swoosh weapon swing miss sound effect, fantasy combat', duration: 2 },
  { id: 'backstab', label: 'Backstab', category: 'combat', description: 'Backstab hit', prompt: 'sharp stabbing blade backstab critical hit sound effect, dark fantasy', duration: 2 },
  { id: 'enemy_death', label: 'Enemy Death', category: 'combat', description: 'Generic NPC death fallback', prompt: 'enemy creature death groan collapse sound effect, fantasy RPG', duration: 3 },
  { id: 'coin_pickup', label: 'Coin Pickup', category: 'loot', description: 'Picking up coins', prompt: 'short coins jingling pickup collect sound effect, fantasy game', duration: 2 },
  { id: 'item_pickup', label: 'Item Pickup', category: 'loot', description: 'Picking up items', prompt: 'short item pickup grab sound effect, fantasy RPG inventory', duration: 2 },
  { id: 'loot_drop', label: 'Loot Drop', category: 'loot', description: 'Loot drops on ground', prompt: 'items dropping on ground loot drop sound effect, fantasy game', duration: 2 },
  { id: 'potion_drink', label: 'Potion Drink', category: 'item', description: 'Using a potion', prompt: 'potion drinking gulp magical liquid sound effect, fantasy RPG', duration: 3 },
  { id: 'spell_fizzle', label: 'Spell Fizzle', category: 'magic', description: 'Spell fails/fizzles', prompt: 'magic spell fizzle failure dissipate sound effect, fantasy', duration: 3 },
  { id: 'footstep_cobblestone', label: 'Footstep: Cobblestone', category: 'movement', description: 'Room transition on cobblestone', prompt: 'footsteps walking on cobblestone stone path sound effect', duration: 3 },
  { id: 'footstep_dirt', label: 'Footstep: Dirt', category: 'movement', description: 'Room transition on dirt', prompt: 'footsteps walking on dirt ground path sound effect', duration: 3 },
  { id: 'footstep_grass', label: 'Footstep: Grass', category: 'movement', description: 'Room transition on grass', prompt: 'footsteps walking through grass field sound effect', duration: 3 },
  { id: 'footstep_marble', label: 'Footstep: Marble', category: 'movement', description: 'Room transition on marble', prompt: 'footsteps walking on marble stone floor hall sound effect', duration: 3 },
  { id: 'footstep_splash', label: 'Footstep: Splash', category: 'movement', description: 'Room transition in water', prompt: 'footsteps splashing through shallow water puddle sound effect', duration: 3 },
  { id: 'footstep_wood', label: 'Footstep: Wood', category: 'movement', description: 'Room transition on wood', prompt: 'footsteps walking on wooden floor planks creaking sound effect', duration: 3 },
]

export async function seedDefaultSfx(prisma: PrismaClient): Promise<void> {
  for (const entry of DEFAULT_SFX) {
    await prisma.defaultSfx.upsert({
      where: { id: entry.id },
      create: {
        id: entry.id,
        label: entry.label,
        category: entry.category,
        description: entry.description,
        prompt: entry.prompt,
        duration: entry.duration,
      },
      update: {},
    })
  }
}
