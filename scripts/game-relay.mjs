#!/usr/bin/env node
/**
 * game-relay.mjs — WebSocket relay for Claude playtesting
 *
 * Maintains a persistent WebSocket connection to the NeoMud server and
 * exposes game state via files that Claude can read/write each turn.
 *
 * Usage: node scripts/game-relay.mjs <username> <password>
 *        node scripts/game-relay.mjs --register <username> <password> <charName> <class> <race> <gender>
 */

import { WebSocket } from 'ws';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const STATE_FILE = path.join(__dirname, 'relay-state.json');
const COMMAND_FILE = path.join(__dirname, 'relay-command.json');
const TEMP_STATE_FILE = path.join(__dirname, '.relay-state.tmp');
const LOCK_FILE = path.join(__dirname, 'relay.lock');

const SERVER_URL = process.env.NEOMUD_URL || 'ws://localhost:8080/game';
const COMMAND_POLL_MS = 250;
const PING_INTERVAL_MS = 30_000;
const COMMAND_SPACING_MS = 150;
const MAX_RECENT_EVENTS = 50;
const STATE_WRITE_DEBOUNCE_MS = 100;

// ---------------------------------------------------------------------------
// CLI args
// ---------------------------------------------------------------------------
const args = process.argv.slice(2);
let registerMode = false;
let registerOpts = {};
let username, password;

if (args[0] === '--register') {
  registerMode = true;
  username = args[1];
  password = args[2];
  registerOpts = {
    charName: args[3],
    charClass: args[4],
    race: args[5] || 'HUMAN',
    gender: args[6] || 'male',
    stats: null, // computed after we receive catalogs
  };
  if (!username || !password || !registerOpts.charName || !registerOpts.charClass) {
    console.error('Usage: node scripts/game-relay.mjs --register <user> <pass> <charName> <class> [race] [gender]');
    process.exit(1);
  }
} else {
  username = args[0];
  password = args[1];
  if (!username || !password) {
    console.error('Usage: node scripts/game-relay.mjs <username> <password>');
    process.exit(1);
  }
}

// ---------------------------------------------------------------------------
// Catalog data (received on connect, used for registration stat allocation)
// ---------------------------------------------------------------------------
let classCatalog = [];
let raceCatalog = [];
let skillCatalog = [];
let spellCatalog = [];
let itemCatalogMap = {}; // itemId -> item name, built from ItemCatalogSync
let catalogsReceived = { classes: false, races: false };
let registrationSent = false;

/**
 * Cross-reference the player's class with skill/spell catalogs to determine
 * what abilities are available, then write them into state.
 */
function computePlayerAbilities() {
  if (!state.player) return;
  const classDef = classCatalog.find(c => c.id === state.player.class);
  if (!classDef) return;

  // Skills: mirror server logic — classRestrictions empty = all classes,
  // otherwise the player's class must be in the list
  const playerClass = state.player.class;
  state.availableSkills = skillCatalog
    .filter(s => {
      const restrictions = s.classRestrictions || [];
      return restrictions.length === 0 || restrictions.includes(playerClass);
    })
    .map(s => ({
      id: s.id,
      name: s.name,
      description: s.description,
      category: s.category,
      manaCost: s.manaCost || 0,
      cooldownTicks: s.cooldownTicks || 0,
      isPassive: s.isPassive || false,
    }));

  // Spells: filter by class magic schools and player level
  const magicSchools = classDef.magicSchools || {};
  state.availableSpells = spellCatalog
    .filter(sp => {
      const schoolMax = magicSchools[sp.school];
      if (schoolMax == null) return false; // class doesn't have this school
      if (sp.schoolLevel > schoolMax) return false; // school level too high
      return true;
    })
    .map(sp => ({
      id: sp.id,
      name: sp.name,
      description: sp.description,
      school: sp.school,
      spellType: sp.spellType,
      manaCost: sp.manaCost,
      cooldownTicks: sp.cooldownTicks || 0,
      levelRequired: sp.levelRequired || 1,
      targetType: sp.targetType,
      basePower: sp.basePower || 0,
    }));

  scheduleStateWrite();
}

function tryRegister() {
  if (registrationSent || !registerMode) return;
  if (!catalogsReceived.classes || !catalogsReceived.races) return;

  const classDef = classCatalog.find(c => c.id === registerOpts.charClass);
  if (!classDef) {
    console.error(`[relay] Unknown class: ${registerOpts.charClass}`);
    console.error('[relay] Available classes:', classCatalog.map(c => c.id).join(', '));
    process.exit(1);
  }
  const raceDef = raceCatalog.find(r => r.id === registerOpts.race);
  const raceMods = raceDef?.statModifiers || { strength: 0, agility: 0, intellect: 0, willpower: 0, health: 0, charm: 0 };
  const mins = classDef.minimumStats;

  // Effective minimums = class mins + race mods (min 1)
  const base = {
    strength: Math.max(1, mins.strength + raceMods.strength),
    agility: Math.max(1, mins.agility + raceMods.agility),
    intellect: Math.max(1, mins.intellect + raceMods.intellect),
    willpower: Math.max(1, mins.willpower + raceMods.willpower),
    health: Math.max(1, mins.health + raceMods.health),
    charm: Math.max(1, mins.charm + raceMods.charm),
  };

  // Spread 60 CP evenly: +10 to each stat above base
  registerOpts.stats = {
    strength: base.strength + 10,
    agility: base.agility + 10,
    intellect: base.intellect + 10,
    willpower: base.willpower + 10,
    health: base.health + 10,
    charm: base.charm + 10,
  };

  console.log('[relay] Registering with stats:', JSON.stringify(registerOpts.stats));
  registrationSent = true;
  send({
    type: 'register',
    username,
    password,
    characterName: registerOpts.charName,
    characterClass: registerOpts.charClass,
    race: registerOpts.race,
    gender: registerOpts.gender,
    allocatedStats: registerOpts.stats,
  });
}

// ---------------------------------------------------------------------------
// Game state model
// ---------------------------------------------------------------------------
const state = {
  connected: false,
  loggedIn: false,
  player: null,
  room: null,
  npcsInRoom: [],
  playersInRoom: [],
  groundItems: [],
  groundCoins: { copper: 0, silver: 0, gold: 0 },
  inventory: [],
  equipment: {},
  coins: { copper: 0, silver: 0, gold: 0 },
  attackMode: false,
  selectedTarget: null,
  isHidden: false,
  isMeditating: false,
  isResting: false,
  activeEffects: [],
  availableSkills: [],
  availableSpells: [],
  recentEvents: [],
};

function pushEvent(type, summary) {
  const time = new Date().toLocaleTimeString('en-US', { hour12: false });
  state.recentEvents.push({ time, type, summary });
  if (state.recentEvents.length > MAX_RECENT_EVENTS) {
    state.recentEvents = state.recentEvents.slice(-MAX_RECENT_EVENTS);
  }
  scheduleStateWrite();
}

// ---------------------------------------------------------------------------
// State file writing (debounced, atomic)
// ---------------------------------------------------------------------------
let writeTimer = null;

function scheduleStateWrite() {
  if (writeTimer) return;
  writeTimer = setTimeout(() => {
    writeTimer = null;
    writeStateFile();
  }, STATE_WRITE_DEBOUNCE_MS);
}

function writeStateFile() {
  try {
    const json = JSON.stringify(state, null, 2);
    fs.writeFileSync(TEMP_STATE_FILE, json, 'utf8');
    fs.renameSync(TEMP_STATE_FILE, STATE_FILE);
  } catch (err) {
    console.error('[relay] Failed to write state file:', err.message);
  }
}

// ---------------------------------------------------------------------------
// ServerMessage handlers
// ---------------------------------------------------------------------------
const handlers = {
  // Auth
  register_ok() {
    pushEvent('system', 'Registration successful. Logging in...');
    send({ type: 'login', username, password });
  },
  login_ok(msg) {
    state.loggedIn = true;
    const p = msg.player;
    state.player = {
      name: p.name,
      class: p.characterClass,
      race: p.race || '',
      gender: p.gender || '',
      level: p.level,
      hp: p.currentHp,
      maxHp: p.maxHp,
      mp: p.currentMp || 0,
      maxMp: p.maxMp || 0,
      xp: p.currentXp || 0,
      xpToNextLevel: p.xpToNextLevel || 0,
      stats: p.stats || null,
    };
    pushEvent('system', `Logged in as ${p.name} (Lv${p.level} ${p.race || ''} ${p.characterClass})`);
    computePlayerAbilities();
  },
  auth_error(msg) {
    pushEvent('error', `Auth error: ${msg.reason}`);
    console.error('[relay] Auth error:', msg.reason);
  },

  // Room / Movement
  room_info(msg) {
    updateRoom(msg.room, msg.players, msg.npcs);
    pushEvent('room', `Entered ${msg.room.name}`);
  },
  move_ok(msg) {
    updateRoom(msg.room, msg.players, msg.npcs);
    pushEvent('move', `Moved ${msg.direction} to ${msg.room.name}`);
  },
  move_error(msg) {
    pushEvent('error', `Move failed: ${msg.reason}`);
  },

  // Presence
  player_entered(msg) {
    if (msg.playerInfo) {
      state.playersInRoom = state.playersInRoom.filter(p => p.name !== msg.playerName);
      state.playersInRoom.push(formatPlayerInfo(msg.playerInfo));
    }
    pushEvent('presence', `${msg.playerName} entered the room`);
  },
  player_left(msg) {
    state.playersInRoom = state.playersInRoom.filter(p => p.name !== msg.playerName);
    pushEvent('presence', `${msg.playerName} left ${msg.direction}`);
  },
  npc_entered(msg) {
    if (msg.spawned) {
      pushEvent('spawn', `${msg.npcName} appeared`);
    } else {
      pushEvent('presence', `${msg.npcName} entered the room`);
    }
    state.npcsInRoom.push({
      id: msg.npcId,
      name: msg.npcName,
      templateId: msg.templateId || '',
      hostile: msg.hostile,
      hp: msg.currentHp,
      maxHp: msg.maxHp,
    });
    scheduleStateWrite();
  },
  npc_left(msg) {
    state.npcsInRoom = state.npcsInRoom.filter(n => n.id !== msg.npcId);
    pushEvent('presence', `${msg.npcName} left ${msg.direction}`);
  },

  // Chat
  player_says(msg) {
    pushEvent('chat', `${msg.playerName} says: "${msg.message}"`);
  },

  // Combat
  combat_hit(msg) {
    // Update NPC HP in our local list
    if (!msg.isPlayerDefender && msg.defenderId) {
      const npc = state.npcsInRoom.find(n => n.id === msg.defenderId);
      if (npc) {
        npc.hp = msg.defenderHp;
        npc.maxHp = msg.defenderMaxHp;
      }
    }
    // Update player HP if we're the defender
    if (msg.isPlayerDefender && state.player && msg.defenderName === state.player.name) {
      state.player.hp = msg.defenderHp;
      state.player.maxHp = msg.defenderMaxHp;
    }

    let summary;
    if (msg.isMiss) {
      summary = `${msg.attackerName} missed ${msg.defenderName}`;
    } else if (msg.isDodge) {
      summary = `${msg.defenderName} dodged ${msg.attackerName}'s attack`;
    } else if (msg.isParry) {
      summary = `${msg.defenderName} parried ${msg.attackerName}'s attack`;
    } else if (msg.isBackstab) {
      summary = `${msg.attackerName} backstabbed ${msg.defenderName} for ${msg.damage} damage (${msg.defenderHp}/${msg.defenderMaxHp} HP)`;
    } else {
      summary = `${msg.attackerName} hit ${msg.defenderName} for ${msg.damage} damage (${msg.defenderHp}/${msg.defenderMaxHp} HP)`;
    }
    pushEvent('combat_hit', summary);
  },
  skill_effect(msg) {
    if (msg.targetId) {
      const npc = state.npcsInRoom.find(n => n.id === msg.targetId);
      if (npc) {
        npc.hp = msg.targetHp;
        npc.maxHp = msg.targetMaxHp;
      }
    }
    pushEvent('skill_effect', `${msg.userName} used ${msg.skillName} on ${msg.targetName}: ${msg.damage} damage (${msg.targetHp}/${msg.targetMaxHp} HP)`);
  },
  spell_effect(msg) {
    if (msg.targetId && !msg.isPlayerTarget) {
      const npc = state.npcsInRoom.find(n => n.id === msg.targetId);
      if (npc) {
        npc.hp = msg.targetNewHp;
        npc.maxHp = msg.targetMaxHp;
      }
    }
    if (msg.isPlayerTarget && state.player && msg.targetName === state.player.name) {
      state.player.hp = msg.targetNewHp;
      state.player.maxHp = msg.targetMaxHp;
    }
    pushEvent('spell_effect', `${msg.casterName} cast ${msg.spellName} on ${msg.targetName}: ${msg.effectAmount} (${msg.targetNewHp}/${msg.targetMaxHp} HP)`);
  },
  spell_cast_result(msg) {
    if (state.player) state.player.mp = msg.newMp;
    if (msg.newHp != null && state.player) state.player.hp = msg.newHp;
    pushEvent('spell', `${msg.spellName}: ${msg.message}`);
  },
  npc_died(msg) {
    state.npcsInRoom = state.npcsInRoom.filter(n => n.id !== msg.npcId);
    pushEvent('npc_killed', `${msg.npcName} was killed by ${msg.killerName}`);
  },
  player_died(msg) {
    if (state.player) {
      state.player.hp = msg.respawnHp;
      state.player.mp = msg.respawnMp || 0;
    }
    state.attackMode = false;
    state.selectedTarget = null;
    pushEvent('player_died', `Killed by ${msg.killerName}! Respawned.`);
  },
  attack_mode_update(msg) {
    state.attackMode = msg.enabled;
    pushEvent('combat', `Attack mode ${msg.enabled ? 'ON' : 'OFF'}`);
  },

  // Effects
  active_effects_update(msg) {
    state.activeEffects = (msg.effects || []).map(e => ({
      name: e.name,
      remainingTicks: e.remainingTicks,
      type: e.type || '',
    }));
    scheduleStateWrite();
  },
  effect_tick(msg) {
    if (state.player) state.player.hp = msg.newHp;
    if (msg.newMp >= 0 && state.player) state.player.mp = msg.newMp;
    pushEvent('effect_tick', msg.message);
  },

  // Stealth / Meditation
  stealth_update(msg) {
    state.isHidden = msg.hidden;
    if (msg.message) pushEvent('stealth', msg.message);
    else pushEvent('stealth', msg.hidden ? 'You are hidden' : 'You are visible');
  },
  meditate_update(msg) {
    state.isMeditating = msg.meditating;
    if (msg.message) pushEvent('meditate', msg.message);
    else pushEvent('meditate', msg.meditating ? 'Meditating...' : 'Stopped meditating');
  },
  rest_update(msg) {
    state.isResting = msg.resting;
    if (msg.message) pushEvent('rest', msg.message);
    else pushEvent('rest', msg.resting ? 'Resting...' : 'Stopped resting');
  },
  track_result(msg) {
    pushEvent('track', msg.message);
  },

  // Inventory / Items
  inventory_update(msg) {
    state.inventory = (msg.inventory || []).map(formatInventoryItem);
    state.equipment = msg.equipment || {};
    if (msg.coins) state.coins = formatCoins(msg.coins);
    scheduleStateWrite();
  },
  room_items_update(msg) {
    state.groundItems = (msg.items || []).map(i => ({
      itemId: i.itemId,
      name: itemCatalogMap[i.itemId] || i.itemId,
      quantity: i.quantity || 1,
    }));
    state.groundCoins = formatCoins(msg.coins);
    scheduleStateWrite();
  },
  loot_received(msg) {
    const names = (msg.items || []).map(i => `${i.itemName}${i.quantity > 1 ? ' x' + i.quantity : ''}`).join(', ');
    pushEvent('loot', `Received from ${msg.npcName}: ${names}`);
  },
  loot_dropped(msg) {
    const names = (msg.items || []).map(i => `${i.itemName}${i.quantity > 1 ? ' x' + i.quantity : ''}`).join(', ');
    const coinStr = formatCoinString(msg.coins);
    const parts = [names, coinStr].filter(Boolean);
    pushEvent('loot', `${msg.npcName} dropped: ${parts.join(', ') || 'nothing'}`);
  },
  pickup_result(msg) {
    pushEvent('pickup', `Picked up ${msg.quantity}x ${msg.itemName}${msg.isCoin ? ' (coins)' : ''}`);
  },
  item_used(msg) {
    if (state.player) {
      state.player.hp = msg.newHp;
      state.player.mp = msg.newMp;
    }
    pushEvent('item', `Used ${msg.itemName}: ${msg.message}`);
  },
  equip_update(msg) {
    if (msg.itemId) {
      state.equipment[msg.slot] = msg.itemId;
    } else {
      delete state.equipment[msg.slot];
    }
    pushEvent('equip', msg.itemId ? `Equipped ${msg.itemName} in ${msg.slot}` : `Unequipped ${msg.slot}`);
  },

  // Progression
  xp_gained(msg) {
    if (state.player) {
      state.player.xp = msg.currentXp;
      state.player.xpToNextLevel = msg.xpToNextLevel;
    }
    pushEvent('xp', `Gained ${msg.amount} XP (${msg.currentXp}/${msg.xpToNextLevel})`);
  },
  level_up(msg) {
    if (state.player) {
      state.player.level = msg.newLevel;
      state.player.maxHp = msg.newMaxHp;
      state.player.hp = msg.newMaxHp;      // Full heal on level up
      state.player.maxMp = msg.newMaxMp;
      state.player.mp = msg.newMaxMp;      // Full mana on level up
      state.player.unspentCp = msg.totalUnspentCp;
      state.player.xpToNextLevel = msg.xpToNextLevel;
    }
    pushEvent('level_up', `LEVEL UP! Now level ${msg.newLevel} (+${msg.hpRoll} HP, +${msg.mpRoll} MP, +${msg.cpGained} CP)`);
    computePlayerAbilities();
  },
  trainer_info(msg) {
    const lines = ['The trainer can help you level up and allocate Character Points (CP) to improve your stats. You earn CP each time you level up.'];
    if (msg.canLevelUp) {
      lines.push('You are ready to level up!');
    } else {
      lines.push('You are not ready to level up yet. Gain more XP by defeating enemies.');
    }
    if (msg.totalCpEarned > 0) {
      lines.push(`CP: ${msg.unspentCp} unspent / ${msg.totalCpEarned} total earned.`);
    } else {
      lines.push('You have no CP yet. Level up to earn CP for stat training!');
    }
    pushEvent('trainer', lines.join(' '));
  },
  stat_trained(msg) {
    pushEvent('trainer', `Trained ${msg.stat} to ${msg.newValue} (${msg.remainingCp} CP remaining)`);
  },

  // Vendor
  vendor_info(msg) {
    const itemList = (msg.items || []).map(i => `${i.item?.name || i.itemId || '?'} (${formatCoinString(i.price) || 'free'})`).join(', ');
    pushEvent('vendor', `${msg.vendorName} sells: ${itemList}`);
  },
  buy_result(msg) {
    if (msg.success) {
      state.inventory = (msg.updatedInventory || []).map(formatInventoryItem);
      state.equipment = msg.equipment || state.equipment;
      state.coins = formatCoins(msg.updatedCoins);
    }
    pushEvent('vendor', `Buy: ${msg.message}`);
  },
  sell_result(msg) {
    if (msg.success) {
      state.inventory = (msg.updatedInventory || []).map(formatInventoryItem);
      state.equipment = msg.equipment || state.equipment;
      state.coins = formatCoins(msg.updatedCoins);
    }
    pushEvent('vendor', `Sell: ${msg.message}`);
  },

  // World features
  interact_result(msg) {
    pushEvent('interact', `${msg.featureName}: ${msg.message}`);
  },

  // System
  system_message(msg) {
    pushEvent('system_message', msg.message);
  },
  error(msg) {
    pushEvent('error', msg.message);
  },
  pong() { /* no-op */ },

  // Catalog syncs — store class/race for registration, log all
  class_catalog_sync(msg) {
    classCatalog = msg.classes || [];
    catalogsReceived.classes = true;
    pushEvent('system', `Received class catalog (${classCatalog.length} classes)`);
    tryRegister();
  },
  item_catalog_sync(msg) {
    itemCatalogMap = {};
    for (const item of (msg.items || [])) {
      itemCatalogMap[item.id] = item.name;
    }
    pushEvent('system', `Received item catalog (${Object.keys(itemCatalogMap).length} items)`);
  },
  skill_catalog_sync(msg) {
    skillCatalog = msg.skills || [];
    pushEvent('system', `Received skill catalog (${skillCatalog.length} skills)`);
    computePlayerAbilities();
  },
  race_catalog_sync(msg) {
    raceCatalog = msg.races || [];
    catalogsReceived.races = true;
    pushEvent('system', `Received race catalog (${raceCatalog.length} races)`);
    tryRegister();
  },
  spell_catalog_sync(msg) {
    spellCatalog = msg.spells || [];
    pushEvent('system', `Received spell catalog (${spellCatalog.length} spells)`);
    computePlayerAbilities();
  },

  // Server shutdown
  server_shutdown(msg) {
    pushEvent('server_shutdown', msg.message);
    console.log(`[relay] SERVER SHUTDOWN: ${msg.message} (${msg.secondsRemaining}s remaining)`);
    if (msg.secondsRemaining <= 0) {
      serverShuttingDown = true;
    }
  },

  // Map data — just log
  map_data() { pushEvent('system', 'Received map data'); },
};

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
function updateRoom(room, players, npcs) {
  state.room = {
    id: room.id,
    name: room.name,
    description: room.description || '',
    exits: room.exits || {},
  };
  state.playersInRoom = (players || []).map(formatPlayerInfo);
  state.npcsInRoom = (npcs || []).map(n => ({
    id: n.id || '',
    name: n.name,
    templateId: n.templateId || '',
    hostile: n.hostile ?? false,
    hp: n.currentHp ?? n.hp ?? 0,
    maxHp: n.maxHp ?? 0,
  }));
  // Reset ground items — server will send room_items_update separately
  state.groundItems = [];
  state.groundCoins = { copper: 0, silver: 0, gold: 0 };
  scheduleStateWrite();
}

function formatPlayerInfo(p) {
  return {
    name: p.name || p.playerName || '',
    class: p.characterClass || '',
    level: p.level || 0,
  };
}

function formatInventoryItem(i) {
  return {
    itemId: i.itemId,
    name: itemCatalogMap[i.itemId] || i.itemId,
    quantity: i.quantity || 1,
    equipped: i.equipped || false,
    slot: i.slot || '',
  };
}

function formatCoins(c) {
  if (!c) return { copper: 0, silver: 0, gold: 0 };
  return { copper: c.copper || 0, silver: c.silver || 0, gold: c.gold || 0 };
}

function formatCoinString(c) {
  if (!c) return '';
  const parts = [];
  if (c.gold) parts.push(`${c.gold}g`);
  if (c.silver) parts.push(`${c.silver}s`);
  if (c.copper) parts.push(`${c.copper}c`);
  return parts.join(' ');
}

// ---------------------------------------------------------------------------
// WebSocket connection
// ---------------------------------------------------------------------------
let ws = null;
let pingTimer = null;
let commandPollTimer = null;
let serverShuttingDown = false;

function send(msg) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(msg));
  }
}

function connect() {
  console.log(`[relay] Connecting to ${SERVER_URL}...`);
  ws = new WebSocket(SERVER_URL);

  ws.on('open', () => {
    console.log('[relay] Connected');
    reconnectAttempts = 0;
    state.connected = true;
    scheduleStateWrite();

    // Authenticate (register waits for catalogs; login sends immediately)
    registrationSent = false;
    if (!registerMode) {
      send({ type: 'login', username, password });
    }
    // If registerMode, tryRegister() fires once catalogs arrive

    // Keepalive ping
    pingTimer = setInterval(() => send({ type: 'ping' }), PING_INTERVAL_MS);

    // Start polling for command file
    commandPollTimer = setInterval(pollCommandFile, COMMAND_POLL_MS);
  });

  ws.on('message', (data) => {
    try {
      const msg = JSON.parse(data.toString());
      const handler = handlers[msg.type];
      if (handler) {
        handler(msg);
      } else {
        console.log(`[relay] Unhandled message type: ${msg.type}`);
      }
    } catch (err) {
      console.error('[relay] Failed to parse message:', err.message);
    }
  });

  ws.on('close', (code, reason) => {
    if (serverShuttingDown) {
      console.log(`[relay] Server shut down gracefully (code=${code}). Exiting.`);
      shutdownRelay(0);
    } else {
      console.error(`[relay] Connection lost (code=${code}). Exiting.`);
      shutdownRelay(1);
    }
  });

  ws.on('error', (err) => {
    console.error(`[relay] WebSocket error: ${err.message}`);
    // close event will fire after this and trigger shutdownRelay
  });
}

function cleanup() {
  if (pingTimer) { clearInterval(pingTimer); pingTimer = null; }
  if (commandPollTimer) { clearInterval(commandPollTimer); commandPollTimer = null; }
}

// ---------------------------------------------------------------------------
// Command file polling
// ---------------------------------------------------------------------------
async function pollCommandFile() {
  if (!fs.existsSync(COMMAND_FILE)) return;

  let commands;
  try {
    const raw = fs.readFileSync(COMMAND_FILE, 'utf8');
    commands = JSON.parse(raw);
    // Delete immediately so we don't re-process
    fs.unlinkSync(COMMAND_FILE);
  } catch (err) {
    console.error('[relay] Failed to read command file:', err.message);
    // Try to delete corrupt file
    try { fs.unlinkSync(COMMAND_FILE); } catch {}
    return;
  }

  if (!Array.isArray(commands)) {
    commands = [commands];
  }

  console.log(`[relay] Processing ${commands.length} command(s)`);
  for (const cmd of commands) {
    send(cmd);
    console.log(`[relay]   -> ${cmd.type}`);
    if (commands.length > 1) {
      await sleep(COMMAND_SPACING_MS);
    }
  }
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// ---------------------------------------------------------------------------
// PID lockfile — prevent multiple relay instances
// ---------------------------------------------------------------------------
function isProcessRunning(pid) {
  try {
    process.kill(pid, 0); // signal 0 = existence check, doesn't kill
    return true;
  } catch {
    return false; // ESRCH = no such process
  }
}

function acquireLock() {
  try {
    const existing = fs.readFileSync(LOCK_FILE, 'utf8').trim();
    const pid = parseInt(existing, 10);
    if (!isNaN(pid) && pid !== process.pid && isProcessRunning(pid)) {
      console.error(`[relay] Another relay is already running (PID ${pid}).`);
      console.error('[relay] Kill it first or delete scripts/relay.lock to override.');
      process.exit(1);
    }
    // Stale lock from a dead process — reclaim it
    console.log(`[relay] Removing stale lock (PID ${pid})`);
  } catch {
    // No lock file exists — fine
  }
  fs.writeFileSync(LOCK_FILE, String(process.pid), 'utf8');
}

function releaseLock() {
  try {
    const contents = fs.readFileSync(LOCK_FILE, 'utf8').trim();
    if (contents === String(process.pid)) {
      fs.unlinkSync(LOCK_FILE);
    }
  } catch {}
}

// ---------------------------------------------------------------------------
// Startup
// ---------------------------------------------------------------------------
console.log('[relay] NeoMud Game Relay');
console.log(`[relay] User: ${username}, Mode: ${registerMode ? 'register' : 'login'}`);

acquireLock();

// Clean up stale files
try { fs.unlinkSync(STATE_FILE); } catch {}
try { fs.unlinkSync(COMMAND_FILE); } catch {}
try { fs.unlinkSync(TEMP_STATE_FILE); } catch {}

// Write initial state
writeStateFile();
connect();

// Graceful shutdown
function shutdownRelay(exitCode = 0) {
  cleanup();
  if (ws) { try { ws.close(); } catch {} }
  try { fs.unlinkSync(STATE_FILE); } catch {}
  try { fs.unlinkSync(TEMP_STATE_FILE); } catch {}
  releaseLock();
  process.exit(exitCode);
}

process.on('SIGINT', () => {
  console.log('\n[relay] Shutting down...');
  shutdownRelay(0);
});
process.on('SIGTERM', () => {
  console.log('[relay] Received SIGTERM. Shutting down...');
  shutdownRelay(0);
});
