package org.j3y.HuskerBot2.service

import org.j3y.HuskerBot2.model.HuskerGameEntity
import org.j3y.HuskerBot2.repository.HuskerGameRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class GameService(private final val huskerGameRepository: HuskerGameRepository) {
    
    private final val log = LoggerFactory.getLogger(GameService::class.java)
    
    fun getNextGame(): HuskerGameEntity? {
        return try {
            val nextGame = huskerGameRepository.findNextGame(LocalDateTime.now())
            if (nextGame != null) {
                log.info("Found next game: ${nextGame.opponent} on ${nextGame.gameDate}")
            } else {
                log.info("No upcoming games found")
            }
            nextGame
        } catch (e: Exception) {
            log.error("Error retrieving next game", e)
            null
        }
    }
    
    fun isGameWithinWeek(gameDate: LocalDateTime): Boolean {
        val now = LocalDateTime.now()
        val daysUntilGame = ChronoUnit.DAYS.between(now, gameDate)
        return daysUntilGame <= 7 && daysUntilGame >= 0
    }
}