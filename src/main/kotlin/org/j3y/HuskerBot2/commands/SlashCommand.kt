package org.j3y.HuskerBot2.commands

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.slf4j.LoggerFactory

open class SlashCommand : ListenerAdapter() {
    private val log = LoggerFactory.getLogger(SlashCommand::class.java)

    open fun getCommandKey(): String = ""
    open fun getDescription(): String = ""
    open fun getOptions(): List<OptionData> = emptyList()

    open fun execute(commandEvent: SlashCommandInteractionEvent) {
        // default no-op
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        execute(event)
    }

    fun sendMessage(channel: MessageChannel, message: String) {
        channel.sendMessage(message).queue()
    }
}
