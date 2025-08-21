package org.j3y.HuskerBot2.repository

import org.j3y.HuskerBot2.model.HuskerGameEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface HuskerGameRepository : JpaRepository<HuskerGameEntity, Long> {
    @Query("SELECT g FROM HuskerGameEntity g WHERE g.gameDate > :now ORDER BY g.gameDate ASC")
    fun findNextGame(@Param("now") now: LocalDateTime): HuskerGameEntity?
}