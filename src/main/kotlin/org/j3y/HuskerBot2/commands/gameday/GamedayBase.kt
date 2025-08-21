package org.j3y.HuskerBot2.commands.gameday

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.j3y.HuskerBot2.commands.SlashCommand
import org.springframework.beans.factory.annotation.Value

open class GamedayBase : SlashCommand() {
    @Value("\${discord.categories.gameday}")lateinit var gamedayCategoryId: String
    @Value("\${discord.categories.general}")lateinit var generalCategoryId: String

    fun setGameday(commandEvent: SlashCommandInteractionEvent, gameDayOn: Boolean) {
        val guild = commandEvent.guild

        if (guild == null) {
            throw RuntimeException("This command must be executed in a server")
        }

        // Fetch gameday channels by category
        val gamedayChannels = guild.getCategoryById(gamedayCategoryId)?.channels ?: emptyList()

        // Fetch general channels by category
        val generalChannels = guild.getCategoryById(generalCategoryId)?.channels ?: emptyList()

        // For every general channel, make everyone unable to send messages
        val everyoneRole = guild.publicRole

        generalChannels.forEach { channel ->
            val permOverride = channel.permissionContainer.upsertPermissionOverride(everyoneRole)
            if (gameDayOn) { permOverride.deny(Permission.MESSAGE_SEND) } else { permOverride.grant(Permission.MESSAGE_SEND) }
            permOverride.queue()
        }

        gamedayChannels.forEach { channel ->
            val permOverride = channel.permissionContainer.upsertPermissionOverride(everyoneRole)
            if (gameDayOn) {
                permOverride.grant(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL)
            } else {
                permOverride.deny(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL)
            }
            permOverride.queue()
        }
    }
}
