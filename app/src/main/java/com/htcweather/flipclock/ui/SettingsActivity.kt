package com.htcweather.flipclock.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.htcweather.flipclock.databinding.ActivitySettingsBinding
import com.htcweather.flipclock.utils.AppPreferences
import com.htcweather.flipclock.wallpaper.WeatherWallpaperService
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: AppPreferences

    private val refreshOptions = listOf(15, 30, 60, 120)
    private val refreshLabels = listOf("15 min", "30 min", "1 h", "2 h")
    private val langCodes = listOf("fr", "en", "ar")
    private val langLabels = listOf("Français", "English", "العربية")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPreferences(this)
        loadCurrent()

        binding.spinnerRefresh.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, refreshLabels
        )
        binding.spinnerRefresh.setSelection(
            refreshOptions.indexOf(prefs.autoRefreshMinutes).coerceAtLeast(0)
        )

        binding.spinnerLanguage.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, langLabels
        )
        binding.spinnerLanguage.setSelection(
            langCodes.indexOf(prefs.language).coerceAtLeast(0)
        )

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
        val rPos = binding.spinnerRefresh.selectedItemPosition.coerceIn(0, refreshOptions.lastIndex)
        prefs.autoRefreshMinutes = refreshOptions[rPos]

        val lPos = binding.spinnerLanguage.selectedItemPosition.coerceIn(0, langCodes.lastIndex)
        val newLang = langCodes[lPos]
        val langChanged = newLang != prefs.language
        prefs.language = newLang

        if (langChanged) {
            applyLocale(newLang)
        }

        Toast.makeText(this, getString(com.htcweather.flipclock.R.string.settings_saved), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun applyLocale(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        // Restart main to apply
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
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
                Toast.makeText(this, "Settings → Wallpaper → Live wallpapers", Toast.LENGTH_LONG).show()
            }
        }
    }
}
