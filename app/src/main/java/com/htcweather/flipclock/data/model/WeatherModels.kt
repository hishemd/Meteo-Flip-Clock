package com.htcweather.flipclock.data.model

import com.google.gson.annotations.SerializedName

// --- OpenWeatherMap responses ---

data class CurrentWeatherResponse(
    val coord: Coord?,
    val weather: List<WeatherCondition>?,
    val main: MainWeather?,
    val wind: Wind?,
    val sys: Sys?,
    val name: String?,
    val dt: Long?,
    val timezone: Int?
)

data class Coord(
    val lon: Double,
    val lat: Double
)

data class WeatherCondition(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class MainWeather(
    val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("temp_max") val tempMax: Double,
    val humidity: Int,
    val pressure: Int
)

data class Wind(
    val speed: Double,
    val deg: Int?
)

data class Sys(
    val country: String?,
    val sunrise: Long?,
    val sunset: Long?
)

// Geocoding search
data class GeoResult(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String?,
    val state: String?
)

// Local domain model used by the UI
data class WeatherUiModel(
    val cityName: String,
    val temperature: Double,
    val tempMin: Double,
    val tempMax: Double,
    val description: String,
    val conditionId: Int,          // OpenWeather condition id
    val iconCode: String,
    val humidity: Int,
    val windSpeed: Double,
    val sunrise: Long,
    val sunset: Long,
    val feelsLike: Double,
    val timestamp: Long
)

enum class WeatherType {
    CLEAR, CLOUDS, RAIN, DRIZZLE, THUNDERSTORM, SNOW, MIST, UNKNOWN
}

fun Int.toWeatherType(): WeatherType = when (this) {
    in 200..232 -> WeatherType.THUNDERSTORM
    in 300..321 -> WeatherType.DRIZZLE
    in 500..531 -> WeatherType.RAIN
    in 600..622 -> WeatherType.SNOW
    in 701..781 -> WeatherType.MIST
    800 -> WeatherType.CLEAR
    in 801..804 -> WeatherType.CLOUDS
    else -> WeatherType.UNKNOWN
}
