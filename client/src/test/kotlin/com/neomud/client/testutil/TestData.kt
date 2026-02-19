package com.neomud.client.testutil

import com.neomud.shared.model.*
import com.neomud.shared.protocol.ServerMessage

object TestData {

    fun player(
        name: String = "TestHero",
        characterClass: String = "warrior",
        currentHp: Int = 80,
        maxHp: Int = 100,
        currentMp: Int = 30,
        maxMp: Int = 50,
        level: Int = 5,
        race: String = "human",
        currentXp: Long = 1200,
        xpToNextLevel: Long = 2000,
        unspentCp: Int = 0,
        stats: Stats = Stats()
    ) = Player(
        name = name,
        characterClass = characterClass,
        stats = stats,
        currentHp = currentHp,
        maxHp = maxHp,
        currentMp = currentMp,
        maxMp = maxMp,
        level = level,
        currentRoomId = "town:square",
        race = race,
        currentXp = currentXp,
        xpToNextLevel = xpToNextLevel,
        unspentCp = unspentCp
    )

    fun item(
        id: String = "iron_sword",
        name: String = "Iron Sword",
        type: String = "weapon",
        slot: String = "weapon",
        damageBonus: Int = 5,
        damageRange: Int = 3,
        armorValue: Int = 0,
        value: Int = 100,
        stackable: Boolean = false,
        useEffect: String = ""
    ) = Item(
        id = id,
        name = name,
        description = "A test item",
        type = type,
        slot = slot,
        damageBonus = damageBonus,
        damageRange = damageRange,
        armorValue = armorValue,
        value = value,
        stackable = stackable,
        useEffect = useEffect
    )

    fun inventoryItem(
        itemId: String = "iron_sword",
        quantity: Int = 1,
        equipped: Boolean = false,
        slot: String = ""
    ) = InventoryItem(
        itemId = itemId,
        quantity = quantity,
        equipped = equipped,
        slot = slot
    )

    fun coins(
        copper: Int = 50,
        silver: Int = 10,
        gold: Int = 2,
        platinum: Int = 0
    ) = Coins(copper = copper, silver = silver, gold = gold, platinum = platinum)

    fun npc(
        id: String = "goblin_01",
        name: String = "Goblin",
        hostile: Boolean = true,
        currentHp: Int = 30,
        maxHp: Int = 30
    ) = Npc(
        id = id,
        name = name,
        description = "A test NPC",
        currentRoomId = "town:square",
        behaviorType = "unknown",
        hostile = hostile,
        currentHp = currentHp,
        maxHp = maxHp
    )

    fun groundItem(
        itemId: String = "health_potion",
        quantity: Int = 1
    ) = GroundItem(itemId = itemId, quantity = quantity)

    fun vendorItem(
        item: Item = item(id = "shop_sword", name = "Shop Sword"),
        price: Coins = Coins(gold = 1)
    ) = VendorItem(item = item, price = price)

    fun vendorInfo(
        vendorName: String = "Test Vendor",
        items: List<VendorItem> = listOf(vendorItem()),
        playerCoins: Coins = coins(),
        playerInventory: List<InventoryItem> = emptyList(),
        playerCharm: Int = 30,
        hasHaggle: Boolean = false
    ) = ServerMessage.VendorInfo(
        vendorName = vendorName,
        items = items,
        playerCoins = playerCoins,
        playerInventory = playerInventory,
        playerCharm = playerCharm,
        hasHaggle = hasHaggle
    )

    fun spellDef(
        id: String = "fireball",
        name: String = "Fireball",
        school: String = "mage",
        spellType: SpellType = SpellType.DAMAGE,
        manaCost: Int = 10,
        levelRequired: Int = 1,
        targetType: TargetType = TargetType.ENEMY
    ) = SpellDef(
        id = id,
        name = name,
        description = "A test spell",
        school = school,
        spellType = spellType,
        manaCost = manaCost,
        levelRequired = levelRequired,
        targetType = targetType
    )

    fun characterClassDef(
        id: String = "warrior",
        name: String = "Warrior",
        skills: List<String> = listOf("BASH", "KICK"),
        magicSchools: Map<String, Int> = emptyMap()
    ) = CharacterClassDef(
        id = id,
        name = name,
        description = "A test class",
        minimumStats = Stats(),
        skills = skills,
        magicSchools = magicSchools
    )

    fun skillDef(
        id: String = "BASH",
        name: String = "Bash",
        category: String = "combat"
    ) = SkillDef(
        id = id,
        name = name,
        description = "A test skill",
        category = category,
        primaryStat = "strength",
        secondaryStat = "agility"
    )

    fun activeEffect(
        name: String = "Poison",
        type: EffectType = EffectType.POISON,
        remainingTicks: Int = 5,
        magnitude: Int = 3
    ) = ActiveEffect(
        name = name,
        type = type,
        remainingTicks = remainingTicks,
        magnitude = magnitude
    )

    fun itemCatalog(): Map<String, Item> = mapOf(
        "iron_sword" to item(id = "iron_sword", name = "Iron Sword", type = "weapon", slot = "weapon"),
        "iron_helm" to item(id = "iron_helm", name = "Iron Helm", type = "armor", slot = "head", armorValue = 3, damageBonus = 0, damageRange = 0),
        "chain_chest" to item(id = "chain_chest", name = "Chain Chest", type = "armor", slot = "chest", armorValue = 5, damageBonus = 0, damageRange = 0),
        "health_potion" to item(id = "health_potion", name = "Health Potion", type = "consumable", slot = "", stackable = true, useEffect = "heal:20", value = 25),
        "gold_ring" to item(id = "gold_ring", name = "Gold Ring", type = "misc", slot = "", value = 50)
    )
}
