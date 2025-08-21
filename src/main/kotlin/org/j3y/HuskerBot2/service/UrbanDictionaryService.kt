package org.j3y.HuskerBot2.service

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Service
class UrbanDictionaryService(
    @Value("\${urban.base-url}") private val baseUrl: String,
    @Value("\${urban.user-agent}") private val userAgent: String,
) {
    private val log = LoggerFactory.getLogger(UrbanDictionaryService::class.java)
    private val client = RestTemplate()

    data class UrbanDefinition(
        val word: String,
        val definition: String,
        val example: String?,
        val thumbsUp: Int,
        val thumbsDown: Int,
        val author: String?,
        val permalink: String?
    )

    fun define(term: String): UrbanDefinition? {
        return try {
            val uri = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path("/v0/define")
                .queryParam("term", term)
                .build(true)
                .toUri()

            val headers = HttpHeaders()
            headers.set("User-Agent", userAgent)
            val entity = HttpEntity<Void>(headers)

            val response = client.exchange(uri, HttpMethod.GET, entity, JsonNode::class.java).body
            val list = response?.get("list")
            if (list != null && list.isArray && list.size() > 0) {
                val first = list.get(0)
                UrbanDefinition(
                    word = first.get("word")?.asText() ?: term,
                    definition = cleanUdText(first.get("definition")?.asText() ?: ""),
                    example = cleanUdText(first.get("example")?.asText() ?: ""),
                    thumbsUp = first.get("thumbs_up")?.asInt() ?: 0,
                    thumbsDown = first.get("thumbs_down")?.asInt() ?: 0,
                    author = first.get("author")?.asText(),
                    permalink = first.get("permalink")?.asText()
                )
            } else {
                null
            }
        } catch (ex: Exception) {
            log.error("Error fetching Urban Dictionary definition for term: {}", term, ex)
            null
        }
    }

    private fun cleanUdText(text: String): String {
        // Urban Dictionary wraps words in [brackets]; remove them for clarity in embeds
        return text.replace("[", "").replace("]", "")
    }
}
