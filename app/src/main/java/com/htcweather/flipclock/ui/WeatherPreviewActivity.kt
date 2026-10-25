package com.htcweather.flipclock.ui

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.htcweather.flipclock.R
import com.htcweather.flipclock.animation.WeatherBackgroundView
import com.htcweather.flipclock.data.model.WeatherType

/**
 * Offline gallery of all weather background modes (HTC Sense style).
 */
class WeatherPreviewActivity : AppCompatActivity() {

    private lateinit var background: WeatherBackgroundView
    private lateinit var tvName: TextView
    private lateinit var tvIndex: TextView
    private lateinit var modesContainer: LinearLayout

    private var index = 0
    private var isDay = true

    private data class Mode(val type: WeatherType, val nameFr: String, val nameAr: String, val nameEn: String)

    private val modes = listOf(
        Mode(WeatherType.CLEAR, "Ciel dégagé / Soleil", "صافٍ / شمس", "Clear / Sunny"),
        Mode(WeatherType.CLOUDS, "Nuageux", "غيوم", "Cloudy"),
        Mode(WeatherType.MIST, "Brouillard / Brume", "ضباب", "Fog / Mist"),
        Mode(WeatherType.DRIZZLE, "Bruine", "رذاذ", "Drizzle"),
        Mode(WeatherType.RAIN, "Pluie", "مطر", "Rain"),
        Mode(WeatherType.THUNDERSTORM, "Orage", "عاصفة رعدية", "Thunderstorm"),
        Mode(WeatherType.SNOW, "Neige", "ثلج", "Snow"),
        Mode(WeatherType.UNKNOWN, "Vent", "رياح", "Wind")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_preview)

        background = findViewById(R.id.previewBackground)
        tvName = findViewById(R.id.tvModeName)
        tvIndex = findViewById(R.id.tvModeIndex)
        modesContainer = findViewById(R.id.modesContainer)

        findViewById<MaterialButton>(R.id.btnPrev).setOnClickListener {
            index = if (index == 0) modes.lastIndex else index - 1
            applyMode()
        }
        findViewById<MaterialButton>(R.id.btnNext).setOnClickListener {
            index = (index + 1) % modes.size
            applyMode()
        }
        findViewById<MaterialButton>(R.id.btnDayNight).setOnClickListener {
            isDay = !isDay
            applyMode()
        }

        buildChips()
        applyMode()
    }

    private fun buildChips() {
        modesContainer.removeAllViews()
        modes.forEachIndexed { i, mode ->
            val chip = MaterialButton(this).apply {
                text = when (i) {
                    0 -> "☀️"
                    1 -> "☁️"
                    2 -> "🌫️"
                    3 -> "🌦️"
                    4 -> "🌧️"
                    5 -> "⛈️"
                    6 -> "❄️"
                    else -> "💨"
                }
                textSize = 18f
                minimumWidth = 0
                minWidth = 0
                setPadding(24, 8, 24, 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 8 }
                setOnClickListener {
                    index = i
                    applyMode()
                }
            }
            modesContainer.addView(chip)
        }
    }

    private fun applyMode() {
        val mode = modes[index]
        background.setWeather(mode.type, isDay)
        val dayNight = if (isDay) "☀ Jour" else "☾ Nuit"
        tvName.text = "${mode.nameFr}\n${mode.nameAr}"
        tvIndex.text = "${index + 1} / ${modes.size}  •  $dayNight  •  Hors ligne"
        // highlight chip
        for (i in 0 until modesContainer.childCount) {
            val btn = modesContainer.getChildAt(i) as? MaterialButton
            btn?.alpha = if (i == index) 1f else 0.5f
        }
    }
}
