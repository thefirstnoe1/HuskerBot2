package org.j3y.HuskerBot2.commands.pickem

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.repository.NflPickRepo
import org.j3y.HuskerBot2.service.NflPickemLeaderboardService
import org.j3y.HuskerBot2.util.SeasonResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class NflPickemLeaderboard : SlashCommand() {

    @Autowired lateinit var nflPickRepo: NflPickRepo
    @Autowired(required = false)
    var leaderboardService: NflPickemLeaderboardService? = null

    override fun getCommandKey(): String = "leaderboard"
    override fun isSubcommand(): Boolean = true
    override fun getDescription(): String = "Show the NFL Pick'em leaderboard for the current year."

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply().queue()

        val season = SeasonResolver.currentNflSeason()
        val picks = nflPickRepo.findAll().filter { it.season == season }
        val anyCorrect = picks.any { it.correctPick }
        if (!anyCorrect) {
            commandEvent.hook.sendMessage("No picks recorded yet for $season.").queue()
            return
        }
        val svc = leaderboardService ?: NflPickemLeaderboardService()

        val embed = svc.buildLeaderboardEmbed(
            picks.filter { it.correctPick || !it.correctPick }, // include all to show total attempts
            title = "NFL Pick'em — Season Leaderboard ($season)",
            guild = commandEvent.guild,
            color = Color(0x00, 0x66, 0xCC)
        )

        commandEvent.hook.sendMessageEmbeds(embed).queue()
    }
}