package org.j3y.HuskerBot2.chat

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.j3y.HuskerBot2.model.EmojiUsage
import org.j3y.HuskerBot2.repository.EmojiUsageRepo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EmojiReactionListener(
    private val emojiUsageRepo: EmojiUsageRepo
) : ListenerAdapter() {

    private val log = LoggerFactory.getLogger(EmojiReactionListener::class.java)

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        try {
            // We don't care about unicode emojis or bots
            if (event.emoji.type == Emoji.Type.UNICODE) return
            if (event.user?.isBot == true) return

            val emoji = event.reaction.emoji.asCustom()
            val richEmoji = try { event.guild.retrieveEmojiById(emoji.idLong).complete() ?: return
            } catch (e: Exception) { return }

            val emojiSourceGuildId = richEmoji.guild.idLong

            // We don't care about emojis not from this guild
            if (emojiSourceGuildId != event.guild.idLong) return

            val existing = emojiUsageRepo.findByEmojiName(emoji.name)
            if (existing == null) {
                emojiUsageRepo.save(EmojiUsage(emojiId = emoji.idLong, emojiName = emoji.name, imageUrl = emoji.imageUrl, count = 1))
            } else {
                existing.count = (existing.count + 1)
                emojiUsageRepo.save(existing)
            }
        } catch (e: Exception) {
            log.error("Error processing emoji reaction add", e)
        }
    }
}
