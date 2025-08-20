package org.j3y.HuskerBot2.service

import com.fasterxml.jackson.databind.JsonNode

interface EspnService {
    fun getCfbScoreboard(league: Int, week: Int): JsonNode
    fun getNflScoreboard(week: Int): JsonNode
    fun getNhlScoreboard(days: Int): JsonNode

    fun buildEventString(apiData: JsonNode): String
}
