package org.j3y.HuskerBot2.chat

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SocialEmbedFixerListener : ListenerAdapter() {
    private val log = LoggerFactory.getLogger(SocialEmbedFixerListener::class.java)

    // Regex to roughly match URLs (whitespace-terminated)
    private val urlRegex = Regex("\\bhttps?://\\S+", RegexOption.IGNORE_CASE)

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val message = event.message
        val author = message.author

        // Ignore bots/system/webhooks and empty messages
        if (author.isBot || author.isSystem || message.isWebhookMessage) return
        val raw = message.contentRaw
        if (raw.isBlank()) return

        try {
            // Find all URLs in message
            val matches = urlRegex.findAll(raw).map { it.value }.toList()
            if (matches.isEmpty()) return

            // Build replacements only for problematic services
            val replacements = matches.mapNotNull { url ->
                val lower = url.lowercase()
                when {
                    // Skip if already using better domains
                    lower.contains("fxtwitter.com") || lower.contains("vxtiktok.com") || lower.contains("kkinstagram.com") -> null

                    // Twitter/X -> fxtwitter
                    lower.contains("://twitter.com/") || lower.contains("://www.twitter.com/") ||
                            lower.contains("://x.com/") || lower.contains("://www.x.com/") ->
                        url.replace(Regex("^(https?://)(?:www\\.)?(?:twitter|x)\\.com", RegexOption.IGNORE_CASE),
                            "$1fxtwitter.com")

                    // TikTok -> vxtiktok
                    lower.contains("://tiktok.com/") || lower.contains("://www.tiktok.com/") ->
                        url.replace(Regex("^(https?://)(?:www\\.)?tiktok\\.com", RegexOption.IGNORE_CASE),
                            "$1vxtiktok.com")

                    // Instagram -> kkinstagram
                    lower.contains("://instagram.com/") || lower.contains("://www.instagram.com/") ->
                        url.replace(Regex("^(https?://)(?:www\\.)?instagram\\.com", RegexOption.IGNORE_CASE),
                            "$1kkinstagram.com")

                    else -> null
                }
            }

            if (replacements.isEmpty()) return

            // Suppress embeds on the original message (best-effort)
            message.suppressEmbeds(true).queue({
                // success - no op
            }, { ex ->
                log.debug("Failed to suppress embeds on original message: ${ex.message}")
            })

            // Post rewritten links so Discord re-embeds them nicely
            val response = buildString {
                replacements.distinct().forEach { append(it).append('\n') }
            }

            message.channel.sendMessage(response.trim()).queue({ /* ok */ }, { ex ->
                log.warn("Failed to send re-embed message", ex)
            })
        } catch (e: Exception) {
            log.error("Error in SocialEmbedFixerListener", e)
        }
    }
}