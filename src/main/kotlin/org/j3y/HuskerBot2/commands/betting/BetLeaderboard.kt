package org.j3y.HuskerBot2.commands.betting

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.model.BetEntity
import org.j3y.HuskerBot2.repository.BetRepo
import org.j3y.HuskerBot2.repository.ScheduleRepo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.LocalDate

@Component
class BetLeaderboard : SlashCommand() {
    private val log = LoggerFactory.getLogger(BetLeaderboard::class.java)

    @Autowired lateinit var betRepo: BetRepo
    @Autowired lateinit var scheduleRepo: ScheduleRepo

    override fun getCommandKey(): String = "leaderboard"
    override fun isSubcommand(): Boolean = true
    override fun getDescription(): String = "Show the season-long betting leaderboard."

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        val season = LocalDate.now().year
        val bets: List<BetEntity> = betRepo.findBySeason(season)

        if (bets.isEmpty()) {
            commandEvent.reply("No bets found for the ${season} season.").queue()
            return
        }

        // Aggregate points per user
        data class Totals(
            var points: Int = 0,
            var winners: Int = 0,
            var spreads: Int = 0,
            var overUnders: Int = 0,
            var userTag: String = ""
        )

        val perUser = mutableMapOf<Long, Totals>()
        bets.forEach { bet ->
            val totals = perUser.computeIfAbsent(bet.userId) { Totals(userTag = bet.userTag) }
            totals.userTag = if (bet.userTag.isNotBlank()) bet.userTag else totals.userTag

            if (bet.correctWinner == true) {
                totals.points += 1
                totals.winners += 1
            }
            if (bet.correctSpread == true) {
                totals.points += 2
                totals.spreads += 1
            }
            if (bet.correctPoints == true) {
                totals.points += 2
                totals.overUnders += 1
            }
        }

        // Build a sorted leaderboard
        val ranking = perUser.entries
            .sortedWith(compareByDescending<Map.Entry<Long, Totals>> { it.value.points }
                .thenBy { it.value.userTag.lowercase() })

        val embed = EmbedBuilder()
            .setTitle("🏆 Husker Betting Leaderboard — $season Season")
            .setColor(Color(200, 16, 46)) // Huskers Red-ish
            .setDescription("Scoring: Winner = 1, Spread = 2, Over/Under = 2")

        // Resolve names and build lines
        val guild = commandEvent.guild
        val lines = ranking.mapIndexed { index, entry ->
            val (userId, totals) = entry
            val rank = index + 1
            val medal = when (rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "${rank}."
            }

            val displayName = try {
                guild?.retrieveMember(UserSnowflake.fromId(userId))?.complete()?.effectiveName
            } catch (e: Exception) { null } ?: totals.userTag.ifBlank { userId.toString() }

            val breakdown = "W ${totals.winners} • S ${totals.spreads} • O/U ${totals.overUnders}"
            "$medal $displayName — ${totals.points} pts  ($breakdown)"
        }

        // Discord embed field max limitations; chunk if necessary
        val chunkSize = 20 // safe chunk size for readability
        if (lines.isEmpty()) {
            embed.setDescription(embed.descriptionBuilder.append("\nNo scored bets yet.").toString())
        } else {
            lines.chunked(chunkSize).forEachIndexed { idx, chunk ->
                val name = if (idx == 0) "Leaderboard" else "Leaderboard (cont.)"
                embed.addField(name, chunk.joinToString("\n"), false)
            }
        }

        commandEvent.replyEmbeds(embed.build()).queue()
    }
}
