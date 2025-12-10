package org.j3y.HuskerBot2.commands.ai

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.utils.FileUpload
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.service.GoogleGeminiService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AiImage : SlashCommand() {
    @Autowired
    lateinit var gemini: GoogleGeminiService
    @Value("\${discord.channels.bot-spam}") lateinit var botSpamChannelId: String

    override fun getCommandKey(): String = "ai-image"
    override fun getDescription(): String = "Generate an AI image from a prompt using Gemini"
    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.STRING, "prompt", "Describe the image to generate", true)
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        val prompt = commandEvent.getOption("prompt")?.asString
        if (prompt.isNullOrBlank()) {
            commandEvent.reply("Prompt is required.").setEphemeral(true).queue()
            return
        }

        commandEvent.deferReply(true).queue()

        val result = gemini.generateImage(prompt)

        when (result) {
            is GoogleGeminiService.ImageResult.Error -> {
                commandEvent.hook.sendMessage("Error: ${result.message}").queue()
            }
            is GoogleGeminiService.ImageResult.ImageBytes -> {
                val upload = FileUpload.fromData(result.bytes, "ai-image.png")

                val spamChannel = commandEvent.jda.getTextChannelById(botSpamChannelId)

                if (spamChannel == null) {
                    commandEvent.reply("Bot spam channel not found.").setEphemeral(true).queue()
                    return
                }

                val link = spamChannel.sendMessageEmbeds(
                    EmbedBuilder()
                    .addField("Prompt", prompt, false)
                    .addField("Requested by", commandEvent.user.asMention, false)
                    .setFooter("This AI slop is brought to you by Gemini")
                    .build())
                    .addFiles(upload)
                    .complete().jumpUrl
                commandEvent.hook.sendMessage("Sent gemini output to bot spam channel: $link").setEphemeral(true).queue()
            }
        }
    }
}