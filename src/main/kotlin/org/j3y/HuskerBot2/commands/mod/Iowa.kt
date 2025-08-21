package org.j3y.HuskerBot2.commands.mod

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.exceptions.PermissionException
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.springframework.stereotype.Component
import java.awt.Color
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
        val user = commandEvent.getOption("user")?.asMember
        if (user == null) { commandEvent.reply("Invalid user.").setEphemeral(true).queue(); return }

        val length = commandEvent.getOption("minutes")?.asInt ?: 30
        val duration = Duration.ofMinutes(length.toLong())

        val reason = commandEvent.getOption("reason")?.asString ?: "You have been sent to Iowa for being a rhuligan."

        try {
            user.timeoutFor(duration).reason(reason).queue(
                {
                    commandEvent.replyEmbeds(
                        EmbedBuilder()
                            .setTitle("Banished to Iowa")
                            .setColor(Color.YELLOW)
                            .setDescription("${user.effectiveName} has been banished to Iowa!")
                            .addField("Reason", reason, false)
                            .addField("Duration", "${duration.toMinutes()} minutes", false)
                            .build()
                    ).queue()
                },
                { commandEvent.reply("Unable to Iowa ${user.user.effectiveName}.").setEphemeral(true).queue() }
            )
        } catch(e: PermissionException) {
            commandEvent.reply("You do not have permission to Iowa ${user.user.effectiveName}.").setEphemeral(true).queue()
        }
    }
}
