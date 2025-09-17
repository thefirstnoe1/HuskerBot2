package org.j3y.HuskerBot2.commands.pickem

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.model.NflGameEntity
import org.j3y.HuskerBot2.repository.NflGameRepo
import org.j3y.HuskerBot2.repository.NflPickRepo
import org.j3y.HuskerBot2.util.SeasonResolver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class NflPickemShow : SlashCommand() {
    private val log = LoggerFactory.getLogger(NflPickemShow::class.java)

    @Autowired lateinit var nflPickRepo: NflPickRepo
    @Autowired lateinit var nflGameRepo: NflGameRepo

    override fun getCommandKey(): String = "show"
    override fun isSubcommand(): Boolean = true
    override fun getDescription(): String = "Show a user's NFL Pick'em choices for a specific week."

    override fun getOptions(): List<OptionData> {
        val weekChoices: MutableList<Command.Choice> = mutableListOf()

        for (w in 1..SeasonResolver.currentNflWeek()) {
            weekChoices.add(Command.Choice("Week $w", w.toLong()))
        }

        return listOf(
            OptionData(OptionType.INTEGER, "week", "NFL week number. Default is current NFL week.", false).addChoices(weekChoices)
        )
    }

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        val week = commandEvent.getOption("week")?.asInt ?: SeasonResolver.currentNflWeek()
        handleEvent(commandEvent, week)
    }

    fun handleEvent(commandEvent: IReplyCallback, week: Int) {
        commandEvent.deferReply(true).queue()
        try {
            val targetUser: User = commandEvent.user

            val season = SeasonResolver.currentNflSeason()

            val picks = try { nflPickRepo.findByUserIdAndSeasonAndWeek(targetUser.idLong, season, week) } catch (e: Exception) { emptyList() }

            // Fetch all games for this season and week (using existing repo method + filter to keep changes minimal)
            val allGames: List<NflGameEntity> = try {
                nflGameRepo.findBySeasonAndWeekOrderByDateTimeAsc(season, week)
            } catch (e: Exception) { emptyList() }

            // Map picks by gameId for quick lookup
            val picksByGameId = picks.associateBy { it.gameId }

            // Determine whether results are final: all games have winners
            val allProcessed = allGames.isNotEmpty() && allGames.all { it.winnerId != null }
            val eb = EmbedBuilder()
                .setColor(Color(0x00, 0x66, 0xCC))
                .setTitle("NFL Pick'em — Week $week Picks")
                .setDescription("User: ${targetUser.asMention}${if (allProcessed) "\nResults are final." else "\nResults pending for some games."}")

            var correctCount = 0
            val sb = StringBuilder()

            // If no games were found, keep previous behavior but inform gracefully
            if (allGames.isEmpty()) {
                sb.append("No games found for week $week.")
            } else {
                allGames.forEach { game ->
                    val pick = picksByGameId[game.id]
                    val awayMark = if (pick != null && game.awayTeamId == pick.winningTeamId) "☑\uFE0F" else if (pick != null) "❌" else " "
                    val homeMark = if (pick != null && game.homeTeamId == pick.winningTeamId) "☑\uFE0F" else if (pick != null) "❌" else " "
                    val awayTeam = (if (awayMark.isNotBlank()) "$awayMark " else "") + game.awayTeam
                    val homeTeam = game.homeTeam + (if (homeMark.isNotBlank()) " $homeMark" else "")
                    val matchup = "$awayTeam @ $homeTeam"
                    if (pick == null) {
                        sb.append("$matchup — No pick\n")
                    } else if (allProcessed) {
                        val correct = pick.correctPick
                        if (correct) correctCount++
                        val mark = if (correct) "✅" else "❌"
                        sb.append("$matchup — Correct?: $mark\n")
                    } else {
                        sb.append("$matchup\n")
                    }
                }
            }

            eb.addField("Picks", sb.toString().ifBlank { "No picks." }, false)
            if (allProcessed) {
                val points = correctCount * 10
                val totalPicked = picks.count()
                eb.setFooter("Week $week score: $points pts (${correctCount}/$totalPicked correct)")
            }

            commandEvent.hook.sendMessageEmbeds(eb.build()).queue()
        } catch (e: Exception) {
            log.error("Error executing nfl-pickem-show", e)
            commandEvent.hook.sendMessage("An error occurred fetching picks.").queue()
        }
    }
}
