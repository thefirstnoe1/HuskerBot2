package org.j3y.HuskerBot2.commands.impl

import com.fasterxml.jackson.databind.JsonNode
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.service.EspnService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
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
        LocalDateTime.parse("${YEAR}-12-01T00:00:00")
    )

    private val leagueMap: Map<String, Int> = mapOf(
        "top25" to 0,
        "acc" to 1,
        "american" to 151,
        "big12" to 4,
        "b12" to 4,
        "big10" to 5,
        "b10" to 5,
        "sec" to 8,
        "pac12" to 9,
        "p12" to 9,
        "mac" to 15,
        "independent" to 18
    )

    override fun getCommandKey(): String = "cfbsched"

    override fun getDescription(): String = "Get the CFB schedules for a given week and/or league"

    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.STRING, "league", "The league to get the schedule for (top25, acc, american, b12, b10, sec, p12, mac, independent)", false),
        OptionData(OptionType.INTEGER, "week-number", "The CFB week you would like the schedule for", false),
    )

    @Transactional
    @Synchronized
    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply().queue()

        val leagueStr = commandEvent.getOption("league")?.getAsString() ?: "top25"
        val league = leagueMap[leagueStr] ?: -1
        if (league == -1) {
            commandEvent.hook.sendMessage("That league was not recognized.")
            return
        }

        val weekInt = commandEvent.getOption("week-number")?.getAsInt() ?: getCurrentWeek()
        val apiJson: JsonNode = espnService.getCfbScoreboard(league, weekInt)

        commandEvent.hook.sendMessage(espnService.buildEventString(apiJson)).queue()
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
