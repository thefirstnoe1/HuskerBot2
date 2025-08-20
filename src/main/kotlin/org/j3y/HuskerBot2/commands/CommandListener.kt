package org.j3y.HuskerBot2.commands

import jakarta.annotation.PostConstruct
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
class CommandListener : ListenerAdapter() {

    private var commands: Map<String, SlashCommand> = emptyMap()

    @Autowired
    private lateinit var allCommands: List<SlashCommand>

    @PostConstruct
    fun init() {
        this.commands = allCommands.associateBy { it.getCommandKey() }
    }


    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        println("GOT EVENT " + event.name)
        if (commands.containsKey(event.name)) {
            commands[event.name]?.execute(event)
        }
    }
}