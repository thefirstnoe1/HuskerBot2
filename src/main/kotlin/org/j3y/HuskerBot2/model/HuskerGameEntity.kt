package org.j3y.HuskerBot2.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "husker_games")
class HuskerGameEntity(
    @Id var id: Long = 0,
    var gameDate: LocalDateTime = LocalDateTime.now(),
    var opponent: String = "",
    var isHomeGame: Boolean = true,
    var location: String = "",
    var venue: String = ""
)