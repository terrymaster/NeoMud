import AdmZip from 'adm-zip'
import { createProject, db } from './db.js'

/**
 * Import an .nmd bundle (ZIP) into a new maker project.
 */
export async function importNmd(nmdPath: string, projectName: string, readOnly = false): Promise<void> {
  const zip = new AdmZip(nmdPath)

  // Create the project DB
  await createProject(projectName, readOnly)
  const prisma = db()

  // ─── Manifest ──────────────────────────────────────────
  const manifestEntry = zip.getEntry('manifest.json')
  if (manifestEntry) {
    const manifest = JSON.parse(zip.readAsText(manifestEntry))
    const metaEntries = Object.entries(manifest) as [string, unknown][]
    for (const [key, value] of metaEntries) {
      if (key === 'readOnly') continue // Don't overwrite our readOnly flag
      await prisma.projectMeta.upsert({
        where: { key },
        create: { key, value: String(value) },
        update: { value: String(value) },
      })
    }
  }

  // ─── Classes ───────────────────────────────────────────
  const classesEntry = zip.getEntry('world/classes.json')
  if (classesEntry) {
    const { classes } = JSON.parse(zip.readAsText(classesEntry))
    for (const cls of classes) {
      await prisma.characterClass.create({
        data: {
          id: cls.id,
          name: cls.name,
          description: cls.description ?? '',
          minimumStats: JSON.stringify(cls.minimumStats ?? {}),
          skills: JSON.stringify(cls.skills ?? []),
          properties: JSON.stringify(cls.properties ?? {}),
          hpPerLevelMin: cls.hpPerLevelMin ?? 4,
          hpPerLevelMax: cls.hpPerLevelMax ?? 8,
          mpPerLevelMin: cls.mpPerLevelMin ?? 0,
          mpPerLevelMax: cls.mpPerLevelMax ?? 0,
          xpModifier: cls.xpModifier ?? 1.0,
          magicSchools: JSON.stringify(cls.magicSchools ?? {}),
        },
      })
    }
  }

  // ─── Races ─────────────────────────────────────────────
  const racesEntry = zip.getEntry('world/races.json')
  if (racesEntry) {
    const { races } = JSON.parse(zip.readAsText(racesEntry))
    for (const race of races) {
      await prisma.race.create({
        data: {
          id: race.id,
          name: race.name,
          description: race.description ?? '',
          statModifiers: JSON.stringify(race.statModifiers ?? {}),
          xpModifier: race.xpModifier ?? 1.0,
        },
      })
    }
  }

  // ─── Items ─────────────────────────────────────────────
  const itemsEntry = zip.getEntry('world/items.json')
  if (itemsEntry) {
    const { items } = JSON.parse(zip.readAsText(itemsEntry))
    for (const item of items) {
      await prisma.item.create({
        data: {
          id: item.id,
          name: item.name,
          description: item.description ?? '',
          type: item.type ?? '',
          slot: item.slot ?? '',
          damageBonus: item.damageBonus ?? 0,
          damageRange: item.damageRange ?? 0,
          armorValue: item.armorValue ?? 0,
          value: item.value ?? 0,
          weight: item.weight ?? 0,
          stackable: item.stackable ?? false,
          maxStack: item.maxStack ?? 1,
          useEffect: item.useEffect ?? '',
          levelRequirement: item.levelRequirement ?? 0,
          attackSound: item.attackSound ?? '',
          missSound: item.missSound ?? '',
          useSound: item.useSound ?? '',
        },
      })
    }
  }

  // ─── Skills ────────────────────────────────────────────
  const skillsEntry = zip.getEntry('world/skills.json')
  if (skillsEntry) {
    const { skills } = JSON.parse(zip.readAsText(skillsEntry))
    for (const skill of skills) {
      await prisma.skill.create({
        data: {
          id: skill.id,
          name: skill.name,
          description: skill.description ?? '',
          category: skill.category ?? '',
          primaryStat: skill.primaryStat ?? '',
          secondaryStat: skill.secondaryStat ?? '',
          cooldownTicks: skill.cooldownTicks ?? 0,
          manaCost: skill.manaCost ?? 0,
          difficulty: skill.difficulty ?? 15,
          isPassive: skill.isPassive ?? false,
          classRestrictions: JSON.stringify(skill.classRestrictions ?? []),
          properties: JSON.stringify(skill.properties ?? {}),
        },
      })
    }
  }

  // ─── Spells ────────────────────────────────────────────
  const spellsEntry = zip.getEntry('world/spells.json')
  if (spellsEntry) {
    const { spells } = JSON.parse(zip.readAsText(spellsEntry))
    for (const spell of spells) {
      await prisma.spell.create({
        data: {
          id: spell.id,
          name: spell.name,
          description: spell.description ?? '',
          school: spell.school ?? '',
          spellType: spell.spellType ?? '',
          manaCost: spell.manaCost ?? 0,
          cooldownTicks: spell.cooldownTicks ?? 0,
          levelRequired: spell.levelRequired ?? 1,
          primaryStat: spell.primaryStat ?? 'intellect',
          basePower: spell.basePower ?? 0,
          targetType: spell.targetType ?? 'ENEMY',
          effectType: spell.effectType ?? '',
          effectDuration: spell.effectDuration ?? 0,
          castMessage: spell.castMessage ?? '',
          castSound: spell.castSound ?? '',
          impactSound: spell.impactSound ?? '',
          missSound: spell.missSound ?? '',
        },
      })
    }
  }

  // ─── Loot Tables ───────────────────────────────────────
  const lootEntry = zip.getEntry('world/loot_tables.json')
  if (lootEntry) {
    const { tables } = JSON.parse(zip.readAsText(lootEntry))
    for (const [npcId, table] of Object.entries(tables) as [string, any][]) {
      await prisma.lootTable.create({
        data: {
          id: npcId,
          items: JSON.stringify(table.items ?? []),
          coinDrop: JSON.stringify(table.coinDrop ?? {}),
        },
      })
    }
  }

  // ─── Prompt Templates ──────────────────────────────────
  const promptEntry = zip.getEntry('world/prompt_templates.json')
  if (promptEntry) {
    const { templates } = JSON.parse(zip.readAsText(promptEntry))
    for (const tmpl of templates) {
      await prisma.promptTemplate.create({
        data: {
          entityType: tmpl.entityType,
          entityId: tmpl.entityId,
          prompt: tmpl.prompt ?? '',
          style: tmpl.style ?? '',
          negativePrompt: tmpl.negativePrompt ?? '',
          width: tmpl.width ?? 1024,
          height: tmpl.height ?? 576,
        },
      })
    }
  }

  // ─── Zone files ────────────────────────────────────────
  // Two-pass approach: first create all zones/rooms/npcs, then exits
  // (exits can reference rooms in other zones)
  const zoneEntries = zip.getEntries().filter((e) => e.entryName.match(/^world\/.*\.zone\.json$/))
  const parsedZones: any[] = []

  for (const entry of zoneEntries) {
    const zone = JSON.parse(zip.readAsText(entry))
    parsedZones.push(zone)

    const spawnConfig = zone.spawnConfig ?? {}

    await prisma.zone.create({
      data: {
        id: zone.id,
        name: zone.name,
        description: zone.description ?? '',
        safe: zone.safe ?? true,
        bgm: zone.bgm ?? '',
        spawnRoom: zone.spawnRoom ?? null,
        spawnMaxEntities: spawnConfig.maxEntities ?? 0,
        spawnMaxPerRoom: spawnConfig.maxPerRoom ?? 0,
        spawnRateTicks: spawnConfig.rateTicks ?? 0,
      },
    })

    // Rooms
    for (const room of zone.rooms ?? []) {
      await prisma.room.create({
        data: {
          id: room.id,
          zoneId: zone.id,
          name: room.name,
          description: room.description ?? '',
          x: room.x ?? 0,
          y: room.y ?? 0,
          backgroundImage: room.backgroundImage ?? '',
          healPerTick: room.healPerTick ?? 0,
          bgm: room.bgm ?? '',
          departSound: room.departSound ?? '',
        },
      })
    }

    // NPCs
    for (const npc of zone.npcs ?? []) {
      await prisma.npc.create({
        data: {
          id: npc.id,
          zoneId: zone.id,
          name: npc.name,
          description: npc.description ?? '',
          startRoomId: npc.startRoomId ?? '',
          behaviorType: npc.behaviorType ?? 'idle',
          patrolRoute: JSON.stringify(npc.patrolRoute ?? []),
          hostile: npc.hostile ?? false,
          maxHp: npc.maxHp ?? 0,
          damage: npc.damage ?? 0,
          level: npc.level ?? 1,
          perception: npc.perception ?? 0,
          xpReward: npc.xpReward ?? 0,
          accuracy: npc.accuracy ?? 0,
          defense: npc.defense ?? 0,
          evasion: npc.evasion ?? 0,
          vendorItems: JSON.stringify(npc.vendorItems ?? []),
          attackSound: npc.attackSound ?? '',
          missSound: npc.missSound ?? '',
          deathSound: npc.deathSound ?? '',
        },
      })
    }
  }

  // Second pass: create all exits now that every room exists
  for (const zone of parsedZones) {
    for (const room of zone.rooms ?? []) {
      const exits = room.exits ?? {}
      for (const [direction, toRoomId] of Object.entries(exits) as [string, string][]) {
        await prisma.exit.create({
          data: {
            fromRoomId: room.id,
            direction,
            toRoomId,
          },
        })
      }
    }
  }
}
