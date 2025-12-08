package org.j3y.HuskerBot2.commands.ai

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.service.LinkSummarizerService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SummarizeLink(
    private val linkSummarizerService: LinkSummarizerService
) : SlashCommand() {

    private val log = LoggerFactory.getLogger(SummarizeLink::class.java)
    override fun getCommandKey(): String = "summarize-link"
    override fun getDescription(): String = "Fetch a web page, extract text, and summarize it with Gemini"

    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.STRING, "link", "The URL to summarize", true)
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply(false).queue()
        try {
            val link = commandEvent.getOption("link")?.asString?.trim()
            if (link.isNullOrBlank()) {
                commandEvent.hook.sendMessage("You must provide a valid link.").queue()
                return
            }
            val embed = linkSummarizerService.summarizeToEmbed(
                link,
                commandEvent.member?.effectiveName ?: commandEvent.user.effectiveName,
                commandEvent.user.avatarUrl
            )
            commandEvent.hook.sendMessageEmbeds(embed).queue()
        } catch (e: Exception) {
            log.error("Error executing /summarize-link", e)
            commandEvent.hook.sendMessage("Error while summarizing link: ${e.message}").setEphemeral(true).queue()
        }
    }
}
