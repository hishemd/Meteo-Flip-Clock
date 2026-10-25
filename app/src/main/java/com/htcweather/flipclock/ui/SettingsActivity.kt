package com.htcweather.flipclock.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.htcweather.flipclock.databinding.ActivitySettingsBinding
import com.htcweather.flipclock.utils.AppPreferences
import com.htcweather.flipclock.wallpaper.WeatherWallpaperService

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: AppPreferences

    private val refreshOptions = listOf(15, 30, 60, 120)
    private val refreshLabels = listOf("15 min", "30 min", "1 heure", "2 heures")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPreferences(this)
        loadCurrent()

        binding.spinnerRefresh.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, refreshLabels
        )
        val idx = refreshOptions.indexOf(prefs.autoRefreshMinutes).coerceAtLeast(0)
        binding.spinnerRefresh.setSelection(idx)

        binding.btnSave.setOnClickListener { save() }
        binding.btnSetWallpaper.setOnClickListener { openLiveWallpaperPicker() }
    }

    private fun loadCurrent() {
        binding.etApiKey.setText(prefs.apiKey)
        binding.switchGps.isChecked = prefs.useGps
        if (prefs.useCelsius) binding.rbCelsius.isChecked = true
        else binding.rbFahrenheit.isChecked = true
    }

    private fun save() {
        prefs.apiKey = binding.etApiKey.text?.toString()?.trim() ?: ""
        prefs.useCelsius = binding.rbCelsius.isChecked
        prefs.useGps = binding.switchGps.isChecked
        val pos = binding.spinnerRefresh.selectedItemPosition.coerceIn(0, refreshOptions.lastIndex)
        prefs.autoRefreshMinutes = refreshOptions[pos]
        Toast.makeText(this, getString(com.htcweather.flipclock.R.string.settings_saved), Toast.LENGTH_SHORT).show()
        finish()
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
                Toast.makeText(this, "Paramètres → Fond d'écran → Fonds animés", Toast.LENGTH_LONG).show()
            }
        }
    }
}
