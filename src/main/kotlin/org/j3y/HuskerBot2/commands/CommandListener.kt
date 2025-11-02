package org.j3y.HuskerBot2.commands

import jakarta.annotation.PostConstruct
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.Command
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CommandListener : ListenerAdapter() {
    private val log = LoggerFactory.getLogger(CommandListener::class.java)

    private var commands: Map<String, SlashCommand> = emptyMap()
    private var userContextCommands: Map<String, ContextCommand> = emptyMap()
    private var messageContextCommands: Map<String, ContextCommand> = emptyMap()

    @Autowired
    private lateinit var slashCommands: List<SlashCommand>
    @Autowired
    private lateinit var contextCommands: List<ContextCommand>

    @PostConstruct
    fun init() {
        this.commands = slashCommands.associateBy { it.getCommandKey() }
        this.userContextCommands = contextCommands.filter { it.getCommandType() == Command.Type.USER }.associateBy { it.getCommandMenuText() }
        this.messageContextCommands = contextCommands.filter { it.getCommandType() == Command.Type.MESSAGE }.associateBy { it.getCommandMenuText() }
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

    override fun onUserContextInteraction(event: UserContextInteractionEvent) {
        if (userContextCommands.containsKey(event.name)) {
            userContextCommands[event.name]?.execute(event)
        }
    }

    override fun onMessageContextInteraction(event: MessageContextInteractionEvent) {
        if (messageContextCommands.containsKey(event.name)) {
            messageContextCommands[event.name]?.execute(event)
        }
    }
}