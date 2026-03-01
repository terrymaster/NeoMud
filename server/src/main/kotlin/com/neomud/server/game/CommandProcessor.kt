package com.neomud.server.game

import com.neomud.server.game.commands.AdminCommand
import com.neomud.server.game.commands.AttackCommand
import com.neomud.server.game.commands.BashCommand
import com.neomud.server.game.commands.InventoryCommand
import com.neomud.server.game.commands.KickCommand
import com.neomud.server.game.commands.LookCommand
import com.neomud.server.game.commands.MeditateCommand
import com.neomud.server.game.commands.RestCommand
import com.neomud.server.game.commands.MoveCommand
import com.neomud.server.game.commands.InteractCommand
import com.neomud.server.game.commands.PickLockCommand
import com.neomud.server.game.commands.PickupCommand
import com.neomud.server.game.commands.SayCommand
import com.neomud.server.game.commands.SneakCommand
import com.neomud.server.game.commands.SpellCommand
import com.neomud.server.game.commands.TrackCommand
import com.neomud.server.game.commands.TrainerCommand
import com.neomud.server.game.commands.VendorCommand
import com.neomud.server.game.inventory.LootService
import com.neomud.server.game.inventory.RoomItemManager
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.persistence.repository.CoinRepository
import com.neomud.server.persistence.repository.DiscoveryRepository
import com.neomud.server.persistence.repository.InventoryRepository
import com.neomud.server.persistence.repository.PlayerRepository
import com.neomud.shared.model.Coins
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.ClassCatalog
import com.neomud.server.world.ItemCatalog
import com.neomud.server.world.LootTableCatalog
import com.neomud.server.world.RaceCatalog
import com.neomud.server.world.SkillCatalog
import com.neomud.server.world.PcSpriteCatalog
import com.neomud.server.world.SpellCatalog
import com.neomud.server.world.WorldGraph
import com.neomud.shared.protocol.ClientMessage
import com.neomud.shared.protocol.ServerMessage

import org.slf4j.LoggerFactory

class CommandProcessor(
    private val worldGraph: WorldGraph,
    private val sessionManager: SessionManager,
    private val npcManager: NpcManager,
    private val playerRepository: PlayerRepository,
    private val classCatalog: ClassCatalog,
    private val itemCatalog: ItemCatalog,
    private val skillCatalog: SkillCatalog,
    private val raceCatalog: RaceCatalog,
    private val inventoryCommand: InventoryCommand,
    private val pickupCommand: PickupCommand,
    private val roomItemManager: RoomItemManager,
    private val trainerCommand: TrainerCommand,
    private val spellCommand: SpellCommand,
    private val spellCatalog: SpellCatalog,
    private val vendorCommand: VendorCommand,
    private val lootService: LootService,
    private val lootTableCatalog: LootTableCatalog,
    private val inventoryRepository: InventoryRepository,
    private val coinRepository: CoinRepository,
    private val discoveryRepository: DiscoveryRepository,
    private val adminUsernames: Set<String> = emptySet(),
    private val movementTrailManager: MovementTrailManager? = null,
    private val pcSpriteCatalog: PcSpriteCatalog? = null
) {
    private val logger = LoggerFactory.getLogger(CommandProcessor::class.java)
    private val adminCommand = AdminCommand(
        sessionManager, playerRepository, npcManager, worldGraph,
        inventoryCommand, inventoryRepository, itemCatalog, classCatalog, raceCatalog, roomItemManager
    )
    fun setGameLoop(loop: GameLoop) {
        adminCommand.setGameLoop(loop)
    }

    private val moveCommand = MoveCommand(worldGraph, sessionManager, npcManager, playerRepository, roomItemManager, skillCatalog, classCatalog, movementTrailManager)
    private val lookCommand = LookCommand(worldGraph, sessionManager, npcManager, roomItemManager, skillCatalog, classCatalog)
    private val sayCommand = SayCommand(sessionManager, adminCommand)
    private val attackCommand = AttackCommand(npcManager, worldGraph)
    private val sneakCommand = SneakCommand(sessionManager, npcManager, skillCatalog, classCatalog)
    private val bashCommand = BashCommand(npcManager)
    private val kickCommand = KickCommand(npcManager, worldGraph)
    private val meditateCommand = MeditateCommand()
    private val restCommand = RestCommand()
    private val trackCommand = TrackCommand()
    private val pickLockCommand = PickLockCommand(worldGraph, sessionManager, npcManager)
    private val interactCommand = InteractCommand(worldGraph, sessionManager, npcManager, roomItemManager, lootService, lootTableCatalog)

    suspend fun sendCatalogSync(session: PlayerSession) {
        session.send(ServerMessage.ClassCatalogSync(classCatalog.getAllClasses()))
        session.send(ServerMessage.ItemCatalogSync(itemCatalog.getAllItems()))
        session.send(ServerMessage.SkillCatalogSync(skillCatalog.getAllSkills()))
        session.send(ServerMessage.RaceCatalogSync(raceCatalog.getAllRaces()))
        session.send(ServerMessage.SpellCatalogSync(spellCatalog.getAllSpells()))
    }

    suspend fun process(session: PlayerSession, message: ClientMessage) {
        when (message) {
            // Auth and read-only commands don't need the game state lock
            is ClientMessage.Register -> handleRegister(session, message)
            is ClientMessage.Login -> handleLogin(session, message)
            is ClientMessage.Ping -> session.send(ServerMessage.Pong)
            // All state-mutating commands acquire the global mutex
            else -> GameStateLock.withLock { processLocked(session, message) }
        }
    }

    private suspend fun processLocked(session: PlayerSession, message: ClientMessage) {
        when (message) {
            is ClientMessage.Move -> {
                requireAuth(session) { moveCommand.execute(session, message.direction) }
            }
            is ClientMessage.Look -> {
                requireAuth(session) { lookCommand.execute(session) }
            }
            is ClientMessage.Say -> {
                requireAuth(session) { sayCommand.execute(session, message.message) }
            }
            is ClientMessage.AttackToggle -> {
                requireAuth(session) { attackCommand.handleToggle(session, message.enabled) }
            }
            is ClientMessage.SelectTarget -> {
                requireAuth(session) { attackCommand.handleSelectTarget(session, message.npcId) }
            }
            is ClientMessage.ViewInventory -> {
                requireAuth(session) { inventoryCommand.handleViewInventory(session) }
            }
            is ClientMessage.EquipItem -> {
                requireAuth(session) { inventoryCommand.handleEquipItem(session, message.itemId, message.slot) }
            }
            is ClientMessage.UnequipItem -> {
                requireAuth(session) { inventoryCommand.handleUnequipItem(session, message.slot) }
            }
            is ClientMessage.UseItem -> {
                requireAuth(session) { inventoryCommand.handleUseItem(session, message.itemId) }
            }
            is ClientMessage.PickupItem -> {
                requireAuth(session) { pickupCommand.handlePickupItem(session, message.itemId, message.quantity) }
            }
            is ClientMessage.PickupCoins -> {
                requireAuth(session) { pickupCommand.handlePickupCoins(session, message.coinType) }
            }
            is ClientMessage.SneakToggle -> {
                requireAuth(session) {
                    if (message.enabled && !canUseSkill(session, "SNEAK")) return@requireAuth
                    sneakCommand.handleToggle(session, message.enabled)
                }
            }
            is ClientMessage.UseSkill -> {
                requireAuth(session) {
                    val skillId = message.skillId.removePrefix("skill:").uppercase()
                    if (!canUseSkill(session, skillId)) return@requireAuth
                    when (skillId) {
                        "BASH" -> bashCommand.execute(session, message.targetId)
                        "KICK" -> kickCommand.execute(session, message.targetId)
                        "MEDITATE" -> meditateCommand.execute(session)
                        "REST" -> restCommand.execute(session)
                        "TRACK" -> trackCommand.execute(session, message.targetId)
                        "PICK_LOCK" -> {
                            val unlocked = pickLockCommand.execute(session, message.targetId)
                            if (unlocked) lookCommand.execute(session)
                        }
                        else -> session.send(ServerMessage.SystemMessage("Unknown skill: $skillId"))
                    }
                }
            }
            is ClientMessage.InteractTrainer -> {
                requireAuth(session) { trainerCommand.handleInteract(session) }
            }
            is ClientMessage.TrainLevelUp -> {
                requireAuth(session) { trainerCommand.handleLevelUp(session) }
            }
            is ClientMessage.TrainStat -> {
                requireAuth(session) { trainerCommand.handleTrainStat(session, message.stat, message.points) }
            }
            is ClientMessage.AllocateTrainedStats -> {
                requireAuth(session) { trainerCommand.handleAllocateTrainedStats(session, message.stats) }
            }
            is ClientMessage.CastSpell -> {
                requireAuth(session) {
                    spellCommand.execute(session, message.spellId, message.targetId)
                }
            }
            is ClientMessage.InteractVendor -> {
                requireAuth(session) { vendorCommand.handleInteract(session) }
            }
            is ClientMessage.BuyItem -> {
                requireAuth(session) { vendorCommand.handleBuy(session, message.itemId, message.quantity) }
            }
            is ClientMessage.SellItem -> {
                requireAuth(session) { vendorCommand.handleSell(session, message.itemId, message.quantity) }
            }
            is ClientMessage.InteractFeature -> {
                requireAuth(session) { interactCommand.execute(session, message.featureId) }
            }
            is ClientMessage.ReadySpell -> {
                requireAuth(session) { handleReadySpell(session, message) }
            }
            else -> {} // Register, Login, Ping already handled in process()
        }
    }

    private suspend fun handleRegister(session: PlayerSession, msg: ClientMessage.Register) {
        if (session.isAuthenticated) {
            session.send(ServerMessage.AuthError("Already logged in"))
            return
        }

        val nameRegex = Regex("^[a-zA-Z0-9_]{3,20}$")
        if (!nameRegex.matches(msg.username)) {
            session.send(ServerMessage.AuthError("Username must be 3-20 alphanumeric characters or underscores."))
            return
        }
        if (msg.password.length < 4 || msg.password.length > 64) {
            session.send(ServerMessage.AuthError("Password must be 4-64 characters."))
            return
        }
        if (!Regex("^[a-zA-Z][a-zA-Z0-9_ ]{1,19}$").matches(msg.characterName)) {
            session.send(ServerMessage.AuthError("Character name must be 2-20 characters, start with a letter, and contain only letters, numbers, spaces, or underscores."))
            return
        }

        val result = playerRepository.createPlayer(
            username = msg.username,
            password = msg.password,
            characterName = msg.characterName,
            characterClass = msg.characterClass,
            race = msg.race,
            gender = msg.gender,
            allocatedStats = msg.allocatedStats,
            spawnRoomId = worldGraph.defaultSpawnRoom,
            classCatalog = classCatalog,
            raceCatalog = raceCatalog,
            pcSpriteCatalog = pcSpriteCatalog
        )

        result.fold(
            onSuccess = {
                // Grant starter equipment
                val starterWeapon = GameConfig.StarterEquipment.weaponForClass(msg.characterClass)
                inventoryRepository.addItem(msg.characterName, starterWeapon)
                inventoryRepository.equipItem(msg.characterName, starterWeapon, "weapon")
                inventoryRepository.addItem(msg.characterName, GameConfig.StarterEquipment.ARMOR_ITEM_ID)
                inventoryRepository.equipItem(msg.characterName, GameConfig.StarterEquipment.ARMOR_ITEM_ID, GameConfig.StarterEquipment.ARMOR_SLOT)
                coinRepository.addCoins(msg.characterName, Coins(copper = GameConfig.StarterEquipment.STARTING_COPPER))
                logger.info("Granted starter equipment to ${msg.characterName}: $starterWeapon + ${GameConfig.StarterEquipment.ARMOR_ITEM_ID} + ${GameConfig.StarterEquipment.STARTING_COPPER} copper")

                session.send(ServerMessage.RegisterOk)
                logger.info("Player registered: ${msg.characterName}")
            },
            onFailure = {
                session.send(ServerMessage.AuthError(it.message ?: "Registration failed"))
            }
        )
    }

    private suspend fun handleLogin(session: PlayerSession, msg: ClientMessage.Login) {
        if (session.isAuthenticated) {
            session.send(ServerMessage.AuthError("Already logged in"))
            return
        }

        if (sessionManager.isLoggedIn(msg.username)) {
            session.send(ServerMessage.AuthError("Account already logged in"))
            return
        }

        val result = playerRepository.authenticate(msg.username, msg.password)

        result.fold(
            onSuccess = { player ->
                // Auto-promote admin from env var
                val effectivePlayer = if (msg.username.lowercase() in adminUsernames && !player.isAdmin) {
                    playerRepository.promoteAdmin(msg.username)
                    player.copy(isAdmin = true)
                } else {
                    player
                }

                session.player = effectivePlayer
                session.playerName = effectivePlayer.name
                session.currentRoomId = effectivePlayer.currentRoomId

                // Load persisted discovery data
                val discovery = discoveryRepository.loadPlayerDiscovery(effectivePlayer.name)
                session.visitedRooms.addAll(discovery.visitedRooms)
                session.discoveredHiddenExits.addAll(discovery.discoveredHiddenExits)
                session.discoveredLockedExits.addAll(discovery.discoveredLockedExits)
                session.discoveredInteractables.addAll(discovery.discoveredInteractables)
                session.visitedRooms.add(effectivePlayer.currentRoomId)

                sessionManager.addSession(effectivePlayer.name, session)
                session.combatGraceTicks = GameConfig.Combat.GRACE_TICKS

                session.send(ServerMessage.LoginOk(effectivePlayer))

                // Send initial room info
                val room = worldGraph.getRoom(effectivePlayer.currentRoomId)
                if (room != null) {
                    val playersInRoom = sessionManager.getVisiblePlayerInfosInRoom(effectivePlayer.currentRoomId)
                        .filter { it.name != effectivePlayer.name }
                    val npcsInRoom = npcManager.getNpcsInRoom(effectivePlayer.currentRoomId)
                    session.send(ServerMessage.RoomInfo(room, playersInRoom, npcsInRoom))

                    val mapRooms = MapRoomFilter.enrichForPlayer(
                        worldGraph.getRoomsNear(effectivePlayer.currentRoomId), session, worldGraph, sessionManager, npcManager
                    )
                    session.send(ServerMessage.MapData(mapRooms, effectivePlayer.currentRoomId, session.visitedRooms.toSet()))

                    // Broadcast to others in room
                    sessionManager.broadcastToRoom(
                        effectivePlayer.currentRoomId,
                        ServerMessage.PlayerEntered(effectivePlayer.name, effectivePlayer.currentRoomId, session.toPlayerInfo()),
                        exclude = effectivePlayer.name
                    )
                }

                // Send initial inventory
                inventoryCommand.sendInventoryUpdate(session)

                // Send ground items for current room
                val groundItems = roomItemManager.getGroundItems(effectivePlayer.currentRoomId)
                val groundCoins = roomItemManager.getGroundCoins(effectivePlayer.currentRoomId)
                session.send(ServerMessage.RoomItemsUpdate(groundItems, groundCoins))

                logger.info("Player logged in: ${effectivePlayer.name}${if (effectivePlayer.isAdmin) " [ADMIN]" else ""}")
            },
            onFailure = {
                session.send(ServerMessage.AuthError(it.message ?: "Login failed"))
            }
        )
    }

    private suspend fun handleReadySpell(session: PlayerSession, msg: ClientMessage.ReadySpell) {
        val player = session.player ?: return
        val spellId = msg.spellId

        if (spellId == null) {
            session.readiedSpellId = null
            return
        }

        val spell = spellCatalog.getSpell(spellId)
        if (spell == null) {
            session.send(ServerMessage.SystemMessage("Unknown spell."))
            return
        }

        val classDef = classCatalog.getClass(player.characterClass)
        val schoolLevel = classDef?.magicSchools?.get(spell.school)
        if (schoolLevel == null) {
            session.send(ServerMessage.SystemMessage("Your class cannot cast ${spell.school} spells."))
            return
        }
        if (schoolLevel < spell.schoolLevel) {
            session.send(ServerMessage.SystemMessage("Your training in ${spell.school} magic is not advanced enough."))
            return
        }
        if (player.level < spell.levelRequired) {
            session.send(ServerMessage.SystemMessage("You need level ${spell.levelRequired} to cast ${spell.name}."))
            return
        }

        session.readiedSpellId = spellId

        // Entering spell combat breaks meditation, rest, stealth, and grace period
        MeditationUtils.breakMeditation(session, "You stop meditating.")
        RestUtils.breakRest(session, "You stop resting.")
        StealthUtils.breakStealth(session, sessionManager, "Casting a spell reveals your presence!")
        session.combatGraceTicks = 0
    }

    /**
     * Check if the player's class can use the given skill.
     * Returns true if allowed, false (and sends error) if not.
     */
    private suspend fun canUseSkill(session: PlayerSession, skillId: String): Boolean {
        val player = session.player ?: return false
        val skill = skillCatalog.getSkill(skillId)
        if (skill == null) {
            session.send(ServerMessage.SystemMessage("Unknown skill: $skillId"))
            return false
        }
        if (skill.classRestrictions.isNotEmpty() && player.characterClass !in skill.classRestrictions) {
            session.send(ServerMessage.SystemMessage("Your class cannot use ${skill.name}."))
            return false
        }
        return true
    }

    private suspend inline fun requireAuth(session: PlayerSession, block: () -> Unit) {
        if (!session.isAuthenticated) {
            session.send(ServerMessage.Error("You must log in first"))
            return
        }
        block()
    }
}
