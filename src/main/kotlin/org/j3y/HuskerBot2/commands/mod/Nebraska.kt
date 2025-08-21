package org.j3y.HuskerBot2.commands.mod

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.exceptions.PermissionException
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class Nebraska : SlashCommand() {

    override fun getCommandKey(): String = "nebraska"
    override fun getDescription(): String = "Remove a rhuligan from Iowa."
    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.USER, "user", "The user who is allowed to leave Iowa", true)
    )
    override fun getPermissions(): DefaultMemberPermissions = DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        val user = commandEvent.getOption("user")?.asMember
        if (user == null) { commandEvent.hook.sendMessage("Invalid user.").queue(); return }

        try {
            user.removeTimeout().queue(
                {
                    commandEvent.replyEmbeds(
                        EmbedBuilder()
                            .setTitle("Return to Nebraska")
                            .setColor(Color.RED)
                            .addField("Welcome Back!", "${user.effectiveName} is welcomed back to Nebraska!", false)
                            .addField("Welcomed by", commandEvent.user.effectiveName, false)
                            .build()
                    ).queue()
                },
                { commandEvent.hook.sendMessage("Unable to remove ${user.user.effectiveName} from Iowa.").setEphemeral(true).queue() }
            )
        } catch(e: PermissionException) {
            commandEvent.reply("You do not have permission to Nebraska ${user.user.effectiveName}.").setEphemeral(true).queue()
        }
    }
}
