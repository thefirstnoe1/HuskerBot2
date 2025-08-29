package org.j3y.HuskerBot2.repository

import org.j3y.HuskerBot2.model.ScheduleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface ScheduleRepo : JpaRepository<ScheduleEntity, Long> {
    fun findAllBySeasonOrderByDateTimeAsc(season: Int): List<ScheduleEntity>

    fun findBySeasonAndWeek(season: Int, week: Int): ScheduleEntity?

    fun findFirstByDateTimeAfterOrderByDateTimeAsc(now: Instant): ScheduleEntity?
}
