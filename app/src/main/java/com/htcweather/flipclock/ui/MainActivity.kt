package com.htcweather.flipclock.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.htcweather.flipclock.data.model.toWeatherType
import com.htcweather.flipclock.databinding.ActivityMainBinding
import com.htcweather.flipclock.viewmodel.MainViewModel
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupButtons()

        viewModel.loadWeather()
    }

    private fun setupObservers() {
        viewModel.weather.observe(this, Observer { weather ->
            weather ?: return@Observer
            binding.tvCityName.text = weather.cityName
            binding.tvTemperature.text = String.format(Locale.FRENCH, "%.1f°C", weather.temperature)
            binding.tvHighLow.text = String.format(
                Locale.FRENCH,
                "%.1f° / %.1f°",
                weather.tempMax,
                weather.tempMin
            )
            binding.tvDate.text = viewModel.formatDate()
            binding.tvSunrise.text = "☀  ${viewModel.formatTime(weather.sunrise)}"
            binding.tvSunset.text = "☾  ${viewModel.formatTime(weather.sunset)}"

            binding.weatherIcon.setWeather(weather.conditionId.toWeatherType())
        })

        viewModel.weatherType.observe(this, Observer { type ->
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isDay = hour in 6..19
            binding.weatherBackground.setWeather(type, isDay)
        })

        viewModel.isLoading.observe(this, Observer { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        })

        viewModel.error.observe(this, Observer { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupButtons() {
        binding.btnCities.setOnClickListener {
            startActivity(Intent(this, CityListActivity::class.java))
        }
        binding.btnRefresh.setOnClickListener {
            viewModel.loadWeather()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload in case a city was added/selected
        viewModel.loadWeather()
    }
}
