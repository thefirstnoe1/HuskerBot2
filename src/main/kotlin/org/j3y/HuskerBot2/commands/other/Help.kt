package org.j3y.HuskerBot2.commands.other

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import org.j3y.HuskerBot2.commands.SlashCommand
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class Help(
    private val allCommands: List<SlashCommand>
) : SlashCommand() {
    override fun getCommandKey(): String = "help"
    override fun getDescription(): String = "List all commands and their descriptions"

    override fun isLogged(): Boolean = false

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        // ephemeral reply
        commandEvent.deferReply(true).queue()

        // Collect top-level commands (exclude subcommands) and ignore this Help's subcommands notion
        val commands = allCommands
            .filter { !it.isSubcommand() && it.getPermissions() == DefaultMemberPermissions.ENABLED}
            .sortedBy { it.getCommandKey() }

        val desc = StringBuilder()
        commands.forEach { cmd ->
            val name = cmd.getCommandKey()
            val description = cmd.getDescription().ifBlank { "(no description)" }
            desc.append("/%s — %s\n".format(name, description))
        }

        val embed = EmbedBuilder()
            .setTitle("HuskerBot Commands")
            .setColor(Color(207, 0, 0))
            .setDescription("Use the following slash commands:\n\n" + desc.toString())
            .setFooter("Tip: Some commands have subcommands and options.", null)
            .build()

        commandEvent.hook.sendMessageEmbeds(embed).queue()
    }
}
