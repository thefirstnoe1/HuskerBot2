package org.j3y.HuskerBot2.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.j3y.HuskerBot2.model.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@Service
class WeatherService(
    @Value("\${weather.nominatim.base-url}") private val nominatimBaseUrl: String,
    @Value("\${weather.nominatim.rate-limit-delay}") private val rateLimitDelayMs: Long,
    @Value("\${weather.nws.base-url}") private val nwsBaseUrl: String,
    @Value("\${weather.nws.user-agent}") private val userAgent: String,
    @Value("\${weather.tomorrow.base-url}") private val tomorrowBaseUrl: String,
    @Value("\${weather.tomorrow.api-key}") private val tomorrowApiKey: String
) {
    
    @Autowired
    private lateinit var googleGeminiService: GoogleGeminiService
    
    private final val log = LoggerFactory.getLogger(WeatherService::class.java)
    private final val restTemplate = RestTemplate()
    private final val objectMapper = ObjectMapper()
    private final val rateLimitDelay: Duration = Duration.ofMillis(rateLimitDelayMs)
    
    @Cacheable("coordinates", unless = "#result == null")
    fun getCoordinates(location: String): GeocodingResult? {
        return searchNominatim(location)
    }
    
    @Cacheable("weather-forecast", unless = "#result == null") 
    fun getWeatherForecast(latitude: Double, longitude: Double, targetDate: LocalDateTime): WeatherForecast? {
        try {
            val now = LocalDateTime.now()
            val hoursUntilGame = ChronoUnit.HOURS.between(now, targetDate)
            val daysUntilGame = ChronoUnit.DAYS.between(now, targetDate)
            
            // Use Tomorrow Weather API if within 5 days (120 hours), otherwise fallback to NWS
            return if (daysUntilGame <= 5) {
                log.info("Using Tomorrow Weather API for forecast $daysUntilGame days out")
                getTomorrowWeatherForecast(latitude, longitude, targetDate, hoursUntilGame)
            } else {
                log.info("Using NWS API for forecast $daysUntilGame days out (beyond Tomorrow Weather 5-day limit)")
                getNWSWeatherForecast(latitude, longitude, targetDate)
            }
        } catch (e: Exception) {
            log.error("Error getting weather forecast for $latitude, $longitude", e)
            return null
        }
    }
    
    private fun getTomorrowWeatherForecast(latitude: Double, longitude: Double, targetDate: LocalDateTime, hoursUntilGame: Long): WeatherForecast? {
        try {
            val headers = HttpHeaders()
            headers.set("Content-Type", "application/json")
            val entity = HttpEntity<String>(headers)
            
            // Determine which timeline to use based on time until game
            val timeline = if (hoursUntilGame <= 120) "hourly" else "daily"
            val fields = "temperature,windSpeed,windDirection,humidity,precipitationProbability,weatherCode"
            
            val url = "$tomorrowBaseUrl/weather/forecast?location=$latitude,$longitude&apikey=$tomorrowApiKey&timesteps=${timeline}&fields=$fields"
            
            val response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                TomorrowWeatherResponse::class.java
            )
            
            val weatherData = response.body
            if (weatherData != null) {
                val forecast = findTomorrowForecastForDate(weatherData, targetDate, timeline == "hourly")
                return if (forecast != null) {
                    addSnarkyDescription(forecast)
                } else {
                    log.warn("No suitable Tomorrow Weather forecast found for $targetDate")
                    null
                }
            }
        } catch (e: Exception) {
            log.error("Error getting Tomorrow Weather forecast, falling back to NWS", e)
            return getNWSWeatherForecast(latitude, longitude, targetDate)
        }
        return null
    }
    
    private fun getNWSWeatherForecast(latitude: Double, longitude: Double, targetDate: LocalDateTime): WeatherForecast? {
        try {
            val forecastData = getNWSForecast(latitude, longitude)
            if (forecastData != null) {
                val forecast = findNWSForecastForDate(forecastData, targetDate)
                return if (forecast != null) {
                    addSnarkyDescription(forecast)
                } else {
                    log.warn("No suitable NWS forecast found for $targetDate")
                    null
                }
            }
        } catch (e: Exception) {
            log.error("Error getting NWS forecast for $latitude, $longitude", e)
        }
        return null
    }
    
    private fun findTomorrowForecastForDate(weatherData: TomorrowWeatherResponse, targetDate: LocalDateTime, useHourly: Boolean): WeatherForecast? {
        val timeline = if (useHourly) weatherData.timelines.hourly else weatherData.timelines.daily
        
        if (timeline == null || timeline.isEmpty()) {
            log.warn("No timeline data available")
            return null
        }
        
        val targetZoned = targetDate.atZone(ZoneId.of("America/Chicago"))
        
        // Find the closest forecast entry to the target time
        val closestEntry = if (useHourly) {
            timeline.minByOrNull { entry ->
                val entryTime = java.time.Instant.parse(entry.time).atZone(ZoneId.of("America/Chicago"))
                kotlin.math.abs(ChronoUnit.HOURS.between(targetZoned, entryTime))
            }
        } else {
            timeline.find { entry ->
                val entryTime = java.time.Instant.parse(entry.time).atZone(ZoneId.of("America/Chicago"))
                entryTime.toLocalDate() == targetZoned.toLocalDate()
            } ?: timeline.firstOrNull()
        }
        
        return closestEntry?.let { entry ->
            val values = entry.values
            WeatherForecast(
                temperature = values.temperature?.roundToInt() ?: 0,
                shortForecast = getWeatherCodeDescription(values.weatherCode ?: 0),
                detailedForecast = createDetailedForecast(values),
                windSpeed = "${values.windSpeed?.roundToInt() ?: 0} mph",
                windDirection = getWindDirection(values.windDirection ?: 0.0),
                humidity = values.humidity?.let { "${it.roundToInt()}% humidity" },
                precipitationProbability = values.precipitationProbability?.roundToInt()
            )
        }
    }
    
    private fun findNWSForecastForDate(forecastData: JsonNode, targetDate: LocalDateTime): WeatherForecast? {
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
                                humidity = period.get("probabilityOfPrecipitation")?.get("value")?.asText()?.let { "${it}% chance of precipitation" }
                            )
                        }
                    }
                }
                
                // Fallback to first period
                if (periods.size() > 0) {
                    val firstPeriod = periods.get(0)
                    log.info("Using first available NWS forecast period as fallback")
                    return WeatherForecast(
                        temperature = firstPeriod.get("temperature")?.asInt() ?: 0,
                        shortForecast = firstPeriod.get("shortForecast")?.asText() ?: "Unknown",
                        detailedForecast = firstPeriod.get("detailedForecast")?.asText() ?: "No details available",
                        windSpeed = firstPeriod.get("windSpeed")?.asText() ?: "Unknown",
                        windDirection = firstPeriod.get("windDirection")?.asText() ?: "Unknown",
                        humidity = firstPeriod.get("probabilityOfPrecipitation")?.get("value")?.asText()?.let { "${it}% chance of precipitation" }
                    )
                }
            }
            null
        } catch (e: Exception) {
            log.error("Error parsing NWS forecast data for target date: $targetDate", e)
            null
        }
    }
    
    private fun addSnarkyDescription(forecast: WeatherForecast): WeatherForecast {
        return try {
            val weatherPrompt = createWeatherPrompt(forecast)
            val snarkyDescription = googleGeminiService.generateResponse(weatherPrompt)
            forecast.copy(snarkyDescription = snarkyDescription)
        } catch (e: Exception) {
            log.error("Error generating snarky weather description", e)
            forecast
        }
    }
    
    private fun createWeatherPrompt(forecast: WeatherForecast): String {
        return """
            Create a hilariously snarky and unhinged weather forecast description for a Nebraska Huskers football game. 
            Keep it under 100 words and make it brutally honest but entertaining. Include references to how this weather 
            will affect the game, the fans, and general Nebraska football culture.
            
            Weather details:
            - Temperature: ${forecast.temperature}°F
            - Conditions: ${forecast.shortForecast}
            - Wind: ${forecast.windSpeed} ${forecast.windDirection}
            - Humidity: ${forecast.humidity ?: "Unknown"}
            ${forecast.precipitationProbability?.let { "- Precipitation chance: $it%" } ?: ""}
            
            Make it spicy but keep it family-friendly for Discord.
        """.trimIndent()
    }
    
    private fun getWeatherCodeDescription(code: Int): String {
        return when (code) {
            0 -> "Unknown"
            1000 -> "Clear"
            1001 -> "Cloudy"
            1100 -> "Mostly Clear"
            1101 -> "Partly Cloudy"
            1102 -> "Mostly Cloudy"
            2000 -> "Fog"
            2100 -> "Light Fog"
            3000 -> "Light Wind"
            3001 -> "Wind"
            3002 -> "Strong Wind"
            4000 -> "Drizzle"
            4001 -> "Rain"
            4200 -> "Light Rain"
            4201 -> "Heavy Rain"
            5000 -> "Snow"
            5001 -> "Flurries"
            5100 -> "Light Snow"
            5101 -> "Heavy Snow"
            6000 -> "Freezing Drizzle"
            6001 -> "Freezing Rain"
            6200 -> "Light Freezing Rain"
            6201 -> "Heavy Freezing Rain"
            7000 -> "Ice Pellets"
            7101 -> "Heavy Ice Pellets"
            7102 -> "Light Ice Pellets"
            8000 -> "Thunderstorm"
            else -> "Unknown Conditions"
        }
    }
    
    private fun createDetailedForecast(values: TomorrowWeatherValues): String {
        val parts = mutableListOf<String>()
        
        values.temperature?.let { parts.add("Temperature around ${it.roundToInt()}°F") }
        values.windSpeed?.let { parts.add("winds at ${it.roundToInt()} mph") }
        values.humidity?.let { parts.add("${it.roundToInt()}% humidity") }
        values.precipitationProbability?.let { 
            if (it > 0) parts.add("${it.roundToInt()}% chance of precipitation") 
        }
        
        return if (parts.isNotEmpty()) {
            parts.joinToString(", ") + "."
        } else {
            "Weather details unavailable."
        }
    }
    
    private fun getWindDirection(degrees: Double): String {
        return when (degrees.roundToInt()) {
            in 0..22, in 338..360 -> "N"
            in 23..67 -> "NE"
            in 68..112 -> "E"
            in 113..157 -> "SE"
            in 158..202 -> "S"
            in 203..247 -> "SW"
            in 248..292 -> "W"
            in 293..337 -> "NW"
            else -> "Variable"
        }
    }
    
    private fun searchNominatim(location: String): GeocodingResult? {
        return try {
            Thread.sleep(rateLimitDelay.toMillis())
            
            val headers = HttpHeaders()
            headers.set("User-Agent", userAgent)
            val entity = HttpEntity<String>(headers)
            
            val encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8)
            val url = "$nominatimBaseUrl/search?q=$encodedLocation&format=json&limit=1"
            
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
            headers.set("User-Agent", userAgent)
            val entity = HttpEntity<String>(headers)
            
            val pointsUrl = "$nwsBaseUrl/points/$latitude,$longitude"
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
}