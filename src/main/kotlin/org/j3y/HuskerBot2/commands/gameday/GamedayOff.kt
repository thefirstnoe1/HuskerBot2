package org.j3y.HuskerBot2.commands.gameday

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.repository.ImageRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class GamedayOff : GamedayBase() {
    @Value("\${discord.channels.general}")lateinit var generalChannelId: String

    override fun getCommandKey(): String = "off"
    override fun isSubcommand(): Boolean = true
    override fun getDescription(): String = "Turns gameday mode off"

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        try {
            this.setGameday(commandEvent, false)
        } catch (e: Exception) {
            commandEvent.reply("Error while trying to disable gameday mode: ${e.message}").setEphemeral(true).queue()
            return
        }

        val channel = commandEvent.guild?.getTextChannelById(generalChannelId) ?: return

        channel.sendMessageEmbeds(EmbedBuilder().setTitle("Gameday Mode Disabled!").setColor(Color.RED).setDescription("Hopefully it was a W. Keep it civil.").build()).queue()
        commandEvent.reply("Gameday mode disabled.").setEphemeral(true).queue()
    }
}
