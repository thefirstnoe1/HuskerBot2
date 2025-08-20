package org.j3y.HuskerBot2.commands.mod

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.exceptions.PermissionException
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class Iowa : SlashCommand() {

    override fun getCommandKey(): String = "iowa"
    override fun getDescription(): String = "Send a rhuligan to Iowa."
    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.USER, "user", "The user who is being a rhuligan", true),
        OptionData(OptionType.STRING, "reason", "The reason the user is being sent to Iowa", false),
        OptionData(OptionType.INTEGER, "minutes", "How long to banish them to Iowa (default 30 minutes)", false),
    )
    override fun getPermissions(): DefaultMemberPermissions = DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply(true).queue()

        val user = commandEvent.getOption("user")?.asMember
        if (user == null) { commandEvent.hook.sendMessage("Invalid user.").queue(); return }

        val length = commandEvent.getOption("minutes")?.asInt ?: 30
        val duration = Duration.ofMinutes(length.toLong())

        val reason = commandEvent.getOption("reason")?.asString ?: "You have been sent to Iowa for being a rhuligan."

        try {
            user.timeoutFor(duration).reason(reason).queue(
                {
                    commandEvent.channel.sendMessage("${user.user.effectiveName} has been sent to Iowa for ${length} minutes with reason: ${reason}").queue()
                    commandEvent.hook.deleteOriginal().queue()
                },
                { commandEvent.hook.sendMessage("Unable to Iowa ${user.user.effectiveName}.").queue() }
            )
        } catch(e: PermissionException) {
            e.printStackTrace()
            commandEvent.hook.sendMessage("You do not have permission to Iowa ${user.user.effectiveName}.").queue()
        }
    }
}
