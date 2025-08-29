package org.j3y.HuskerBot2.commands.betting

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.automation.betting.BetProcessing
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.repository.BetRepo
import org.j3y.HuskerBot2.repository.ScheduleRepo
import org.j3y.HuskerBot2.util.SeasonResolver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BetReload : SlashCommand() {
    private final val log = LoggerFactory.getLogger(BetReload::class.java)

    @Autowired lateinit var scheduleRepo: ScheduleRepo
    @Autowired private lateinit var betProcessing: BetProcessing

    override fun getCommandKey(): String = "reload"
    override fun isSubcommand(): Boolean = true
    override fun getDescription(): String = "Reload the bet channel"
    override fun getOptions(): List<OptionData> {
        val season = SeasonResolver.currentCfbSeason()
        val choices: List<Command.Choice> = scheduleRepo.findAllBySeasonOrderByDateTimeAsc(season)
            .map { Command.Choice("${it.opponent} - Week ${it.week}", it.week.toLong()) }

        return listOf(
            OptionData(OptionType.INTEGER, "week", "The week of the huskers game.", true).addChoices(choices),
        )
    }

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply(true).queue()

        val week = commandEvent.getOption("week")?.asInt ?: SeasonResolver.currentCfbWeek()

        if (commandEvent.member?.hasPermission(net.dv8tion.jda.api.Permission.MESSAGE_MANAGE) != true) {
            commandEvent.hook.sendMessage("You do not have permission to use this command.").queue()
            return
        }

        betProcessing.postWeeklyBets(week)
        commandEvent.hook.sendMessage("Reloaded the bet channel.").queue()
    }
}
