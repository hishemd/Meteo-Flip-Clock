package com.htcweather.flipclock.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.htcweather.flipclock.databinding.ActivitySettingsBinding
import com.htcweather.flipclock.utils.AppPreferences
import com.htcweather.flipclock.wallpaper.WeatherWallpaperService
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPreferences(this)
        loadCurrent()
        wireActions()
    }

    private fun loadCurrent() {
        binding.switchDynBg.isChecked = prefs.dynamicBackground
        binding.switchFlipSound.isChecked = prefs.flipSound
        binding.switchGps.isChecked = prefs.useGps
        updateUnitButtons(prefs.useCelsius)
        updateFinishButtons(prefs.flipFinish)

        val city = prefs.lastCityName
        val temp = String.format(Locale.FRENCH, "%.0f°C", prefs.lastTemp)
        binding.tvWidgetPreview1.text = "10:48 • $city $temp"
    }

    private fun wireActions() {
        // Instant apply toggles
        binding.switchDynBg.setOnCheckedChangeListener { _, checked ->
            prefs.dynamicBackground = checked
        }
        binding.switchFlipSound.setOnCheckedChangeListener { _, checked ->
            prefs.flipSound = checked
        }
        binding.switchGps.setOnCheckedChangeListener { _, checked ->
            prefs.useGps = checked
        }

        binding.btnUnitC.setOnClickListener {
            prefs.useCelsius = true
            updateUnitButtons(true)
        }
        binding.btnUnitF.setOnClickListener {
            prefs.useCelsius = false
            updateUnitButtons(false)
        }

        binding.btnFinishWhite.setOnClickListener {
            prefs.flipFinish = 0
            updateFinishButtons(0)
            Toast.makeText(this, "Finition : Blanc nacré", Toast.LENGTH_SHORT).show()
        }
        binding.btnFinishBlack.setOnClickListener {
            prefs.flipFinish = 1
            updateFinishButtons(1)
            Toast.makeText(this, "Finition : Noir carbone", Toast.LENGTH_SHORT).show()
        }
        binding.btnFinishFrost.setOnClickListener {
            prefs.flipFinish = 2
            updateFinishButtons(2)
            Toast.makeText(this, "Finition : Givré 3D", Toast.LENGTH_SHORT).show()
        }

        binding.btnThemeRain.setOnClickListener {
            startActivity(Intent(this, WeatherPreviewActivity::class.java))
        }
        binding.btnThemeSun.setOnClickListener {
            startActivity(Intent(this, WeatherPreviewActivity::class.java))
        }
        binding.btnThemeMist.setOnClickListener {
            startActivity(Intent(this, WeatherPreviewActivity::class.java))
        }

        binding.btnSetWallpaper.setOnClickListener { openLiveWallpaperPicker() }
        binding.btnPreviewWeather.setOnClickListener {
            startActivity(Intent(this, WeatherPreviewActivity::class.java))
        }
        binding.btnSave.setOnClickListener {
            Toast.makeText(this, getString(com.htcweather.flipclock.R.string.settings_saved), Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.navHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
        }
        binding.navMeteo.setOnClickListener {
            startActivity(Intent(this, DetailsActivity::class.java))
        }
        binding.navCities.setOnClickListener {
            startActivity(Intent(this, CityListActivity::class.java))
        }
    }

    private fun updateUnitButtons(celsius: Boolean) {
        binding.btnUnitC.setBackgroundColor(if (celsius) 0xFF4FC3F7.toInt() else 0xFF37474F.toInt())
        binding.btnUnitF.setBackgroundColor(if (!celsius) 0xFF4FC3F7.toInt() else 0xFF37474F.toInt())
    }

    private fun updateFinishButtons(finish: Int) {
        fun tint(btn: MaterialButton, selected: Boolean) {
            btn.setBackgroundColor(if (selected) 0xFF4FC3F7.toInt() else 0xFF37474F.toInt())
        }
        tint(binding.btnFinishWhite, finish == 0)
        tint(binding.btnFinishBlack, finish == 1)
        tint(binding.btnFinishFrost, finish == 2)
    }

    private fun openLiveWallpaperPicker() {
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@SettingsActivity, WeatherWallpaperService::class.java)
                )
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            } catch (e2: Exception) {
                Toast.makeText(this, "Paramètres système → Fond d'écran animé", Toast.LENGTH_LONG).show()
            }
        }
    }
}
