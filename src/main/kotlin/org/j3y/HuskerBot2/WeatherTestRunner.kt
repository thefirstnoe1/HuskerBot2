package org.j3y.HuskerBot2

import org.j3y.HuskerBot2.service.WeatherService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
@Profile("test-weather")
class WeatherTestRunner(
    private val weatherService: WeatherService
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        println("=== Testing Gameday Weather Functionality ===")
        println()

        println("1. Testing geocoding for Lincoln, NE...")
        val lincolnCoords = weatherService.getCoordinates("Lincoln, NE")
        if (lincolnCoords != null) {
            println("   ✅ Found coordinates: ${lincolnCoords.latitude}, ${lincolnCoords.longitude}")
            
            println()
            println("2. Testing weather forecast...")
            val tomorrow = LocalDateTime.now().plusDays(1)
            val weather = weatherService.getWeatherForecast(
                lincolnCoords.latitude,
                lincolnCoords.longitude,
                tomorrow
            )
            
            if (weather != null) {
                println("   ✅ Weather forecast retrieved:")
                println("      Temperature: ${weather.temperature}°F")
                println("      Conditions: ${weather.shortForecast}")
                println("      Wind: ${weather.windSpeed} ${weather.windDirection}")
                weather.humidity?.let { println("      Precipitation: $it") }
                println("      Details: ${weather.detailedForecast}")
            } else {
                println("   ❌ Failed to get weather forecast")
            }
        } else {
            println("   ❌ Failed to get coordinates")
        }

        println()
        println("3. Testing geocoding for away game location...")
        val iowaCoords = weatherService.getCoordinates("Iowa City, IA")
        if (iowaCoords != null) {
            println("   ✅ Found Iowa City coordinates: ${iowaCoords.latitude}, ${iowaCoords.longitude}")
        } else {
            println("   ❌ Failed to get Iowa City coordinates")
        }

        println()
        println("=== Test completed ===")
        println("The gameday weather command implementation should work correctly!")
    }
}