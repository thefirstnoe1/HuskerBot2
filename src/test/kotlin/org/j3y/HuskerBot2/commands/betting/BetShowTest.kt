package org.j3y.HuskerBot2.commands.betting

import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.restaction.MessageEditAction
import net.dv8tion.jda.api.utils.messages.MessageEditData
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import org.j3y.HuskerBot2.model.BetEntity
import org.j3y.HuskerBot2.model.Messages
import org.j3y.HuskerBot2.model.ScheduleEntity
import org.j3y.HuskerBot2.repository.BetRepo
import org.j3y.HuskerBot2.repository.MessageRepo
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

class BetShowTest {

    private lateinit var betShow: BetShow
    private lateinit var scheduleRepo: ScheduleRepo
    private lateinit var betRepo: BetRepo
    private lateinit var messageRepo: MessageRepo

    @BeforeEach
    fun setup() {
        betShow = BetShow()
        scheduleRepo = Mockito.mock(ScheduleRepo::class.java)
        betRepo = Mockito.mock(BetRepo::class.java)
        messageRepo = Mockito.mock(MessageRepo::class.java)
        betShow.scheduleRepo = scheduleRepo
        betShow.betRepo = betRepo
        // Inject private lateinit val messageRepo via reflection since it is private in class under test
        val field = BetShow::class.java.getDeclaredField("messageRepo")
        field.isAccessible = true
        field.set(betShow, messageRepo)
    }

    @Test
    fun `getOptions builds expected week choices`() {
        val season = LocalDate.now().minusMonths(1).year
        val games = listOf(
            ScheduleEntity(id = 1, opponent = "Iowa", season = season, week = 3),
            ScheduleEntity(id = 2, opponent = "Minnesota", season = season, week = 5)
        )
        `when`(scheduleRepo.findAllBySeasonOrderByDateTimeAsc(season)).thenReturn(games)

        val opts: List<OptionData> = betShow.getOptions()

        assertEquals(1, opts.size)
        assertEquals("week", opts[0].name)
        assertEquals(OptionType.INTEGER, opts[0].type)
        val weekChoices: List<Command.Choice> = opts[0].choices
        assertEquals(2, weekChoices.size)
        assertEquals("Iowa - Week 3", weekChoices[0].name)
        assertEquals(3L, weekChoices[0].asLong)
        assertEquals("Minnesota - Week 5", weekChoices[1].name)
        assertEquals(5L, weekChoices[1].asLong)
    }

    @Test
    fun `buildEmbeds returns single embed with no bets`() {
        val season = LocalDate.now().minusMonths(1).year
        val guild = Mockito.mock(Guild::class.java)
        val sched = ScheduleEntity(id = 10, opponent = "Iowa", season = season, week = 1, dateTime = Instant.now())
        `when`(scheduleRepo.findBySeasonAndWeek(season, 1)).thenReturn(sched)
        `when`(betRepo.findBySeasonAndWeek(season, 1)).thenReturn(emptyList())

        val embeds = betShow.buildEmbeds(1, guild)

        assertEquals(1, embeds.size)
        val embed = embeds[0]
        assertEquals("Nebraska vs Iowa (Week 1) Bets", embed.title)
        assertEquals("No bets found for this week.", embed.description)
    }

    @Test
    fun `buildEmbeds returns users and totals with guild name resolution`() {
        val season = LocalDate.now().minusMonths(1).year
        val week = 2
        val sched = ScheduleEntity(id = 20, opponent = "Minnesota", season = season, week = week, dateTime = Instant.now())
        `when`(scheduleRepo.findBySeasonAndWeek(season, week)).thenReturn(sched)

        val bet1 = BetEntity(userId = 100L, season = season, week = week, userTag = "u100#tag", winner = "Nebraska", predictPoints = "Over", predictSpread = "Nebraska")
        val bet2 = BetEntity(userId = 200L, season = season, week = week, userTag = "u200#tag", winner = "Opponent", predictPoints = "Under", predictSpread = "Opponent")
        `when`(betRepo.findBySeasonAndWeek(season, week)).thenReturn(listOf(bet1, bet2))

        val guild = Mockito.mock(Guild::class.java)

        @Suppress("UNCHECKED_CAST")
        val action100 = Mockito.mock(net.dv8tion.jda.api.requests.restaction.CacheRestAction::class.java) as net.dv8tion.jda.api.requests.restaction.CacheRestAction<Member>
        val member100 = Mockito.mock(Member::class.java)
        `when`(member100.effectiveName).thenReturn("GuildNick100")
        `when`(action100.complete()).thenReturn(member100)

        @Suppress("UNCHECKED_CAST")
        val action200 = Mockito.mock(net.dv8tion.jda.api.requests.restaction.CacheRestAction::class.java) as net.dv8tion.jda.api.requests.restaction.CacheRestAction<Member>
        `when`(action200.complete()).thenReturn(null)

        Mockito.`when`(guild.retrieveMember(Mockito.any(UserSnowflake::class.java))).thenAnswer { invocation ->
            val snow = invocation.getArgument<UserSnowflake>(0)
            when (snow?.idLong) {
                100L -> action100
                200L -> action200
                else -> action200
            }
        }

        val embeds = betShow.buildEmbeds(week, guild)

        assertEquals(2, embeds.size)
        val usersEmbed = embeds.first { (it.title ?: "").contains("Bets") }
        val totalsEmbed = embeds.first { (it.title ?: "").contains("Totals") }

        assertEquals("Nebraska vs Minnesota (Week $week) Bets", usersEmbed.title)
        val userFieldNames = usersEmbed.fields.map { it.name }
        assertTrue(userFieldNames.contains("Winner: Nebraska"))
        assertTrue(userFieldNames.contains("Winner: Minnesota"))
        assertTrue(userFieldNames.contains("Over"))
        assertTrue(userFieldNames.contains("Under"))
        assertTrue(userFieldNames.contains("Spread: Nebraska"))
        assertTrue(userFieldNames.contains("Spread: Minnesota"))

        assertEquals("Totals for Nebraska vs Minnesota (Week $week)", totalsEmbed.title)
        val totals = totalsEmbed.fields.associate { it.name to it.value }
        assertEquals("1", totals["Nebraska Win"]) // bet1
        assertEquals("1", totals["Minnesota Win"]) // bet2
        assertEquals("1", totals["Over"]) // bet1
        assertEquals("1", totals["Under"]) // bet2
        assertEquals("1", totals["Nebraska Spread"]) // bet1
        assertEquals("1", totals["Minnesota Spread"]) // bet2
    }

    @Test
    fun `execute replies ephemeral when used outside a guild`() {
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val action = Mockito.mock(ReplyCallbackAction::class.java)
        `when`(event.guild).thenReturn(null)
        `when`(event.reply(Mockito.anyString())).thenReturn(action)
        `when`(action.setEphemeral(true)).thenReturn(action)

        betShow.execute(event)

        Mockito.verify(event).reply("This command must be executed in a server.")
        Mockito.verify(action).setEphemeral(true)
        Mockito.verify(action).queue()
    }

    @Test
    fun `sendBetChannelMessage edits existing message when stored`() {
        val season = LocalDate.now().minusMonths(1).year
        val guild = Mockito.mock(Guild::class.java)
        val channel = Mockito.mock(MessageChannel::class.java)

        // Ensure embeds are non-empty
        val week = 1
        val sched = ScheduleEntity(id = 30, opponent = "Iowa", season = season, week = week, dateTime = Instant.now())
        `when`(scheduleRepo.findBySeasonAndWeek(season, week)).thenReturn(sched)
        `when`(betRepo.findBySeasonAndWeek(season, week)).thenReturn(emptyList())

        // Existing message present
        `when`(messageRepo.findById("huskerbet-bets")).thenReturn(Optional.of(Messages("huskerbet-bets", 999L)))

        // Mock edit action chain
        val editAction = Mockito.mock(MessageEditAction::class.java)
        `when`(channel.editMessageById(Mockito.eq(999L), Mockito.any(MessageEditData::class.java))).thenReturn(editAction)
        Mockito.doNothing().`when`(editAction).queue()

        betShow.sendBetChannelMessage(guild, channel)

        Mockito.verify(channel).editMessageById(Mockito.eq(999L), Mockito.any(MessageEditData::class.java))
        Mockito.verify(editAction).queue()
        Mockito.verify(messageRepo, Mockito.never()).save(Mockito.any())
    }
}
