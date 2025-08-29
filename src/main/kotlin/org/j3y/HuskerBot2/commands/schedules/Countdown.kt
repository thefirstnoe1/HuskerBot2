package org.j3y.HuskerBot2.commands.schedules

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.repository.ScheduleRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class Countdown : SlashCommand() {

    @Autowired
    lateinit var scheduleRepo: ScheduleRepo

    override fun getCommandKey(): String = "countdown"
    override fun getDescription(): String = "Get the countdown until the next husker game"
    override fun getOptions(): List<OptionData> = emptyList()

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply().queue()

        val upcoming = scheduleRepo.findFirstByDateTimeAfterOrderByDateTimeAsc(Instant.now())
        if (upcoming == null) {
            commandEvent.hook.sendMessage("No upcoming games found.").queue()
            return
        }

        val title = "Nebraska vs. ${upcoming.opponent}"
        val dateOfGame = upcoming.dateTime
        val dateNow = Instant.now()
        val timeBetween = Duration.between(dateNow, dateOfGame)

        val days = timeBetween.toDaysPart()
        val hours = timeBetween.toHoursPart()
        val minutes = timeBetween.toMinutesPart()
        val seconds = timeBetween.toSecondsPart()

        val zone = ZoneId.of("America/Chicago")
        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy h:mm a z").withZone(zone)
        val formattedDate = formatter.format(dateOfGame)

        val requesterName = commandEvent.member?.effectiveName ?: commandEvent.user?.effectiveName ?: "Unknown"
        val requesterAvatar = commandEvent.user.avatarUrl

        val embed = EmbedBuilder()
            .setTitle("Countdown to $title")
            .setColor(Color.RED)
            .setDescription("There are $days days, $hours hours, $minutes minutes and $seconds seconds until game time!\nKickoff: $formattedDate")
            .addField("Kickoff", formattedDate, true)
            .addField("Home/Away", upcoming.venueType.replaceFirstChar { it.titlecase() }, true)
            .addField("Location", upcoming.location, true)
            .setFooter("Requested by $requesterName", requesterAvatar)
            .build()

        commandEvent.hook.sendMessageEmbeds(embed).queue()
    }
}