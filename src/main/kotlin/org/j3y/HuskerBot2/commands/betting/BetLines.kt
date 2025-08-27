package org.j3y.HuskerBot2.commands.betting

import com.fasterxml.jackson.databind.JsonNode
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
import org.j3y.HuskerBot2.service.EspnService
import org.j3y.HuskerBot2.util.SeasonResolver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Component
class BetLines : SlashCommand() {
    val log = LoggerFactory.getLogger(BetLines::class.java)

    final val BIG_10_ID: Int = 5

    @Autowired lateinit var scheduleRepo: ScheduleRepo
    @Autowired lateinit var espnService: EspnService

    override fun getCommandKey(): String = "lines"
    override fun isSubcommand(): Boolean = true
    override fun getDescription(): String = "Show the lines for a specific game."
    override fun getOptions(): List<OptionData> {
        val season = SeasonResolver.currentCfbSeason()
        val choices: List<Command.Choice> = scheduleRepo.findAllBySeasonOrderByDateTimeAsc(season)
            .map { Command.Choice("${it.opponent} - Week ${it.week}", it.week.toLong()) }

        return listOf(
            OptionData(OptionType.INTEGER, "week", "The week of the opponent for the husker game.", true).addChoices(choices),
        )
    }

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply().queue()

        val season = SeasonResolver.currentCfbSeason()
        val week = commandEvent.getOption("week")?.asInt ?: 1

        val sched = scheduleRepo.findBySeasonAndWeek(season, week)
        val scoreboardData: JsonNode = espnService.getCfbScoreboard(BIG_10_ID, week)

        val event: JsonNode = scoreboardData.path("events").find { it.path("name").asText().contains("Nebraska Cornhuskers") } ?:
            return commandEvent.hook.sendMessage("Unable to find Nebraska Cornhuskers game for week $week.").queue()

        val lines = event.path("competitions").path(0).path("odds").path(0)

        val details = lines.path("details").asText()
        val spread = lines.path("spread").asDouble()
        val overUnder = lines.path("overUnder").asDouble()

        var embed = EmbedBuilder()
            .setTitle("Opponent Betting Lines")
            .setColor(Color.RED)
            .addField("Opponent", sched?.opponent ?: "N/A", true)
            .addField("Year", "$season", true)
            .addField("Week", "$week", true)
            .addField("Spread", "$spread", true)
            .addField("Over/Under", "$overUnder", true)
            .addField("Details", "$details", true)

        commandEvent.hook.sendMessageEmbeds(embed.build()).queue()
    }
}
