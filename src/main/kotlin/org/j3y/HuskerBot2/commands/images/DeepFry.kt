package org.j3y.HuskerBot2.commands.images

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.utils.FileUpload
import org.j3y.HuskerBot2.commands.SlashCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DeepFry(
    private val fryer: org.j3y.HuskerBot2.service.DeepFryProcessor
) : SlashCommand() {
    private final val log = LoggerFactory.getLogger(DeepFry::class.java)

    override fun getCommandKey(): String = "deepfry"
    override fun getDescription(): String = "Download an image from a URL and randomly 'deep fry' it with meme-style filters."
    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.STRING, "url", "Direct URL to an image (http/https)", true)
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply().queue() // public by default

        val url = commandEvent.getOption("url")?.asString?.trim()
        if (url.isNullOrBlank() || !fryer.isValidHttpUrl(url)) {
            commandEvent.hook.sendMessage("Please provide a valid http/https image URL.").queue()
            return
        }

        try {
            val img = fryer.downloadImage(url)
            if (img == null) {
                commandEvent.hook.sendMessage("Could not download that image. Make sure the URL is reachable and points to an image.").queue()
                return
            }
            val jpegBytes = fryer.fryToJpeg(img)
            val filename = "deepfried_${System.currentTimeMillis()}.jpg"

            commandEvent.hook.sendFiles(FileUpload.fromData(jpegBytes, filename)).queue()
        } catch (e: Exception) {
            e.printStackTrace()
            commandEvent.hook.sendMessage("Image processing failed: ${e.message}").queue()
        }
    }
}