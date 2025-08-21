package org.j3y.HuskerBot2.repository

import org.j3y.HuskerBot2.model.NflGameEntity
import org.j3y.HuskerBot2.model.ScheduleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NflGameRepo : JpaRepository<NflGameEntity, Long> {
    fun findAllBySeasonOrderByDateTimeAsc(season: Int): List<NflGameEntity>

    fun findBySeasonAndWeek(season: Int, week: Int): NflGameEntity?
}
