package org.j3y.HuskerBot2.commands.other

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Police : SlashCommand() {
    private val log = LoggerFactory.getLogger(Police::class.java)

    override fun getCommandKey(): String = "police"
    override fun getDescription(): String = "Call the police on someone"
    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.USER, "arrestee", "The user to arrest", true)
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        val arrestee = commandEvent.getOption("arrestee")?.asMember
        if (arrestee == null) {
            commandEvent.reply("Invalid user.").setEphemeral(true).queue()
            return
        }

        val message = buildString {
            append("Wee woo, wee woo!Halt!")
            append("🚨 NANI 🚨..")
            append("🚨 THE 🚨...")
            append("🚨 FUCK 🚨....")
            append("🚨 DID 🚨.....")
            append("🚨 YOU 🚨....")
            append("🚨 JUST 🚨...")
            append("🚨 SAY 🚨..")
            append("🚨 ${arrestee.asMention} 🚨")
            append("🚨🚨🚨🚨🚨🚨🚨🚨🚨")
            append("👮‍📢 Information ℹ provided in the VIP 👑 Room 🏆 is intended for Husker247 🌽🎈 members only ‼🔫. ")
            append("Please do not copy ✏ and paste 🖨 or summarize this content elsewhere‼ ")
            append("Please try to keep all replies in this thread 🧵 for Husker247 members only! 🚫 ⛔ 👎 🙅‍♀️")
            append("Thanks for your cooperation. 😍🤩😘")
        }

        commandEvent.reply(message).queue()
    }
}