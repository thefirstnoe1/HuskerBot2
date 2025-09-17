package org.j3y.HuskerBot2.service

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.UserSnowflake
import org.j3y.HuskerBot2.model.NflPick
import org.springframework.stereotype.Service
import java.awt.Color

@Service
class NflPickemLeaderboardService {
    data class Entry(
        val userId: Long,
        val correct: Int,
        val total: Int,
        val points: Int
    )

    /**
     * Compute leaderboard entries from a collection of picks. Scoring: 10 points per correct pick.
     */
    fun computeEntries(picks: Collection<NflPick>): List<Entry> {
        if (picks.isEmpty()) return emptyList()
        val byUser = picks.groupBy { it.userId }
        return byUser.map { (userId, userPicks) ->
            val correct = userPicks.count { it.correctPick }
            val total = userPicks.size
            val points = correct * 10
            Entry(userId, correct, total, points)
        }.sortedWith(compareByDescending<Entry> { it.points }.thenBy { it.userId })
    }

    /**
     * Builds a Discord embed for a pick'em leaderboard from raw picks.
     * @param title Title for the embed
     * @param guild Optional guild for resolving member display names
     * @param color Embed color
     */
    fun buildLeaderboardEmbed(
        picks: Collection<NflPick>,
        title: String,
        guild: Guild? = null,
        color: Color = Color(0x00, 0x66, 0xCC)
    ) = EmbedBuilder().apply {
        setColor(color)
        setTitle(title)
        setDescription("Each correct pick is worth 10 points.")

        val ranking = computeEntries(picks)
        if (ranking.isEmpty()) {
            addField("Leaderboard", "No results.", false)
            return@apply
        }

        var lastScore = -1
        var lastRank = 0
        val lines = ranking.mapIndexed { index, e ->
            var rank = index + 1
            if (e.points == lastScore) rank = lastRank else { lastScore = e.points; lastRank = rank }
            val medal = when (rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "$rank."
            }
            val displayName = try {
                guild?.retrieveMember(UserSnowflake.fromId(e.userId))?.complete()?.effectiveName
            } catch (_: Exception) { null } ?: "<@${e.userId}>"
            "$medal $displayName — ${e.points} pts (${e.correct}/${e.total} correct)"
        }

        // Discord field value length limit ~1024. Keep buffer and chunk.
        var current = StringBuilder()
        fun flushField(first: Boolean) {
            if (current.isNotEmpty()) {
                addField(if (first) "Leaderboard" else "Leaderboard (cont.)", current.toString(), false)
                current = StringBuilder()
            }
        }
        var first = true
        for (line in lines) {
            val toAdd = if (current.isEmpty()) line else "\n$line"
            if (current.length + toAdd.length > 1000) {
                flushField(first)
                first = false
                current.append(line)
            } else {
                current.append(toAdd)
            }
        }
        flushField(first)
    }.build()
}
