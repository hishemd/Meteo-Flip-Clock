package com.htcweather.flipclock.data.model

import com.google.gson.annotations.SerializedName

// --- OpenWeatherMap responses ---

data class CurrentWeatherResponse(
    val coord: Coord?,
    val weather: List<WeatherCondition>?,
    val main: MainWeather?,
    val wind: Wind?,
    val visibility: Int?,
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

// --- Forecast ---
data class ForecastResponse(
    val list: List<ForecastItem>?,
    val city: ForecastCity?
)

data class ForecastCity(
    val name: String?,
    val sunrise: Long?,
    val sunset: Long?
)

data class ForecastItem(
    val dt: Long,
    val main: MainWeather?,
    val weather: List<WeatherCondition>?,
    val pop: Double?, // probability of precipitation 0-1
    val wind: Wind?,
    val visibility: Int?,
    val dt_txt: String?
)

data class DailyForecastUi(
    val dayLabel: String,
    val dateLabel: String,
    val iconCode: String,
    val conditionId: Int,
    val tempMax: Double,
    val tempMin: Double,
    val popPercent: Int
)

data class HourlyForecastUi(
    val hourLabel: String,
    val iconCode: String,
    val conditionId: Int,
    val temp: Double,
    val popPercent: Int
)

data class WeatherDetailsUi(
    val humidity: Int,
    val windSpeed: Double,
    val pressure: Int,
    val visibilityKm: Double,
    val feelsLike: Double,
    val sunrise: Long,
    val sunset: Long,
    val description: String,
    val daily: List<DailyForecastUi>,
    val hourly: List<HourlyForecastUi>
)
