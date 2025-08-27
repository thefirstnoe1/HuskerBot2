package org.j3y.HuskerBot2.commands.betting

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.model.BetEntity
import org.j3y.HuskerBot2.repository.BetRepo
import org.j3y.HuskerBot2.repository.ScheduleRepo
import org.j3y.HuskerBot2.util.SeasonResolver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Component
class BetCreate : SlashCommand() {
    val log = LoggerFactory.getLogger(BetCreate::class.java)

    @Autowired lateinit var scheduleRepo: ScheduleRepo
    @Autowired lateinit var betRepo: BetRepo

    override fun getCommandKey(): String = "create"
    override fun isSubcommand(): Boolean = true
    override fun getDescription(): String = "Place a bet on a Nebraska game"
    override fun getOptions(): List<OptionData> {
        val season = SeasonResolver.currentCfbSeason()
        val choices: List<Command.Choice> = scheduleRepo.findAllBySeasonOrderByDateTimeAsc(season)
            .map { Command.Choice("${it.opponent} - Week ${it.week}", it.week.toLong()) }

        return listOf(
            OptionData(OptionType.INTEGER, "week", "The name of the opponent for the husker game.", true).addChoices(choices),
            OptionData(OptionType.STRING, "winner", "The winner of the game.", true)
                .addChoice("Nebraska", "Nebraska")
                .addChoice("Opponent", "Opponent"),
            OptionData(OptionType.STRING, "predict-points", "Whether you think the total points will be over or under.", true)
                .addChoice("Over", "Over")
                .addChoice("Under", "Under"),
            OptionData(OptionType.STRING, "predict-spread", "Whether you pick Nebraska or the Opponent to win against the spread.", true)
                .addChoice("Nebraska", "Nebraska")
                .addChoice("Opponent", "Opponent")
        )
    }

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        val season = SeasonResolver.currentCfbSeason()
        val week = commandEvent.getOption("week")?.asInt ?: 1
        val winner = commandEvent.getOption("winner")?.asString ?: "Nebraska"
        val predictPoints = commandEvent.getOption("predict-points")?.asString ?: "Over"
        val predictSpread = commandEvent.getOption("predict-spread")?.asString ?: "Nebraska"
        val userId = commandEvent.user.idLong
        val userTag = commandEvent.user.asTag

        val sched = scheduleRepo.findBySeasonAndWeek(season, week)
        if (sched == null) {
            commandEvent.reply("Unable to find scheduled game for $season - $week.").setEphemeral(true).queue()
            return
        }

        var opponent = sched.opponent

        val hourBeforeGameTime = sched.dateTime.minus(1, ChronoUnit.HOURS)
        if (Instant.now().isAfter(hourBeforeGameTime)) {
            commandEvent.reply("You can not set a bet less than an hour before game time.").setEphemeral(true).queue()
            return
        }

        var isUpdate = false
        val betOpt = betRepo.findByUserIdAndSeasonAndWeek(userId, season, week)
        var bet: BetEntity?

        if (betOpt.isPresent) {
            bet = betOpt.get()
            isUpdate = true
        } else {
            bet = BetEntity(userId, season, week)
        }

        bet.winner = winner
        bet.predictPoints = predictPoints
        bet.predictSpread = predictSpread
        bet.userTag = userTag

        betRepo.save(bet)

        commandEvent.replyEmbeds(
            EmbedBuilder()
                .setTitle("Submitted Bet")
                .setColor(Color.RED)
                .setDescription("${commandEvent.member?.effectiveName ?: commandEvent.user.effectiveName} ${if (isUpdate) "updated" else "submitted"} their bet.")
                .addField("Opponent", opponent, true)
                .addField("Week", week.toString(), true)
                .addBlankField(true)
                .addField("Winner", winner, true)
                .addField("Over/Under", predictPoints, true)
                .addField("Spread", predictSpread, true)
                .setThumbnail(sched.opponentLogo)
                .build()
        ).setEphemeral(true).queue()
    }
}
