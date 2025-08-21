package org.j3y.HuskerBot2.commands.other

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.service.UrbanDictionaryService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class UrbanDictionary : SlashCommand() {

    @Autowired
    lateinit var urbanService: UrbanDictionaryService

    private val log = LoggerFactory.getLogger(UrbanDictionary::class.java)

    override fun getCommandKey(): String = "ud"
    override fun getDescription(): String = "Search Urban Dictionary for a term"

    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.STRING, "term", "The term to define", true)
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply().queue()
        try {
            val term = commandEvent.getOption("term")?.asString?.trim()
            if (term.isNullOrEmpty()) {
                commandEvent.hook.sendMessage("Please provide a term to search.").queue()
                return
            }

            val def = urbanService.define(term)
            if (def == null) {
                commandEvent.hook.sendMessage("No results found for '$term'.").queue()
                return
            }

            val embed = EmbedBuilder()
                .setTitle("Urban Dictionary: ${def.word}", def.permalink)
                .setColor(Color(0x1D, 0xA1, 0xF2))
                .addField("Definition", truncate(def.definition, 1024), false)
                .apply {
                    val example = def.example?.takeIf { it.isNotBlank() }
                    if (example != null) {
                        addField("Example", truncate(example, 1024), false)
                    }
                    addField("Votes", "👍 ${def.thumbsUp}   👎 ${def.thumbsDown}", true)
                    def.author?.let { addField("Author", it, true) }
                }
                .build()

            commandEvent.hook.sendMessageEmbeds(embed).queue()
        } catch (e: Exception) {
            log.error("Error executing /urban command", e)
            commandEvent.hook.sendMessage("Sorry, there was an error searching Urban Dictionary.").queue()
        }
    }

    private fun truncate(text: String, max: Int): String {
        if (text.length <= max) return text
        return text.substring(0, max - 3) + "..."
    }
}
