package org.j3y.HuskerBot2.commands.betting

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.commands.gameday.GamedayOff
import org.j3y.HuskerBot2.commands.gameday.GamedayOn
import org.j3y.HuskerBot2.commands.images.AddImage
import org.j3y.HuskerBot2.commands.images.DeleteImage
import org.j3y.HuskerBot2.commands.images.ListImages
import org.j3y.HuskerBot2.commands.images.ShowImage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class Betting : SlashCommand() {
    @Autowired lateinit var betCreate: BetCreate
    @Autowired lateinit var betShow: BetShow
    @Autowired lateinit var betLines: BetLines

    override fun getCommandKey(): String = "bet"
    override fun getDescription(): String = "Betting commands"
    override fun getSubcommands(): List<SlashCommand> = listOf(
        betCreate, betShow, betLines
    )
}
