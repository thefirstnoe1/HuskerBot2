package org.j3y.HuskerBot2.service

import com.fasterxml.jackson.databind.JsonNode
import org.j3y.HuskerBot2.model.GeocodingResult
import org.j3y.HuskerBot2.model.WeatherForecast
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class WeatherService {
    
    private final val log = LoggerFactory.getLogger(WeatherService::class.java)
    private final val restTemplate = RestTemplate()
    
    companion object {
        private const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org"
        private const val NWS_BASE_URL = "https://api.weather.gov"
        private const val USER_AGENT = "HuskerBot2 (huskerbot@example.com)"
        private val RATE_LIMIT_DELAY = Duration.ofSeconds(1)
    }
    
    @Cacheable("coordinates", unless = "#result == null")
    fun getCoordinates(location: String): GeocodingResult? {
        return searchNominatim(location)
    }
    
    @Cacheable("weather-forecast", unless = "#result == null") 
    fun getWeatherForecast(latitude: Double, longitude: Double, targetDate: LocalDateTime): WeatherForecast? {
        try {
            val forecastData = getNWSForecast(latitude, longitude)
            if (forecastData != null) {
                return findForecastForDate(forecastData, targetDate)
            }
        } catch (e: Exception) {
            log.error("Error getting weather forecast for $latitude, $longitude", e)
        }
        return null
    }
    
    private fun searchNominatim(location: String): GeocodingResult? {
        return try {
            Thread.sleep(RATE_LIMIT_DELAY.toMillis())
            
            val headers = HttpHeaders()
            headers.set("User-Agent", USER_AGENT)
            val entity = HttpEntity<String>(headers)
            
            val encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8)
            val url = "$NOMINATIM_BASE_URL/search?q=$encodedLocation&format=json&limit=1"
            
            val response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Array<JsonNode>::class.java
            )
            
            val results = response.body
            if (results != null && results.isNotEmpty()) {
                val result = results[0]
                val lat = result.get("lat").asDouble()
                val lon = result.get("lon").asDouble()
                log.info("Found coordinates for $location: $lat, $lon")
                GeocodingResult(lat, lon)
            } else {
                log.warn("No coordinates found for location: $location")
                null
            }
        } catch (e: Exception) {
            log.error("Error geocoding location: $location", e)
            null
        }
    }
    
    private fun getNWSForecast(latitude: Double, longitude: Double): JsonNode? {
        return try {
            val headers = HttpHeaders()
            headers.set("User-Agent", USER_AGENT)
            val entity = HttpEntity<String>(headers)
            
            val pointsUrl = "$NWS_BASE_URL/points/$latitude,$longitude"
            val pointsResponse = restTemplate.exchange(
                pointsUrl,
                HttpMethod.GET,
                entity,
                JsonNode::class.java
            )
            
            val forecastUrl = pointsResponse.body?.get("properties")?.get("forecast")?.asText()
            if (forecastUrl != null) {
                val forecastResponse = restTemplate.exchange(
                    forecastUrl,
                    HttpMethod.GET,
                    entity,
                    JsonNode::class.java
                )
                forecastResponse.body
            } else {
                log.warn("No forecast URL found for coordinates: $latitude, $longitude")
                null
            }
        } catch (e: Exception) {
            log.error("Error getting NWS forecast for $latitude, $longitude", e)
            null
        }
    }
    
    private fun findForecastForDate(forecastData: JsonNode, targetDate: LocalDateTime): WeatherForecast? {
        return try {
            val periods = forecastData.get("properties")?.get("periods")
            if (periods != null && periods.isArray) {
                val targetZoned = targetDate.atZone(ZoneId.of("America/Chicago"))
                
                for (period in periods) {
                    val startTime = period.get("startTime")?.asText()
                    val endTime = period.get("endTime")?.asText()
                    
                    if (startTime != null && endTime != null) {
                        val periodStart = java.time.Instant.parse(startTime).atZone(ZoneId.of("America/Chicago"))
                        val periodEnd = java.time.Instant.parse(endTime).atZone(ZoneId.of("America/Chicago"))
                        
                        if (targetZoned.isAfter(periodStart) && targetZoned.isBefore(periodEnd)) {
                            return WeatherForecast(
                                temperature = period.get("temperature")?.asInt() ?: 0,
                                shortForecast = period.get("shortForecast")?.asText() ?: "Unknown",
                                detailedForecast = period.get("detailedForecast")?.asText() ?: "No details available",
                                windSpeed = period.get("windSpeed")?.asText() ?: "Unknown",
                                windDirection = period.get("windDirection")?.asText() ?: "Unknown",
                                humidity = period.get("relativeHumidity")?.get("value")?.asText()
                            )
                        }
                    }
                }
                
                if (periods.size() > 0) {
                    val firstPeriod = periods.get(0)
                    log.info("Using first available forecast period as fallback")
                    return WeatherForecast(
                        temperature = firstPeriod.get("temperature")?.asInt() ?: 0,
                        shortForecast = firstPeriod.get("shortForecast")?.asText() ?: "Unknown",
                        detailedForecast = firstPeriod.get("detailedForecast")?.asText() ?: "No details available",
                        windSpeed = firstPeriod.get("windSpeed")?.asText() ?: "Unknown",
                        windDirection = firstPeriod.get("windDirection")?.asText() ?: "Unknown",
                        humidity = firstPeriod.get("relativeHumidity")?.get("value")?.asText()
                    )
                }
            }
            log.warn("No suitable forecast period found for target date: $targetDate")
            null
        } catch (e: Exception) {
            log.error("Error parsing forecast data for target date: $targetDate", e)
            null
        }
    }
}