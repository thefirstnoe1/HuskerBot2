package org.j3y.HuskerBot2.commands.schedules

import com.fasterxml.jackson.databind.JsonNode
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.service.EspnService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Component
class Countdown : SlashCommand() {

    @Autowired
    lateinit var espnService: EspnService

    override fun getCommandKey(): String = "countdown"
    override fun getDescription(): String = "Get the countdown until the next husker game"
    override fun getOptions(): List<OptionData> = emptyList()

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply().queue()
        val apiJson: JsonNode = espnService.getTeamData("nebraska")
        val nextGame = apiJson.path("team").path("nextEvent").path(0)

        val title = nextGame.path("name").asText()
        val dateOfGame = OffsetDateTime.parse(nextGame.path("date").asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
        val dateNow = Instant.now()
        val timeBetween = Duration.between(dateNow, dateOfGame)

        val days = timeBetween.toDaysPart()
        val hours = timeBetween.toHoursPart()
        val minutes = timeBetween.toMinutesPart()
        val seconds = timeBetween.toSecondsPart()

        val embed = EmbedBuilder()
            .setTitle("Countdown to $title")
            .setColor(Color.RED)
            .setDescription("There are $days days, $hours hours, $minutes minutes and $seconds seconds til gameday!")
            .build()

        commandEvent.hook.sendMessageEmbeds(embed).queue()
    }
}