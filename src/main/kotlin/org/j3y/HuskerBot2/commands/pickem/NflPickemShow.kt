package org.j3y.HuskerBot2.commands.pickem

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.model.NflGameEntity
import org.j3y.HuskerBot2.repository.NflGameRepo
import org.j3y.HuskerBot2.repository.NflPickRepo
import org.j3y.HuskerBot2.util.WeekResolver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.LocalDate

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

        for (w in 1..WeekResolver.currentNflWeek()) {
            weekChoices.add(Command.Choice("Week $w", w.toLong()))
        }

        return listOf(
            OptionData(OptionType.INTEGER, "week", "NFL week number. Default is current NFL week.", false).addChoices(weekChoices)
        )
    }

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        val week = commandEvent.getOption("week")?.asInt ?: WeekResolver.currentNflWeek()
        handleEvent(commandEvent, week)
    }

    fun handleEvent(commandEvent: IReplyCallback, week: Int) {
        commandEvent.deferReply(true).queue()
        try {
            val targetUser: User = commandEvent.user

            val season = LocalDate.now().year

            val picks = try { nflPickRepo.findByUserIdAndSeasonAndWeek(targetUser.idLong, season, week) } catch (e: Exception) { emptyList() }

            if (picks.isEmpty()) {
                commandEvent.hook.sendMessage("No picks found for ${targetUser.asMention} in week $week.").queue()
                return
            }

            // Load related games (avoid N+1 by bulk fetching via ids if desired; here minimal changes per instruction)
            val gamesById: Map<Long, NflGameEntity?> = picks.associate { it.gameId to nflGameRepo.findById(it.gameId).orElse(null) }

            val allProcessed = picks.all { it.processed }
            val eb = EmbedBuilder()
                .setColor(Color(0x00, 0x66, 0xCC))
                .setTitle("NFL Pick'em — Week $week Picks")
                .setDescription("User: ${targetUser.asMention}${if (allProcessed) "\nResults are final." else "\nResults pending for some games."}")

            var correctCount = 0
            val sb = StringBuilder()
            picks.sortedBy { gamesById[it.gameId]?.dateTime }.forEach { pick ->
                val game = gamesById[pick.gameId]
                if (game == null) {
                    sb.append("Game ${pick.gameId}: Picked team ${pick.winningTeamId}" )
                } else {
                    val awayTeam = (if (game.awayTeamId == pick.winningTeamId) "☑\uFE0F" else "❌") + " ${game.awayTeam}"
                    val homeTeam = "${game.homeTeam} " + (if (game.homeTeamId == pick.winningTeamId) "☑\uFE0F" else "❌")
                    val matchup = "$awayTeam @ $homeTeam"
                    if (allProcessed) {
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
                eb.setFooter("Week $week score: $points pts (${correctCount}/${picks.size} correct)")
            }

            commandEvent.hook.sendMessageEmbeds(eb.build()).queue()
        } catch (e: Exception) {
            log.error("Error executing nfl-pickem-show", e)
            commandEvent.hook.sendMessage("An error occurred fetching picks.").queue()
        }
    }
}
