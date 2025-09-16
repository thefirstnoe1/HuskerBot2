package org.j3y.HuskerBot2.commands.gameday

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class GamedayOffTest {

    @Test
    fun `metadata methods return expected values`() {
        val cmd = GamedayOff()
        assertEquals("off", cmd.getCommandKey())
        assertEquals(true, cmd.isSubcommand())
        assertEquals("Turns gameday mode off", cmd.getDescription())
    }

    @Test
    fun `execute sends embed to general channel and replies ephemeral when setGameday succeeds`() {
        val cmd = GamedayOff()
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val replyAction = Mockito.mock(ReplyCallbackAction::class.java)
        val guild = Mockito.mock(Guild::class.java)
        val role = Mockito.mock(Role::class.java)
        val textChannel = Mockito.mock(TextChannel::class.java)
        val messageAction = Mockito.mock(MessageCreateAction::class.java)

        // Inject required @Value properties to avoid lateinit access exceptions
        cmd.gamedayCategoryId = "123"
        cmd.generalCategoryId = "456"
        cmd.generalChannelId = "789"

        // Provide a guild so setGameday does not throw; return null categories to avoid deep permission mocking
        `when`(event.guild).thenReturn(guild)
        `when`(guild.getCategoryById(Mockito.anyString())).thenReturn(null)
        `when`(guild.publicRole).thenReturn(role)
        `when`(guild.getTextChannelById("789")).thenReturn(textChannel)

        // Mock sending embed to channel
        `when`(textChannel.sendMessageEmbeds(Mockito.any(MessageEmbed::class.java))).thenReturn(messageAction)
        `when`(messageAction.queue()).then { }

        // Mock ephemeral reply after sending
        `when`(event.reply(Mockito.anyString())).thenReturn(replyAction)
        `when`(replyAction.setEphemeral(true)).thenReturn(replyAction)

        cmd.execute(event)

        val embedCaptor = ArgumentCaptor.forClass(MessageEmbed::class.java)
        Mockito.verify(textChannel).sendMessageEmbeds(embedCaptor.capture())
        Mockito.verify(messageAction).queue()

        // Verify the ephemeral confirmation reply
        val msgCaptor = ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(event).reply(msgCaptor.capture())
        assertEquals("Gameday mode disabled.", msgCaptor.value)
        Mockito.verify(replyAction).setEphemeral(true)
        Mockito.verify(replyAction).queue()

        val embed = embedCaptor.value
        assertEquals("Gameday Mode Disabled!", embed.title)
        assertEquals("Hopefully it was a W. Keep it civil.", embed.description)
    }

    @Test
    fun `execute replies with ephemeral error when setGameday throws`() {
        val cmd = GamedayOff()
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val replyAction = Mockito.mock(ReplyCallbackAction::class.java)

        // Null guild triggers RuntimeException in setGameday
        `when`(event.guild).thenReturn(null)

        // Chain: reply(String) -> setEphemeral(true) -> queue()
        `when`(event.reply(Mockito.anyString())).thenReturn(replyAction)
        `when`(replyAction.setEphemeral(true)).thenReturn(replyAction)

        cmd.execute(event)

        val msgCaptor = ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(event).reply(msgCaptor.capture())
        Mockito.verify(replyAction).setEphemeral(true)
        Mockito.verify(replyAction).queue()

        val msg = msgCaptor.value
        // Message should start with the error preface used in GamedayOff
        assertEquals(true, msg.startsWith("Error while trying to disable gameday mode:"))
    }
}
