# Playtester Agent Memory

## Characters Created
- **Brunak Ironbeard** (username: brunak01) -- DWARF WARRIOR, Level 1 (previous session)
- **Aelindra Starweave** (username: elfmage02, password: testpass123) -- ELF MAGE, Level 1, 66/100 XP

## Areas Explored
- **Town**: Temple of the Dawn (respawn), Town Square (trainer), Market Street (blacksmith vendor), The Enchanted Emporium (magic vendor), The Rusty Tankard (barkeep vendor), North Gate (guard)
- **Forest**: Forest Edge (rats, bandits, spiders), Winding Forest Path (hidden passage north), Sunlit Clearing (spider), Deep Forest (spider)
- **Locked**: Tavern Cellar (DOWN from tavern, "The way down is locked")

## Known Bugs Already Reported
- #53 -- Multiple relay instances corrupt shared state file
- #54 -- Forest Edge spider kills level 1 players instantly with no reaction time
- #55 -- No way to discover available spells -- mage cannot figure out what to cast
- #56 -- pickup_coins does not support "all" coin type
- #57 -- No HP regeneration outside of potions
- #58 -- Hostile NPCs attack immediately with no grace period for new arrivals

## Game State Observations
- **Combat**: Mage melee does 7-12 damage per hit with Wooden Staff. Bandits (20 HP, 1 dmg) manageable, rats (15 HP, 1 dmg) easy, spiders (20 HP, 2-3 dmg) deadly for mages
- **Economy**: Rat drops 1-3c, Bandit drops 10-12c + sometimes health potion or leather gloves. Health potion 20c, sell price ~60% buy.
- **XP**: Rats 15 XP, Bandits 18 XP. Level 2 = 100 XP total.
- **Spells**: ALL spell names return "Unknown spell" -- fireball, magic_missile, ice_bolt, flame_bolt, fire_bolt, arcane_bolt, lightning_bolt, heal (with and without spell: prefix)
- **Vendor commands**: buy_item (itemId), sell_item (itemId), interact_vendor
- **Death**: No item/coin loss, respawn at Temple with full HP/MP
- **Hidden passages**: "You notice a hidden passage" message at Winding Forest Path

## TODOs
- Add periodic ADB screenshot capability
- Test with a martial class (Warrior/Ranger) to compare
- Try to level up and test trainer CP allocation
- Explore beyond Deep Forest when stronger
- Test multiplayer interactions
