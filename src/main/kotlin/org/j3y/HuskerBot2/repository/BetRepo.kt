package org.j3y.HuskerBot2.repository

import org.j3y.HuskerBot2.model.BetEntity
import org.j3y.HuskerBot2.model.BetId
import org.j3y.HuskerBot2.model.ScheduleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface BetRepo : JpaRepository<BetEntity, BetId> {
    fun findByUserIdAndSeasonAndWeek(userId: Long, season: Int, week: Int): Optional<BetEntity>
    fun findBySeasonAndWeek(season: Int, week: Int): List<BetEntity>
}
