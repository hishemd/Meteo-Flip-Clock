package com.htcweather.flipclock.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.htcweather.flipclock.R
import com.htcweather.flipclock.data.WeatherRepository
import com.htcweather.flipclock.data.local.AppDatabase
import com.htcweather.flipclock.data.model.WeatherType
import com.htcweather.flipclock.data.model.toWeatherType
import com.htcweather.flipclock.databinding.ActivityDetailsBinding
import com.htcweather.flipclock.utils.AppPreferences
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = AppPreferences(this)
        val repo = WeatherRepository(AppDatabase.getInstance(this).cityDao())

        binding.tvDetailsCity.text = "📍 ${prefs.lastCityName}"

        lifecycleScope.launch {
            val result = repo.fetchDetails(
                prefs.lastLat.toDouble(),
                prefs.lastLon.toDouble(),
                prefs.apiKey
            )
            result.onSuccess { d ->
                binding.tvHumidity.text = "${d.humidity} %"
                binding.tvWind.text = String.format(Locale.FRENCH, "%.0f km/h", d.windSpeed)
                binding.tvPressure.text = "${d.pressure}"
                binding.tvVisibility.text = String.format(Locale.FRENCH, "%.1f km", d.visibilityKm)
                binding.tvFeelsLike.text = String.format(Locale.FRENCH, "%.1f°", d.feelsLike)
                val tf = SimpleDateFormat("HH:mm", Locale.FRENCH)
                val sr = if (d.sunrise > 0) tf.format(Date(d.sunrise * 1000)) else "--"
                val ss = if (d.sunset > 0) tf.format(Date(d.sunset * 1000)) else "--"
                binding.tvSunTimes.text = "$sr  →  $ss"

                binding.dailyContainer.removeAllViews()
                d.daily.forEach { day ->
                    val v = LayoutInflater.from(this@DetailsActivity)
                        .inflate(R.layout.item_daily, binding.dailyContainer, false)
                    v.findViewById<TextView>(R.id.tvDay).text = day.dayLabel
                    v.findViewById<TextView>(R.id.tvDate).text = day.dateLabel
                    v.findViewById<TextView>(R.id.tvIcon).text = iconEmoji(day.conditionId)
                    v.findViewById<TextView>(R.id.tvMax).text = "${day.tempMax.toInt()}°"
                    v.findViewById<TextView>(R.id.tvMin).text = "${day.tempMin.toInt()}°"
                    v.findViewById<TextView>(R.id.tvPop).text = "${day.popPercent} %"
                    binding.dailyContainer.addView(v)
                }

                binding.hourlyContainer.removeAllViews()
                d.hourly.forEach { h ->
                    val v = LayoutInflater.from(this@DetailsActivity)
                        .inflate(R.layout.item_hourly, binding.hourlyContainer, false)
                    v.findViewById<TextView>(R.id.tvHour).text = h.hourLabel
                    v.findViewById<TextView>(R.id.tvIcon).text = iconEmoji(h.conditionId)
                    v.findViewById<TextView>(R.id.tvTemp).text = "${h.temp.toInt()}°"
                    binding.hourlyContainer.addView(v)
                }
            }
        }
    }

    private fun iconEmoji(id: Int): String = when (id.toWeatherType()) {
        WeatherType.CLEAR -> "☀️"
        WeatherType.CLOUDS -> "☁️"
        WeatherType.RAIN, WeatherType.DRIZZLE -> "🌧️"
        WeatherType.THUNDERSTORM -> "⛈️"
        WeatherType.SNOW -> "❄️"
        WeatherType.MIST -> "🌫️"
        else -> "🌤️"
    }
}
