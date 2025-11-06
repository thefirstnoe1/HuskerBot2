package org.j3y.HuskerBot2.service

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.UserSnowflake
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Global application cache for mapping Discord userId -> effectiveName.
 */
@Component
class UserNameCache(
    private val jda: JDA
) {
    private val cache = ConcurrentHashMap<Long, String>()

    /**
     * Get the effective name for the given user id, resolving via JDA if missing and caching the result.
     */
    fun getOrResolve(userId: Long): String {
        return cache.computeIfAbsent(userId) { resolveEffectiveName(it) }
    }

    /** Optional manual put (not used currently). */
    fun put(userId: Long, effectiveName: String) {
        cache[userId] = effectiveName
    }

    /** Optional manual eviction (not used currently). */
    fun evict(userId: Long) {
        cache.remove(userId)
    }

    private fun resolveEffectiveName(userId: Long): String {
        // Try cached members first
        jda.guilds.forEach { guild ->
            val member = guild.getMemberById(userId)
            if (member != null) return member.effectiveName
        }
        // Fallback to REST retrieval (first guild that can resolve)
        jda.guilds.forEach { guild ->
            try {
                val member = guild.retrieveMember(UserSnowflake.fromId(userId)).complete()
                if (member != null) return member.effectiveName
            } catch (_: Exception) { /* ignore */ }
        }
        return userId.toString()
    }
}
