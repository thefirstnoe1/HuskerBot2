package org.j3y.HuskerBot2.automation.pickem.nfl

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.j3y.HuskerBot2.model.NflPick
import org.j3y.HuskerBot2.repository.NflGameRepo
import org.j3y.HuskerBot2.repository.NflPickRepo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate

@Component
class NflPickemListener : ListenerAdapter() {
    private val log = LoggerFactory.getLogger(NflPickemListener::class.java)

    @Autowired lateinit var nflGameRepo: NflGameRepo
    @Autowired lateinit var nflPickRepo: NflPickRepo

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val id = event.componentId

        if (!id.startsWith("nflpickem|")) return
        try {
            val parts = id.split("|")
            if (parts.size < 3) return
            val eventId = parts[1]
            val teamId = parts[2]

            val game = nflGameRepo.findById(eventId.toLong()).orElse(null)
            if (game == null) {
                event.reply("Sorry, we don't have any data for game $eventId.").setEphemeral(true).queue()
            }

            if (Instant.now().isAfter(game.dateTime)) {
                event.reply("You cannot make a pick after the game has already started!").setEphemeral(true).queue()
            }

            val pick = if (game.homeTeamId == teamId.toLong()) game.homeTeam else game.awayTeam

            val pickEntity = getPick(game.id, event.user.idLong)
            pickEntity.season = game.season
            pickEntity.week = game.week
            pickEntity.winningTeamId = teamId.toLong()

            nflPickRepo.save(pickEntity)

            // We don't persist in this issue; acknowledge selection ephemerally
            event.reply("You picked team $pick for game ${game.awayTeam} @ ${game.homeTeam}. Thanks!")
                .setEphemeral(true)
                .queue()
        } catch (e: Exception) {
            log.error("Error handling NFL pick'em button", e)
            event.reply("An error occurred processing your pick.").setEphemeral(true).queue()
        }
    }

    private fun getPick(gameId: Long, userId: Long): NflPick {
        return nflPickRepo.findByGameIdAndUserId(gameId, userId) ?: NflPick(gameId, userId)
    }
}