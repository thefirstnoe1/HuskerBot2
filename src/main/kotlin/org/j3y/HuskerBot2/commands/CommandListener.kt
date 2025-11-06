package org.j3y.HuskerBot2.commands

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.Command
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Component
class CommandListener : ListenerAdapter() {
    private val log = LoggerFactory.getLogger(CommandListener::class.java)

    private var commands: Map<String, SlashCommand> = emptyMap()
    private var userContextCommands: Map<String, ContextCommand> = emptyMap()
    private var messageContextCommands: Map<String, ContextCommand> = emptyMap()

    private val executor: ExecutorService = Executors.newCachedThreadPool { r ->
        Thread(r, "command-exec-").apply { isDaemon = true }
    }

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

    @PreDestroy
    fun shutdown() {
        executor.shutdown()
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (commands.containsKey(event.name)) {
            val command = commands[event.name] ?: return

            val subcommandIsLogged = command.getSubcommands().find { it.getCommandKey() == event.subcommandName }?.isLogged() ?: true
            if (command.isLogged() && subcommandIsLogged) {
                log.info("{} sent slash command: '{}' (subcommand: '{}') with options: {}", event.user.effectiveName, event.name, event.subcommandName, event.options.map { "${it.name}: ${it.asString}" })
            }

            executor.submit {
                try {
                    command.execute(event)
                } catch (ex: Exception) {
                    log.error("Error executing slash command ${event.name}", ex)
                }
            }
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val parts = event.componentId.split("|")

        if (commands.containsKey(parts[0])) {
            executor.submit {
                try {
                    commands[parts[0]]?.buttonEvent(event)
                } catch (ex: Exception) {
                    log.error("Error handling button interaction ${event.componentId}", ex)
                }
            }
        }
    }

    override fun onUserContextInteraction(event: UserContextInteractionEvent) {
        if (userContextCommands.containsKey(event.name)) {
            executor.submit {
                try {
                    userContextCommands[event.name]?.execute(event)
                } catch (ex: Exception) {
                    log.error("Error executing user context command ${event.name}", ex)
                }
            }
        }
    }

    override fun onMessageContextInteraction(event: MessageContextInteractionEvent) {
        if (messageContextCommands.containsKey(event.name)) {
            executor.submit {
                try {
                    messageContextCommands[event.name]?.execute(event)
                } catch (ex: Exception) {
                    log.error("Error executing message context command ${event.name}", ex)
                }
            }
        }
    }
}