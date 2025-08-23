package org.j3y.HuskerBot2.commands.betting

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.entities.MessageEmbed
import org.j3y.HuskerBot2.model.BetEntity
import org.j3y.HuskerBot2.model.ScheduleEntity
import org.j3y.HuskerBot2.repository.BetRepo
import org.j3y.HuskerBot2.repository.ScheduleRepo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.LocalDate
import java.util.*

class BetCreateTest {

    private lateinit var betCreate: BetCreate
    private lateinit var scheduleRepo: ScheduleRepo
    private lateinit var betRepo: BetRepo

    @BeforeEach
    fun setup() {
        betCreate = BetCreate()
        scheduleRepo = Mockito.mock(ScheduleRepo::class.java)
        betRepo = Mockito.mock(BetRepo::class.java)
        betCreate.scheduleRepo = scheduleRepo
        betCreate.betRepo = betRepo
    }

    @Test
    fun `getOptions builds expected choices and options`() {
        val season = LocalDate.now().year
        val games = listOf(
            ScheduleEntity(id = 1, opponent = "Iowa", season = season, week = 3),
            ScheduleEntity(id = 2, opponent = "Minnesota", season = season, week = 5)
        )
        `when`(scheduleRepo.findAllBySeasonOrderByDateTimeAsc(season)).thenReturn(games)

        val opts: List<OptionData> = betCreate.getOptions()

        // Verify there are 4 options with expected names/types
        assertEquals(4, opts.size)
        assertEquals("week", opts[0].name)
        assertEquals(OptionType.INTEGER, opts[0].type)
        val weekChoices: List<Command.Choice> = opts[0].choices
        assertEquals(2, weekChoices.size)
        assertEquals("Iowa - Week 3", weekChoices[0].name)
        assertEquals(3L, weekChoices[0].asLong)
        assertEquals("Minnesota - Week 5", weekChoices[1].name)
        assertEquals(5L, weekChoices[1].asLong)

        assertEquals("winner", opts[1].name)
        assertEquals(OptionType.STRING, opts[1].type)
        assertEquals(listOf("Nebraska", "Opponent"), opts[1].choices.map { it.name })

        assertEquals("predict-points", opts[2].name)
        assertEquals(listOf("Over", "Under"), opts[2].choices.map { it.name })

        assertEquals("predict-spread", opts[3].name)
        assertEquals(listOf("Nebraska", "Opponent"), opts[3].choices.map { it.name })
    }

    @Test
    fun `execute replies ephemeral when schedule not found`() {
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val replyAction = Mockito.mock(ReplyCallbackAction::class.java)

        `when`(event.reply(Mockito.anyString())).thenReturn(replyAction)
        `when`(replyAction.setEphemeral(true)).thenReturn(replyAction)

        val season = LocalDate.now().year
        // default week is 1 when option is missing
        `when`(scheduleRepo.findBySeasonAndWeek(season, 1)).thenReturn(null)

        // User
        val user = Mockito.mock(User::class.java)
        `when`(user.idLong).thenReturn(123L)
        `when`(user.asTag).thenReturn("user#0001")
        `when`(user.effectiveName).thenReturn("UserName")
        `when`(event.getUser()).thenReturn(user)

        betCreate.execute(event)

        Mockito.verify(event).reply("Unable to find scheduled game for $season - 1.")
        Mockito.verify(replyAction).setEphemeral(true)
        Mockito.verify(replyAction).queue()
        Mockito.verifyNoMoreInteractions(betRepo)
    }

    @Test
    fun `execute replies ephemeral when bet attempted within 1 hour of game`() {
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val replyAction = Mockito.mock(ReplyCallbackAction::class.java)

        `when`(event.reply(Mockito.anyString())).thenReturn(replyAction)
        `when`(replyAction.setEphemeral(true)).thenReturn(replyAction)

        val season = LocalDate.now().year
        val sched = ScheduleEntity(id = 10, opponent = "Iowa", opponentLogo = "http://img", season = season, week = 1, dateTime = Instant.now().plusSeconds(30 * 60))
        `when`(scheduleRepo.findBySeasonAndWeek(season, 1)).thenReturn(sched)

        val user = Mockito.mock(User::class.java)
        `when`(user.idLong).thenReturn(42L)
        `when`(user.asTag).thenReturn("user#0042")
        `when`(user.effectiveName).thenReturn("User")
        `when`(event.getUser()).thenReturn(user)

        betCreate.execute(event)

        Mockito.verify(event).reply("You can not set a bet less than an hour before game time.")
        Mockito.verify(replyAction).setEphemeral(true)
        Mockito.verify(replyAction).queue()
        Mockito.verifyNoMoreInteractions(betRepo)
    }

    @Test
    fun `execute creates new bet and replies with submitted embed`() {
        val season = LocalDate.now().year
        val week = 6
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val replyEmbedAction = Mockito.mock(ReplyCallbackAction::class.java)


        val user = Mockito.mock(User::class.java)
        `when`(user.idLong).thenReturn(111L)
        `when`(user.asTag).thenReturn("user#0111")
        `when`(user.effectiveName).thenReturn("User111")
        `when`(event.getUser()).thenReturn(user)
        // No member, force using user.effectiveName
        `when`(event.getMember()).thenReturn(null)

        `when`(event.replyEmbeds(Mockito.any(MessageEmbed::class.java))).thenReturn(replyEmbedAction)
        `when`(replyEmbedAction.setEphemeral(true)).thenReturn(replyEmbedAction)

        // Default week will be 1 because options are missing
        val sched = ScheduleEntity(id = 55, opponent = "Iowa", opponentLogo = "http://img", season = season, week = 1, dateTime = Instant.now().plusSeconds(3 * 60 * 60))
        `when`(scheduleRepo.findBySeasonAndWeek(season, 1)).thenReturn(sched)
        `when`(betRepo.findByUserIdAndSeasonAndWeek(111L, season, 1)).thenReturn(Optional.empty())

        betCreate.execute(event)

        // Verify save with correct values
        val captor = ArgumentCaptor.forClass(BetEntity::class.java)
        Mockito.verify(betRepo).save(captor.capture())
        val saved = captor.value
        assertEquals(111L, saved.userId)
        assertEquals(season, saved.season)
        assertEquals(1, saved.week)
        assertEquals("Nebraska", saved.winner)
        assertEquals("Over", saved.predictPoints)
        assertEquals("Nebraska", saved.predictSpread)
        assertEquals("user#0111", saved.userTag)

        // Capture embed reply and verify description contains 'submitted'
        val embedCaptor = ArgumentCaptor.forClass(MessageEmbed::class.java)
        Mockito.verify(event).replyEmbeds(embedCaptor.capture())
        val embed = embedCaptor.value
        assertEquals("Submitted Bet", embed.title)
        assertTrue(embed.description?.contains("submitted") == true)
        // Verify key fields are present
        val fields = embed.fields.associate { it.name to it.value }
        assertEquals("Iowa", fields["Opponent"])
        assertEquals("1", fields["Week"])
        assertEquals("Nebraska", fields["Winner"])
        assertEquals("Over", fields["Over/Under"])
        assertEquals("Nebraska", fields["Spread"])
        assertEquals("http://img", embed.thumbnail?.url)

        Mockito.verify(replyEmbedAction).queue()
    }

    @Test
    fun `execute updates existing bet and replies with updated embed`() {
        val season = LocalDate.now().year
        val week = 8
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val replyEmbedAction = Mockito.mock(ReplyCallbackAction::class.java)


        val user = Mockito.mock(User::class.java)
        `when`(user.idLong).thenReturn(222L)
        `when`(user.asTag).thenReturn("user#0222")
        `when`(user.effectiveName).thenReturn("User222")
        `when`(event.getUser()).thenReturn(user)

        val member = Mockito.mock(Member::class.java)
        `when`(member.effectiveName).thenReturn("GuildNick")
        `when`(event.getMember()).thenReturn(member)

        `when`(event.replyEmbeds(Mockito.any(MessageEmbed::class.java))).thenReturn(replyEmbedAction)
        `when`(replyEmbedAction.setEphemeral(true)).thenReturn(replyEmbedAction)

        val sched = ScheduleEntity(id = 66, opponent = "Minnesota", opponentLogo = "http://logo2", season = season, week = 1, dateTime = Instant.now().plusSeconds(4 * 60 * 60))
        `when`(scheduleRepo.findBySeasonAndWeek(season, 1)).thenReturn(sched)

        val existing = BetEntity(userId = 222L, season = season, week = 1, userTag = "old#tag", winner = "Opponent", predictPoints = "Under", predictSpread = "Opponent")
        `when`(betRepo.findByUserIdAndSeasonAndWeek(222L, season, 1)).thenReturn(Optional.of(existing))

        betCreate.execute(event)

        val captor = ArgumentCaptor.forClass(BetEntity::class.java)
        Mockito.verify(betRepo).save(captor.capture())
        val saved = captor.value
        assertSame(existing, saved) // updated in place
        // Defaults are used because options are missing
        assertEquals("Nebraska", saved.winner)
        assertEquals("Over", saved.predictPoints)
        assertEquals("Nebraska", saved.predictSpread)
        assertEquals("user#0222", saved.userTag)

        val embedCaptor = ArgumentCaptor.forClass(MessageEmbed::class.java)
        Mockito.verify(event).replyEmbeds(embedCaptor.capture())
        val embed = embedCaptor.value
        assertEquals("Submitted Bet", embed.title)
        assertTrue(embed.description?.contains("updated") == true)
        val fields = embed.fields.associate { it.name to it.value }
        assertEquals("Minnesota", fields["Opponent"])
        assertEquals("1", fields["Week"])
        assertEquals("Nebraska", fields["Winner"])
        assertEquals("Over", fields["Over/Under"])
        assertEquals("Nebraska", fields["Spread"])
        assertEquals("http://logo2", embed.thumbnail?.url)

        Mockito.verify(replyEmbedAction).queue()
    }

}
