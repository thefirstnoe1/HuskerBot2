package org.j3y.HuskerBot2.commands

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.slf4j.LoggerFactory

open class SlashCommand {
    private val log = LoggerFactory.getLogger(SlashCommand::class.java)

    open fun getCommandKey(): String = ""
    open fun isSubcommand(): Boolean = false
    open fun getSubcommands(): List<SlashCommand> = emptyList()
    open fun getDescription(): String = ""
    open fun getOptions(): List<OptionData> = emptyList()
    open fun getPermissions(): DefaultMemberPermissions = DefaultMemberPermissions.ENABLED

    open fun execute(commandEvent: SlashCommandInteractionEvent) {
        getSubcommands().find { it.getCommandKey() == commandEvent.subcommandName }?.execute(commandEvent)
    }

    fun getSubcommandData(): List<SubcommandData> {
        return getSubcommands().map { SubcommandData(it.getCommandKey(), it.getDescription()).addOptions(it.getOptions()) }
    }
}
