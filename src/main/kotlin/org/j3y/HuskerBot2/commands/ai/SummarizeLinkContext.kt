package org.j3y.HuskerBot2.commands.ai

import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import org.j3y.HuskerBot2.commands.ContextCommand
import org.j3y.HuskerBot2.service.LinkSummarizerService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SummarizeLinkContext(
    private val linkSummarizerService: LinkSummarizerService
) : ContextCommand() {

    private val log = LoggerFactory.getLogger(SummarizeLinkContext::class.java)

    override fun getCommandMenuText() = "Summarize Link"
    override fun getCommandType() = Command.Type.MESSAGE
    override fun getDescription(): String = "Summarize the first link found in the selected message"

    override fun execute(commandEvent: MessageContextInteractionEvent) {
        try {
            val channel = try {
                commandEvent.channel?.asGuildMessageChannel()
            } catch (e: Exception) {
                commandEvent.reply("This command can only be used in guild text channels.").setEphemeral(true).queue()
                return
            }

            if (channel == null) { return }

            val messageContent = commandEvent.target.contentRaw ?: ""
            val link = extractFirstLink(messageContent)

            if (link == null) {
                // Stay ephemeral when no link is found
                //commandEvent.hook.deleteOriginal().queue()
                commandEvent.reply("No link found in message.").setEphemeral(true).queue()
                return
            }

            commandEvent.deferReply(false).queue()

            val embed = linkSummarizerService.summarizeToEmbed(
                link,
                commandEvent.member?.effectiveName ?: commandEvent.user.effectiveName,
                commandEvent.user.avatarUrl
            )

            // Post the summary publicly in the same channel as the selected message
            commandEvent.hook.sendMessageEmbeds(embed).queue()
        } catch (e: Exception) {
            log.error("Error executing SummarizeLinkContext", e)
            commandEvent.hook
                .sendMessage("Error while summarizing link: ${e.message}")
                .setEphemeral(true)
                .queue()
        }
    }

    private fun extractFirstLink(text: String): String? {
        // Look for an explicit http(s) URL first
        val httpRegex = Regex("(?i)https?://[\\w.-]+(?:/[\\w\\-._~:/?#[@]!$&'()*+,;=%]*)?")
        val httpMatch = httpRegex.find(text)
        if (httpMatch != null) return httpMatch.value

        // Fallback: angle-bracketed URLs like <https://example.com>
        val bracketRegex = Regex("<(?i)https?://[^>]+>")
        val bracketMatch = bracketRegex.find(text)
        if (bracketMatch != null) return bracketMatch.value.trim('<', '>')

        return null
    }
}
