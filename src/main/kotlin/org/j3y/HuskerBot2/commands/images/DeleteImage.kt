package org.j3y.HuskerBot2.commands.images

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
class DeleteImage : SlashCommand() {
    @Autowired
    lateinit var imageRepo: ImageRepo

    override fun getCommandKey(): String = "delete"
    override fun isSubcommand(): Boolean = true
    override fun getDescription(): String = "Delete an image"
    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.STRING, "name", "The name of the image you want to delete", true)
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        val name = commandEvent.getOption("name")?.asString

        if (name.isNullOrBlank()) {
            commandEvent.reply("Both name and url are required.").setEphemeral(true).queue()
            return
        }

        // At this point, the URL looks valid and points to an image.
        val image = imageRepo.findById(name).orElse(null)

        if (image == null) {
            commandEvent.reply("An image with name '$name' does not exist.").setEphemeral(true).queue()
            return
        }

        val hasPerm = commandEvent.member?.hasPermission(Permission.MESSAGE_MANAGE) ?: false

        if (!commandEvent.user.id.equals(image.uploadingUser) && !hasPerm) {
            commandEvent.reply("You do not have permission to delete this image (you are not the creator or you are not a mod.)").setEphemeral(true).queue()
            return
        }

        imageRepo.delete(image)

        commandEvent.reply("Image '$name' has been deleted by ${commandEvent.user.effectiveName}.").queue()
    }
}
