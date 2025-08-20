package org.j3y.HuskerBot2.commands.mod

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.exceptions.PermissionException
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.springframework.stereotype.Component

@Component
class Nebraska : SlashCommand() {

    override fun getCommandKey(): String = "nebraska"
    override fun getDescription(): String = "Remove a rhuligan from Iowa."
    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.USER, "user", "The user who is allowed to leave Iowa", true)
    )
    override fun getPermissions(): DefaultMemberPermissions = DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply(true).queue()

        val user = commandEvent.getOption("user")?.asMember
        if (user == null) { commandEvent.hook.sendMessage("Invalid user.").queue(); return }

        try {
            user.removeTimeout().queue(
                {
                    commandEvent.channel.sendMessage("${user.user.effectiveName} is welcome back to Nebraska.").queue()
                    commandEvent.hook.deleteOriginal().queue()
                },
                { commandEvent.hook.sendMessage("Unable to remove ${user.user.effectiveName} from Iowa.").queue() }
            )
        } catch(e: PermissionException) {
            commandEvent.hook.sendMessage("You do not have permission to Nebraska ${user.user.effectiveName}.").queue()
        }
    }
}
