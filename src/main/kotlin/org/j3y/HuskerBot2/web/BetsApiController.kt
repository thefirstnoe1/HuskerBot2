package org.j3y.HuskerBot2.web

import org.j3y.HuskerBot2.model.BetEntity
import org.j3y.HuskerBot2.repository.BetRepo
import org.j3y.HuskerBot2.service.UserNameCache
import org.j3y.HuskerBot2.util.SeasonResolver
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bets")
class BetsApiController(
    private val betRepo: BetRepo,
    private val userNameCache: UserNameCache
) {
    data class WeekSummary(
        val week: Int,
        val total: Int,
        val correctWinner: Int,
        val correctPoints: Int,
        val correctSpread: Int
    )

    data class UserSummary(
        val userId: Long,
        val displayName: String,
        val total: Int,
        val correctWinner: Int,
        val correctPoints: Int,
        val correctSpread: Int
    )

    @GetMapping("/weekly")
    fun weekly(
        @RequestParam(required = false) season: Int?
    ): List<WeekSummary> {
        val yr = season ?: SeasonResolver.currentCfbSeason()
        val bets: List<BetEntity> = betRepo.findBySeason(yr)
        return bets
            .groupBy { it.week }
            .toSortedMap()
            .map { (week, list) ->
                WeekSummary(
                    week = week,
                    total = list.size,
                    correctWinner = list.count { it.correctWinner == true },
                    correctPoints = list.count { it.correctPoints == true },
                    correctSpread = list.count { it.correctSpread == true }
                )
            }
    }

    @GetMapping("/users")
    fun users(
        @RequestParam(required = false) season: Int?
    ): List<UserSummary> {
        val yr = season ?: SeasonResolver.currentCfbSeason()
        val bets: List<BetEntity> = betRepo.findBySeason(yr)
        return bets
            .groupBy { it.userId }
            .values
            .map { list ->
                val sample = list.first()
                val name = userNameCache.getOrResolve(sample.userId)
                UserSummary(
                    userId = sample.userId,
                    displayName = name,
                    total = list.size,
                    correctWinner = list.count { it.correctWinner == true },
                    correctPoints = list.count { it.correctPoints == true },
                    correctSpread = list.count { it.correctSpread == true }
                )
            }
            .sortedByDescending { it.correctWinner }
    }
}