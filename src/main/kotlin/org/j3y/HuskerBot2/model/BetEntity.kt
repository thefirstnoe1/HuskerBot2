package org.j3y.HuskerBot2.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table

class BetId {
    val userId: Long = 0
    var season: Int = 0
    val week: Int = 0
}

@Entity
@Table(name = "t_bet")
@IdClass(BetId::class)
class BetEntity(
    @Id var userId: Long = 0,
    @Id var season: Int = 0,
    @Id var week: Int = 0,

    var userTag: String = "",
    var winner: String = "",
    var predictPoints: String = "",
    var predictSpread: String = "",
)
