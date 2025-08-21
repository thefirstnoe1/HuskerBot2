package org.j3y.HuskerBot2.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "t_nfl_game")
data class NflGameEntity(
    @Id
    var id: Long = 0,
    var homeTeam: String = "",
    var homeTeamId: Long = 0,
    var awayTeam: String = "",
    var awayTeamId: Long = 0,
    var dateTime: Instant = Instant.now(),
    var season: Int = 2025,
    var week: Int = 0,
    var homeScore: Int = 0,
    var awayScore: Int = 0,
    var winner: String = "",
)
