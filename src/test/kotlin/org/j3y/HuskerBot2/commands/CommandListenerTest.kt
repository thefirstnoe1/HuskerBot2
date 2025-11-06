package org.j3y.HuskerBot2.commands

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class CommandListenerTest {

    private fun buildListener(
        slash: List<SlashCommand> = emptyList(),
        context: List<ContextCommand> = emptyList()
    ): CommandListener {
        val listener = CommandListener()

        val slashField = CommandListener::class.java.getDeclaredField("slashCommands")
        slashField.isAccessible = true
        slashField.set(listener, slash)

        val ctxField = CommandListener::class.java.getDeclaredField("contextCommands")
        ctxField.isAccessible = true
        ctxField.set(listener, context)

        listener.init()
        return listener
    }

    @Test
    fun `onSlashCommandInteraction dispatches to matching command`() {
        val foo = Mockito.mock(SlashCommand::class.java)
        `when`(foo.getCommandKey()).thenReturn("foo")
        val bar = Mockito.mock(SlashCommand::class.java)
        `when`(bar.getCommandKey()).thenReturn("bar")

        val listener = buildListener(slash = listOf(foo, bar))

        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val user = Mockito.mock(User::class.java)
        `when`(user.effectiveName).thenReturn("Alice")
        `when`(event.user).thenReturn(user)
        `when`(event.name).thenReturn("foo")
        `when`(event.subcommandName).thenReturn(null)
        `when`(event.options).thenReturn(emptyList())

        listener.onSlashCommandInteraction(event)

        Mockito.verify(foo, Mockito.timeout(1000)).execute(event)
        Mockito.verify(bar, Mockito.never()).execute(event)
    }

    @Test
    fun `onSlashCommandInteraction ignores unknown command`() {
        val foo = Mockito.mock(SlashCommand::class.java)
        `when`(foo.getCommandKey()).thenReturn("foo")

        val listener = buildListener(slash = listOf(foo))

        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val user = Mockito.mock(User::class.java)
        `when`(user.effectiveName).thenReturn("Bob")
        `when`(event.user).thenReturn(user)
        `when`(event.name).thenReturn("unknown")
        `when`(event.subcommandName).thenReturn(null)
        `when`(event.options).thenReturn(emptyList())

        listener.onSlashCommandInteraction(event)

        Mockito.verify(foo, Mockito.never()).execute(event)
    }

    @Test
    fun `onButtonInteraction dispatches based on componentId prefix`() {
        val bar = Mockito.mock(SlashCommand::class.java)
        `when`(bar.getCommandKey()).thenReturn("bar")
        val listener = buildListener(slash = listOf(bar))

        val event = Mockito.mock(ButtonInteractionEvent::class.java)
        `when`(event.componentId).thenReturn("bar|123|extra")

        listener.onButtonInteraction(event)

        Mockito.verify(bar, Mockito.timeout(1000)).buttonEvent(event)
    }

    @Test
    fun `onButtonInteraction ignores when no matching command`() {
        val foo = Mockito.mock(SlashCommand::class.java)
        `when`(foo.getCommandKey()).thenReturn("foo")
        val listener = buildListener(slash = listOf(foo))

        val event = Mockito.mock(ButtonInteractionEvent::class.java)
        `when`(event.componentId).thenReturn("nope|42")

        listener.onButtonInteraction(event)

        Mockito.verify(foo, Mockito.never()).buttonEvent(event)
    }

    @Test
    fun `onUserContextInteraction dispatches to matching user context command`() {
        val userCtx = Mockito.mock(ContextCommand::class.java)
        `when`(userCtx.getCommandType()).thenReturn(Command.Type.USER)
        `when`(userCtx.getCommandMenuText()).thenReturn("Quote User")
        val msgCtx = Mockito.mock(ContextCommand::class.java)
        `when`(msgCtx.getCommandType()).thenReturn(Command.Type.MESSAGE)
        `when`(msgCtx.getCommandMenuText()).thenReturn("Quote Message")

        val listener = buildListener(context = listOf(userCtx, msgCtx))

        val event = Mockito.mock(UserContextInteractionEvent::class.java)
        `when`(event.name).thenReturn("Quote User")

        listener.onUserContextInteraction(event)

        Mockito.verify(userCtx, Mockito.timeout(1000)).execute(event)
    }

    @Test
    fun `onUserContextInteraction ignores unknown name`() {
        val userCtx = Mockito.mock(ContextCommand::class.java)
        `when`(userCtx.getCommandType()).thenReturn(Command.Type.USER)
        `when`(userCtx.getCommandMenuText()).thenReturn("Do Thing")

        val listener = buildListener(context = listOf(userCtx))

        val event = Mockito.mock(UserContextInteractionEvent::class.java)
        `when`(event.name).thenReturn("Other Thing")

        listener.onUserContextInteraction(event)

        Mockito.verify(userCtx, Mockito.never()).execute(event)
    }

    @Test
    fun `onMessageContextInteraction dispatches to matching message context command`() {
        val userCtx = Mockito.mock(ContextCommand::class.java)
        `when`(userCtx.getCommandType()).thenReturn(Command.Type.USER)
        `when`(userCtx.getCommandMenuText()).thenReturn("Do User")
        val msgCtx = Mockito.mock(ContextCommand::class.java)
        `when`(msgCtx.getCommandType()).thenReturn(Command.Type.MESSAGE)
        `when`(msgCtx.getCommandMenuText()).thenReturn("Do Message")

        val listener = buildListener(context = listOf(userCtx, msgCtx))

        val event = Mockito.mock(MessageContextInteractionEvent::class.java)
        `when`(event.name).thenReturn("Do Message")

        listener.onMessageContextInteraction(event)

        Mockito.verify(msgCtx, Mockito.timeout(1000)).execute(event)
    }

    @Test
    fun `onMessageContextInteraction ignores unknown name`() {
        val msgCtx = Mockito.mock(ContextCommand::class.java)
        `when`(msgCtx.getCommandType()).thenReturn(Command.Type.MESSAGE)
        `when`(msgCtx.getCommandMenuText()).thenReturn("Do Message")

        val listener = buildListener(context = listOf(msgCtx))

        val event = Mockito.mock(MessageContextInteractionEvent::class.java)
        `when`(event.name).thenReturn("Other")

        listener.onMessageContextInteraction(event)

        Mockito.verify(msgCtx, Mockito.never()).execute(event)
    }
}
