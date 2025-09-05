package org.j3y.HuskerBot2.commands.other

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.j3y.HuskerBot2.commands.SlashCommand
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class About : SlashCommand() {
    override fun getCommandKey(): String = "about"
    override fun getDescription(): String = "Learn about HuskerBot2 and find the GitHub link."

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply().queue()

        val embed = EmbedBuilder()
            .setTitle("HuskerBot", "https://github.com/jjg18/HuskerBot2")
            .setColor(Color(207, 0, 0)) // Husker red-ish
            .setDescription(
                "A bot for the Huskers discord.\n" +
                        "• Schedules and countdowns\n" +
                        "• NFL/CFB pick'em and fun betting\n" +
                        "• Image tools (deep fry, AI), inspiration, reminders, and more"
            )
            .addField("Contribute", "https://github.com/jjg18/HuskerBot2 - Feature suggestions and pull requests welcome.", false)
            .setFooter("Use /help to discover commands", null)
            .build()

        commandEvent.hook.sendMessageEmbeds(embed).queue()
    }
}
