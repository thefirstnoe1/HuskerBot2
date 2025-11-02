package org.j3y.HuskerBot2.commands

import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.slf4j.LoggerFactory

open class ContextCommand {
    private val log = LoggerFactory.getLogger(ContextCommand::class.java)

    open fun getCommandType(): Command.Type = Command.Type.MESSAGE
    open fun getCommandMenuText(): String = ""
    open fun isLogged(): Boolean = true
    open fun getDescription(): String = ""
    open fun getPermissions(): DefaultMemberPermissions = DefaultMemberPermissions.ENABLED

    open fun execute(event: UserContextInteractionEvent) {
    }

    open fun execute(event: MessageContextInteractionEvent) {
    }
}
