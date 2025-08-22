package org.j3y.HuskerBot2.commands.other

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.service.GoogleGeminiService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class Gemini(
    private val geminiService: GoogleGeminiService
) : SlashCommand() {

    private val log = LoggerFactory.getLogger(Gemini::class.java)

    override fun getCommandKey(): String = "gemini"
    override fun getDescription(): String = "Send a text prompt to Google Gemini (free tier)"

    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.STRING, "prompt", "The text prompt to send to Gemini", true)
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        try {
            commandEvent.deferReply().queue()
            val prompt = commandEvent.getOption("prompt")?.asString?.trim()
            if (prompt.isNullOrBlank()) {
                commandEvent.hook.sendMessage("Please provide a prompt.").setEphemeral(true).queue()
                return
            }
            val sendPrompt = "$prompt\nPlease keep the response between 1 and 4 paragraphs."

            val response = geminiService.generateText(sendPrompt)
            var message = sanitizeForDiscord(response)

            if (message.length >= 1800) {
                // Truncate to avoid exceeding Discord message limit
                message = message.substring(0, 1800) + "..."
            }

            commandEvent.hook.sendMessageEmbeds(
                EmbedBuilder().setTitle("Gemini AI")
                    .setColor(Color.CYAN)
                    .addField("Prompt", prompt, false)
                    .addField("Response", message, false)
                    .build()
            ).queue()
        } catch (e: Exception) {
            log.error("Error executing /gemini", e)
            commandEvent.hook.sendMessage("Error while calling Gemini: ${e.message}").setEphemeral(true).queue()
        }
    }

    private fun sanitizeForDiscord(text: String): String {
        // Basic sanitization to prevent unintended mentions, etc.
        return text
            .replace("@everyone", "@\u200Beveryone")
            .replace("@here", "@\u200Bhere")
            .takeIf { it.isNotBlank() } ?: "(no content)"
    }
}
