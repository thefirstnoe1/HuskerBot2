package org.j3y.HuskerBot2.commands.images

import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.utils.FileUpload
import org.j3y.HuskerBot2.commands.ContextCommand
import org.j3y.HuskerBot2.service.DeepFryProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DeepFryContext(
    private val fryer: DeepFryProcessor
) : ContextCommand() {

    private val log = LoggerFactory.getLogger(DeepFryContext::class.java)

    override fun getCommandMenuText() = "Deep Fry"
    override fun getCommandType() = Command.Type.MESSAGE
    override fun getDescription(): String = "Deep fry the image attachments and embeds in the selected message"

    override fun execute(event: MessageContextInteractionEvent) {
        try {
            val message = event.target

            // Collect image URLs from attachments and embeds
            val urls = mutableListOf<String>()
            // Attachments that are images
            message.attachments
                .filter { it.isImage && (it.contentType?.startsWith("image/") == true) }
                .forEach { att -> urls.add(att.proxyUrl) }

            // Embeds with image or thumbnail
            message.embeds.forEach { emb ->
                emb.image?.url?.let { urls.add(it) }
                emb.thumbnail?.url?.let { urls.add(it) }
            }

            val distinct = urls.distinct().take(4)
            if (distinct.isEmpty()) {
                event.reply("No images found in that message.").setEphemeral(true).queue()
                return
            }
            event.deferReply().queue()

            val outputs = mutableListOf<FileUpload>()
            for ((idx, url) in distinct.withIndex()) {
                try {
                    val img = fryer.downloadImage(url) ?: continue
                    val fried = fryer.fryToJpeg(img)
                    val name = "deepfried_${System.currentTimeMillis()}_${idx + 1}.jpg"
                    outputs.add(FileUpload.fromData(fried, name))
                } catch (e: Exception) {
                    log.warn("Failed to process image from $url: ${e.message}")
                }
            }

            if (outputs.isEmpty()) {
                event.hook.sendMessage("Failed to deep fry any images from that message.").setEphemeral(true).queue()
                return
            }

            // If we have at least one image, send the response in the same channel as the message (public)
            event.hook.sendFiles(outputs).queue()
        } catch (e: Exception) {
            log.error("Error executing DeepFryContext", e)
            event.hook.sendMessage("Error while deep frying: ${e.message}").setEphemeral(true).queue()
        }
    }
}
