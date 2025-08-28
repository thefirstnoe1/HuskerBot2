package org.j3y.HuskerBot2.commands.other

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.repository.EmojiUsageRepo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.OffsetDateTime

@Component
class EmojiUsage(
    private val emojiUsageRepo: EmojiUsageRepo,
    @Value("\${discord.channels.bot-spam}") private val botSpamChannelId: String,
) : SlashCommand() {

    private val log = LoggerFactory.getLogger(EmojiUsage::class.java)

    override fun getCommandKey(): String = "emoji-usage"
    override fun getDescription(): String = "Display emoji usage stats for this server"

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply(true).queue()
        try {
            val guild = commandEvent.guild
            if (guild == null) {
                commandEvent.hook.sendMessage("This command can only be used in a server.").setEphemeral(true).queue()
                return
            }

            val usages = emojiUsageRepo.findAllByOrderByCountDesc()

            if (usages.isEmpty()) {
                commandEvent.hook.sendMessage("No emoji usage recorded yet.").queue()
                return
            }

            val lines = usages.map { usage ->
                val resolved = try {
                    guild.retrieveEmojiById(usage.emojiId).complete().asMention
                } catch (e: Exception) { null }

                val display = resolved ?: ":${usage.emojiName}:"
                "$display — ${usage.count}"
            }

            // Build a single embed description with truncation to stay within limits
            val description = buildString {
                var totalChars = 0
                for (line in lines) {
                    val toAdd = if (isNotEmpty()) "\n$line" else line
                    if (totalChars + toAdd.length > 4000) break
                    append(toAdd)
                    totalChars += toAdd.length
                }
            }

            val totalCount = usages.sumOf { it.count.toLong() }

            val embed = EmbedBuilder()
                .setTitle("Emoji Usage")
                .setColor(Color(0xE5, 0x1C, 0x23))
                .setDescription(description)
                .addField("Unique Emojis", usages.size.toString(), true)
                .addField("Total Reactions", totalCount.toString(), true)
                .setFooter("Requested by ${commandEvent.user.asTag}")
                .setTimestamp(OffsetDateTime.now())
                .build()

            val spamChannel = commandEvent.jda.getTextChannelById(botSpamChannelId)

            if (spamChannel == null) {
                commandEvent.reply("Bot spam channel not found.").setEphemeral(true).queue()
                return
            }

            val link = spamChannel.sendMessageEmbeds(embed).complete().jumpUrl
            commandEvent.hook.sendMessage("Sent emoji usage stats to bot spam channel: $link").setEphemeral(true).queue()
        } catch (e: Exception) {
            log.error("Error executing /emojiusage", e)
            commandEvent.hook.sendMessage("Error while retrieving emoji usage: ${e.message}").setEphemeral(true).queue()
        }
    }
}
