package org.j3y.HuskerBot2.service

import com.fasterxml.jackson.databind.JsonNode
import net.dv8tion.jda.api.entities.MessageEmbed

interface HuskersDotComService {
    fun getSchedule(year: Int): JsonNode
}
