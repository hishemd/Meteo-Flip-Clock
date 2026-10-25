package com.htcweather.flipclock.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.htcweather.flipclock.data.WeatherRepository
import com.htcweather.flipclock.data.local.AppDatabase
import com.htcweather.flipclock.data.model.CityEntity
import com.htcweather.flipclock.data.model.GeoResult
import com.htcweather.flipclock.utils.AppPreferences
import kotlinx.coroutines.launch

class CityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository(AppDatabase.getInstance(application).cityDao())
    private val prefs = AppPreferences(application)

    val cities: LiveData<List<CityEntity>> = repository.getAllCities().asLiveData()

    private val _searchResults = MutableLiveData<List<GeoResult>>()
    val searchResults: LiveData<List<GeoResult>> = _searchResults

    private val _isSearching = MutableLiveData(false)
    val isSearching: LiveData<Boolean> = _isSearching

    fun search(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            repository.searchCities(query, prefs.apiKey)
                .onSuccess { _searchResults.value = it }
                .onFailure { _searchResults.value = emptyList() }
            _isSearching.value = false
        }
    }

    fun addCity(geo: GeoResult) {
        viewModelScope.launch {
            val entity = CityEntity(
                name = geo.name,
                country = geo.country,
                lat = geo.lat,
                lon = geo.lon,
                isHome = false
            )
            repository.addCity(entity)
        }
    }

    fun deleteCity(city: CityEntity) {
        viewModelScope.launch {
            repository.deleteCity(city)
        }
    }
}
