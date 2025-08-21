package org.j3y.HuskerBot2

import org.j3y.HuskerBot2.model.ScheduleEntity
import org.j3y.HuskerBot2.repository.ScheduleRepo
import org.j3y.HuskerBot2.service.GameService
import org.j3y.HuskerBot2.service.WeatherService
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@SpringBootTest
class GamedayWeatherTest {

    private val log = LoggerFactory.getLogger(GamedayWeatherTest::class.java)

    @Autowired
    private lateinit var gameService: GameService

    @Autowired
    private lateinit var weatherService: WeatherService

    @Autowired
    private lateinit var scheduleRepo: ScheduleRepo

    @Test
    fun testGamedayWeatherFunctionality() {
        log.info("=== Testing Gameday Weather Functionality ===")

        val testGame = ScheduleEntity(
            id = 999999,
            opponent = "Iowa Hawkeyes",
            location = "Iowa City, IA",
            isConference = true,
            venueType = "away",
            dateTime = Instant.now().plus(java.time.Duration.ofDays(3)),
            season = LocalDateTime.now().year,
            week = 6
        )

        scheduleRepo.save(testGame)
        log.info("Saved test game: ${testGame.opponent} on ${testGame.dateTime}")

        val nextGame = gameService.getNextGame()
        if (nextGame != null) {
            log.info("Found next game: ${nextGame.opponent} on ${nextGame.dateTime}")
            
            val withinWeek = gameService.isGameWithinWeek(nextGame.dateTime)
            log.info("Game is within forecast window: $withinWeek")

            if (withinWeek) {
                val gameLocation = gameService.getGameLocation(nextGame)
                log.info("Testing geocoding for location: $gameLocation")
                val coordinates = weatherService.getCoordinates(gameLocation)
                
                if (coordinates != null) {
                    log.info("Found coordinates: ${coordinates.latitude}, ${coordinates.longitude}")
                    
                    val gameDateTime = LocalDateTime.ofInstant(nextGame.dateTime, ZoneId.of("America/Chicago"))
                    log.info("Getting weather forecast...")
                    val weather = weatherService.getWeatherForecast(
                        coordinates.latitude,
                        coordinates.longitude,
                        gameDateTime
                    )
                    
                    if (weather != null) {
                        log.info("Weather forecast retrieved successfully:")
                        log.info("  Temperature: ${weather.temperature}°F")
                        log.info("  Conditions: ${weather.shortForecast}")
                        log.info("  Wind: ${weather.windSpeed} ${weather.windDirection}")
                        weather.humidity?.let { log.info("  Precipitation: $it") }
                        log.info("  Details: ${weather.detailedForecast}")
                    } else {
                        log.warn("Failed to get weather forecast")
                    }
                } else {
                    log.warn("Failed to get coordinates for location: $gameLocation")
                }
            } else {
                log.warn("Game is not within 7-day forecast window")
            }
        } else {
            log.warn("No upcoming games found")
        }

        scheduleRepo.delete(testGame)
        log.info("=== Test completed ===")
    }

    @Test
    fun testWeatherServiceDirectly() {
        log.info("=== Testing Weather Service Directly ===")
        
        log.info("Testing geocoding for Lincoln, NE...")
        val lincolnCoords = weatherService.getCoordinates("Lincoln, NE")
        
        if (lincolnCoords != null) {
            log.info("Lincoln coordinates: ${lincolnCoords.latitude}, ${lincolnCoords.longitude}")
            
            val tomorrow = LocalDateTime.now().plusDays(1)
            log.info("Getting weather forecast for tomorrow...")
            
            val weather = weatherService.getWeatherForecast(
                lincolnCoords.latitude,
                lincolnCoords.longitude,
                tomorrow
            )
            
            if (weather != null) {
                log.info("Weather forecast for Lincoln, NE:")
                log.info("  Temperature: ${weather.temperature}°F")
                log.info("  Conditions: ${weather.shortForecast}")
                log.info("  Wind: ${weather.windSpeed} ${weather.windDirection}")
                weather.humidity?.let { log.info("  Precipitation: $it") }
            } else {
                log.warn("Failed to get weather forecast for Lincoln")
            }
        } else {
            log.warn("Failed to get coordinates for Lincoln, NE")
        }
        
        log.info("=== Direct weather service test completed ===")
    }
}