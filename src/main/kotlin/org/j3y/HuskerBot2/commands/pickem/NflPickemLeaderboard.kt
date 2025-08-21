package org.j3y.HuskerBot2.commands.pickem

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.repository.NflPickRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class NflPickemLeaderboard : SlashCommand() {

    @Autowired lateinit var nflPickRepo: NflPickRepo

    override fun getCommandKey(): String = "nfl-pickem-leaderboard"
    override fun getDescription(): String = "Show the NFL Pick'em leaderboard for the current year."

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply().queue()

        val season = LocalDate.now().year
        val allPicks = nflPickRepo.findAll()
        val correctByUser = allPicks
            .asSequence()
            .filter { it.season == season && it.correctPick }
            .groupBy { it.userId }
            .mapValues { (_, picks) -> picks.size }

        if (correctByUser.isEmpty()) {
            commandEvent.hook.sendMessage("No picks recorded yet for $season.").queue()
            return
        }

        val leaderboard = correctByUser
            .map { (userId, correctCount) ->
                val points = correctCount * 10
                Triple(userId, correctCount, points)
            }
            .sortedWith(compareByDescending<Triple<Long, Int, Int>> { it.third }
                .thenBy { it.first })

        val sb = StringBuilder()
        sb.append("NFL Pick'em Leaderboard — ").append(season).append("\n")
        var rank = 1
        leaderboard.forEach { (userId, correctCount, points) ->
            val mention = "<@${userId}>"
            sb.append("$rank. $mention — ${points} pts (${correctCount} correct)\n")
            rank++
        }

        commandEvent.hook.sendMessage(sb.toString()).queue()
    }
}