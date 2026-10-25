package com.htcweather.flipclock.wallpaper

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.htcweather.flipclock.data.model.WeatherType
import com.htcweather.flipclock.data.model.toWeatherType
import com.htcweather.flipclock.utils.AppPreferences
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Live Wallpaper style HTC Sense / Diamond – fond animé synchronisé avec la météo.
 */
class WeatherWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = WeatherEngine()

    inner class WeatherEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var width = 0
        private var height = 0

        private var weatherType = WeatherType.CLEAR
        private var isDay = true

        private val particles = mutableListOf<Particle>()
        private val clouds = mutableListOf<Cloud>()
        private var flashAlpha = 0f
        private var lastFlash = 0L
        private var sunAngle = 0f

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) handler.postDelayed(this, 16)
            }
        }

        data class Particle(var x: Float, var y: Float, var speed: Float, var size: Float, var alpha: Int = 200)
        data class Cloud(var x: Float, var y: Float, var speed: Float, var scale: Float)

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            loadWeather()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            this.width = width
            this.height = height
            initParticles()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                loadWeather()
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onDestroy() {
            handler.removeCallbacks(drawRunnable)
            super.onDestroy()
        }

        private fun loadWeather() {
            val prefs = AppPreferences(this@WeatherWallpaperService)
            weatherType = prefs.lastConditionId.toWeatherType()
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            isDay = hour in 6..19
            initParticles()
        }

        private fun initParticles() {
            particles.clear()
            clouds.clear()
            val w = width.toFloat().coerceAtLeast(1f)
            val h = height.toFloat().coerceAtLeast(1f)

            when (weatherType) {
                WeatherType.RAIN, WeatherType.DRIZZLE -> {
                    repeat(100) {
                        particles.add(Particle(Random.nextFloat() * w, Random.nextFloat() * h, 14f + Random.nextFloat() * 20f, 2f + Random.nextFloat() * 3f))
                    }
                }
                WeatherType.THUNDERSTORM -> {
                    repeat(90) {
                        particles.add(Particle(Random.nextFloat() * w, Random.nextFloat() * h, 18f + Random.nextFloat() * 25f, 2.5f + Random.nextFloat() * 3f))
                    }
                }
                WeatherType.SNOW -> {
                    repeat(70) {
                        particles.add(Particle(Random.nextFloat() * w, Random.nextFloat() * h, 2f + Random.nextFloat() * 5f, 3f + Random.nextFloat() * 6f, 140 + Random.nextInt(100)))
                    }
                }
                WeatherType.CLEAR -> {
                    repeat(15) {
                        particles.add(Particle(Random.nextFloat() * w, Random.nextFloat() * h * 0.7f, 0.25f + Random.nextFloat() * 0.5f, 25f + Random.nextFloat() * 50f, 25 + Random.nextInt(35)))
                    }
                }
                WeatherType.CLOUDS, WeatherType.MIST -> {
                    repeat(7) {
                        clouds.add(Cloud(Random.nextFloat() * w * 1.4f - w * 0.2f, 60f + Random.nextFloat() * h * 0.45f, 0.35f + Random.nextFloat() * 0.7f, 0.8f + Random.nextFloat() * 1.0f))
                    }
                }
                else -> {}
            }
        }

        private fun update() {
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            sunAngle = (sunAngle + 0.4f) % 360f

            when (weatherType) {
                WeatherType.RAIN, WeatherType.DRIZZLE, WeatherType.THUNDERSTORM -> {
                    particles.forEach {
                        it.y += it.speed
                        it.x += it.speed * 0.12f
                        if (it.y > h) { it.y = -20f; it.x = Random.nextFloat() * w }
                    }
                    if (weatherType == WeatherType.THUNDERSTORM) {
                        val now = System.currentTimeMillis()
                        if (now - lastFlash > 3500 + Random.nextLong(6000)) {
                            flashAlpha = 0.75f
                            lastFlash = now
                        }
                        flashAlpha *= 0.90f
                    }
                }
                WeatherType.SNOW -> {
                    particles.forEach {
                        it.y += it.speed
                        it.x += sin(it.y * 0.015f) * 1.5f
                        if (it.y > h) { it.y = -15f; it.x = Random.nextFloat() * w }
                    }
                }
                WeatherType.CLEAR -> {
                    particles.forEach {
                        it.x += it.speed
                        if (it.x > w + it.size) it.x = -it.size
                    }
                }
                WeatherType.CLOUDS, WeatherType.MIST -> {
                    clouds.forEach {
                        it.x += it.speed
                        if (it.x > w + 250) it.x = -350f
                    }
                }
                else -> {}
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    update()
                    drawBackground(canvas)
                    drawElements(canvas)
                }
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
                }
            }
        }

        private fun drawBackground(canvas: Canvas) {
            val paint = Paint()
            val colors = when (weatherType) {
                WeatherType.CLEAR -> if (isDay)
                    intArrayOf(Color.parseColor("#0D47A1"), Color.parseColor("#1565C0"), Color.parseColor("#1E88E5"))
                else
                    intArrayOf(Color.parseColor("#0A0A1F"), Color.parseColor("#12122F"), Color.parseColor("#1A1A3A"))
                WeatherType.CLOUDS -> intArrayOf(Color.parseColor("#263238"), Color.parseColor("#37474F"), Color.parseColor("#455A64"))
                WeatherType.RAIN, WeatherType.DRIZZLE -> intArrayOf(Color.parseColor("#1A237E"), Color.parseColor("#283593"), Color.parseColor("#3949AB"))
                WeatherType.THUNDERSTORM -> intArrayOf(Color.parseColor("#0D0D15"), Color.parseColor("#1A1A2E"), Color.parseColor("#212121"))
                WeatherType.SNOW -> intArrayOf(Color.parseColor("#37474F"), Color.parseColor("#546E7A"), Color.parseColor("#78909C"))
                WeatherType.MIST -> intArrayOf(Color.parseColor("#424242"), Color.parseColor("#616161"), Color.parseColor("#757575"))
                else -> intArrayOf(Color.parseColor("#0D0D15"), Color.parseColor("#1A1A2E"), Color.parseColor("#212121"))
            }
            paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), colors, null, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            if (flashAlpha > 0.04f) {
                canvas.drawColor(Color.argb((flashAlpha * 200).toInt(), 255, 255, 230))
            }
        }

        private fun drawElements(canvas: Canvas) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            when (weatherType) {
                WeatherType.CLEAR -> {
                    // Sun with rotating rays (HTC style)
                    val cx = width * 0.75f
                    val cy = height * 0.22f
                    val r = width * 0.12f
                    p.color = Color.argb(60, 255, 230, 150)
                    canvas.drawCircle(cx, cy, r * 2.2f, p)
                    p.color = Color.parseColor("#FFD54F")
                    canvas.drawCircle(cx, cy, r, p)
                    p.color = Color.parseColor("#FFECB3")
                    p.strokeWidth = 5f
                    p.style = Paint.Style.STROKE
                    p.strokeCap = Paint.Cap.ROUND
                    for (i in 0 until 12) {
                        val a = Math.toRadians((i * 30 + sunAngle).toDouble())
                        canvas.drawLine(
                            cx + (cos(a) * r * 1.35).toFloat(),
                            cy + (sin(a) * r * 1.35).toFloat(),
                            cx + (cos(a) * r * 1.85).toFloat(),
                            cy + (sin(a) * r * 1.85).toFloat(),
                            p
                        )
                    }
                    p.style = Paint.Style.FILL
                    // Soft orbs
                    particles.forEach {
                        p.color = Color.argb(it.alpha, 255, 240, 180)
                        canvas.drawCircle(it.x, it.y, it.size, p)
                    }
                }
                WeatherType.RAIN, WeatherType.DRIZZLE, WeatherType.THUNDERSTORM -> {
                    p.color = Color.argb(190, 170, 200, 255)
                    p.strokeWidth = 3f
                    p.strokeCap = Paint.Cap.ROUND
                    particles.forEach {
                        canvas.drawLine(it.x, it.y, it.x - it.size * 0.4f, it.y + it.size * 5, p)
                    }
                }
                WeatherType.SNOW -> {
                    particles.forEach {
                        p.color = Color.argb(it.alpha, 255, 255, 255)
                        canvas.drawCircle(it.x, it.y, it.size, p)
                    }
                }
                WeatherType.CLOUDS, WeatherType.MIST -> {
                    p.color = Color.argb(if (weatherType == WeatherType.MIST) 100 else 170, 220, 220, 230)
                    clouds.forEach { c ->
                        val s = c.scale
                        canvas.drawCircle(c.x, c.y, 55f * s, p)
                        canvas.drawCircle(c.x + 45f * s, c.y - 12f * s, 42f * s, p)
                        canvas.drawCircle(c.x - 40f * s, c.y + 8f * s, 38f * s, p)
                        canvas.drawCircle(c.x + 12f * s, c.y + 22f * s, 48f * s, p)
                    }
                }
                else -> {}
            }
        }
    }
}
