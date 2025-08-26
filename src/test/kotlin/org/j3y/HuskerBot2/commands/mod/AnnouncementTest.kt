package org.j3y.HuskerBot2.commands.mod

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message.MentionType
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.awt.Color
import java.util.Collection

class AnnouncementTest {

    @Test
    fun `metadata and options and permissions are correct`() {
        val cmd = Announcement()
        assertEquals("announcement", cmd.getCommandKey())
        assertEquals("Send an announcement to the configured announcements channel.", cmd.getDescription())

        val opts: List<OptionData> = cmd.getOptions()
        assertEquals(2, opts.size)
        val byName = opts.associateBy { it.name }
        assertTrue(byName.containsKey("message"))
        assertEquals(OptionType.STRING, byName["message"]?.type)
        assertFalse(byName["message"]?.isRequired ?: true)
        assertTrue(byName.containsKey("text-file"))
        assertEquals(OptionType.STRING, byName["text-file"]?.type)
        assertFalse(byName["text-file"]?.isRequired ?: true)

        val perms: DefaultMemberPermissions = cmd.getPermissions()
        assertNotNull(perms)
    }

    @Test
    fun `replies ephemerally when announcement channel not found`() {
        val cmd = Announcement().apply { announcementsChannelId = "999" }
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val jda = Mockito.mock(JDA::class.java)
        val replyAction = Mockito.mock(ReplyCallbackAction::class.java)

        `when`(event.jda).thenReturn(jda)
        `when`(jda.getTextChannelById("999")).thenReturn(null)
        `when`(event.reply("Announcement channel not found.")).thenReturn(replyAction)
        `when`(replyAction.setEphemeral(true)).thenReturn(replyAction)

        cmd.execute(event)

        Mockito.verify(event).reply("Announcement channel not found.")
        Mockito.verify(replyAction).setEphemeral(true)
        Mockito.verify(replyAction).queue()
    }

    @Test
    fun `happy path sends @ everyone with embed and replies success`() {
        val cmd = Announcement().apply { announcementsChannelId = "chan123" }
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val jda = Mockito.mock(JDA::class.java)
        val channel = Mockito.mock(TextChannel::class.java)
        val replyAction = Mockito.mock(ReplyCallbackAction::class.java)
        @Suppress("UNCHECKED_CAST")
        val msgAction = Mockito.mock(MessageCreateAction::class.java) as MessageCreateAction

        // option mapping
        val opt = Mockito.mock(net.dv8tion.jda.api.interactions.commands.OptionMapping::class.java)
        `when`(opt.asString).thenReturn("Big news!")
        `when`(event.getOption("message")).thenReturn(opt)

        `when`(event.jda).thenReturn(jda)
        `when`(jda.getTextChannelById("chan123")).thenReturn(channel)

        val embedCaptor = ArgumentCaptor.forClass(MessageEmbed::class.java)

        `when`(channel.sendMessage("@everyone")).thenReturn(msgAction)
        `when`(msgAction.setEmbeds(embedCaptor.capture())).thenReturn(msgAction)
        // Allow any collection; we will verify later with captor
        `when`(msgAction.setAllowedMentions(Mockito.anyCollection())).thenReturn(msgAction)

        `when`(event.reply("Announcement sent!")).thenReturn(replyAction)
        `when`(replyAction.setEphemeral(true)).thenReturn(replyAction)

        cmd.execute(event)

        val embed = embedCaptor.value
        assertNotNull(embed)
        assertEquals("Server Announcement", embed.title)
        assertEquals("Big news!", embed.description)
        assertEquals(Color.RED.rgb, embed.colorRaw)

        // Verify flow
        Mockito.verify(channel).sendMessage("@everyone")
        Mockito.verify(msgAction).setEmbeds(Mockito.any(MessageEmbed::class.java))
        // Verify that EVERYONE mentions are explicitly allowed
        Mockito.verify(msgAction).setAllowedMentions(Mockito.argThat { c: java.util.Collection<MentionType> -> c.contains(MentionType.EVERYONE) })

        Mockito.verify(event).reply("Announcement sent!")
        Mockito.verify(replyAction).setEphemeral(true)
        Mockito.verify(replyAction).queue()
    }
}
