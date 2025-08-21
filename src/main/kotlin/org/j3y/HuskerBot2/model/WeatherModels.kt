package org.j3y.HuskerBot2.model

data class WeatherForecast(
    val temperature: Int,
    val shortForecast: String,
    val detailedForecast: String,
    val windSpeed: String,
    val windDirection: String,
    val humidity: String?
)

data class GeocodingResult(
    val latitude: Double,
    val longitude: Double
)