package org.j3y.HuskerBot2.commands.gameday

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.awt.Color

class GamedayOffTest {

    @Test
    fun `metadata methods return expected values`() {
        val cmd = GamedayOff()
        assertEquals("off", cmd.getCommandKey())
        assertEquals(true, cmd.isSubcommand())
        assertEquals("Turns gameday mode off", cmd.getDescription())
    }

    @Test
    fun `execute replies with success embed when setGameday succeeds`() {
        val cmd = GamedayOff()
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val replyAction = Mockito.mock(ReplyCallbackAction::class.java)
        val guild = Mockito.mock(Guild::class.java)
        val role = Mockito.mock(Role::class.java)

        // Inject required @Value properties to avoid lateinit access exceptions
        cmd.gamedayCategoryId = "123"
        cmd.generalCategoryId = "456"

        // Provide a guild so setGameday does not throw; return null categories to avoid deep permission mocking
        `when`(event.guild).thenReturn(guild)
        `when`(guild.getCategoryById(Mockito.anyString())).thenReturn(null)
        `when`(guild.publicRole).thenReturn(role)

        // Success path replies with embed
        `when`(event.replyEmbeds(Mockito.any(MessageEmbed::class.java))).thenReturn(replyAction)

        cmd.execute(event)

        val embedCaptor = ArgumentCaptor.forClass(MessageEmbed::class.java)
        Mockito.verify(event).replyEmbeds(embedCaptor.capture())
        Mockito.verify(replyAction).queue()

        val embed = embedCaptor.value
        assertEquals("Gameday Mode Disabled!", embed.title)
        assertEquals("Hopefully it was a W. Keep it civil.", embed.description)
        // Color should match exactly Color.RED.rgb (-65536)
        assertEquals(Color.RED.rgb, embed.colorRaw)
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
