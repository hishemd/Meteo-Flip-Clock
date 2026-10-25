package com.htcweather.flipclock.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.htcweather.flipclock.data.model.toWeatherType
import com.htcweather.flipclock.databinding.ActivityMainBinding
import com.htcweather.flipclock.utils.AppPreferences
import com.htcweather.flipclock.viewmodel.MainViewModel
import java.util.*
import android.content.res.Configuration
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var prefs: AppPreferences

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            viewModel.loadWeather(forceGps = true)
        } else {
            viewModel.loadWeather()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = AppPreferences(this)
        applySavedLocale()

        setupObservers()
        setupButtons()
        requestLocationIfNeeded()
    }

    private fun requestLocationIfNeeded() {
        if (!prefs.useGps) {
            viewModel.loadWeather()
            return
        }
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            viewModel.loadWeather(forceGps = true)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun setupObservers() {
        viewModel.weather.observe(this, Observer { weather ->
            weather ?: return@Observer
            binding.tvCityName.text = weather.cityName
            binding.tvTemperature.text = viewModel.formatTemp(weather.temperature)
            binding.tvHighLow.text =
                "${viewModel.formatTemp(weather.tempMax)} / ${viewModel.formatTemp(weather.tempMin)}"
            binding.tvDate.text = viewModel.formatDate()
            binding.tvSunrise.text = "☀  ${viewModel.formatTime(weather.sunrise)}"
            binding.tvSunset.text = "☾  ${viewModel.formatTime(weather.sunset)}"
            binding.weatherIcon.setWeather(weather.conditionId.toWeatherType())
        })

        viewModel.weatherType.observe(this, Observer { type ->
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            binding.weatherBackground.setWeather(type, hour in 6..19)
        })

        viewModel.isLoading.observe(this, Observer { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        })

        viewModel.error.observe(this, Observer { msg ->
            msg?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        })
    }

    private fun setupButtons() {
        binding.btnCities.setOnClickListener {
            startActivity(Intent(this, CityListActivity::class.java))
        }
        binding.btnRefresh.setOnClickListener {
            viewModel.loadWeather(forceGps = prefs.useGps)
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.weatherCard.setOnClickListener {
            startActivity(Intent(this, DetailsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (::prefs.isInitialized) {
            viewModel.loadWeather(forceGps = prefs.useGps)
        }
    }

    private fun applySavedLocale() {
        val lang = prefs.language
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
