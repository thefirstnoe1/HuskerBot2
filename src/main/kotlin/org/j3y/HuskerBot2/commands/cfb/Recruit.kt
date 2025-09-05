package org.j3y.HuskerBot2.commands.cfb

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.service.RivalsService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class Recruit(
    private val rivalsService: RivalsService
) : SlashCommand() {

    private val log = LoggerFactory.getLogger(Recruit::class.java)

    override fun getCommandKey(): String = "recruit"
    override fun getDescription(): String = "Look up CFB recruit info by year and name"

    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.INTEGER, "year", "Recruit class year (e.g., 2025)", true),
        OptionData(OptionType.STRING, "name", "Recruit name (first last)", true)
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply().queue()
        try {
            val year = commandEvent.getOption("year")?.asInt
            val name = commandEvent.getOption("name")?.asString?.trim()

            if (year == null || name.isNullOrBlank()) {
                commandEvent.hook.sendMessage("Please provide both year and name.").queue()
                return
            }

            val data = rivalsService.getRecruitData(year, name)
            if (data == null) {
                commandEvent.hook.sendMessage("No recruit found for '$name' in class $year.").queue()
                return
            }

            val embed = buildRecruitEmbed(data)
            commandEvent.hook.sendMessageEmbeds(embed).queue()
        } catch (e: Exception) {
            log.error("Error executing /recruit command", e)
            commandEvent.hook.sendMessage("Sorry, there was an error looking up that recruit.").queue()
        }
    }

    private fun buildRecruitEmbed(d: RivalsService.RecruitData): net.dv8tion.jda.api.entities.MessageEmbed {
        val title = "${d.firstName} ${d.lastName} — ${d.position} (${d.year})"
        val description = buildString {
            if (d.hometown.isNotBlank()) append("🏠 ${d.hometown}\n")
            if (d.highSchool.isNotBlank()) append("🏫 ${d.highSchool}\n")
            val hw = listOfNotNull(d.height.takeIf { it.isNotBlank() }, d.weight.takeIf { it.isNotBlank() }).joinToString(" / ")
            if (hw.isNotBlank()) append("📏 $hw\n")
            val statusTeam = when {
                d.status.equals("committed", true) && d.commitTeam.isNotBlank() -> "Committed to ${d.commitTeam}"
                d.status.isNotBlank() -> d.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                else -> "Status: Unknown"
            }
            append("📣 $statusTeam\n")
        }.trim()

        val stars = if (d.starRating > 0) "⭐".repeat(d.starRating.coerceAtMost(5)) else "Unrated"
        val eb = EmbedBuilder()
            .setTitle(title)
            .setColor(Color(207, 0, 0))
            .setDescription(description)
            .addField("On3 Profile", d.link, false)
            .addField("Rating", stars, false)
            .addField("National Rank", if (d.nationalRank > 0) "#${d.nationalRank}" else "N/A", true)
            .addField("Position Rank", if (d.positionRank > 0) "#${d.positionRank}" else "N/A", true)
            .addField("State Rank", if (d.stateRank > 0) "#${d.stateRank}" else "N/A", true)
            .setFooter("Data via On3 Rivals - Consensus Rankings")

        log.info("Img: ${d.imageUrl}")

        // Add prospects section if available
        if (d.prospects.isNotEmpty()) {
            val sorted = d.prospects.sortedByDescending { it.prediction }
            val lines = mutableListOf<String>("TEAM - STATUS - PREDICTION (OFFICIAL / UNOFFICIAL VISITS)")
            var totalLen = 0
            val maxFieldLen = 1000 // keep under Discord's 1024 char limit for field values
            for ((index, p) in sorted.withIndex()) {
                if (index >= 5) break // show top 5
                val team = p.teamName.ifBlank { "Unknown Team" }
                val status = p.status.ifBlank { "Unknown" }
                val pct = if (p.prediction <= 1.0) (p.prediction * 100.0) else p.prediction
                val line = "- ${team} — ${status}, ${"%.0f".format(pct)}% (OV:${p.officialVisitCount} UV:${p.unofficialVisitCount})"
                if (totalLen + line.length + 1 > maxFieldLen) break
                lines.add(line)
                totalLen += line.length + 1
            }
            if (lines.isNotEmpty()) {
                val value = lines.joinToString("\n")
                eb.addField("Prospects", value, false)
            }
        }

        d.imageUrl?.takeIf { it.isNotBlank() }?.let { eb.setThumbnail(it) }
        return eb.build()
    }
}
