package com.htcweather.flipclock.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import com.htcweather.flipclock.data.model.WeatherType
import com.htcweather.flipclock.data.model.toWeatherType
import com.htcweather.flipclock.databinding.ActivityMainBinding
import com.htcweather.flipclock.utils.AppPreferences
import com.htcweather.flipclock.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var prefs: AppPreferences
    private var manualWeatherOverride: WeatherType? = null
    private var flipDemoRunning = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        viewModel.loadWeather(forceGps = granted && prefs.useGps)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = AppPreferences(this)
        applySavedLocale()

        setupObservers()
        setupButtons()
        setupWeatherModeChips()
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
            binding.tvCityName.text = "📍 ${weather.cityName}"
            binding.tvTemperature.text = viewModel.formatTemp(weather.temperature)
            binding.tvHighLow.text = "${viewModel.formatTemp(weather.tempMax)} / ${viewModel.formatTemp(weather.tempMin)}"
            binding.tvDate.text = viewModel.formatDate()
            binding.tvDescription.text = weather.description
            binding.tvSunrise.text = "☀ ${viewModel.formatTime(weather.sunrise)}"
            binding.tvSunset.text = "☾ ${viewModel.formatTime(weather.sunset)}"
            binding.tvWindRow.text = String.format(Locale.FRENCH, "💨 %.0f km/h", weather.windSpeed)
            binding.tvPressureRow.text = "—" // filled if available later
            binding.tvUpdate.text = "MAJ " + SimpleDateFormat("HH:mm", Locale.FRENCH).format(Date())
            binding.weatherIcon.setWeather(weather.conditionId.toWeatherType())
        })

        viewModel.weatherType.observe(this, Observer { type ->
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isDay = hour in 6..19
            val show = manualWeatherOverride ?: type
            binding.weatherBackground.setWeather(show, isDay)
        })

        viewModel.isLoading.observe(this, Observer { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        })

        viewModel.error.observe(this, Observer { msg ->
            msg?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        })
    }

    private fun setupButtons() {
        binding.weatherCard.setOnClickListener {
            startActivity(Intent(this, DetailsActivity::class.java))
        }
        binding.btnRefresh.setOnClickListener {
            manualWeatherOverride = null
            viewModel.loadWeather(forceGps = prefs.useGps)
        }
        binding.btnQuickDetails.setOnClickListener {
            startActivity(Intent(this, DetailsActivity::class.java))
        }
        binding.btnQuickPreview.setOnClickListener {
            startActivity(Intent(this, WeatherPreviewActivity::class.java))
        }
        binding.btnFlipDemo.setOnClickListener {
            // trigger visual refresh of clock by setting time again
            val cal = Calendar.getInstance()
            binding.flipClock.setTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), animate = true)
            Toast.makeText(this, "Flip !", Toast.LENGTH_SHORT).show()
        }
        binding.btnFlipLoop.setOnClickListener {
            flipDemoRunning = !flipDemoRunning
            if (flipDemoRunning) {
                Toast.makeText(this, "Démo continue…", Toast.LENGTH_SHORT).show()
                runFlipDemo()
            }
        }
        binding.navCities.setOnClickListener {
            startActivity(Intent(this, CityListActivity::class.java))
        }
        binding.navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.navMeteo.setOnClickListener {
            startActivity(Intent(this, DetailsActivity::class.java))
        }
        binding.navHome.setOnClickListener { /* already home */ }
    }

    private fun runFlipDemo() {
        if (!flipDemoRunning) return
        val cal = Calendar.getInstance()
        var m = cal.get(Calendar.MINUTE)
        var h = cal.get(Calendar.HOUR_OF_DAY)
        m = (m + 1) % 60
        if (m == 0) h = (h + 1) % 24
        binding.flipClock.setTime(h, m, animate = true)
        binding.root.postDelayed({
            if (flipDemoRunning) {
                // restore real time after a few flips
                val now = Calendar.getInstance()
                binding.flipClock.setTime(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), animate = true)
                flipDemoRunning = false
            }
        }, 2500)
    }

    private fun setupWeatherModeChips() {
        val modes = listOf(
            "🌧️ Pluie" to WeatherType.RAIN,
            "⚡ Orage" to WeatherType.THUNDERSTORM,
            "☀️ Soleil" to WeatherType.CLEAR,
            "☁️ Nuages" to WeatherType.CLOUDS,
            "🌫️ Brume" to WeatherType.MIST,
            "❄️ Neige" to WeatherType.SNOW
        )
        binding.weatherModesRow.removeAllViews()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isDay = hour in 6..19
        modes.forEach { (label, type) ->
            val btn = MaterialButton(this).apply {
                text = label
                textSize = 11f
                isAllCaps = false
                minimumWidth = 0
                minWidth = 0
                setPadding(28, 8, 28, 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 10 }
                setOnClickListener {
                    manualWeatherOverride = type
                    binding.weatherBackground.setWeather(type, isDay)
                }
            }
            binding.weatherModesRow.addView(btn)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::prefs.isInitialized) {
            binding.flipClock.applyFinish(prefs.flipFinish)
            binding.weatherBackground.visibility =
                if (prefs.dynamicBackground) android.view.View.VISIBLE else android.view.View.GONE
            viewModel.loadWeather(forceGps = prefs.useGps)
        }
    }

    private fun applySavedLocale() {
        val lang = prefs.language
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
