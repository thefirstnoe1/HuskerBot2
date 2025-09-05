package org.j3y.HuskerBot2.service

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Service
class RivalsService {
    private val url = "https://www.on3.com/_next/data/B6o5wzgOQ5dNJQbbZj248/rivals/search.json?searchText={name}&minClassYear={year}&maxClassYear={year}&sportKey=1"
    private val client = RestTemplate()

    data class RecruitData(
        val year: Int,
        val firstName: String,
        val lastName: String,
        val hometown: String,
        val highSchool: String,
        val height: String,
        val weight: String,
        val position: String,
        val status: String,
        val commitTeam: String,
        val commitTeamImageUrl: String? = null,
        val starRating: Int,
        val nationalRank: Int,
        val positionRank: Int,
        val stateRank: Int,
        val imageUrl: String? = null,
        val link: String
    )

    fun getRecruitData(year: Int, name: String): RecruitData? {
        val uri = UriComponentsBuilder.fromUriString(url).buildAndExpand(name, year, year).toUri()
        val response = client.getForObject(uri, JsonNode::class.java)

        val recruit = response?.at("/pageProps/searchData/list/0") ?: return null
        val rating = recruit.path("rating")

        val link = "https://www.on3.com/rivals/${recruit.path("slug").asText()}/"

        return RecruitData(
            year = recruit.path("classYear").asInt(),
            firstName = recruit.path("firstName").asText("N/A"),
            lastName = recruit.path("lastName").asText("N/A"),
            hometown = recruit.path("homeTownName").asText("N/A"),
            highSchool = recruit.path("highSchoolName").asText("N/A"),
            height = recruit.path("formattedHeight").asText("N/A"),
            weight = recruit.path("weight").asText("N/A"),
            position = recruit.path("position").path("name").asText("N/A"),
            status = recruit.path("status").path("type").asText("N/A"),
            commitTeam = recruit.path("status").path("committedOrganization").path("fullName").asText("N/A"),
            commitTeamImageUrl = recruit.path("status").path("committedOrganization").path("assetUrl").asText(null),
            starRating = rating.path("consensusStars").asInt(),
            nationalRank = rating.path("consensusNationalRank").asInt(),
            positionRank = rating.path("consensusPositionRank").asInt(),
            stateRank = rating.path("consensusStateRank").asInt(),
            imageUrl = recruit.path("defaultAssetUrl").asText(null),
            link = link
        )
    }
}