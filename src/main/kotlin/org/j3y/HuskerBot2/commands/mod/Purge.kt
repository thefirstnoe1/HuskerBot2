package org.j3y.HuskerBot2.commands.mod

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class Purge : SlashCommand() {
    override fun getCommandKey(): String = "purge"
    override fun getDescription(): String = "Delete the last X messages in this channel. Optionally only delete bot messages (default: true)."
    override fun getPermissions(): DefaultMemberPermissions = DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)

    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.INTEGER, "amount", "Number of messages to delete (1-100)", true)
            .setRequiredRange(1, 100),
        OptionData(OptionType.BOOLEAN, "bot-only", "If true, only delete messages sent by this bot", false)
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        val ch = commandEvent.channel
        if (ch !is TextChannel) {
            commandEvent.reply("This command can only be used in text channels.").setEphemeral(true).queue()
            return
        }
        val channel: TextChannel = ch

        // Permission gate at runtime too (in case command perms out of sync)
        val member = commandEvent.member
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            commandEvent.reply("You must be an administrator to use this command.").setEphemeral(true).queue()
            return
        }

        val amount = (commandEvent.getOption("amount")?.asLong ?: 0L).toInt()
        val botOnly = commandEvent.getOption("bot-only")?.asBoolean ?: true

        if (amount <= 0) {
            commandEvent.reply("Amount must be at least 1.").setEphemeral(true).queue()
            return
        }

        commandEvent.deferReply(true).queue()

        val selfUserId = commandEvent.jda.selfUser.idLong
        try {
            // Retrieve up to 'amount' recent messages
            var toDelete: MutableList<Message> = mutableListOf()
            var remaining = amount

            // JDA can retrieve history in batches; start from most recent
            var beforeId: String? = null
            while (toDelete.size < amount) {
                val batch = if (beforeId == null) channel.history.retrievePast(minOf(remaining, 100)).complete()
                else channel.getHistoryBefore(beforeId, minOf(remaining, 100)).complete().retrievedHistory
                if (batch.isEmpty()) break
                val filtered = if (botOnly) batch.filter { m: Message -> m.author.idLong == selfUserId } else batch
                toDelete.addAll(filtered)
                if (toDelete.size >= amount) break
                remaining = amount - toDelete.size
                beforeId = batch.last().id
            }

            // Limit to requested amount
            if (toDelete.size > amount) {
                toDelete = toDelete.subList(0, amount)
            }

            if (toDelete.isEmpty()) {
                commandEvent.hook.sendMessage("No messages matched the criteria.").queue()
                return
            }

            // Split into <14 days (bulk deletable) and older (cannot be bulk deleted)
            val twoWeeksAgo = OffsetDateTime.now().minusDays(14)
            val recent = toDelete.filter { it.timeCreated.isAfter(twoWeeksAgo) }
            val old = toDelete.filter { it.timeCreated.isBefore(twoWeeksAgo) }

            var deletedCount = 0

            // Bulk delete supports 2..100 messages at once; handle 1 separately
            if (recent.isNotEmpty()) {
                val chunks = recent.chunked(100)
                for (chunk in chunks) {
                    if (chunk.size == 1) {
                        // Single delete
                        channel.deleteMessageById(chunk[0].id).complete()
                        deletedCount += 1
                    } else {
                        channel.deleteMessages(chunk).complete()
                        deletedCount += chunk.size
                    }
                }
            }

            // For old messages, attempt individual delete (Discord forbids bulk delete for >14 days; individual delete may still fail by permissions)
            for (msg in old) {
                try {
                    channel.deleteMessageById(msg.id).complete()
                    deletedCount += 1
                } catch (_: Throwable) {
                    // Ignore failures for old messages
                }
            }

            val scopeText = if (botOnly) "bot messages" else "messages"
            commandEvent.hook.sendMessage("Deleted $deletedCount $scopeText in ${channel.asMention}.").queue()
        } catch (e: Throwable) {
            e.printStackTrace()
            commandEvent.hook.sendMessage("Failed to purge messages: ${e.message}").queue()
        }
    }
}
