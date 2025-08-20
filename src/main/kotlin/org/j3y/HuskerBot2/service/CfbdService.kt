package org.j3y.HuskerBot2.service

import com.fasterxml.jackson.databind.JsonNode
import net.dv8tion.jda.api.entities.MessageEmbed

interface CfbdService {
    fun getCfbScoreboard(league: Int, week: Int): JsonNode
    fun getNflScoreboard(week: Int): JsonNode
    fun getNhlScoreboard(days: Int): JsonNode

    fun buildEventString(apiData: JsonNode, title: String): String
    fun buildEventEmbed(apiData: JsonNode, title: String): MessageEmbed
}
