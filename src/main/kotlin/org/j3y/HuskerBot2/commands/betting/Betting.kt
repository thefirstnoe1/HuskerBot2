package org.j3y.HuskerBot2.commands.betting

import org.j3y.HuskerBot2.commands.SlashCommand
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class Betting : SlashCommand() {
    @Autowired private lateinit var betProcess: BetProcess
    @Autowired lateinit var betReload: BetReload
    @Autowired lateinit var betShow: BetShow
    @Autowired lateinit var betLines: BetLines
    @Autowired lateinit var betLeaderboard: BetLeaderboard

    override fun getCommandKey(): String = "bet"
    override fun getDescription(): String = "Betting commands"
    override fun getSubcommands(): List<SlashCommand> = listOf(
        betReload, betShow, betLines, betLeaderboard, betProcess
    )
}
