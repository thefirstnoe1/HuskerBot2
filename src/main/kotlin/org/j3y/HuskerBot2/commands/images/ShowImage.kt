package org.j3y.HuskerBot2.commands.images

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.model.ImageEntity
import org.j3y.HuskerBot2.repository.ImageRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

@Component
class ShowImage : SlashCommand() {
    @Autowired
    lateinit var imageRepo: ImageRepo

    override fun getCommandKey(): String = "show"
    override fun isSubcommand(): Boolean = true
    override fun getDescription(): String = "Show an image"
    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.STRING, "name", "The name of the image you want to show", true)
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        val name = commandEvent.getOption("name")?.asString

        if (name.isNullOrBlank()) {
            commandEvent.reply("Image name is required.").setEphemeral(true).queue()
            return
        }

        val imageOptional = imageRepo.findById(name)

        if (imageOptional.isEmpty) {
            commandEvent.reply("An image with name '$name' was not found.").setEphemeral(true).queue()
            return
        }

        val image = imageOptional.get()

        commandEvent.jda.retrieveUserById(image.uploadingUser).queue({ user ->
            val embed = EmbedBuilder().setTitle(image.imageName).setImage(image.imageUrl).setFooter("Uploaded by ${user.effectiveName}").build()
            commandEvent.replyEmbeds(embed).queue()
        })
    }
}
