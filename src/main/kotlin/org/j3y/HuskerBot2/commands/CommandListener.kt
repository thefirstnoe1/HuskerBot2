package org.j3y.HuskerBot2.commands

import jakarta.annotation.PostConstruct
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CommandListener : ListenerAdapter() {
    private val log = LoggerFactory.getLogger(CommandListener::class.java)

    private var commands: Map<String, SlashCommand> = emptyMap()

    @Autowired
    private lateinit var allCommands: List<SlashCommand>

    @PostConstruct
    fun init() {
        this.commands = allCommands.associateBy { it.getCommandKey() }
    }


    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (commands.containsKey(event.name)) {
            val command = commands[event.name] ?: return

            val subcommandIsLogged = command.getSubcommands().find { it.getCommandKey() == event.subcommandName }?.isLogged() ?: true
            if (command.isLogged() && subcommandIsLogged) {
                log.info("{} sent slash command: '{}' (subcommand: '{}') with options: {}", event.user.effectiveName, event.name, event.subcommandName, event.options.map { "${it.name}: ${it.asString}" })
            }

            command.execute(event)
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val parts = event.componentId.split("|")

        if (commands.containsKey(parts[0])) {
            commands[parts[0]]?.buttonEvent(event)
        }
    }
}