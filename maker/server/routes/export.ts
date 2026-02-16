import { Router } from 'express'
import fs from 'fs'
import path from 'path'
import AdmZip from 'adm-zip'
import { db, getActiveProject, getProjectsDir } from '../db.js'

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
        const lockedExits = parseJsonField(room.lockedExits, {})
        roomsOut[room.id] = {
          name: room.name,
          description: room.description,
          backgroundImage: room.backgroundImage,
          bgm: room.bgm,
          departSound: room.departSound,
          healPerTick: room.healPerTick,
          exits,
          ...(Object.keys(lockedExits).length > 0 ? { lockedExits } : {}),
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
          agility: npc.agility,
          vendorItems: parseJsonField(npc.vendorItems, []),
          attackSound: npc.attackSound,
          missSound: npc.missSound,
          deathSound: npc.deathSound,
          interactSound: npc.interactSound,
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

// GET /nmd — export complete .nmd bundle (JSON data + assets)
exportRouter.get('/nmd', async (_req, res) => {
  try {
    const prisma = db()
    const projectName = getActiveProject()
    if (!projectName) {
      res.status(400).json({ error: 'No project is open' })
      return
    }

    const zip = new AdmZip()

    // ─── Manifest ───────────────────────────────────────
    const metaRows = await prisma.projectMeta.findMany()
    const manifest: Record<string, any> = {}
    for (const row of metaRows) {
      if (row.key === 'readOnly') continue
      // Try to parse numeric/boolean values back
      const num = Number(row.value)
      if (!isNaN(num) && row.value !== '') manifest[row.key] = num
      else manifest[row.key] = row.value
    }
    zip.addFile('manifest.json', Buffer.from(JSON.stringify(manifest, null, 2)))

    // ─── Fetch all data ─────────────────────────────────
    const [zones, items, npcs, classes, races, skills, spells, lootTables, promptTemplates] =
      await Promise.all([
        prisma.zone.findMany({ include: { rooms: { include: { exits: true } } } }),
        prisma.item.findMany(),
        prisma.npc.findMany(),
        prisma.characterClass.findMany(),
        prisma.race.findMany(),
        prisma.skill.findMany(),
        prisma.spell.findMany(),
        prisma.lootTable.findMany(),
        prisma.promptTemplate.findMany(),
      ])

    // ─── Zone files (one per zone) ──────────────────────
    for (const zone of zones) {
      const zoneNpcs = npcs.filter((n) => n.zoneId === zone.id)
      const zoneOut: any = {
        id: zone.id,
        name: zone.name,
        description: zone.description,
        safe: zone.safe,
        bgm: zone.bgm,
        spawnRoom: zone.spawnRoom,
        spawnConfig: {
          maxEntities: zone.spawnMaxEntities,
          maxPerRoom: zone.spawnMaxPerRoom,
          rateTicks: zone.spawnRateTicks,
        },
        rooms: zone.rooms.map((room) => {
          const exits: Record<string, string> = {}
          for (const exit of room.exits) {
            exits[exit.direction] = exit.toRoomId
          }
          const lockedExits = parseJsonField(room.lockedExits, {})
          return {
            id: room.id,
            name: room.name,
            description: room.description,
            x: room.x,
            y: room.y,
            backgroundImage: room.backgroundImage,
            healPerTick: room.healPerTick,
            bgm: room.bgm,
            departSound: room.departSound,
            exits,
            ...(Object.keys(lockedExits).length > 0 ? { lockedExits } : {}),
          }
        }),
        npcs: zoneNpcs.map((npc) => ({
          id: npc.id,
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
          agility: npc.agility,
          vendorItems: parseJsonField(npc.vendorItems, []),
          attackSound: npc.attackSound,
          missSound: npc.missSound,
          deathSound: npc.deathSound,
          interactSound: npc.interactSound,
        })),
      }
      zip.addFile(`world/${zone.id}.zone.json`, Buffer.from(JSON.stringify(zoneOut, null, 2)))
    }

    // ─── Catalog files ──────────────────────────────────
    // Items
    zip.addFile(
      'world/items.json',
      Buffer.from(
        JSON.stringify(
          {
            items: items.map((item) => ({
              id: item.id,
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
            })),
          },
          null,
          2
        )
      )
    )

    // Classes
    zip.addFile(
      'world/classes.json',
      Buffer.from(
        JSON.stringify(
          {
            classes: classes.map((cls) => ({
              id: cls.id,
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
            })),
          },
          null,
          2
        )
      )
    )

    // Races
    zip.addFile(
      'world/races.json',
      Buffer.from(
        JSON.stringify(
          {
            races: races.map((race) => ({
              id: race.id,
              name: race.name,
              description: race.description,
              statModifiers: parseJsonField(race.statModifiers),
              xpModifier: race.xpModifier,
            })),
          },
          null,
          2
        )
      )
    )

    // Skills
    zip.addFile(
      'world/skills.json',
      Buffer.from(
        JSON.stringify(
          {
            skills: skills.map((skill) => ({
              id: skill.id,
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
            })),
          },
          null,
          2
        )
      )
    )

    // Spells
    zip.addFile(
      'world/spells.json',
      Buffer.from(
        JSON.stringify(
          {
            spells: spells.map((spell) => ({
              id: spell.id,
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
            })),
          },
          null,
          2
        )
      )
    )

    // Loot Tables
    const tablesObj: Record<string, any> = {}
    for (const lt of lootTables) {
      tablesObj[lt.id] = {
        items: parseJsonField(lt.items, []),
        coinDrop: parseJsonField(lt.coinDrop),
      }
    }
    zip.addFile('world/loot_tables.json', Buffer.from(JSON.stringify({ tables: tablesObj }, null, 2)))

    // Prompt Templates
    zip.addFile(
      'world/prompt_templates.json',
      Buffer.from(
        JSON.stringify(
          {
            templates: promptTemplates.map((t) => ({
              entityType: t.entityType,
              entityId: t.entityId,
              prompt: t.prompt,
              style: t.style,
              negativePrompt: t.negativePrompt,
              width: t.width,
              height: t.height,
            })),
          },
          null,
          2
        )
      )
    )

    // ─── Assets ─────────────────────────────────────────
    const assetsDir = path.join(getProjectsDir(), `${projectName}_assets`, 'assets')
    if (fs.existsSync(assetsDir)) {
      function addDirToZip(dirPath: string, zipPrefix: string) {
        for (const entry of fs.readdirSync(dirPath)) {
          const fullPath = path.join(dirPath, entry)
          const zipPath = `${zipPrefix}/${entry}`
          if (fs.statSync(fullPath).isDirectory()) {
            addDirToZip(fullPath, zipPath)
          } else {
            zip.addLocalFile(fullPath, path.dirname(zipPath))
          }
        }
      }
      addDirToZip(assetsDir, 'assets')
    }

    // Send the zip
    const buffer = zip.toBuffer()
    res.set({
      'Content-Type': 'application/zip',
      'Content-Disposition': `attachment; filename="${projectName}.nmd"`,
      'Content-Length': buffer.length.toString(),
    })
    res.send(buffer)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})
