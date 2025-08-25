package org.j3y.HuskerBot2.chat

import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.EmojiUnion
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mockito.*
import org.mockito.Mockito.`when`

class HallOfFameListenerTest {
    
    private val hallOfFameChannelId = "487431877792104470"
    private val hallOfShameChannelId = "860686057850798090"
    
    private lateinit var listener: HallOfFameListener
    
    private fun createEventWithReactions(
        messageId: String = "123456789",
        isBot: Boolean = false,
        reactions: List<MessageReaction> = emptyList()
    ): Triple<MessageReactionAddEvent, Message, Guild> {
        val event = mock(MessageReactionAddEvent::class.java)
        val message = mock(Message::class.java, RETURNS_DEEP_STUBS)
        val guild = mock(Guild::class.java)
        val user = mock(User::class.java)
        val messageChannel = mock(MessageChannelUnion::class.java)
        val retrieveAction = mock(RestAction::class.java) as RestAction<Message>
        
        `when`(event.user).thenReturn(user)
        `when`(event.messageId).thenReturn(messageId)
        `when`(event.retrieveMessage()).thenReturn(retrieveAction)
        `when`(retrieveAction.complete()).thenReturn(message)
        
        `when`(user.isBot).thenReturn(isBot)
        `when`(user.asMention).thenReturn("<@123456789>")
        
        `when`(message.guild).thenReturn(guild)
        `when`(message.author).thenReturn(user)
        `when`(message.contentDisplay).thenReturn("Test message content")
        `when`(message.jumpUrl).thenReturn("https://discord.com/channels/123/456/789")
        `when`(message.channel).thenReturn(messageChannel)
        `when`(message.reactions).thenReturn(reactions)
        
        `when`(messageChannel.asMention).thenReturn("<#456789>")
        `when`(guild.iconUrl).thenReturn("https://example.com/icon.png")
        
        return Triple(event, message, guild)
    }
    
    private fun createReaction(emojiName: String, count: Int): MessageReaction {
        val reaction = mock(MessageReaction::class.java)
        val emoji = mock(EmojiUnion::class.java)
        
        `when`(reaction.emoji).thenReturn(emoji)
        `when`(reaction.count).thenReturn(count)
        `when`(emoji.name).thenReturn(emojiName)
        `when`(emoji.formatted).thenReturn(":$emojiName:")
        
        return reaction
    }
    
    @BeforeEach
    fun setUp() {
        listener = HallOfFameListener(hallOfFameChannelId, hallOfShameChannelId)
    }
    
    @Test
    fun `should not process reactions from bots`() {
        val (event, _, _) = createEventWithReactions(isBot = true)
        
        assertDoesNotThrow {
            listener.onMessageReactionAdd(event)
        }
        
        verify(event, never()).retrieveMessage()
    }
    
    @Test
    fun `should forward message to hall of fame when any emoji reaches 10 reactions`() {
        val reaction = createReaction("fire", 10)
        val (event, _, guild) = createEventWithReactions(reactions = listOf(reaction))
        
        val hallOfFameChannel = mock(TextChannel::class.java)
        val sendAction = mock(MessageCreateAction::class.java)
        
        `when`(guild.getTextChannelById(hallOfFameChannelId)).thenReturn(hallOfFameChannel)
        `when`(hallOfFameChannel.sendMessageEmbeds(any<MessageEmbed>())).thenReturn(sendAction)
        `when`(sendAction.queue(any(), any())).then { }
        
        assertDoesNotThrow {
            listener.onMessageReactionAdd(event)
        }
        
        verify(hallOfFameChannel).sendMessageEmbeds(any<MessageEmbed>())
    }
    
    @Test
    fun `should forward message to hall of shame when slowpoke emoji reaches 10 reactions`() {
        val reaction = createReaction("slowpoke", 10)
        val (event, _, guild) = createEventWithReactions(reactions = listOf(reaction))
        
        val hallOfShameChannel = mock(TextChannel::class.java)
        val sendAction = mock(MessageCreateAction::class.java)
        
        `when`(guild.getTextChannelById(hallOfShameChannelId)).thenReturn(hallOfShameChannel)
        `when`(hallOfShameChannel.sendMessageEmbeds(any<MessageEmbed>())).thenReturn(sendAction)
        `when`(sendAction.queue(any(), any())).then { }
        
        assertDoesNotThrow {
            listener.onMessageReactionAdd(event)
        }
        
        verify(hallOfShameChannel).sendMessageEmbeds(any<MessageEmbed>())
    }
    
    @Test
    fun `should not forward message when reaction count is below threshold`() {
        val reaction = createReaction("fire", 5)
        val (event, _, guild) = createEventWithReactions(reactions = listOf(reaction))
        
        val hallOfFameChannel = mock(TextChannel::class.java)
        val hallOfShameChannel = mock(TextChannel::class.java)
        
        `when`(guild.getTextChannelById(hallOfFameChannelId)).thenReturn(hallOfFameChannel)
        `when`(guild.getTextChannelById(hallOfShameChannelId)).thenReturn(hallOfShameChannel)
        
        assertDoesNotThrow {
            listener.onMessageReactionAdd(event)
        }
        
        verify(hallOfFameChannel, never()).sendMessageEmbeds(any<MessageEmbed>())
        verify(hallOfShameChannel, never()).sendMessageEmbeds(any<MessageEmbed>())
    }
    
    @Test
    fun `should handle missing channels gracefully`() {
        val reaction = createReaction("fire", 10)
        val (event, _, guild) = createEventWithReactions(reactions = listOf(reaction))
        
        `when`(guild.getTextChannelById(anyString())).thenReturn(null)
        
        assertDoesNotThrow {
            listener.onMessageReactionAdd(event)
        }
    }
    
    @Test
    fun `should handle exceptions gracefully`() {
        val event = mock(MessageReactionAddEvent::class.java)
        `when`(event.user).thenReturn(mock(User::class.java))
        `when`(event.retrieveMessage()).thenThrow(RuntimeException("Test exception"))
        
        assertDoesNotThrow {
            listener.onMessageReactionAdd(event)
        }
    }
    
    @Test
    fun `should prioritize hall of shame over hall of fame for slowpoke reactions`() {
        val slowpokeReaction = createReaction("slowpoke", 15)
        val fireReaction = createReaction("fire", 12)
        val (event, _, guild) = createEventWithReactions(reactions = listOf(slowpokeReaction, fireReaction))
        
        val hallOfFameChannel = mock(TextChannel::class.java)
        val hallOfShameChannel = mock(TextChannel::class.java)
        val sendAction = mock(MessageCreateAction::class.java)
        
        `when`(guild.getTextChannelById(hallOfFameChannelId)).thenReturn(hallOfFameChannel)
        `when`(guild.getTextChannelById(hallOfShameChannelId)).thenReturn(hallOfShameChannel)
        `when`(hallOfShameChannel.sendMessageEmbeds(any<MessageEmbed>())).thenReturn(sendAction)
        `when`(sendAction.queue(any(), any())).then { }
        
        assertDoesNotThrow {
            listener.onMessageReactionAdd(event)
        }
        
        verify(hallOfShameChannel).sendMessageEmbeds(any<MessageEmbed>())
        verify(hallOfFameChannel, never()).sendMessageEmbeds(any<MessageEmbed>())
    }
}