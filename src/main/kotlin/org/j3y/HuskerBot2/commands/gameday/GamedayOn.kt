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
class GamedayOn : GamedayBase() {
    override fun getCommandKey(): String = "on"
    override fun isSubcommand(): Boolean = true
    override fun getDescription(): String = "Turns gameday mode on"

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        try {
            this.setGameday(commandEvent, true)
        } catch (e: Exception) {
            commandEvent.reply("Error while trying to enable gameday mode: ${e.message}").setEphemeral(true).queue()
            return
        }
        commandEvent.replyEmbeds(EmbedBuilder().setTitle("Gameday Mode Enabled!").setColor(Color.RED).setDescription("Make your way to the gameday channels.").build()).queue()
    }
}
