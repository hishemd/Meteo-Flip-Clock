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
            val key = apiKey.ifBlank { RetrofitClient.DEFAULT_API_KEY }
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
                pressure = main.pressure,
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
            val key = apiKey.ifBlank { RetrofitClient.DEFAULT_API_KEY }
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
            pressure = 1015,
            sunrise = now - 3600 * 5,
            sunset = now + 3600 * 4,
            feelsLike = temp + 1,
            timestamp = now
        )
    }

    suspend fun fetchDetails(lat: Double, lon: Double, apiKey: String): Result<WeatherDetailsUi> {
        return try {
            val key = apiKey.ifBlank { RetrofitClient.DEFAULT_API_KEY }
            if (key == RetrofitClient.DEFAULT_PLACEHOLDER || key.isBlank()) {
                return Result.success(demoDetails())
            }
            val current = RetrofitClient.weatherApi.getCurrentWeather(lat, lon, apiKey = key)
            val forecast = RetrofitClient.weatherApi.getForecast(lat, lon, apiKey = key)
            val main = current.main
            val sys = current.sys
            val list = forecast.list.orEmpty()

            // Hourly: next 8 slots (3h steps from OWM free)
            val hourly = list.take(8).map { item ->
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = item.dt * 1000 }
                HourlyForecastUi(
                    hourLabel = String.format("%02d:00", cal.get(java.util.Calendar.HOUR_OF_DAY)),
                    iconCode = item.weather?.firstOrNull()?.icon ?: "01d",
                    conditionId = item.weather?.firstOrNull()?.id ?: 800,
                    temp = item.main?.temp ?: 0.0,
                    popPercent = ((item.pop ?: 0.0) * 100).toInt()
                )
            }

            // Daily: group by day
            val byDay = linkedMapOf<String, MutableList<com.htcweather.flipclock.data.model.ForecastItem>>()
            val dayFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val labelFmt = java.text.SimpleDateFormat("EEE", java.util.Locale.FRENCH)
            val dateFmt = java.text.SimpleDateFormat("d/MM", java.util.Locale.FRENCH)
            for (item in list) {
                val keyDay = dayFmt.format(java.util.Date(item.dt * 1000))
                byDay.getOrPut(keyDay) { mutableListOf() }.add(item)
            }
            val daily = byDay.entries.take(6).map { (dayKey, items) ->
                val first = items.first()
                val maxT = items.mapNotNull { it.main?.tempMax }.maxOrNull() ?: first.main?.tempMax ?: 0.0
                val minT = items.mapNotNull { it.main?.tempMin }.minOrNull() ?: first.main?.tempMin ?: 0.0
                val pop = ((items.mapNotNull { it.pop }.maxOrNull() ?: 0.0) * 100).toInt()
                val cond = items.firstOrNull()?.weather?.firstOrNull()
                val d = java.util.Date(first.dt * 1000)
                DailyForecastUi(
                    dayLabel = labelFmt.format(d).replaceFirstChar { it.uppercase() },
                    dateLabel = dateFmt.format(d),
                    iconCode = cond?.icon ?: "01d",
                    conditionId = cond?.id ?: 800,
                    tempMax = maxT,
                    tempMin = minT,
                    popPercent = pop
                )
            }

            val details = WeatherDetailsUi(
                humidity = main?.humidity ?: 0,
                windSpeed = (current.wind?.speed ?: 0.0) * 3.6,
                pressure = main?.pressure ?: 0,
                visibilityKm = (current.visibility?.toDouble() ?: 10000.0) / 1000.0,
                feelsLike = main?.feelsLike ?: 0.0,
                sunrise = sys?.sunrise ?: 0L,
                sunset = sys?.sunset ?: 0L,
                description = current.weather?.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "",
                daily = daily,
                hourly = hourly
            )
            Result.success(details)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun demoDetails(): WeatherDetailsUi {
        val now = System.currentTimeMillis() / 1000
        val hourly = (0 until 6).map { i ->
            HourlyForecastUi("%02d:00".format((22 + i) % 24), "01n", 800, 18.0 - i * 0.2, 5)
        }
        val days = listOf("Auj.", "Lun.", "Mar.", "Mer.", "Jeu.", "Ven.")
        val daily = days.mapIndexed { i, d ->
            DailyForecastUi(d, "%02d/10".format(25 + i), if (i % 2 == 0) "02d" else "01d", 801, 25.0 - i, 16.0 + i % 3, 5 + i * 3)
        }
        return WeatherDetailsUi(72, 6.0, 1019, 37.1, 19.0, now - 3600 * 4, now + 3600 * 5, "Nuageux", daily, hourly)
    }

}
