package org.j3y.HuskerBot2.repository

import org.j3y.HuskerBot2.model.NflGameEntity
import org.j3y.HuskerBot2.model.NflPick
import org.j3y.HuskerBot2.model.NflPickId
import org.j3y.HuskerBot2.model.ScheduleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NflPickRepo : JpaRepository<NflPick, NflPickId> {
    fun findBySeasonAndWeek(season: Int, week: Int): List<NflPick>

    fun findByUserIdAndSeasonAndWeek(userId: Long, season: Int, week: Int): List<NflPick>

    fun findCountByUserIdAndSeasonAndWeekAndCorrectPickIsTrue(userId: Long, season: Int, week: Int): Int

    fun findCountByUserIdAndSeasonAndCorrectPickIsTrue(userId: Long, season: Int): Int

    fun findByGameIdAndUserId(gameId: Long, userId: Long): NflPick?

    fun findByGameId(gameId: Long): List<NflPick>
}
