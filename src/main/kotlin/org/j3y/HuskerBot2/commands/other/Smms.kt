package org.j3y.HuskerBot2.commands.other

import com.fasterxml.jackson.databind.JsonNode
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.service.EspnService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Component
class Smms : SlashCommand() {
    @Value("\${discord.channels.general}") lateinit var generalChannelId: String
    @Value("\${discord.channels.recruiting}") lateinit var recruitingChannelId: String
    @Value("\${discord.channels.admin}") lateinit var adminChannelId: String

    override fun getCommandKey(): String = "smms"
    override fun getDescription(): String = "Tee hee"
    override fun getPermissions(): DefaultMemberPermissions = DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.MESSAGE_MANAGE)
    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.STRING, "destination", "The target channel", true)
            .addChoice("General", "general")
            .addChoice("Recruiting", "recruiting")
            .addChoice("Admin", "admin"),
        OptionData(OptionType.STRING, "message", "The message to share", true)
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply(true).queue()

        val message = commandEvent.getOption("message")?.asString ?: ""
        val destination = commandEvent.getOption("destination")?.asString ?: ""

        var channel: TextChannel? = null

        when (destination) {
            "general" -> channel = commandEvent.jda.getTextChannelById(generalChannelId)
            "recruiting" -> channel = commandEvent.jda.getTextChannelById(recruitingChannelId)
            "admin" -> channel = commandEvent.jda.getTextChannelById(adminChannelId)
        }

        if (channel == null) {
            commandEvent.hook.sendMessage("Channel not found.").queue()
            return
        }

        channel.sendMessageEmbeds(
            EmbedBuilder()
                .setTitle("Secret Mammal Message System (SMMS)")
                .setDescription("These messages have no way to be verified to be accurate.")
                .setColor(Color.RED)
                .addField("Back Channel Communication", message, false)
                .setFooter("These messages are anonymous and there is no way to verify messages are accurate.")
                .setThumbnail("https://i.imgur.com/EGC1qNt.jpg")
                .build()
        ).queue()

        commandEvent.hook.sendMessage("Back channel communication successfully sent to ${channel.asMention}").queue()
    }
}