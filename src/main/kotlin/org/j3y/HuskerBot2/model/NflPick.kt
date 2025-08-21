package org.j3y.HuskerBot2.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

class NflPickId {
    val gameId: Long = 0
    var userId: Long = 0
}

@Entity
@Table(name = "t_nfl_game_pick", indexes = [Index(name = "game_pick_user_id_idx", columnList = "userId")])
@IdClass(NflPickId::class)
data class NflPick(
    @Id var gameId: Long = 0,
    @Id var userId: Long = 0,
    var season: Int = 0,
    var week: Int = 0,
    var winningTeamId: Long = 0,
    var processed: Boolean = false,
    var correctPick: Boolean = false,
    var dateTimePicked: Instant = Instant.now()
)
