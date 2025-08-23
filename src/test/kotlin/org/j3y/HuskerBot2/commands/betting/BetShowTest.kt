package org.j3y.HuskerBot2.commands.betting

import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
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

class BetShowTest {

    private lateinit var betShow: BetShow
    private lateinit var scheduleRepo: ScheduleRepo
    private lateinit var betRepo: BetRepo

    @BeforeEach
    fun setup() {
        betShow = BetShow()
        scheduleRepo = Mockito.mock(ScheduleRepo::class.java)
        betRepo = Mockito.mock(BetRepo::class.java)
        betShow.scheduleRepo = scheduleRepo
        betShow.betRepo = betRepo
    }

    @Test
    fun `getOptions builds expected week choices`() {
        val season = LocalDate.now().year
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
    fun `execute replies with single embed when no bets found`() {
        val season = LocalDate.now().year
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val replyAction = Mockito.mock(ReplyCallbackAction::class.java)

        // schedule used for title only
        val sched = ScheduleEntity(id = 10, opponent = "Iowa", season = season, week = 1, dateTime = Instant.now())
        `when`(scheduleRepo.findBySeasonAndWeek(season, 1)).thenReturn(sched)

        `when`(betRepo.findBySeasonAndWeek(season, 1)).thenReturn(emptyList())
        `when`(event.replyEmbeds(Mockito.any(MessageEmbed::class.java))).thenReturn(replyAction)

        betShow.execute(event)

        val embedCaptor = ArgumentCaptor.forClass(MessageEmbed::class.java)
        Mockito.verify(event).replyEmbeds(embedCaptor.capture())
        Mockito.verify(replyAction).queue()

        val embed = embedCaptor.value
        assertEquals("Nebraska vs Iowa (Week 1) Bets", embed.title)
        assertEquals("No bets found for this week.", embed.description)
    }

    @Test
    fun `execute builds per-user and totals embeds with guild name resolution`() {
        val season = LocalDate.now().year
        val week = 2
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val replyAction = Mockito.mock(ReplyCallbackAction::class.java)

        val sched = ScheduleEntity(id = 20, opponent = "Minnesota", season = season, week = week, dateTime = Instant.now())
        `when`(scheduleRepo.findBySeasonAndWeek(season, week)).thenReturn(sched)

        // Provide an explicit option for week via default (null -> 1), but we want week=2.
        // We'll stub getOption("week").asInt to 2 by returning an OptionMapping-like behavior isn't easy.
        // Simpler: We'll set up findBySeasonAndWeek and also ensure internal week variable resolves to 1 by default.
        // To truly test week=2, mimic getOption("week") returning an object whose asInt is 2.
        @Suppress("UNCHECKED_CAST")
        val option = Mockito.mock(net.dv8tion.jda.api.interactions.commands.OptionMapping::class.java)
        `when`(option.asInt).thenReturn(week)
        `when`(event.getOption("week")).thenReturn(option)

        // Prepare two bets; ensure keys match maps in the code
        val bet1 = BetEntity(userId = 100L, season = season, week = week, userTag = "u100#tag", winner = "Nebraska", predictPoints = "Over", predictSpread = "Nebraska")
        val bet2 = BetEntity(userId = 200L, season = season, week = week, userTag = "u200#tag", winner = "Opponent", predictPoints = "Under", predictSpread = "Opponent")
        `when`(betRepo.findBySeasonAndWeek(season, week)).thenReturn(listOf(bet1, bet2))

        // Mock guild and member lookup: for user 100, resolve to guild nickname; for 200, return null so fallback to userTag
        val guild = Mockito.mock(Guild::class.java)
        `when`(event.guild).thenReturn(guild)

        @Suppress("UNCHECKED_CAST")
        val action100 = Mockito.mock(net.dv8tion.jda.api.requests.restaction.CacheRestAction::class.java) as net.dv8tion.jda.api.requests.restaction.CacheRestAction<Member>
        val member100 = Mockito.mock(Member::class.java)
        `when`(member100.effectiveName).thenReturn("GuildNick100")
        `when`(action100.complete()).thenReturn(member100)

        @Suppress("UNCHECKED_CAST")
        val action200 = Mockito.mock(net.dv8tion.jda.api.requests.restaction.CacheRestAction::class.java) as net.dv8tion.jda.api.requests.restaction.CacheRestAction<Member>
        // Return null to trigger fallback to userTag
        `when`(action200.complete()).thenReturn(null)

        // Route calls based on user id argument using thenAnswer to avoid null matcher issues
        Mockito.`when`(guild.retrieveMember(Mockito.any(UserSnowflake::class.java))).thenAnswer { invocation ->
            val snow = invocation.getArgument<UserSnowflake>(0)
            when (snow?.idLong) {
                100L -> action100
                200L -> action200
                else -> action200
            }
        }

        // replyEmbeds with two embeds
        `when`(event.replyEmbeds(Mockito.any(MessageEmbed::class.java), Mockito.any(MessageEmbed::class.java))).thenReturn(replyAction)

        betShow.execute(event)

        // Capture both embeds
        val embedCaptor1 = ArgumentCaptor.forClass(MessageEmbed::class.java)
        val embedCaptor2 = ArgumentCaptor.forClass(MessageEmbed::class.java)
        Mockito.verify(event).replyEmbeds(embedCaptor1.capture(), embedCaptor2.capture())
        Mockito.verify(replyAction).queue()

        // Identify which is users vs totals by title
        val e1 = embedCaptor1.value
        val e2 = embedCaptor2.value
        val usersEmbed = listOf(e1, e2).first { (it.title ?: "").contains("Bets") }
        val totalsEmbed = listOf(e1, e2).first { (it.title ?: "").contains("Totals") }

        // Users embed assertions
        assertEquals("Nebraska vs Minnesota (Week $week) Bets", usersEmbed.title)
        // Verify expected field names exist (aggregate lists), values may be blank or aggregated
        val userFieldNames = usersEmbed.fields.map { it.name }
        assertTrue(userFieldNames.contains("Winner: Nebraska"))
        assertTrue(userFieldNames.contains("Winner: Minnesota"))
        assertTrue(userFieldNames.contains("Over"))
        assertTrue(userFieldNames.contains("Under"))
        assertTrue(userFieldNames.contains("Spread: Nebraska"))
        assertTrue(userFieldNames.contains("Spread: Minnesota"))

        // Totals embed assertions
        assertEquals("Totals for Nebraska vs Minnesota (Week $week)", totalsEmbed.title)
        val totals = totalsEmbed.fields.associate { it.name to it.value }
        assertEquals("1", totals["Nebraska Win"]) // bet1
        assertEquals("1", totals["Minnesota Win"]) // bet2
        assertEquals("1", totals["Over"]) // bet1
        assertEquals("1", totals["Under"]) // bet2
        assertEquals("1", totals["Nebraska Spread"]) // bet1
        assertEquals("1", totals["Minnesota Spread"]) // bet2
    }
}
