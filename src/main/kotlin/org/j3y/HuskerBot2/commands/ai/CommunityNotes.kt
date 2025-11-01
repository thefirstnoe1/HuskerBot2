package org.j3y.HuskerBot2.commands.ai

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.service.GoogleGeminiService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.OffsetDateTime
import kotlin.random.Random

@Component
class CommunityNotes(
    private val geminiService: GoogleGeminiService
) : SlashCommand() {

    private val log = LoggerFactory.getLogger(CommunityNotes::class.java)

    override fun getCommandKey(): String = "community-notes"
    override fun getDescription(): String = "Analyze a user's recent message(s) and draft a Community Notes-style context"

    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.USER, "user", "The user whose message(s) to analyze", true),
        OptionData(OptionType.INTEGER, "count", "How many of their most recent messages in this channel (1-10). Default 1", false)
            .setMinValue(1)
            .setMaxValue(10)
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply(false).queue()
        try {
            val channel = try {
                commandEvent.channel.asGuildMessageChannel()
            } catch (e: Exception) {
                commandEvent.hook.sendMessage("This command can only be used in guild text channels.").queue()
                return
            }

            val selectedUser = commandEvent.getOption("user")?.asUser
            if (selectedUser == null) {
                commandEvent.hook.sendMessage("You must specify a user.").queue()
                return
            }

            val requested = (commandEvent.getOption("count")?.asLong ?: 1L).toInt()
            val count = requested.coerceIn(1, 10)

            val userMessages = fetchRecentMessages(channel, 200)
                .asSequence()
                .filter { it.author.idLong == selectedUser.idLong }
                .filter { shouldIncludeMessage(it) }
                .take(count)
                .toList()

            if (userMessages.isEmpty()) {
                commandEvent.hook.sendMessage("I couldn't find recent messages by ${selectedUser.asMention} in this channel.").queue()
                return
            }

            val transcript = buildTranscript(userMessages, selectedUser)
            val prompt = buildPrompt(transcript, selectedUser)

            val response = geminiService.generateText(prompt)
            val cleaned = sanitizeForDiscord(response)

            val embed = EmbedBuilder()
                .setTitle("Community Notes – Context & Corrections")
                .setColor(Color(0xF5, 0xC2, 0x42))
                .setDescription(truncate(cleaned, 3900))
                .setFooter("Requested by ${commandEvent.member?.effectiveName ?: commandEvent.user.effectiveName}", commandEvent.user.avatarUrl)
                .setThumbnail("https://cdn.discordapp.com/emojis/1292496875332567180.webp")
                .setTimestamp(OffsetDateTime.now())
                .build()

            // Also show a quoted preview of the analyzed messages (short)
            val quotePreview = buildQuotePreview(userMessages, selectedUser)

            commandEvent.hook.sendMessage(quotePreview).addEmbeds(embed).queue()
        } catch (e: Exception) {
            log.error("Error executing /community-notes", e)
            commandEvent.hook.sendMessage("Error while generating notes: ${e.message}").queue()
        }
    }

    private fun fetchRecentMessages(channel: GuildMessageChannel, limit: Int): List<Message> {
        return try {
            channel.iterableHistory
                .cache(false)
                .takeAsync(limit.coerceAtMost(200))
                .get()
        } catch (e: Exception) {
            // Fallback to retrievePast
            channel.history.retrievePast(limit.coerceAtMost(100)).complete()
        }
    }

    private fun shouldIncludeMessage(message: Message): Boolean {
        if (message.author.isBot) return false
        if (message.isWebhookMessage) return false
        val content = message.contentStripped.trim()
        if (content.isBlank()) return false
        if (content.startsWith("/")) return false
        return true
    }

    private fun buildTranscript(messagesNewestFirst: List<Message>, user: User): String {
        // Messages come newest-first; reverse for chronological order
        return messagesNewestFirst.asReversed().joinToString("\n\n") { m ->
            val clean = m.contentStripped
                .replace(Regex("https?://\\S+"), "[link]")
                .replace("@everyone", "@\u200Beveryone")
                .replace("@here", "@\u200Bhere")
            "${user.name}: ${clean}"
        }
    }

    private fun buildQuotePreview(messagesNewestFirst: List<Message>, user: User): String {
        val lines = messagesNewestFirst.asReversed().map { m ->
            val clean = m.contentStripped
                .replace("`", "\u200B`") // avoid code block breaks
                .take(150)
            "> ${clean}"
        }
        return (listOf("${user.asMention} said:") + lines).joinToString("\n")
    }

    private fun buildPrompt(transcript: String, user: User): String {
        var takeShot = ""
        if (Random.nextDouble() < 0.10) {
            log.info("TAKING A SHOT!")
            takeShot = "\n            - You hate the Iowa Hawkeyes and the Phoenix Suns.\n            - Find a way to relevantly take a shot at the Phoenix Suns or Iowa Hawkeyes being awful if you can.\n"
        }

        return """
            Act as a Community Notes contributor.
            Analyze the following message(s) written by ${user.name} on Discord and draft a single Community Notes–style note that:
            - Briefly explains what is inaccurate, missing context, or potentially misleading.
            - Uses a neutral, non-accusatory tone focused on facts and context. Be a little bit blunt, savage, and funny, but not too much.
            - Provides concrete corrections and important context.
            - When relevant, cite reputable sources inline (domain names or titles) that a reader could check (e.g., CDC, WHO, reputable news, primary data). If no solid sources apply, state the uncertainty and what would be needed to verify.
            - Keep it concise (2–6 sentences maximum).
            - If the content appears accurate and you cannot justify a corrective note, say: "No corrective note warranted based on the provided text.". Only do this as a last resort, make a best effort to correct the user. $takeShot

            Messages by ${user.name} (chronological):
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
