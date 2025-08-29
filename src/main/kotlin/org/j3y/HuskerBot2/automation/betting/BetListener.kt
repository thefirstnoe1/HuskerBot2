package org.j3y.HuskerBot2.automation.betting

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.j3y.HuskerBot2.commands.betting.BetShow
import org.j3y.HuskerBot2.model.BetEntity
import org.j3y.HuskerBot2.repository.BetRepo
import org.j3y.HuskerBot2.repository.ScheduleRepo
import org.j3y.HuskerBot2.util.SeasonResolver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit


@Component
class BetListener : ListenerAdapter() {
    private val log = LoggerFactory.getLogger(BetListener::class.java)

    @Autowired lateinit var scheduleRepo: ScheduleRepo
    @Autowired lateinit var betRepo: BetRepo
    @Autowired lateinit var betShow: BetShow

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val id = event.componentId

        if (!id.startsWith("huskerbets|")) return

        try {
            val parts = id.split("|")
            if (parts.size < 4) return
            val pickType = parts[1]
            val pickWeek = parts[2].toInt()
            val pickValue = parts[3]

            val season = SeasonResolver.currentCfbSeason()
            val schedule = scheduleRepo.findBySeasonAndWeek(season, pickWeek) ?:
            return event.reply("Sorry, we don't have any data for week $pickWeek of $season.").setEphemeral(true).queue()

            if (Instant.now().isAfter(schedule.dateTime.minus(1, ChronoUnit.HOURS))) {
                event.reply("You cannot make a bet less than an hour before the game starts!").setEphemeral(true).queue()
                return
            }

            val bet = betRepo.findByUserIdAndSeasonAndWeek(event.user.idLong, season, pickWeek)
                ?: BetEntity(event.user.idLong, season, pickWeek)

            log.info("Bet type: $pickType with Value: $pickValue")

            when (pickType) {
                "winner" -> bet.winner = pickValue
                "overunder" -> bet.predictPoints = pickValue
                "spread" -> bet.predictSpread = pickValue
                else -> {
                    event.hook.sendMessage("Invalid bet type: $pickType").setEphemeral(true).queue()
                    return
                }
            }

            betRepo.save(bet)

            betShow.sendBetChannelMessage(event.guild!!, event.channel, pickWeek)
        } catch (e: Exception) {
            log.error("Error handling Husker Bet button", e)
            event.reply("An error occurred processing your pick.").setEphemeral(true).queue()
        }

        event.deferEdit().queue()
    }
}
