package com.htcweather.flipclock.data

import com.htcweather.flipclock.data.local.CityDao
import com.htcweather.flipclock.data.model.*
import com.htcweather.flipclock.data.remote.RetrofitClient
import kotlinx.coroutines.flow.Flow

class WeatherRepository(private val cityDao: CityDao) {

    fun getAllCities(): Flow<List<CityEntity>> = cityDao.getAllCities()

    suspend fun getHomeCity(): CityEntity? = cityDao.getHomeCity()

    suspend fun addCity(city: CityEntity): Long = cityDao.insert(city)

    suspend fun deleteCity(city: CityEntity) = cityDao.delete(city)

    suspend fun fetchWeather(lat: Double, lon: Double, apiKey: String): Result<WeatherUiModel> {
        return try {
            val key = apiKey.ifBlank { RetrofitClient.DEFAULT_PLACEHOLDER }
            if (key == RetrofitClient.DEFAULT_PLACEHOLDER || key.isBlank()) {
                return Result.success(createDemoWeather())
            }
            val response = RetrofitClient.weatherApi.getCurrentWeather(
                lat = lat,
                lon = lon,
                apiKey = key
            )
            val condition = response.weather?.firstOrNull()
            val main = response.main
            val sys = response.sys

            if (main == null || condition == null) {
                return Result.failure(Exception("Données météo incomplètes"))
            }

            val model = WeatherUiModel(
                cityName = response.name ?: "Inconnu",
                temperature = main.temp,
                tempMin = main.tempMin,
                tempMax = main.tempMax,
                description = condition.description.replaceFirstChar { it.uppercase() },
                conditionId = condition.id,
                iconCode = condition.icon,
                humidity = main.humidity,
                windSpeed = (response.wind?.speed ?: 0.0) * 3.6,
                sunrise = sys?.sunrise ?: 0L,
                sunset = sys?.sunset ?: 0L,
                feelsLike = main.feelsLike,
                timestamp = response.dt ?: System.currentTimeMillis() / 1000
            )
            Result.success(model)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchCities(query: String, apiKey: String): Result<List<GeoResult>> {
        return try {
            val key = apiKey.ifBlank { RetrofitClient.DEFAULT_PLACEHOLDER }
            if (key == RetrofitClient.DEFAULT_PLACEHOLDER || key.isBlank()) {
                return Result.success(
                    listOf(
                        GeoResult("Paris", 48.8566, 2.3522, "FR", null),
                        GeoResult("Lyon", 45.7640, 4.8357, "FR", null),
                        GeoResult("Marseille", 43.2965, 5.3698, "FR", null),
                        GeoResult("Toulouse", 43.6047, 1.4442, "FR", null),
                        GeoResult("Nice", 43.7102, 7.2620, "FR", null),
                        GeoResult("Bruxelles", 50.8503, 4.3517, "BE", null),
                        GeoResult("Genève", 46.2044, 6.1432, "CH", null),
                        GeoResult("Casablanca", 33.5731, -7.5898, "MA", null)
                    ).filter { it.name.contains(query, ignoreCase = true) }
                )
            }
            val results = RetrofitClient.weatherApi.searchCity(query, apiKey = key)
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createDemoWeather(): WeatherUiModel {
        val now = System.currentTimeMillis() / 1000
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        // Vary demo weather by time of day for nicer wallpaper testing
        val (cond, desc, temp) = when {
            hour in 6..10 -> Triple(800, "Ciel dégagé", 22.0)
            hour in 11..16 -> Triple(801, "Peu nuageux", 28.0)
            hour in 17..20 -> Triple(500, "Pluie légère", 18.0)
            else -> Triple(800, "Nuit claire", 14.0)
        }
        return WeatherUiModel(
            cityName = "Maison",
            temperature = temp,
            tempMin = temp - 4,
            tempMax = temp + 4,
            description = desc,
            conditionId = cond,
            iconCode = "01d",
            humidity = 55,
            windSpeed = 10.0,
            sunrise = now - 3600 * 5,
            sunset = now + 3600 * 4,
            feelsLike = temp + 1,
            timestamp = now
        )
    }
}
