package org.j3y.HuskerBot2.service

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.UserSnowflake
import org.j3y.HuskerBot2.model.BetEntity
import org.springframework.stereotype.Service
import java.awt.Color

@Service
class BetLeaderboardService {
    data class Totals(
        var points: Int = 0,
        var winners: Int = 0,
        var spreads: Int = 0,
        var overUnders: Int = 0,
        var userTag: String = ""
    )

    fun computeTotals(bets: List<BetEntity>): List<Pair<Long, Totals>> {
        val perUser = mutableMapOf<Long, Totals>()
        bets.forEach { bet ->
            val totals = perUser.computeIfAbsent(bet.userId) { Totals(userTag = bet.userTag) }
            if (bet.userTag.isNotBlank()) totals.userTag = bet.userTag

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
        return perUser.entries
            .sortedWith(compareByDescending<Map.Entry<Long, Totals>> { it.value.points }
                .thenBy { it.value.userTag.lowercase() })
            .map { it.key to it.value }
    }

    fun buildLeaderboardEmbed(
        bets: List<BetEntity>,
        title: String,
        guild: Guild? = null,
        color: Color = Color(200, 16, 46)
    ) = EmbedBuilder().apply {
        setTitle(title)
        setColor(color)
        setDescription("Scoring: Winner = 1, Spread = 2, Over/Under = 2")

        val ranking = computeTotals(bets)
        var lastScore = 0
        var lastRank = 0
        val lines = ranking.mapIndexed { index, (userId, totals) ->
            var rank = index + 1
            if (totals.points == lastScore) {
                rank = lastRank
            }
            lastRank = rank
            lastScore = totals.points

            val medal = when (rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "$rank\\."
            }

            val displayName = try {
                guild?.retrieveMember(UserSnowflake.fromId(userId))?.complete()?.effectiveName
            } catch (_: Exception) { null } ?: totals.userTag.ifBlank { userId.toString() }
            val breakdown = "W ${totals.winners} • S ${totals.spreads} • O/U ${totals.overUnders}"
            "$medal $displayName — ${totals.points} pts  ($breakdown)"
        }

        if (lines.isEmpty()) {
            addField("Leaderboard", "No scored bets yet.", false)
        } else {
            lines.chunked(10).forEachIndexed { idx, chunk ->
                val name = if (idx == 0) "Leaderboard" else "Leaderboard (cont.)"
                addField(name, chunk.joinToString("\n"), false)
            }
        }
    }.build()
}
