package org.j3y.HuskerBot2.commands.other

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Component
class OsrsStats : SlashCommand() {

    private val client = RestTemplate()

    private val skills = listOf(
        "Overall", "Attack", "Defence", "Strength", "HP", "Ranged", "Prayer", "Magic", "Cooking",
        "Woodcutting", "Fletching", "Fishing", "Firemaking", "Crafting", "Smithing", "Mining",
        "Herblore", "Agility", "Thieving", "Slayer", "Farming", "Runecrafting", "Hunter", "Construction"
    )

    override fun getCommandKey(): String = "osrs"
    override fun getDescription(): String = "Get the high scores and XP for a OSRS player"
    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.STRING, "player", "The player username you'd like to retrieve stats for", true),
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply().queue()

        val player = commandEvent.getOption("player")?.asString ?: ""

        val url = UriComponentsBuilder
            .fromUriString("https://secure.runescape.com/m=hiscore_oldschool/index_lite.ws")
            .queryParam("player", player)
            .build().toUri()

        val response: String
        try {
            response = client.getForObject(url, String::class.java) ?: ""
        } catch (hsce: RestClientException) {
            commandEvent.hook.sendMessage("High scores were not found for player: $player").queue()
            return
        }

        val respTokens = response.split("\n").filter { it.isNotBlank() }
        val sb = StringBuilder("## ⚔\uFE0F OSRS High Scores for $player")
            .append("\n```prolog\n")
            .append(String.format("%18s%16s%11s\n", "Lvl", "XP", "Rank"))

        skills.forEachIndexed { i, skill ->
            if (i < respTokens.size) {
                val stats = respTokens[i].split(',')
                val lvl = stats.getOrNull(1)?.toLongOrNull() ?: 0L
                val xp = stats.getOrNull(2)?.toLongOrNull() ?: 0L
                val rank = stats.getOrNull(0)?.toLongOrNull() ?: 0L
                val line = String.format("%12s:  %-5s%,14d%,11d\n", skill, lvl.toString(), xp, rank)
                sb.append(line)
            }
        }

        sb.append("\n```")
        commandEvent.hook.sendMessage(sb.toString()).queue()
    }
}
