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
import org.j3y.HuskerBot2.util.SeasonResolver
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
        val season = SeasonResolver.currentCfbSeason()
        val choices: List<Command.Choice> = scheduleRepo.findAllBySeasonOrderByDateTimeAsc(season)
            .map { Command.Choice("${it.opponent} - Week ${it.week}", it.week.toLong()) }

        return listOf(
            OptionData(OptionType.INTEGER, "week", "The week of the opponent for the husker game.", true).addChoices(choices),
        )
    }

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        val season = SeasonResolver.currentCfbSeason()
        val week = commandEvent.getOption("week")?.asInt ?: 1

        val sched = scheduleRepo.findBySeasonAndWeek(season, week)
        val bets = betRepo.findBySeasonAndWeek(season, week)
        val opponent = sched?.opponent ?: "Opponent"
        val embedUsers = EmbedBuilder()
            .setTitle("Nebraska vs $opponent (Week $week) Bets")
            .setColor(Color.RED)
        val embedTotals = EmbedBuilder()
            .setTitle("Totals for Nebraska vs $opponent (Week $week)")
            .setColor(Color.RED)

        if (bets.isEmpty()) {
            embedUsers.setDescription("No bets found for this week.")
            commandEvent.replyEmbeds(embedUsers.build()).queue()
            return
        } else {
            val winnerUserChoices: Map<String, MutableList<String>> = mapOf(
                "Nebraska" to mutableListOf(),
                "Opponent" to mutableListOf()
            )
            val pointsUserChoices: Map<String, MutableList<String>> = mapOf(
                "Over" to mutableListOf(),
                "Under" to mutableListOf(),
            )
            val spreadUserChoices: Map<String, MutableList<String>> = mapOf(
                "Nebraska" to mutableListOf(),
                "Opponent" to mutableListOf()
            )
            bets.forEach { bet ->
                var user: String
                try {
                    user = commandEvent.guild?.retrieveMember(UserSnowflake.fromId(bet.userId))?.complete()?.effectiveName ?: bet.userTag
                } catch (e: Exception) {
                    log.warn("Couldn't find member for tag: {}", bet.userTag)
                    user = bet.userTag
                }
                log.info("Found bet for user: {} - {} - {}", user, bet.userTag, bet.userId)
                winnerUserChoices[bet.winner]?.add(user)
                pointsUserChoices[bet.predictPoints]?.add(user)
                spreadUserChoices[bet.predictSpread]?.add(user)
            }

            embedTotals.addField("Nebraska Win", winnerUserChoices["Nebraska"]?.size.toString(), true)
            embedTotals.addField("$opponent Win", winnerUserChoices["Opponent"]?.size.toString(), true)
            embedTotals.addBlankField(true)
            embedTotals.addField("Over", pointsUserChoices["Over"]?.size.toString(), true)
            embedTotals.addField("Under", pointsUserChoices["Under"]?.size.toString(), true)
            embedTotals.addBlankField(true)
            embedTotals.addField("Nebraska Spread", spreadUserChoices["Nebraska"]?.size.toString(), true)
            embedTotals.addField("$opponent Spread", spreadUserChoices["Opponent"]?.size.toString(), true)
            embedTotals.addBlankField(true)

            winnerUserChoices.values.forEach { value -> if (value.isEmpty()) value.add("None") }
            pointsUserChoices.values.forEach { value -> if (value.isEmpty()) value.add("None") }
            spreadUserChoices.values.forEach { value -> if (value.isEmpty()) value.add("None") }

            embedUsers.addField("Winner: Nebraska", winnerUserChoices["Nebraska"]?.joinToString(", ") ?: "None", true)
            embedUsers.addField("Winner: $opponent", winnerUserChoices["Opponent"]?.joinToString(", ") ?: "None", true)
            embedUsers.addBlankField(true)

            embedUsers.addField("Over", pointsUserChoices["Over"]?.joinToString(", ") ?: "None", true)
            embedUsers.addField("Under", pointsUserChoices["Under"]?.joinToString(", ") ?: "None", true)
            embedUsers.addBlankField(true)

            embedUsers.addField("Spread: Nebraska", spreadUserChoices["Nebraska"]?.joinToString(", ") ?: "None", true)
            embedUsers.addField("Spread: $opponent", spreadUserChoices["Opponent"]?.joinToString(", ") ?: "None", true)
            embedUsers.addBlankField(true)
        }
        commandEvent.replyEmbeds(embedUsers.build(), embedTotals.build()).queue()
    }
}
