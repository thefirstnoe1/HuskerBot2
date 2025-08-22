package org.j3y.HuskerBot2.commands.other

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import org.j3y.HuskerBot2.service.GoogleGeminiService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class GeminiTest {

    @Test
    fun `metadata methods return expected values`() {
        val service = Mockito.mock(GoogleGeminiService::class.java)
        val cmd = Gemini(service)
        assertEquals("gemini", cmd.getCommandKey())
        assertEquals("Send a text prompt to Google Gemini (free tier)", cmd.getDescription())
        assertEquals(1, cmd.getOptions().size)
        assertEquals("prompt", cmd.getOptions()[0].name)
    }

    @Test
    fun `execute replies with model text on success`() {
        val service = Mockito.mock(GoogleGeminiService::class.java)
        val cmd = Gemini(service)
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val replyAction = Mockito.mock(ReplyCallbackAction::class.java)

        val option = Mockito.mock(net.dv8tion.jda.api.interactions.commands.OptionMapping::class.java)
        `when`(option.asString).thenReturn("Hello, Gemini!")
        `when`(event.getOption("prompt")).thenReturn(option)

        `when`(service.generateText("Hello, Gemini!")).thenReturn("Hi from Gemini")
        `when`(event.reply(Mockito.anyString())).thenReturn(replyAction)

        cmd.execute(event)

        val msgCaptor = ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(event).reply(msgCaptor.capture())
        Mockito.verify(replyAction).queue()

        assertEquals("Hi from Gemini", msgCaptor.value)
    }

    @Test
    fun `execute replies with ephemeral error when exception thrown`() {
        val service = Mockito.mock(GoogleGeminiService::class.java)
        val cmd = Gemini(service)
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val replyAction = Mockito.mock(ReplyCallbackAction::class.java)

        val option = Mockito.mock(net.dv8tion.jda.api.interactions.commands.OptionMapping::class.java)
        `when`(option.asString).thenReturn("Cause error")
        `when`(event.getOption("prompt")).thenReturn(option)

        `when`(service.generateText("Cause error")).thenThrow(RuntimeException("boom"))
        `when`(event.reply(Mockito.anyString())).thenReturn(replyAction)
        `when`(replyAction.setEphemeral(true)).thenReturn(replyAction)

        cmd.execute(event)

        val msgCaptor = ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(event).reply(msgCaptor.capture())
        Mockito.verify(replyAction).setEphemeral(true)
        Mockito.verify(replyAction).queue()

        val msg = msgCaptor.value
        assertEquals(true, msg.startsWith("Error while calling Gemini:"))
    }
}
