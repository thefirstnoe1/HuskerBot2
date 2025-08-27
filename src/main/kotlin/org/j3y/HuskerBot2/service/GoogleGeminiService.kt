package org.j3y.HuskerBot2.service

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Service
class GoogleGeminiService(
    @Value("\${gemini.base-url:https://generativelanguage.googleapis.com}") private val baseUrl: String,
    @Value("\${gemini.model:models/gemini-2.5-flash-lite}") private val model: String,
    @Value("\${gemini.api-key:}") private val apiKey: String,
) {
    private val log = LoggerFactory.getLogger(GoogleGeminiService::class.java)
    private val client = RestTemplate()

    fun generateText(prompt: String): String {
        if (apiKey.isBlank()) {
            return "Gemini is not configured. Please set gemini.api-key in application.yml or environment."
        }
        return try {
            val uri = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/v1beta/")
                .path(model)
                .path(":generateContent")
                .queryParam("key", apiKey)
                .build(true)
                .toUri()

            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON

            val body = mapOf(
                "contents" to listOf(
                    mapOf(
                        "parts" to listOf(
                            mapOf("text" to prompt)
                        )
                    )
                )
            )

            val entity = HttpEntity(body, headers)
            val response = client.exchange(uri, HttpMethod.POST, entity, JsonNode::class.java).body

            val text = response
                ?.get("candidates")?.get(0)
                ?.get("content")?.get("parts")?.get(0)
                ?.get("text")?.asText()

            text?.takeIf { it.isNotBlank() }
                ?: "No response from Gemini (empty or blocked)."
        } catch (ex: Exception) {
            log.error("Error calling Google Gemini API", ex)
            "Error calling Gemini: ${ex.message}"
        }
    }
}
