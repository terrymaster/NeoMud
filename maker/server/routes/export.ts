import { Router } from 'express'
import { db } from '../db.js'

export const exportRouter = Router()

/** Safely parse a JSON string field, returning the fallback if empty or invalid. */
function parseJsonField(value: string, fallback: any = {}): any {
  if (!value || value === '') return fallback
  try {
    return JSON.parse(value)
  } catch {
    return fallback
  }
}

// GET /json — export all data as NeoMUD-format JSON
exportRouter.get('/json', async (_req, res) => {
  try {
    const prisma = db()

    // Fetch all data in parallel
    const [
      zones,
      items,
      npcs,
      classes,
      races,
      skills,
      spells,
      lootTables,
    ] = await Promise.all([
      prisma.zone.findMany({ include: { rooms: { include: { exits: true } } } }),
      prisma.item.findMany(),
      prisma.npc.findMany(),
      prisma.characterClass.findMany(),
      prisma.race.findMany(),
      prisma.skill.findMany(),
      prisma.spell.findMany(),
      prisma.lootTable.findMany(),
    ])

    // ─── Zones + Rooms ──────────────────────────────────
    const zonesOut: Record<string, any> = {}
    for (const zone of zones) {
      const roomsOut: Record<string, any> = {}
      for (const room of zone.rooms) {
        const exits: Record<string, string> = {}
        for (const exit of room.exits) {
          exits[exit.direction] = exit.toRoomId
        }
        roomsOut[room.id] = {
          name: room.name,
          description: room.description,
          backgroundImage: room.backgroundImage,
          bgm: room.bgm,
          departSound: room.departSound,
          healPerTick: room.healPerTick,
          exits,
        }
      }

      // Build NPC map for this zone
      const zoneNpcs = npcs.filter((n) => n.zoneId === zone.id)
      const npcsOut: Record<string, any> = {}
      for (const npc of zoneNpcs) {
        npcsOut[npc.id] = {
          name: npc.name,
          description: npc.description,
          startRoomId: npc.startRoomId,
          behaviorType: npc.behaviorType,
          patrolRoute: parseJsonField(npc.patrolRoute, []),
          hostile: npc.hostile,
          maxHp: npc.maxHp,
          damage: npc.damage,
          level: npc.level,
          perception: npc.perception,
          xpReward: npc.xpReward,
          accuracy: npc.accuracy,
          defense: npc.defense,
          evasion: npc.evasion,
          vendorItems: parseJsonField(npc.vendorItems, []),
          attackSound: npc.attackSound,
          missSound: npc.missSound,
          deathSound: npc.deathSound,
        }
      }

      zonesOut[zone.id] = {
        name: zone.name,
        description: zone.description,
        safe: zone.safe,
        bgm: zone.bgm,
        spawnConfig: {
          spawnRoom: zone.spawnRoom,
          maxEntities: zone.spawnMaxEntities,
          maxPerRoom: zone.spawnMaxPerRoom,
          rateTicks: zone.spawnRateTicks,
        },
        rooms: roomsOut,
        npcs: npcsOut,
      }
    }

    // ─── Items ──────────────────────────────────────────
    const itemsOut: Record<string, any> = {}
    for (const item of items) {
      itemsOut[item.id] = {
        name: item.name,
        description: item.description,
        type: item.type,
        slot: item.slot,
        damageBonus: item.damageBonus,
        damageRange: item.damageRange,
        armorValue: item.armorValue,
        value: item.value,
        weight: item.weight,
        stackable: item.stackable,
        maxStack: item.maxStack,
        useEffect: item.useEffect,
        levelRequirement: item.levelRequirement,
        attackSound: item.attackSound,
        missSound: item.missSound,
        useSound: item.useSound,
      }
    }

    // ─── Classes ────────────────────────────────────────
    const classesOut: Record<string, any> = {}
    for (const cls of classes) {
      classesOut[cls.id] = {
        name: cls.name,
        description: cls.description,
        minimumStats: parseJsonField(cls.minimumStats),
        skills: parseJsonField(cls.skills, []),
        properties: parseJsonField(cls.properties),
        hpPerLevelMin: cls.hpPerLevelMin,
        hpPerLevelMax: cls.hpPerLevelMax,
        mpPerLevelMin: cls.mpPerLevelMin,
        mpPerLevelMax: cls.mpPerLevelMax,
        xpModifier: cls.xpModifier,
        magicSchools: parseJsonField(cls.magicSchools),
      }
    }

    // ─── Races ──────────────────────────────────────────
    const racesOut: Record<string, any> = {}
    for (const race of races) {
      racesOut[race.id] = {
        name: race.name,
        description: race.description,
        statModifiers: parseJsonField(race.statModifiers),
        xpModifier: race.xpModifier,
      }
    }

    // ─── Skills ─────────────────────────────────────────
    const skillsOut: Record<string, any> = {}
    for (const skill of skills) {
      skillsOut[skill.id] = {
        name: skill.name,
        description: skill.description,
        category: skill.category,
        primaryStat: skill.primaryStat,
        secondaryStat: skill.secondaryStat,
        cooldownTicks: skill.cooldownTicks,
        manaCost: skill.manaCost,
        difficulty: skill.difficulty,
        isPassive: skill.isPassive,
        classRestrictions: parseJsonField(skill.classRestrictions, []),
        properties: parseJsonField(skill.properties),
      }
    }

    // ─── Spells ─────────────────────────────────────────
    const spellsOut: Record<string, any> = {}
    for (const spell of spells) {
      spellsOut[spell.id] = {
        name: spell.name,
        description: spell.description,
        school: spell.school,
        spellType: spell.spellType,
        manaCost: spell.manaCost,
        cooldownTicks: spell.cooldownTicks,
        levelRequired: spell.levelRequired,
        primaryStat: spell.primaryStat,
        basePower: spell.basePower,
        targetType: spell.targetType,
        effectType: spell.effectType,
        effectDuration: spell.effectDuration,
        castMessage: spell.castMessage,
        castSound: spell.castSound,
        impactSound: spell.impactSound,
        missSound: spell.missSound,
      }
    }

    // ─── Loot Tables ────────────────────────────────────
    const lootTablesOut: Record<string, any> = {}
    for (const lt of lootTables) {
      lootTablesOut[lt.id] = {
        items: parseJsonField(lt.items, []),
        coinDrop: parseJsonField(lt.coinDrop),
      }
    }

    res.json({
      zones: zonesOut,
      items: itemsOut,
      classes: classesOut,
      races: racesOut,
      skills: skillsOut,
      spells: spellsOut,
      lootTables: lootTablesOut,
    })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})
