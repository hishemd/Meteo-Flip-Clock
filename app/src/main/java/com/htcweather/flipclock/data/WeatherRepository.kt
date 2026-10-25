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

    suspend fun setAsHome(cityId: Long) {
        // simple approach: clear previous home then set new one
        val all = cityDao.getAllCities()
        // We will handle in ViewModel for simplicity
    }

    suspend fun fetchWeather(lat: Double, lon: Double): Result<WeatherUiModel> {
        return try {
            if (RetrofitClient.API_KEY == "YOUR_OPENWEATHERMAP_API_KEY") {
                // Demo mode – return fake sunny data so the UI works without a key
                return Result.success(createDemoWeather())
            }
            val response = RetrofitClient.weatherApi.getCurrentWeather(
                lat = lat,
                lon = lon,
                apiKey = RetrofitClient.API_KEY
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
                windSpeed = (response.wind?.speed ?: 0.0) * 3.6, // m/s → km/h
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

    suspend fun searchCities(query: String): Result<List<GeoResult>> {
        return try {
            if (RetrofitClient.API_KEY == "YOUR_OPENWEATHERMAP_API_KEY") {
                // Demo results
                return Result.success(
                    listOf(
                        GeoResult("Paris", 48.8566, 2.3522, "FR", null),
                        GeoResult("Lyon", 45.7640, 4.8357, "FR", null),
                        GeoResult("Marseille", 43.2965, 5.3698, "FR", null),
                        GeoResult("Toulouse", 43.6047, 1.4442, "FR", null),
                        GeoResult("Nice", 43.7102, 7.2620, "FR", null)
                    ).filter { it.name.contains(query, ignoreCase = true) }
                )
            }
            val results = RetrofitClient.weatherApi.searchCity(query, apiKey = RetrofitClient.API_KEY)
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createDemoWeather(): WeatherUiModel {
        val now = System.currentTimeMillis() / 1000
        return WeatherUiModel(
            cityName = "Maison",
            temperature = 27.8,
            tempMin = 24.9,
            tempMax = 31.1,
            description = "Ciel dégagé",
            conditionId = 800,
            iconCode = "01d",
            humidity = 45,
            windSpeed = 12.0,
            sunrise = now - 3600 * 4,
            sunset = now + 3600 * 5,
            feelsLike = 28.5,
            timestamp = now
        )
    }
}
