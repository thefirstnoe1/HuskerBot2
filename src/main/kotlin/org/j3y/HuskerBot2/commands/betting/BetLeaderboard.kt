package org.j3y.HuskerBot2.commands.betting

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.model.BetEntity
import org.j3y.HuskerBot2.repository.BetRepo
import org.j3y.HuskerBot2.repository.ScheduleRepo
import org.j3y.HuskerBot2.service.BetLeaderboardService
import org.j3y.HuskerBot2.util.SeasonResolver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BetLeaderboard : SlashCommand() {
    private val log = LoggerFactory.getLogger(BetLeaderboard::class.java)

    @Autowired lateinit var betRepo: BetRepo
    @Autowired lateinit var scheduleRepo: ScheduleRepo
    @Autowired lateinit var leaderboardService: BetLeaderboardService

    override fun getCommandKey(): String = "leaderboard"
    override fun isSubcommand(): Boolean = true
    override fun getDescription(): String = "Show the season-long betting leaderboard."

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        val season = SeasonResolver.currentCfbSeason()
        val bets: List<BetEntity> = betRepo.findBySeason(season)

        if (bets.isEmpty()) {
            commandEvent.reply("No bets found for the ${season} season.").queue()
            return
        }

        val embed = leaderboardService.buildLeaderboardEmbed(
            bets = bets,
            title = "🏆 Husker Betting Leaderboard — $season Season",
            guild = commandEvent.guild
        )

        commandEvent.replyEmbeds(embed).queue()
    }
}
