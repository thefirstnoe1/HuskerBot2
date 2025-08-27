package org.j3y.HuskerBot2.commands.gameday

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.model.ScheduleEntity
import org.j3y.HuskerBot2.model.WeatherForecast
import org.j3y.HuskerBot2.repository.ScheduleRepo
import org.j3y.HuskerBot2.service.WeatherService
import org.j3y.HuskerBot2.util.SeasonResolver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Component
class GamedayWeather : SlashCommand() {
    
    @Autowired lateinit var scheduleRepo: ScheduleRepo
    @Autowired lateinit var weatherService: WeatherService
    
    private final val log = LoggerFactory.getLogger(GamedayWeather::class.java)
    
    override fun getCommandKey(): String = "gameday-weather"
    
    override fun getDescription(): String = "Get weather forecast for the next Huskers game"
    
    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply().queue()
        
        try {
            val nextGame = getNextGame()
            if (nextGame == null) {
                commandEvent.hook.sendMessage("No upcoming games found.").queue()
                return
            }
            
            if (!isGameWithinWeek(nextGame.dateTime)) {
                commandEvent.hook.sendMessage("Game is beyond 14-day weather forecast range.").queue()
                return
            }
            
            val gameLocation = getGameLocation(nextGame)
            val coordinates = weatherService.getCoordinates(gameLocation)
            if (coordinates == null) {
                commandEvent.hook.sendMessage("Unable to find location coordinates for $gameLocation.").queue()
                return
            }
            
            val gameDateTime = LocalDateTime.ofInstant(nextGame.dateTime, ZoneId.of("America/Chicago"))
            val weather = weatherService.getWeatherForecast(
                coordinates.latitude, 
                coordinates.longitude, 
                gameDateTime
            )
            
            val embed = createWeatherEmbed(nextGame, weather)
            commandEvent.hook.sendMessageEmbeds(embed).queue()
            
        } catch (e: Exception) {
            log.error("Error executing gameday-weather command", e)
            commandEvent.hook.sendMessage("Sorry, there was an error getting the weather forecast.").queue()
        }
    }
    
    private fun getNextGame(): ScheduleEntity? {
        return try {
            val currentYear = SeasonResolver.currentCfbSeason()
            val allGames = scheduleRepo.findAllBySeasonOrderByDateTimeAsc(currentYear)
            val now = Instant.now()
            
            val nextGame = allGames.firstOrNull { it.dateTime.isAfter(now) }
            if (nextGame != null) {
                log.info("Found next game: ${nextGame.opponent} on ${nextGame.dateTime}")
            } else {
                log.info("No upcoming games found for season $currentYear")
            }
            nextGame
        } catch (e: Exception) {
            log.error("Error retrieving next game", e)
            null
        }
    }
    
    private fun isGameWithinWeek(gameDateTime: Instant): Boolean {
        val gameDate = LocalDateTime.ofInstant(gameDateTime, ZoneId.systemDefault())
        val now = LocalDateTime.now()
        val daysUntilGame = ChronoUnit.DAYS.between(now, gameDate)
        return daysUntilGame <= 14 && daysUntilGame >= 0
    }
    
    private fun getGameLocation(scheduleEntity: ScheduleEntity): String {
        return when {
            scheduleEntity.venueType.equals("home", ignoreCase = true) -> "Lincoln, NE"
            scheduleEntity.location.isNotBlank() -> scheduleEntity.location
            else -> "Unknown Location"
        }
    }
    
    private fun getGameVenue(scheduleEntity: ScheduleEntity): String {
        return when {
            scheduleEntity.venueType.equals("home", ignoreCase = true) -> "Memorial Stadium"
            scheduleEntity.location.isNotBlank() -> scheduleEntity.location
            else -> "Unknown Venue"
        }
    }
    
    private fun isHomeGame(scheduleEntity: ScheduleEntity): Boolean {
        return scheduleEntity.venueType.equals("home", ignoreCase = true)
    }
    
    private fun createWeatherEmbed(game: ScheduleEntity, weather: WeatherForecast?): MessageEmbed {
        val embed = EmbedBuilder()
        
        embed.setTitle("🏈 Huskers Game Day Weather")
        embed.setColor(Color.RED)
        
        embed.addField("🆚 Opponent", game.opponent, true)
        embed.addField("📅 Game Time", formatGameTime(game.dateTime), true)
        embed.addField("📍 Location", getGameLocation(game), true)
        embed.addField("🏠 Home/Away", if (isHomeGame(game)) "Home" else "Away", true)
        
        if (weather != null) {
            embed.addField("🌡️ Temperature", "${weather.temperature}°F", true)
            weather.micksTemp?.let {
                embed.addField("🌡️ Micks Temp", it, true)
            }
            embed.addField("☁️ Conditions", weather.shortForecast, true)
            embed.addField("💨 Wind", "${weather.windSpeed} ${weather.windDirection}", true)
            weather.humidity?.let { 
                embed.addField("🌧️ Precipitation", it, true) 
            }
            weather.precipitationProbability?.let {
                embed.addField("☔ Rain Chance", "${it}%", true)
            }
            
            // Add snarky description if available
            weather.snarkyDescription?.let {
                embed.addField("🔥 Forecast Hot Take", it, false)
            }
            
            embed.addField("📋 Detailed Forecast", weather.detailedForecast, false)
        } else {
            embed.addField("⚠️ Weather", "Weather data unavailable", false)
        }
        
        embed.setFooter("Weather data from Tomorrow.io & National Weather Service")
        embed.setTimestamp(Instant.now())
        
        return embed.build()
    }
    
    private fun formatGameTime(gameDateTime: Instant): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a z")
        val zonedDateTime = gameDateTime.atZone(ZoneId.of("America/Chicago"))
        return zonedDateTime.format(formatter)
    }
}