package org.j3y.HuskerBot2.web

import org.j3y.HuskerBot2.model.NflPick
import org.j3y.HuskerBot2.repository.NflPickRepo
import org.j3y.HuskerBot2.service.UserNameCache
import org.j3y.HuskerBot2.util.SeasonResolver
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/pickem/nfl")
class NflPickemApiController(
    private val nflPickRepo: NflPickRepo,
    private val userNameCache: UserNameCache
) {
    data class WeekSummary(
        val week: Int,
        val total: Int,
        val correct: Int
    )

    data class UserSummary(
        val userId: Long,
        val displayName: String,
        val total: Int,
        val correct: Int,
        val points: Int
    )

    @GetMapping("/weekly")
    fun weekly(
        @RequestParam(required = false) season: Int?
    ): List<WeekSummary> {
        val yr = season ?: SeasonResolver.currentNflSeason()
        val picks: List<NflPick> = nflPickRepo.findAll().filter { it.season == yr }
        return picks
            .groupBy { it.week }
            .toSortedMap()
            .map { (week, list) ->
                WeekSummary(
                    week = week,
                    total = list.size,
                    correct = list.count { it.correctPick }
                )
            }
    }

    @GetMapping("/users")
    fun users(
        @RequestParam(required = false) season: Int?
    ): List<UserSummary> {
        val yr = season ?: SeasonResolver.currentNflSeason()
        val picks: List<NflPick> = nflPickRepo.findAll().filter { it.season == yr }
        return picks
            .groupBy { it.userId }
            .values
            .map { list ->
                val correct = list.count { it.correctPick }
                val total = list.size
                val uid = list.first().userId
                val name = userNameCache.getOrResolve(uid)
                UserSummary(
                    userId = uid,
                    displayName = name,
                    total = total,
                    correct = correct,
                    points = correct * 10
                )
            }
            .sortedByDescending { it.points }
    }

    @GetMapping("/weekUsers")
    fun weekUsers(
        @RequestParam(required = true) week: Int,
        @RequestParam(required = false) season: Int?
    ): List<UserSummary> {
        val yr = season ?: SeasonResolver.currentNflSeason()
        val picks: List<NflPick> = nflPickRepo.findAll().filter { it.season == yr && it.week == week }
        return picks
            .groupBy { it.userId }
            .values
            .map { list ->
                val correct = list.count { it.correctPick }
                val total = list.size
                val uid = list.first().userId
                val name = userNameCache.getOrResolve(uid)
                UserSummary(
                    userId = uid,
                    displayName = name,
                    total = total,
                    correct = correct,
                    points = correct * 10
                )
            }
            .sortedByDescending { it.points }
    }
}
