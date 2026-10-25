package com.htcweather.flipclock.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.htcweather.flipclock.data.WeatherRepository
import com.htcweather.flipclock.data.local.AppDatabase
import com.htcweather.flipclock.data.model.CityEntity
import com.htcweather.flipclock.data.model.WeatherType
import com.htcweather.flipclock.data.model.WeatherUiModel
import com.htcweather.flipclock.data.model.toWeatherType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository(AppDatabase.getInstance(application).cityDao())

    private val _weather = MutableLiveData<WeatherUiModel?>()
    val weather: LiveData<WeatherUiModel?> = _weather

    private val _weatherType = MutableLiveData(WeatherType.CLEAR)
    val weatherType: LiveData<WeatherType> = _weatherType

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _currentCity = MutableLiveData<CityEntity?>()
    val currentCity: LiveData<CityEntity?> = _currentCity

    private var selectedCityId: Long? = null

    fun loadWeather(cityId: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val cities = repository.getAllCities().first()
                val city = when {
                    cityId != null -> cities.find { it.id == cityId }
                    selectedCityId != null -> cities.find { it.id == selectedCityId }
                    else -> cities.find { it.isHome } ?: cities.firstOrNull()
                }

                if (city == null) {
                    // First launch – create a default home city (Paris as demo)
                    val default = CityEntity(
                        name = "Maison",
                        country = "FR",
                        lat = 48.8566,
                        lon = 2.3522,
                        isHome = true
                    )
                    val id = repository.addCity(default)
                    selectedCityId = id
                    _currentCity.value = default.copy(id = id)
                    fetchFor(default.lat, default.lon, default.name)
                } else {
                    selectedCityId = city.id
                    _currentCity.value = city
                    fetchFor(city.lat, city.lon, city.name)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Erreur"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchFor(lat: Double, lon: Double, fallbackName: String) {
        val result = repository.fetchWeather(lat, lon)
        result.onSuccess { model ->
            val display = if (fallbackName == "Maison") model.copy(cityName = "Maison") else model
            _weather.value = display
            _weatherType.value = display.conditionId.toWeatherType()
        }.onFailure {
            _error.value = it.message ?: "Impossible de récupérer la météo"
            // Keep previous weather if any
        }
    }

    fun selectCity(city: CityEntity) {
        selectedCityId = city.id
        loadWeather(city.id)
    }

    fun formatTime(unix: Long): String {
        if (unix == 0L) return "--:--"
        val sdf = SimpleDateFormat("H:mm", Locale.FRENCH)
        return sdf.format(Date(unix * 1000))
    }

    fun formatDate(): String {
        val sdf = SimpleDateFormat("EEE d MMM", Locale.FRENCH)
        return sdf.format(Date()).replaceFirstChar { it.uppercase() }
    }
}
