package com.neomud.server.game.commands

import com.neomud.server.game.MeditationUtils
import com.neomud.server.game.StealthUtils
import com.neomud.server.game.combat.CombatUtils
import com.neomud.server.game.skills.SkillCheck
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.SkillCatalog
import com.neomud.shared.protocol.ServerMessage

class MeditateCommand(
    private val skillCatalog: SkillCatalog,
    private val sessionManager: SessionManager
) {
    suspend fun execute(session: PlayerSession) {
        val player = session.player ?: return

        // If already meditating, cancel
        if (session.isMeditating) {
            session.isMeditating = false
            session.send(ServerMessage.MeditateUpdate(false, "You stop meditating."))
            return
        }

        if (session.attackMode) {
            session.send(ServerMessage.SystemMessage("You cannot meditate while in combat!"))
            return
        }

        if (player.currentMp >= player.maxMp) {
            session.send(ServerMessage.SystemMessage("Your mana is already full."))
            return
        }

        val cooldown = session.skillCooldowns["MEDITATE"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("Meditate is on cooldown ($cooldown ticks remaining)."))
            return
        }

        // Cooldown applies regardless of pass/fail
        session.skillCooldowns["MEDITATE"] = 6

        // Break stealth if hidden
        StealthUtils.breakStealth(session, sessionManager, "Meditating reveals your presence!")

        // Skill check: WIL + INT/2 + level/2 + d20 vs difficulty
        val skillDef = skillCatalog.getSkill("MEDITATE")
        if (skillDef == null) {
            session.send(ServerMessage.SystemMessage("Meditate skill not found."))
            return
        }

        val effStats = CombatUtils.effectiveStats(player.stats, session.activeEffects.toList())
        val result = SkillCheck.check(skillDef, effStats, player.level)

        if (!result.success) {
            session.send(ServerMessage.MeditateUpdate(false, "You fail to focus your mind. (roll: ${result.roll})"))
            return
        }

        session.isMeditating = true
        session.send(ServerMessage.MeditateUpdate(true, "You enter a meditative state. (roll: ${result.roll})"))
    }
}
