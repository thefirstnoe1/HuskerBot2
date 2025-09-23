package org.j3y.HuskerBot2.commands.images

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.utils.FileUpload
import org.j3y.HuskerBot2.commands.SlashCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.awt.AlphaComposite
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Component
class Slowking : SlashCommand() {
    private val log = LoggerFactory.getLogger(Slowking::class.java)

    override fun getCommandKey(): String = "slowking"
    override fun getDescription(): String = "Superimpose a user's avatar onto the Slowking template"
    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.USER, "user", "User whose avatar to use", true)
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply().queue()
        try {
            val targetUser = commandEvent.getOption("user")?.asUser
            if (targetUser == null) {
                commandEvent.hook.sendMessage("You must specify a user.").queue()
                return
            }

            val avatarUrl = targetUser.effectiveAvatarUrl + "?size=512"
            val avatarImg = runCatching {
                ImageIO.read(java.net.URL(avatarUrl))
            }.getOrNull()

            if (avatarImg == null) {
                commandEvent.hook.sendMessage("Couldn't download that user's avatar.").queue()
                return
            }

            val templateStream = this::class.java.classLoader.getResourceAsStream("images/slowking.png")
            if (templateStream == null) {
                log.error("images/slowking.png not found in resources")
                commandEvent.hook.sendMessage("Template image not found.").queue()
                return
            }

            val template = templateStream.use { ImageIO.read(it) }
            val composed = compositeAvatarOnTemplate(avatarImg, template)

            val baos = ByteArrayOutputStream()
            ImageIO.write(composed, "png", baos)
            val outBytes = baos.toByteArray()

            val filename = "slowking_${targetUser.id}.png"
            commandEvent.hook.sendMessage(targetUser.asMention).addFiles(FileUpload.fromData(outBytes, filename)).queue()
        } catch (e: Exception) {
            log.error("Slowking command failed", e)
            commandEvent.hook.sendMessage("Slowking failed: ${e.message}").queue()
        }
    }

    private fun compositeAvatarOnTemplate(avatar: BufferedImage, template: BufferedImage): BufferedImage {
        // Prepare output image with alpha
        val out = BufferedImage(template.width, template.height, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // Draw base template first
            g.drawImage(template, 0, 0, null)

            val targetSize = 180
            val resized = resizeImage(avatar, targetSize, targetSize)

            val x = 235
            val y = 255

            // Draw avatar with slight transparency to blend a bit
            g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)
            g.drawImage(resized, x, y, null)
        } finally {
            g.dispose()
        }
        return out
    }

    private fun resizeImage(src: BufferedImage, w: Int, h: Int): BufferedImage {
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g2: Graphics2D = out.createGraphics()
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.drawImage(src, 0, 0, w, h, null)
        } finally {
            g2.dispose()
        }
        return out
    }
}
