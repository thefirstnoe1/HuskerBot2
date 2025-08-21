package org.j3y.HuskerBot2.automation.pickem.nfl

import org.springframework.stereotype.Component

import com.fasterxml.jackson.databind.JsonNode
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.j3y.HuskerBot2.model.NflGameEntity
import org.j3y.HuskerBot2.model.NflPick
import org.j3y.HuskerBot2.repository.NflGameRepo
import org.j3y.HuskerBot2.repository.NflPickRepo
import org.j3y.HuskerBot2.service.EspnService
import org.j3y.HuskerBot2.util.WeekResolver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import java.awt.Color
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class NflPickem {
    @Autowired
    private lateinit var nflPickRepo: NflPickRepo
    @Autowired lateinit var jda: JDA
    @Autowired lateinit var espnService: EspnService
    @Autowired lateinit var nflGameRepo: NflGameRepo
    @Value("\${discord.channels.nfl-pickem}") lateinit var pickemChannelId: String

    private val log = LoggerFactory.getLogger(NflPickem::class.java)

    // Every Tuesday at 2:00 AM Central
    @Scheduled(cron = "0 0 2 * * TUE", zone = "America/Chicago")
    fun postWeeklyPickem() {
        processPreviousWeek()
        deleteAllPosts()

        val week = WeekResolver.currentNflWeek()
        log.info("Posting NFL Pick'em for week {}", week)
        val data = espnService.getNflScoreboard(week)

        val channel: TextChannel? = jda.getTextChannelById(pickemChannelId)
        if (channel == null) {
            log.warn("Did not find the nfl pickem channel with ID: {}", pickemChannelId)
            return
        }
        try {
            val events = data.path("events")
            if (events.isEmpty) {
                channel.sendMessage("No NFL games found for week $week.").queue()
            } else {
                channel.sendMessage("# \uD83C\uDFC8 NFL Pick'em for week $week \uD83C\uDFC8 \nGet your picks in before game time for each game!").queue()
                events.forEach { event ->
                    val embed = buildGameEmbed(event)
                    val (awayName, awayId, homeName, homeId, eventId) = extractIds(event)
                    val dateTime = OffsetDateTime.parse(event.path("date").asText("")).toInstant()
                    val season = LocalDateTime.now().year

                    val nflGame = getGame(eventId)
                    nflGame.homeTeam = homeName
                    nflGame.homeTeamId = homeId.toLong()
                    nflGame.awayTeam = awayName
                    nflGame.awayTeamId = awayId.toLong()
                    nflGame.dateTime = dateTime
                    nflGame.season = season
                    nflGame.week = week

                    nflGameRepo.save(nflGame)

                    val buttons = listOf(
                        Button.primary("nflpickem|$eventId|$awayId", awayName),
                        Button.secondary("nflpickem|$eventId|$homeId", homeName)
                    )
                    channel.sendMessageEmbeds(embed).setActionRow(buttons).queue()
                }
            }
        } catch (e: Exception) {
            log.error("Failed to post pick'em", e)
        }
    }

    private fun buildGameEmbed(event: JsonNode): net.dv8tion.jda.api.entities.MessageEmbed {
        val comp = event.path("competitions").path(0)
        val away = comp.path("competitors").path(1)
        val home = comp.path("competitors").path(0)

        val awayName = away.path("team").path("displayName").asText(away.path("team").path("abbreviation").asText("Away"))
        val homeName = home.path("team").path("displayName").asText(home.path("team").path("abbreviation").asText("Home"))

        val awayLogo = getLogoUrl(away)
        val homeLogo = getLogoUrl(home)

        val awayRec = getRecord(away)
        val homeRec = getRecord(home)

        val status = event.path("status").path("type").path("shortDetail").asText("TBD")
        val dateStr = event.path("date").asText("")
        val kickoffCentral = try {
            val dt = java.time.ZonedDateTime.parse(dateStr).withZoneSameInstant(ZoneId.of("America/Chicago"))
            dt.format(DateTimeFormatter.ofPattern("EEE MMM d, h:mm a z"))
        } catch (e: Exception) { status }

        val title = "$awayName @ $homeName"
        // Odds / Lines
        val odds = comp.path("odds")
        var lineText = "TBD"
        if (odds.isArray && odds.size() > 0) {
            val o = odds.path(0)
            val details = o.path("details").asText("")
            val overUnderNode = o.path("overUnder")
            val spreadNode = o.path("spread")
            val overUnder = if (overUnderNode.isMissingNode || overUnderNode.isNull) null else overUnderNode.asDouble()
            val spread = if (spreadNode.isMissingNode || spreadNode.isNull) null else spreadNode.asDouble()

            lineText = when {
                !details.isNullOrEmpty() && overUnder != null -> "$details (O/U ${"%.1f".format(overUnder)})"
                !details.isNullOrEmpty() -> details
                spread != null && overUnder != null -> "Spread: ${"%.1f".format(spread)} • O/U ${"%.1f".format(overUnder)}"
                spread != null -> "Spread: ${"%.1f".format(spread)}"
                overUnder != null -> "O/U ${"%.1f".format(overUnder)}"
                else -> "TBD"
            }
        }

        return EmbedBuilder()
            .setTitle(title)
            .setAuthor(awayName, null, awayLogo)
            .setFooter(homeName, homeLogo)
            .setColor(Color(0x00, 0x7A, 0x33))
            .addField("Away", "$awayName\nRecord: $awayRec", true)
            .addField("Home", "$homeName\nRecord: $homeRec", true)
            .addField("Kickoff", kickoffCentral, false)
            .addField("Line", lineText, false)
            .build()
    }

    private fun getLogoUrl(teamNode: JsonNode): String {
        val team = teamNode.path("team")
        val direct = team.path("logo").asText(null)
        if (!direct.isNullOrEmpty()) return direct
        val logos = team.path("logos")
        if (logos.isArray && logos.size() > 0) {
            val href = logos.path(0).path("href").asText(null)
            if (!href.isNullOrEmpty()) return href
        }
        return ""
    }

    private fun getRecord(compNode: JsonNode): String {
        val records = compNode.path("records")
        if (records.isArray && records.size() > 0) {
            val summary = records.path(0).path("summary").asText(null)
            if (!summary.isNullOrEmpty()) return summary
        }
        // fallback to wins/losses if present
        val wins = compNode.path("record").path("wins").asInt(-1)
        val losses = compNode.path("record").path("losses").asInt(-1)
        if (wins >= 0 && losses >= 0) return "$wins-$losses"
        return "0-0"
    }

    private data class Ids(
        val awayName: String, val awayId: String,
        val homeName: String, val homeId: String,
        val eventId: String,
    )

    private fun extractIds(event: JsonNode): Ids {
        val comp = event.path("competitions").path(0)
        val away = comp.path("competitors").path(1)
        val home = comp.path("competitors").path(0)
        val awayName = away.path("team").path("abbreviation").asText("AWY")
        val homeName = home.path("team").path("abbreviation").asText("HME")
        val awayId = away.path("team").path("id").asText("0")
        val homeId = home.path("team").path("id").asText("0")
        val eventId = event.path("id").asText("0")
        return Ids(awayName, awayId, homeName, homeId, eventId)
    }
    
    private fun deleteAllPosts() {
        val channel: TextChannel? = jda.getTextChannelById(pickemChannelId)
        if (channel == null) {
            log.warn("Did not find the nfl pickem channel with ID: {}", pickemChannelId)
            return
        }
        try {
            val history = channel.history
            while (true) {
                val messages = history.retrievePast(100).complete()
                if (messages.isEmpty()) break
                messages.forEach { msg ->
                    try {
                        msg.delete().complete()
                    } catch (e: Exception) {
                        log.warn("Error scheduling message deletion for {}", msg.id, e)
                    }
                }
            }
            log.info("Cleared all messages from NFL pick'em channel: {}", channel.id)
        } catch (e: Exception) {
            log.error("Failed to delete pick'em posts", e)
        }
    }

    private fun getGame(eventId: String): NflGameEntity {
        return nflGameRepo.findById(eventId.toLong()).orElse(NflGameEntity(eventId.toLong()))
    }

    private fun processPreviousWeek() {
        val prevWeek = WeekResolver.currentNflWeek() - 1

        if (prevWeek <= 0) {
            log.warn("Not processing previous week - there were no games in week {}", prevWeek)
            return
        }

        val data = espnService.getNflScoreboard(prevWeek)
        val events = data.path("events")
        if (events.isEmpty) {
            log.warn("No NFL games found for week {}", prevWeek)
            return
        }

        events.forEach { event ->
            val (awayName, awayId, homeName, homeId, eventId) = extractIds(event)
            val status = event.path("status").path("type").path("name").asText("TBD")

            if (status != "STATUS_FINAL") {
                log.info("Skipping game {} because it is not final", eventId)
                return@forEach
            }

            val teams = event.path("competitions").path(0).path("competitors")
            val homeTeam = teams.find { it.path("homeAway").asText() == "home" }
            val awayTeam = teams.find { it.path("homeAway").asText() == "away" }
            val homeScore = homeTeam?.path("score")?.asInt(0) ?: 0
            val awayScore = awayTeam?.path("score")?.asInt(0) ?: 0


            val game = nflGameRepo.findById(eventId.toLong())
                .orElseThrow { RuntimeException("Could not find game with id $eventId") }

            game.homeScore = homeScore
            game.awayScore = awayScore
            game.winnerId = if (homeScore > awayScore) homeId.toLong() else awayId.toLong()
            nflGameRepo.save(game)

            val gamePicks = nflPickRepo.findByGameId(game.id)
            gamePicks.forEach { this.processPick(it, game) }
            nflPickRepo.saveAll(gamePicks)
        }
    }

    private fun processPick(nflPick: NflPick, nflGameEntity: NflGameEntity) {
        nflPick.correctPick = nflPick.winningTeamId == nflGameEntity.winnerId
        nflPick.processed = true
    }
}