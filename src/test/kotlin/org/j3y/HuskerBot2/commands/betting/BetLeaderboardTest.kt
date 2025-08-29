package org.j3y.HuskerBot2.commands.betting

import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import org.j3y.HuskerBot2.model.BetEntity
import org.j3y.HuskerBot2.repository.BetRepo
import org.j3y.HuskerBot2.repository.ScheduleRepo
import org.j3y.HuskerBot2.service.BetLeaderboardService
import org.j3y.HuskerBot2.util.SeasonResolver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class BetLeaderboardTest {

    private lateinit var leaderboard: BetLeaderboard
    private lateinit var betRepo: BetRepo
    private lateinit var scheduleRepo: ScheduleRepo

    @BeforeEach
    fun setup() {
        leaderboard = BetLeaderboard()
        betRepo = Mockito.mock(BetRepo::class.java)
        scheduleRepo = Mockito.mock(ScheduleRepo::class.java)
        leaderboard.betRepo = betRepo
        leaderboard.scheduleRepo = scheduleRepo
        leaderboard.leaderboardService = BetLeaderboardService()
    }

    @Test
    fun `execute replies with message when no bets found for season`() {
        val season = SeasonResolver.currentCfbSeason()
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val replyAction = Mockito.mock(ReplyCallbackAction::class.java)

        `when`(betRepo.findBySeason(season)).thenReturn(emptyList())
        `when`(event.reply(Mockito.anyString())).thenReturn(replyAction)

        leaderboard.execute(event)

        Mockito.verify(event).reply("No bets found for the ${season} season.")
        Mockito.verify(replyAction).queue()
        Mockito.verifyNoMoreInteractions(event)
    }

    @Test
    fun `execute builds embed with aggregated scores sorted and medalized`() {
        val season = SeasonResolver.currentCfbSeason()
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val replyEmbedAction = Mockito.mock(ReplyCallbackAction::class.java)

        // Null guild so that display name falls back to userTag
        `when`(event.guild).thenReturn(null)
        `when`(event.replyEmbeds(Mockito.any(MessageEmbed::class.java))).thenReturn(replyEmbedAction)

        val bets = listOf(
            // user 1: winner + spread + points = 1 + 2 + 2 = 5
            BetEntity(userId = 1L, season = season, week = 1, userTag = "u1#tag", correctWinner = true, correctSpread = true, correctPoints = true),
            // user 2: two bets, only winners true twice (2 points total)
            BetEntity(userId = 2L, season = season, week = 1, userTag = "u2#tag", correctWinner = true),
            BetEntity(userId = 2L, season = season, week = 2, userTag = "u2#tag", correctWinner = true),
            // user 3: spread only (2 pts) but userTag initially blank then updated on later bet
            BetEntity(userId = 3L, season = season, week = 1, userTag = "", correctSpread = true),
            BetEntity(userId = 3L, season = season, week = 3, userTag = "u3#good", correctSpread = true), // +2 more points => total 4
            // user 4: points only (2 pts)
            BetEntity(userId = 4L, season = season, week = 1, userTag = "u4#tag", correctPoints = true)
        )

        `when`(betRepo.findBySeason(season)).thenReturn(bets)

        leaderboard.execute(event)

        val embedCaptor = ArgumentCaptor.forClass(MessageEmbed::class.java)
        Mockito.verify(event).replyEmbeds(embedCaptor.capture())
        Mockito.verify(replyEmbedAction).queue()

        val embed = embedCaptor.value
        assertEquals("🏆 Husker Betting Leaderboard — $season Season", embed.title)
        assertTrue(embed.description?.contains("Scoring: Winner = 1, Spread = 2, Over/Under = 2") == true)

        // One field named "Leaderboard" should contain up to 20 entries; here we have 4 users
        assertEquals(1, embed.fields.size)
        assertEquals("Leaderboard", embed.fields[0].name)
        val content = embed.fields[0].value ?: ""

        // Expected totals:
        // u1: 5 pts -> 🥇
        // u3: 4 pts -> 🥈 and display name should be updated to u3#good (not blank or userId)
        // u2: 2 pts -> 🥉
        // u4: 2 pts -> same points as u2, sorted by userTag alphabetical => u2#tag before u4#tag

        val lines = content.lines()
        assertEquals(4, lines.size)

        // Verify medal order and summaries
        assertTrue(lines[0].startsWith("🥇 "))
        assertTrue(lines[1].startsWith("🥈 "))
        // Ties are ranked with standard competition ranking; users with same points share the same rank.
        // For this dataset, lines[2] and lines[3] should both be rank 3 (🥉) rather than having a "4." line.
        assertTrue(lines[2].startsWith("🥉 "))
        assertTrue(lines[3].startsWith("🥉 "))

        // Check that user 1 line contains points and breakdown
        assertTrue(lines[0].contains("— 5 pts"))
        assertTrue(lines[0].contains("W 1 • S 1 • O/U 1"))

        // user 3 should have updated tag and 4 pts with 0W 2S 0P
        assertTrue(lines.any { it.contains("u3#good") && it.contains("— 4 pts") && it.contains("W 0 • S 2 • O/U 0") })

        // users 2 and 4 both 2 pts, order by tag => u2 then u4
        val idx2 = lines.indexOfFirst { it.contains("u2#tag") }
        val idx4 = lines.indexOfFirst { it.contains("u4#tag") }
        assertTrue(idx2 in 0..3 && idx4 in 0..3 && idx2 < idx4)
    }

    @Test
    fun `execute chunks leaderboard into multiple fields when more than 20 users`() {
        val season = SeasonResolver.currentCfbSeason()
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val replyEmbedAction = Mockito.mock(ReplyCallbackAction::class.java)

        `when`(event.guild).thenReturn(null)
        `when`(event.replyEmbeds(Mockito.any(MessageEmbed::class.java))).thenReturn(replyEmbedAction)

        // Create 25 users, descending points to have a deterministic order
        val bets = mutableListOf<BetEntity>()
        var pointsVal = 25
        for (u in 1L..25L) {
            // represent pointsVal by repeated combinations: winner = 1 point, spread/points add 2 each
            // We'll make points by using: one winner if needed, and then as many spreads as possible
            var remaining = pointsVal
            var w = false
            var sCount = 0
            var pCount = 0
            if (remaining % 2 == 1) { w = true; remaining -= 1 }
            sCount = remaining / 2
            repeat(sCount) { bets.add(BetEntity(userId = u, season = season, week = it + 1, userTag = "u$u#tag", correctSpread = true)) }
            if (w) bets.add(BetEntity(userId = u, season = season, week = 50, userTag = "u$u#tag", correctWinner = true))
            // reset for next user
            pointsVal -= 1
        }

        `when`(betRepo.findBySeason(season)).thenReturn(bets)

        leaderboard.execute(event)

        val embedCaptor = ArgumentCaptor.forClass(MessageEmbed::class.java)
        Mockito.verify(event).replyEmbeds(embedCaptor.capture())
        Mockito.verify(replyEmbedAction).queue()

        val embed = embedCaptor.value
        // Expect three fields split by 10, 10 + 10 + 5
        assertEquals(3, embed.fields.size)
        assertEquals("Leaderboard", embed.fields[0].name)
        assertEquals("Leaderboard (cont.)", embed.fields[1].name)

        val firstLines = (embed.fields[0].value ?: "").lines()
        val secondLines = (embed.fields[1].value ?: "").lines()
        val thirdLines = (embed.fields[2].value ?: "").lines()
        assertEquals(10, firstLines.size)
        assertEquals(10, secondLines.size)
        assertEquals(5, thirdLines.size)

        // First line should be 🥇 for the highest points (user 1 with 25 pts constructed)
        assertTrue(firstLines[0].startsWith("🥇 "))
        // The 21st entry (first of second chunk) should start with "21." numbering
        assertTrue(thirdLines[0].startsWith("21\\."))
    }

    @Test
    fun `execute falls back to userId string when userTag is blank`() {
        val season = SeasonResolver.currentCfbSeason()
        val event = Mockito.mock(SlashCommandInteractionEvent::class.java)
        val replyEmbedAction = Mockito.mock(ReplyCallbackAction::class.java)

        `when`(event.guild).thenReturn(null)
        `when`(event.replyEmbeds(Mockito.any(MessageEmbed::class.java))).thenReturn(replyEmbedAction)

        val bets = listOf(
            BetEntity(userId = 999L, season = season, week = 1, userTag = "", correctWinner = true)
        )
        `when`(betRepo.findBySeason(season)).thenReturn(bets)

        leaderboard.execute(event)

        val embedCaptor = ArgumentCaptor.forClass(MessageEmbed::class.java)
        Mockito.verify(event).replyEmbeds(embedCaptor.capture())
        val embed = embedCaptor.value
        val line = (embed.fields[0].value ?: "").lines().first()
        // Should contain the raw userId since tag blank and no guild
        assertTrue(line.contains("999"))
    }
}
