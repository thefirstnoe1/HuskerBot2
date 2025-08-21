package org.j3y.HuskerBot2.commands.betting

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.model.BetEntity
import org.j3y.HuskerBot2.repository.BetRepo
import org.j3y.HuskerBot2.repository.ScheduleRepo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Component
class BetShow : SlashCommand() {
    val log = LoggerFactory.getLogger(BetShow::class.java)

    @Autowired lateinit var scheduleRepo: ScheduleRepo
    @Autowired lateinit var betRepo: BetRepo

    override fun getCommandKey(): String = "show"
    override fun isSubcommand(): Boolean = true
    override fun getDescription(): String = "Show all bets for a specific game."
    override fun getOptions(): List<OptionData> {
        val season = LocalDate.now().year
        val choices: List<Command.Choice> = scheduleRepo.findAllBySeasonOrderByDateTimeAsc(season)
            .map { Command.Choice("${it.opponent} - Week ${it.week}", it.week.toLong()) }

        return listOf(
            OptionData(OptionType.INTEGER, "week", "The week of the opponent for the husker game.", true).addChoices(choices),
        )
    }

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        val season = LocalDate.now().year
        val week = commandEvent.getOption("week")?.asInt ?: 1

        val sched = scheduleRepo.findBySeasonAndWeek(season, week)
        val bets = betRepo.findBySeasonAndWeek(season, week)
        val embedUsers = EmbedBuilder()
            .setTitle("Nebraska vs ${sched?.opponent} (Week $week) Bets")
            .setColor(Color.RED)
        val embedTotals = EmbedBuilder()
            .setTitle("Totals for Nebraska vs ${sched?.opponent} (Week $week)")
            .setColor(Color.RED)

        if (bets.isEmpty()) {
            embedUsers.setDescription("No bets found for this week.")
            commandEvent.replyEmbeds(embedUsers.build()).queue()
            return
        } else {
            val winnerBets = mutableMapOf("Nebraska" to 0, "Opponent" to 0)
            val pointsBets = mutableMapOf("Over" to 0, "Under" to 0)
            val spreadBets = mutableMapOf("Nebraska" to 0, "Opponent" to 0)
            bets.forEach { bet ->
                val user = commandEvent.guild?.retrieveMember(UserSnowflake.fromId(bet.userId))?.complete()?.effectiveName ?: bet.userTag
                embedUsers.addField(user, "**Winner:** ${bet.winner}\n**Points:** ${bet.predictPoints}\n**Spread:** ${bet.predictSpread}", true)

                winnerBets[bet.winner] = winnerBets[bet.winner]!! + 1
                pointsBets[bet.predictPoints] = pointsBets[bet.predictPoints]!! + 1
                spreadBets[bet.predictSpread] = spreadBets[bet.predictSpread]!! + 1
            }

            embedTotals.addField("Nebraska Winner Bets", winnerBets["Nebraska"].toString(), true)
            embedTotals.addField("Opponent Winner Bets", winnerBets["Opponent"].toString(), true)
            embedTotals.addBlankField(true)
            embedTotals.addField("Points Over Bets", pointsBets["Over"].toString(), true)
            embedTotals.addField("Points Under Bets", pointsBets["Under"].toString(), true)
            embedTotals.addBlankField(true)
            embedTotals.addField("Nebraska Spread Bets", spreadBets["Nebraska"].toString(), true)
            embedTotals.addField("Opponent Spread Bets", spreadBets["Opponent"].toString(), true)
            embedTotals.addBlankField(true)
        }
        commandEvent.replyEmbeds(embedUsers.build(), embedTotals.build()).queue()
    }
}
