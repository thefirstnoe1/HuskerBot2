package org.j3y.HuskerBot2.commands.other

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.service.GoogleGeminiService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.OffsetDateTime

@Component
class Summarize(
    private val geminiService: GoogleGeminiService,
    @Value("\${discord.channels.bot-spam}") private val botSpamChannelId: String
) : SlashCommand() {

    private val log = LoggerFactory.getLogger(Summarize::class.java)

    override fun getCommandKey(): String = "summarize"
    override fun getDescription(): String = "Summarize the last N posts in this channel using Gemini"

    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.INTEGER, "count", "How many recent posts to include (1-100). Default 50", false)
            .setMinValue(1)
            .setMaxValue(100)
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply(true).queue()
        try {
            val channel = try {
                commandEvent.channel.asGuildMessageChannel()
            } catch (e: Exception) {
                commandEvent.hook.sendMessage("This command can only be used in guild text channels.").queue()
                return
            }

            val requested = (commandEvent.getOption("count")?.asLong ?: 50L).toInt()
            val count = requested.coerceIn(1, 100)

            val messages = fetchRecentMessages(channel, count * 2) // over-fetch a bit to filter out bots/empties
                .filter { shouldIncludeMessage(it) }
                .take(count)

            if (messages.isEmpty()) {
                commandEvent.hook.sendMessage("I couldn't find any recent user messages to summarize.").queue()
                return
            }

            val transcript = buildTranscript(messages)
            val prompt = buildPrompt(transcript, channel.name)

            val response = geminiService.generateText(prompt)
            val message = sanitizeForDiscord(response)

            val embed = EmbedBuilder()
                .setTitle("Channel Summary")
                .setColor(Color(0x3B, 0x88, 0xC3))
                .setDescription(truncate(message, 3900))
                .addField("Channel", "#${channel.name}", true)
                .addField("Messages summarized", messages.size.toString(), true)
                .setFooter("Requested by ${commandEvent.user.asTag}")
                .setTimestamp(OffsetDateTime.now())
                .build()

            val spamChannel = commandEvent.jda.getTextChannelById(botSpamChannelId)
            if (spamChannel == null) {
                commandEvent.reply("Bot spam channel not found.").setEphemeral(true).queue()
                return
            }

            val link = spamChannel.sendMessageEmbeds(embed).complete().jumpUrl
            commandEvent.hook.sendMessage("View channel summary here: $link").queue()
        } catch (e: Exception) {
            log.error("Error executing /summarize", e)
            commandEvent.hook.sendMessage("Error while summarizing: ${e.message}").setEphemeral(true).queue()
        }
    }

    private fun fetchRecentMessages(channel: GuildMessageChannel, limit: Int): List<Message> {
        return try {
            channel.iterableHistory
                .cache(false)
                .takeAsync(limit.coerceAtMost(200))
                .get()
        } catch (e: Exception) {
            channel.history.retrievePast(limit.coerceAtMost(100)).complete()
        }
    }

    private fun shouldIncludeMessage(message: Message): Boolean {
        if (message.author.isBot) return false
        val content = message.contentStripped.trim()
        if (content.isBlank()) return false
        if (content.startsWith("/")) return false
        return true
    }

    private fun buildTranscript(messages: List<Message>): String {
        // Messages are returned newest-first by default when using retrievePast; iterableHistory returns newest to oldest as well
        // We will reverse to get chronological order
        return messages.asReversed().joinToString("\n") { m ->
            val username = m.author.name
            val clean = m.contentStripped
                .replace(Regex("https?://\\S+"), "[link]")
                .replace("@everyone", "@\u200Beveryone")
                .replace("@here", "@\u200Bhere")
            "${username}: ${clean}"
        }
    }

    private fun buildPrompt(transcript: String, channelName: String): String {
        return """
            You are summarizing a Discord channel conversation (#$channelName).
            Summarize the key points and topics discussed in the conversation below.
            - Be concise (3-6 bullet points or 1-2 short paragraphs).
            - Focus on decisions, questions, action items, and themes.
            - Use neutral tone and avoid personal judgments.
            - Do not invent details. If uncertain, keep it general.
            Conversation transcript (most recent at the end):
            ---
            $transcript
            ---
        """.trimIndent()
    }

    private fun sanitizeForDiscord(text: String): String {
        return text
            .replace("@everyone", "@\u200Beveryone")
            .replace("@here", "@\u200Bhere")
            .ifBlank { "(no content)" }
    }

    private fun truncate(text: String, maxLen: Int): String {
        return if (text.length <= maxLen) text else text.substring(0, maxLen - 3) + "..."
    }
}
