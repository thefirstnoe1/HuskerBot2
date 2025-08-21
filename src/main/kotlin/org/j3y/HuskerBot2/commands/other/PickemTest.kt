package org.j3y.HuskerBot2.commands.other

import com.fasterxml.jackson.databind.JsonNode
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.automation.pickem.nfl.NflPickem
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
class PickemTest : SlashCommand() {
    @Autowired lateinit var nflPickem: NflPickem

    override fun getCommandKey(): String = "pickem-test"
    override fun getDescription(): String = "Derpin'"
    override fun getOptions(): List<OptionData> = emptyList()

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        nflPickem.postWeeklyPickem()
        commandEvent.reply("Derped.").setEphemeral(true).queue()
    }
}