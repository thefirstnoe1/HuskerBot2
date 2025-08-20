package org.j3y.HuskerBot2.commands.images

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.repository.ImageRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ListImages : SlashCommand() {
    @Autowired
    lateinit var imageRepo: ImageRepo

    override fun getCommandKey(): String = "list"
    override fun isSubcommand(): Boolean = true
    override fun getDescription(): String = "List all available images"

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        val images = imageRepo.findAll().map { it.imageName  }

        commandEvent.replyEmbeds(EmbedBuilder().setTitle("Available Images").setDescription(images.joinToString(", ")).build()).queue()
    }
}
