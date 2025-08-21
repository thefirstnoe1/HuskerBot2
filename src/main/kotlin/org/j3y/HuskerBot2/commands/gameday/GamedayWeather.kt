package org.j3y.HuskerBot2.commands.gameday

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.model.HuskerGameEntity
import org.j3y.HuskerBot2.model.WeatherForecast
import org.j3y.HuskerBot2.service.GameService
import org.j3y.HuskerBot2.service.WeatherService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.Instant
import java.time.format.DateTimeFormatter

@Component
class GamedayWeather(
    private final val gameService: GameService,
    private final val weatherService: WeatherService
) : SlashCommand() {
    
    private final val log = LoggerFactory.getLogger(GamedayWeather::class.java)
    
    override fun getCommandKey(): String = "gameday-weather"
    
    override fun getDescription(): String = "Get weather forecast for the next Huskers game"
    
    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply().queue()
        
        try {
            val nextGame = gameService.getNextGame()
            if (nextGame == null) {
                commandEvent.hook.sendMessage("No upcoming games found.").queue()
                return
            }
            
            if (!gameService.isGameWithinWeek(nextGame.gameDate)) {
                commandEvent.hook.sendMessage("Game is beyond 7-day weather forecast range.").queue()
                return
            }
            
            val coordinates = weatherService.getCoordinates(nextGame.location)
            if (coordinates == null) {
                commandEvent.hook.sendMessage("Unable to find location coordinates for ${nextGame.location}.").queue()
                return
            }
            
            val weather = weatherService.getWeatherForecast(
                coordinates.latitude, 
                coordinates.longitude, 
                nextGame.gameDate
            )
            
            val embed = createWeatherEmbed(nextGame, weather)
            commandEvent.hook.sendMessageEmbeds(embed).queue()
            
        } catch (e: Exception) {
            log.error("Error executing gameday-weather command", e)
            commandEvent.hook.sendMessage("Sorry, there was an error getting the weather forecast.").queue()
        }
    }
    
    private fun createWeatherEmbed(game: HuskerGameEntity, weather: WeatherForecast?): MessageEmbed {
        val embed = EmbedBuilder()
        
        embed.setTitle("🏈 Huskers Game Day Weather")
        embed.setColor(Color.RED)
        
        embed.addField("🆚 Opponent", game.opponent, true)
        embed.addField("📅 Game Time", formatGameTime(game.gameDate), true)
        embed.addField("📍 Location", "${game.venue}, ${game.location}", true)
        embed.addField("🏠 Home/Away", if (game.isHomeGame) "Home" else "Away", true)
        
        if (weather != null) {
            embed.addField("🌡️ Temperature", "${weather.temperature}°F", true)
            embed.addField("☁️ Conditions", weather.shortForecast, true)
            embed.addField("💨 Wind", "${weather.windSpeed} ${weather.windDirection}", true)
            weather.humidity?.let { 
                embed.addField("💧 Humidity", "${it}%", true) 
            }
            embed.addField("📋 Detailed Forecast", weather.detailedForecast, false)
        } else {
            embed.addField("⚠️ Weather", "Weather data unavailable", false)
        }
        
        embed.setFooter("Weather data from National Weather Service")
        embed.setTimestamp(Instant.now())
        
        return embed.build()
    }
    
    private fun formatGameTime(gameDate: java.time.LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a")
        return gameDate.format(formatter)
    }
}