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
class CfbSched : SlashCommand() {

    @Autowired
    lateinit var espnService: EspnService

    private final val YEAR = LocalDateTime.now().year

    private val weeks: List<LocalDateTime> = listOf(
        LocalDateTime.parse("${YEAR}-01-01T00:00:00"),
        LocalDateTime.parse("${YEAR}-09-01T00:00:00"),
        LocalDateTime.parse("${YEAR}-09-08T00:00:00"),
        LocalDateTime.parse("${YEAR}-09-15T00:00:00"),
        LocalDateTime.parse("${YEAR}-09-22T00:00:00"),
        LocalDateTime.parse("${YEAR}-09-29T00:00:00"),
        LocalDateTime.parse("${YEAR}-10-06T00:00:00"),
        LocalDateTime.parse("${YEAR}-10-13T00:00:00"),
        LocalDateTime.parse("${YEAR}-10-20T00:00:00"),
        LocalDateTime.parse("${YEAR}-10-27T00:00:00"),
        LocalDateTime.parse("${YEAR}-11-03T00:00:00"),
        LocalDateTime.parse("${YEAR}-11-10T00:00:00"),
        LocalDateTime.parse("${YEAR}-11-17T00:00:00"),
        LocalDateTime.parse("${YEAR}-11-24T00:00:00"),
        LocalDateTime.parse("${YEAR}-12-01T00:00:00"),
        LocalDateTime.parse("${YEAR}-12-08T00:00:00"),
        LocalDateTime.parse("${YEAR}-12-15T00:00:00"),
        LocalDateTime.parse("${YEAR}-12-22T00:00:00")
    )

    private val leagueMap: Map<String, Int> = mapOf(
        "top25" to 0,
        "acc" to 1,
        "american" to 151,
        "big12" to 4,
        "big10" to 5,
        "sec" to 8,
        "pac12" to 9,
        "mac" to 15,
        "independent" to 18
    )

    private val leagueLabelMap: Map<String, String> = mapOf(
        "top25" to "Top 25",
        "acc" to "ACC",
        "american" to "American",
        "big12" to "Big 12",
        "big10" to "Big 10",
        "sec" to "SEC",
        "pac12" to "Pac 12",
        "mac" to "MAC",
        "independent" to "Independent"
    )

    override fun getCommandKey(): String = "cfb"
    override fun getDescription(): String = "Get the CFB schedules for a given week and/or league"
    override fun isSubcommand(): Boolean = true

    override fun getOptions(): List<OptionData> {
        val leagueOption = OptionData(OptionType.STRING, "league", "The league to get the schedule for (top25, acc, american, b12, b10, sec, p12, mac, independent)", false)
        leagueLabelMap.forEach { (league, label) -> leagueOption.addChoice(label, league) }

        return listOf(
            leagueOption,
            OptionData(OptionType.INTEGER, "week", "The CFB week you would like the schedule for", false),
        )
    }

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply().queue()

        val leagueStr = commandEvent.getOption("league")?.getAsString() ?: "top25"
        val league = leagueMap[leagueStr] ?: -1
        if (league == -1) {
            commandEvent.hook.sendMessage("That league was not recognized.").queue()
            return
        }

        val weekInt = commandEvent.getOption("week")?.asInt ?: getCurrentWeek()

        if (weekInt < 1 || weekInt > 17) {
            commandEvent.hook.sendMessage("Week must be between 1 and 17 inclusive.").queue()
            return
        }

        val apiJson: JsonNode = espnService.getCfbScoreboard(league, weekInt)

        val embeds = espnService.buildEventEmbed(apiJson)
        commandEvent.hook.sendMessage("## \uD83C\uDFC8 \u200E CFB Schedule for ${leagueLabelMap[leagueStr]} in Week $weekInt").addEmbeds(embeds).queue()
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
