import { PrismaClient } from './generated/prisma/client.js'

export interface ValidationResult {
  errors: string[]
  warnings: string[]
}

function parseJsonField(value: string, fallback: any = {}): any {
  if (!value || value === '') return fallback
  try {
    return JSON.parse(value)
  } catch {
    return fallback
  }
}

function spritePathFor(entityId: string): string {
  const prefix = entityId.split(':')[0]
  const folder = `${prefix}s` // npc -> npcs, item -> items
  return `assets/images/${folder}/${entityId.replace(':', '_')}.webp`
}

function sfxPathFor(soundId: string): string {
  return `assets/audio/sfx/${soundId}.mp3`
}

function bgmPathFor(trackId: string): string {
  return `assets/audio/bgm/${trackId}.mp3`
}

export async function validateProject(
  prisma: PrismaClient,
  assetExists: (path: string) => boolean
): Promise<ValidationResult> {
  const errors: string[] = []
  const warnings: string[] = []

  const [zones, items, npcs, spells, lootTables, pcSprites] = await Promise.all([
    prisma.zone.findMany({ include: { rooms: { include: { exits: true } } } }),
    prisma.item.findMany(),
    prisma.npc.findMany(),
    prisma.spell.findMany(),
    prisma.lootTable.findMany(),
    prisma.pcSprite.findMany(),
  ])

  // Build lookup sets
  const allRoomIds = new Set<string>()
  for (const zone of zones) {
    for (const room of zone.rooms) {
      allRoomIds.add(room.id)
    }
  }
  const allItemIds = new Set(items.map((i) => i.id))
  const lootTableIds = new Set(lootTables.map((lt) => lt.id))

  // ─── Spawn room ──────────────────────────────────────
  const hasSpawnRoom = zones.some((z) => z.spawnRoom && z.spawnRoom.length > 0)
  if (!hasSpawnRoom) {
    errors.push('No zone defines a spawnRoom. At least one zone must specify a spawnRoom.')
  }

  // ─── Item validation ─────────────────────────────────
  const validSlots = new Set(['weapon', 'head', 'chest', 'legs', 'feet', 'shield', 'hands', 'neck', 'ring'])
  for (const item of items) {
    if (item.type === 'weapon') {
      if (!item.slot) warnings.push(`Weapon '${item.id}' missing slot (should be "weapon")`)
      if (item.damageBonus === 0 && item.damageRange === 0)
        warnings.push(`Weapon '${item.id}' has zero damageBonus and zero damageRange`)
    }
    const accessorySlots = new Set(['neck', 'ring'])
    if (item.type === 'armor' && item.armorValue === 0 && !accessorySlots.has(item.slot)) {
      warnings.push(`Armor '${item.id}' has zero armorValue`)
    }
    if (item.type === 'consumable' && !item.useEffect) {
      warnings.push(`Consumable '${item.id}' missing useEffect`)
    }
    if (item.type === 'crafting' && item.useEffect) {
      warnings.push(`Crafting item '${item.id}' has useEffect — crafting materials shouldn't have use effects`)
    }
    if (item.stackable && item.maxStack <= 1) {
      warnings.push(`Item '${item.id}' is stackable but maxStack=${item.maxStack}`)
    }
    if (item.slot && !validSlots.has(item.slot)) {
      warnings.push(`Item '${item.id}' has unknown slot '${item.slot}'`)
    }
  }

  // ─── NPC validation ──────────────────────────────────
  for (const npc of npcs) {
    if (npc.hostile) {
      if (npc.maxHp === 0) warnings.push(`Hostile NPC '${npc.id}' has maxHp=0`)
      if (npc.damage === 0) warnings.push(`Hostile NPC '${npc.id}' has damage=0`)
      if (npc.xpReward === 0) warnings.push(`Hostile NPC '${npc.id}' has xpReward=0`)
      if (!lootTableIds.has(npc.id)) {
        warnings.push(`Hostile NPC '${npc.id}' has no loot table entry`)
      }
    }
    const vendorItems = parseJsonField(npc.vendorItems, []) as string[]
    if (vendorItems.length > 0) {
      for (const itemId of vendorItems) {
        if (!allItemIds.has(itemId)) {
          warnings.push(`Vendor NPC '${npc.id}' references unknown item '${itemId}'`)
        }
      }
    }
    if (npc.behaviorType === 'vendor' && vendorItems.length === 0) {
      warnings.push(`Vendor NPC '${npc.id}' has empty vendorItems`)
    }
    const patrolRoute = parseJsonField(npc.patrolRoute, []) as string[]
    if (npc.behaviorType === 'patrol' && patrolRoute.length === 0) {
      warnings.push(`Patrol NPC '${npc.id}' has empty patrolRoute`)
    }
    if (!allRoomIds.has(npc.startRoomId)) {
      warnings.push(`NPC '${npc.id}' startRoomId '${npc.startRoomId}' not found in loaded rooms`)
    }
    if (['vendor', 'trainer'].includes(npc.behaviorType) && !npc.interactSound) {
      warnings.push(`NPC '${npc.id}' (${npc.behaviorType}) missing interactSound`)
    }
    if (['vendor', 'trainer'].includes(npc.behaviorType) && !npc.exitSound) {
      warnings.push(`NPC '${npc.id}' (${npc.behaviorType}) missing exitSound`)
    }
  }

  // ─── Room validation ─────────────────────────────────
  const knownEffectTypes = new Set(['HEAL', 'POISON', 'DAMAGE', 'MANA_REGEN', 'MANA_DRAIN', 'SANCTUARY'])
  const valueRequiredTypes = new Set(['HEAL', 'POISON', 'DAMAGE', 'MANA_REGEN', 'MANA_DRAIN'])
  for (const zone of zones) {
    for (const room of zone.rooms) {
      if (room.exits.length === 0) warnings.push(`Room '${room.id}' has zero exits (isolated)`)
      if (!room.backgroundImage) warnings.push(`Room '${room.id}' missing backgroundImage`)
      for (const exit of room.exits) {
        if (!allRoomIds.has(exit.toRoomId)) {
          warnings.push(`Room '${room.id}' exit ${exit.direction} points to unknown room '${exit.toRoomId}'`)
        }
      }
      // Validate room effects
      const roomEffects = parseJsonField((room as any).effects, []) as { type: string; value: number; sound?: string }[]
      for (const eff of roomEffects) {
        if (!knownEffectTypes.has(eff.type)) {
          warnings.push(`Room '${room.id}' has unknown effect type '${eff.type}'`)
        }
        if (valueRequiredTypes.has(eff.type) && eff.value === 0) {
          warnings.push(`Room '${room.id}' has ${eff.type} effect with value 0`)
        }
      }
    }
  }

  // ─── Loot table cross-reference ──────────────────────
  for (const lt of lootTables) {
    const ltItems = parseJsonField(lt.items, []) as { itemId: string }[]
    for (const entry of ltItems) {
      if (!allItemIds.has(entry.itemId)) {
        warnings.push(`Loot table '${lt.id}' references unknown item '${entry.itemId}'`)
      }
    }
  }

  // ─── Sound field validation ──────────────────────────
  for (const item of items) {
    if (item.type === 'weapon') {
      if (!item.attackSound) warnings.push(`Weapon '${item.id}' missing attackSound`)
      if (!item.missSound) warnings.push(`Weapon '${item.id}' missing missSound`)
    }
    if (item.type === 'consumable' && !item.useSound) {
      warnings.push(`Consumable '${item.id}' missing useSound`)
    }
  }
  for (const npc of npcs) {
    if (npc.hostile) {
      if (!npc.attackSound) warnings.push(`Hostile NPC '${npc.id}' missing attackSound`)
      if (!npc.missSound) warnings.push(`Hostile NPC '${npc.id}' missing missSound`)
      if (!npc.deathSound) warnings.push(`Hostile NPC '${npc.id}' missing deathSound`)
    }
  }
  for (const spell of spells) {
    if (!spell.castSound) warnings.push(`Spell '${spell.id}' missing castSound`)
    if (spell.spellType === 'DAMAGE' || spell.spellType === 'DOT') {
      if (!spell.impactSound) warnings.push(`Damage spell '${spell.id}' missing impactSound`)
      if (!spell.missSound) warnings.push(`Damage spell '${spell.id}' missing missSound`)
    }
  }
  for (const zone of zones) {
    for (const room of zone.rooms) {
      if (!room.departSound) warnings.push(`Room '${room.id}' missing departSound`)
    }
  }

  // ─── PC Sprite asset existence ──────────────────────
  for (const sprite of pcSprites) {
    const p = `assets/images/players/${sprite.id}.webp`
    if (!assetExists(p)) warnings.push(`PC sprite '${sprite.id}' missing asset: ${p}`)
  }

  // ─── Asset file existence ────────────────────────────
  // Item sprites
  for (const item of items) {
    const p = spritePathFor(item.id)
    if (!assetExists(p)) warnings.push(`Item '${item.id}' missing sprite asset: ${p}`)
  }
  // NPC sprites
  for (const npc of npcs) {
    const p = spritePathFor(npc.id)
    if (!assetExists(p)) warnings.push(`NPC '${npc.id}' missing sprite asset: ${p}`)
  }
  // Room background images
  for (const zone of zones) {
    for (const room of zone.rooms) {
      if (room.backgroundImage) {
        const p = room.backgroundImage.replace(/^\//, '')
        if (!assetExists(p)) warnings.push(`Room '${room.id}' missing background asset: ${p}`)
      }
    }
  }
  // SFX files (deduped)
  const checkedSfx = new Set<string>()
  for (const item of items) {
    for (const sound of [item.attackSound, item.missSound, item.useSound]) {
      if (sound && checkedSfx.add(sound) && !assetExists(sfxPathFor(sound))) {
        warnings.push(`Missing SFX asset: ${sfxPathFor(sound)} (referenced by item '${item.id}')`)
      }
    }
  }
  for (const npc of npcs) {
    for (const sound of [npc.attackSound, npc.missSound, npc.deathSound, npc.interactSound, npc.exitSound]) {
      if (sound && !checkedSfx.has(sound)) {
        checkedSfx.add(sound)
        if (!assetExists(sfxPathFor(sound))) {
          warnings.push(`Missing SFX asset: ${sfxPathFor(sound)} (referenced by NPC '${npc.id}')`)
        }
      }
    }
  }
  for (const spell of spells) {
    for (const sound of [spell.castSound, spell.impactSound, spell.missSound]) {
      if (sound && !checkedSfx.has(sound)) {
        checkedSfx.add(sound)
        if (!assetExists(sfxPathFor(sound))) {
          warnings.push(`Missing SFX asset: ${sfxPathFor(sound)} (referenced by spell '${spell.id}')`)
        }
      }
    }
  }
  for (const zone of zones) {
    for (const room of zone.rooms) {
      if (room.departSound && !checkedSfx.has(room.departSound)) {
        checkedSfx.add(room.departSound)
        if (!assetExists(sfxPathFor(room.departSound))) {
          warnings.push(`Missing SFX asset: ${sfxPathFor(room.departSound)} (referenced by room '${room.id}')`)
        }
      }
      // Effect sounds
      const effs = parseJsonField((room as any).effects, []) as { sound?: string }[]
      for (const eff of effs) {
        if (eff.sound && !checkedSfx.has(eff.sound)) {
          checkedSfx.add(eff.sound)
          if (!assetExists(sfxPathFor(eff.sound))) {
            warnings.push(`Missing SFX asset: ${sfxPathFor(eff.sound)} (referenced by room '${room.id}' effect)`)
          }
        }
      }
    }
  }
  // BGM files (deduped)
  const checkedBgm = new Set<string>()
  for (const zone of zones) {
    for (const room of zone.rooms) {
      const bgm = room.bgm || zone.bgm
      if (bgm && !checkedBgm.has(bgm)) {
        checkedBgm.add(bgm)
        if (!assetExists(bgmPathFor(bgm))) {
          warnings.push(`Missing BGM asset: ${bgmPathFor(bgm)} (referenced by room '${room.id}')`)
        }
      }
    }
  }

  return { errors, warnings }
}
