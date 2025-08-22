package org.j3y.HuskerBot2.commands.pickem

import org.j3y.HuskerBot2.commands.SlashCommand
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class NflPickem : SlashCommand() {

    @Autowired lateinit var show: NflPickemShow
    @Autowired lateinit var leaderboard: NflPickemLeaderboard
    @Autowired lateinit var reload: NflPickemReload

    override fun getCommandKey(): String = "nfl-pickem"
    override fun getDescription(): String = "NFL Pick'em commands"

    override fun getSubcommands(): List<SlashCommand> = listOf(show, leaderboard, reload)
}
