package org.j3y.HuskerBot2.chat

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.Instant

@Component
class MemberActivityListener : ListenerAdapter() {

    private val log = LoggerFactory.getLogger(MemberActivityListener::class.java)

    @Value("\${discord.channels.general}")
    lateinit var generalChannelId: String

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        try {
            val user = event.user
            if (user.isBot || user.isSystem) return

            val channel = event.jda.getTextChannelById(generalChannelId)
            if (channel == null) {
                log.warn("General channel not found for id={} on guild {}", generalChannelId, event.guild.id)
                return
            }

            val avatarUrl = user.effectiveAvatarUrl
            val embed = EmbedBuilder()
                .setColor(Color(0x57F287)) // Discord blurple green-ish for success
                .setTitle("Member Joined")
                .setDescription(":inbox_tray: ${user.asMention} joined the server. Welcome!")
                .setThumbnail(avatarUrl)
                .setTimestamp(Instant.now())
                .setFooter("${event.guild.name} • ${event.guild.memberCount} members")
                .build()

            channel.sendMessageEmbeds(embed).queue({ /* ok */ }, { ex ->
                log.warn("Failed to send member join embed", ex)
            })
        } catch (e: Exception) {
            log.error("Error handling GuildMemberJoinEvent", e)
        }
    }

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        try {
            val user = event.user
            if (user.isBot || user.isSystem) return

            val channel = event.jda.getTextChannelById(generalChannelId)
            if (channel == null) {
                log.warn("General channel not found for id={} on guild {}", generalChannelId, event.guild.id)
                return
            }

            val avatarUrl = user.effectiveAvatarUrl
            val embed = EmbedBuilder()
                .setColor(Color(0xED4245)) // Discord red for leave
                .setTitle("Member Left")
                .setDescription(":outbox_tray: ${user.asMention} left the server.")
                .setThumbnail(avatarUrl)
                .setTimestamp(Instant.now())
                .setFooter("${event.guild.name} • ${event.guild.memberCount} members")
                .build()

            channel.sendMessageEmbeds(embed).queue({ /* ok */ }, { ex ->
                log.warn("Failed to send member leave embed", ex)
            })
        } catch (e: Exception) {
            log.error("Error handling GuildMemberRemoveEvent", e)
        }
    }
}
