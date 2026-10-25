package com.htcweather.flipclock.data.remote

import com.htcweather.flipclock.data.model.CurrentWeatherResponse
import com.htcweather.flipclock.data.model.GeoResult
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "fr",
        @Query("appid") apiKey: String
    ): CurrentWeatherResponse

    @GET("geo/1.0/direct")
    suspend fun searchCity(
        @Query("q") query: String,
        @Query("limit") limit: Int = 8,
        @Query("appid") apiKey: String
    ): List<GeoResult>
}
