package org.j3y.HuskerBot2.commands.schedules

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.model.ScheduleEntity
import org.j3y.HuskerBot2.repository.ScheduleRepo
import org.j3y.HuskerBot2.util.SeasonResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class CfbHuskersSched : SlashCommand() {

    @Autowired
    lateinit var scheduleRepo: ScheduleRepo

    override fun getCommandKey(): String = "cfb-huskers"
    override fun getDescription(): String = "Get the Nebraska Cornhuskers football schedule"
    override fun isSubcommand(): Boolean = true

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply().queue()

        val season = SeasonResolver.currentCfbSeason()
        val games: List<ScheduleEntity> = scheduleRepo.findAllBySeasonOrderByDateTimeAsc(season)

        if (games.isEmpty()) {
            commandEvent.hook.sendMessage("No schedule data found for $season.").queue()
            return
        }

        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d h:mm a z").withZone(ZoneId.systemDefault())

        val eb = EmbedBuilder()
            .setTitle("🏈 Nebraska Football Schedule — $season")
            .setColor(Color(187, 0, 0))

        games.forEach { game ->
            val whenStr = formatter.format(game.dateTime)
            val where = when (game.venueType.lowercase()) {
                "home" -> "vs"
                "away" -> "at"
                else -> "vs"
            }

            val title = "\uD83D\uDCC6 $whenStr — $where ${game.opponent}"

            val value = if (game.completed == true && game.huskersScore != null && game.opponentScore != null) {
                val winLossEmoji = if (game.huskersScore!! > game.opponentScore!!) "✅" else "❌"
                buildString {
                    append("📍 ")
                    append(game.location.ifBlank { "TBD" })
                    append("\n")
                    append("$winLossEmoji Final: Nebraska ${game.huskersScore} — ${game.opponentScore} ${game.opponent}")
                }
            } else {
                buildString {
                    append("📍 ")
                    append(game.location.ifBlank { "TBD" })
                    if (game.spread.isNotBlank()) append(" • 📈 Line: ${game.spread}")
                    if (game.overUnder != null) append(" • 🎯 O/U: ${game.overUnder}")
                }
            }

            eb.addField(title, value, false)
        }

        commandEvent.hook.sendMessageEmbeds(eb.build()).queue()
    }
}
