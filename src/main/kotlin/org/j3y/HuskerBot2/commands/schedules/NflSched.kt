package org.j3y.HuskerBot2.commands.schedules

import com.fasterxml.jackson.databind.JsonNode
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.service.EspnService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class NflSched : SlashCommand() {

    @Autowired
    lateinit var espnService: EspnService

    private final val YEAR = LocalDateTime.now().year

    private val weeks: List<LocalDateTime> = listOf(
        LocalDateTime.parse("${YEAR}-01-01T00:00:00"),
        LocalDateTime.parse("${YEAR}-09-10T00:00:00"),
        LocalDateTime.parse("${YEAR}-09-17T00:00:00"),
        LocalDateTime.parse("${YEAR}-09-24T00:00:00"),
        LocalDateTime.parse("${YEAR}-10-01T00:00:00"),
        LocalDateTime.parse("${YEAR}-10-08T00:00:00"),
        LocalDateTime.parse("${YEAR}-10-15T00:00:00"),
        LocalDateTime.parse("${YEAR}-10-22T00:00:00"),
        LocalDateTime.parse("${YEAR}-10-29T00:00:00"),
        LocalDateTime.parse("${YEAR}-11-05T00:00:00"),
        LocalDateTime.parse("${YEAR}-11-12T00:00:00"),
        LocalDateTime.parse("${YEAR}-11-19T00:00:00"),
        LocalDateTime.parse("${YEAR}-11-26T00:00:00"),
        LocalDateTime.parse("${YEAR}-12-03T00:00:00"),
        LocalDateTime.parse("${YEAR}-12-10T00:00:00"),
        LocalDateTime.parse("${YEAR}-12-17T00:00:00"),
        LocalDateTime.parse("${YEAR}-12-24T00:00:00")
    )

    override fun getCommandKey(): String = "nfl"
    override fun getDescription(): String = "Get the NFL schedules (or scores) for a given week"
    override fun isSubcommand(): Boolean = true

    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.INTEGER, "week", "The NFL week you would like the schedule for", false),
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply().queue()
        val week = commandEvent.getOption("week")?.getAsInt() ?: getCurrentWeek()

        val apiJson: JsonNode = espnService.getNflScoreboard(week)
        val embeds = espnService.buildEventEmbed(apiJson)
        commandEvent.hook.sendMessage("## \uD83C\uDFC8 \u200E NFL Schedule for Week $week").addEmbeds(embeds).queue()
    }

    private fun getCurrentWeek(): Int {
        val curTime = LocalDateTime.now()

        for (week in weeks.size downTo 1) {
            val cfbWeek = weeks[week - 1] // subtract 1 because 0-based idx
            if (curTime.isAfter(cfbWeek)) return week
        }

        return weeks.size
    }
}
