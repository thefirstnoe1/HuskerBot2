package org.j3y.HuskerBot2.commands.pickem

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.automation.pickem.nfl.NflPickem
import org.j3y.HuskerBot2.commands.SlashCommand
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class NflPickemReload : SlashCommand() {
    private val log = LoggerFactory.getLogger(NflPickemReload::class.java)

    @Autowired lateinit var nflPickem: NflPickem

    override fun getCommandKey(): String = "nfl-pickem-reload"
    override fun getDescription(): String = "Reload the NFL Pick'em Listing. No picks will be deleted."
    override fun getOptions(): List<OptionData> = emptyList()
    override fun getPermissions(): DefaultMemberPermissions = DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.MESSAGE_MANAGE)

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply(true).queue()
        try {
            nflPickem.postWeeklyPickem()
            commandEvent.hook.sendMessage("Reloaded the NFL Pick'em Listing.").queue()
        } catch (e: Exception) {
            log.error("Error while reloading the NFL Pick'em Listing: ${e.message}", e);
            commandEvent.hook.sendMessage("There was an issue reloading the NFL Pick'em Listing: ${e.message}").queue()
        }

    }
}