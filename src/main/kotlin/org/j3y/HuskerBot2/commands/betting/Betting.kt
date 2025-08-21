package org.j3y.HuskerBot2.commands.betting

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import org.j3y.HuskerBot2.commands.SlashCommand
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class Betting : SlashCommand() {
    @Autowired lateinit var betCreate: BetCreate
    @Autowired lateinit var betShow: BetShow
    @Autowired lateinit var betLines: BetLines
    @Autowired lateinit var betLeaderboard: BetLeaderboard

    override fun getCommandKey(): String = "bet"
    override fun getDescription(): String = "Betting commands"
    override fun getSubcommands(): List<SlashCommand> = listOf(
        betCreate, betShow, betLines, betLeaderboard
    )
}
