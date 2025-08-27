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
                    // Send confirmation in the channel
                    commandEvent.replyEmbeds(
                        EmbedBuilder()
                            .setTitle("Banished to Iowa")
                            .setColor(Color.YELLOW)
                            .setDescription("${user.effectiveName} has been banished to Iowa!")
                            .addField("Reason", reason, false)
                            .addField("Duration", "${duration.toMinutes()} minutes", false)
                            .build()
                    ).queue()

                    // Also DM the affected user
                    val dmEmbed = EmbedBuilder()
                        .setTitle("Sent to Iowa")
                        .setDescription("Fuck Iowa. You're here because you did some shit that an Iowa fan would do. Think about what you did and we might let you back into Nebraska. Good luck, you fucking Iowadiot.")
                        .setColor(Color.YELLOW)
                        .addField("Reason", reason, false)
                        .addField("Duration", "${duration.toMinutes()} minutes", false)
                        .build()
                    user.user.openPrivateChannel().queue({ channel ->
                        channel.sendMessageEmbeds(dmEmbed).queue({}, { /* Ignore DM failures (user may have DMs closed) */ })
                    }, { /* Ignore failures opening DM channel */ })
                },
                { commandEvent.reply("Unable to Iowa ${user.user.effectiveName}.").setEphemeral(true).queue() }
            )
        } catch(e: PermissionException) {
            commandEvent.reply("You do not have permission to Iowa ${user.user.effectiveName}.").setEphemeral(true).queue()
        }
    }
}
