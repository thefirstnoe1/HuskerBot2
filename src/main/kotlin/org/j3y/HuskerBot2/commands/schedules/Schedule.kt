package org.j3y.HuskerBot2.commands.schedules

import org.j3y.HuskerBot2.commands.SlashCommand
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class Schedule : SlashCommand() {
    @Autowired lateinit var cfbSched: CfbSched
    @Autowired lateinit var nflSched: NflSched
    @Autowired lateinit var vbSched: VbSched

    override fun getCommandKey(): String = "schedule"
    override fun getDescription(): String = "Schedule commands"
    override fun getSubcommands(): List<SlashCommand> = listOf(
        cfbSched, nflSched, vbSched
    )
}
