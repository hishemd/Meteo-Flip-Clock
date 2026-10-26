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
 * Live Wallpaper riche style HTC Sense – pluie, neige, orage, soleil, lune, étoiles, brouillard.
 */
class WeatherWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = WeatherEngine()

    private data class RainDrop(var x: Float, var y: Float, var speed: Float, var len: Float, var alpha: Int, var thick: Float)
    private data class SnowFlake(var x: Float, var y: Float, var speed: Float, var size: Float, var alpha: Int, var phase: Float)
    private data class Cloud(var x: Float, var y: Float, var speed: Float, var scale: Float, var alpha: Int)
    private data class FogBand(var x: Float, var y: Float, var speed: Float, var w: Float, var h: Float, var alpha: Int)
    private data class Spark(var x: Float, var y: Float, var vx: Float, var size: Float, var alpha: Int)
    private data class WindP(var x: Float, var y: Float, var speed: Float, var size: Float, var alpha: Int)

    inner class WeatherEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var width = 0
        private var height = 0
        private var weatherType = WeatherType.CLEAR
        private var isDay = true
        private var time = 0f
        private var flashAlpha = 0f
        private var boltAlpha = 0f
        private var lastFlash = 0L
        private var boltPath: Path? = null

        private val rain = mutableListOf<RainDrop>()
        private val snow = mutableListOf<SnowFlake>()
        private val clouds = mutableListOf<Cloud>()
        private val fog = mutableListOf<FogBand>()
        private val sparks = mutableListOf<Spark>()
        private val wind = mutableListOf<WindP>()

        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) handler.postDelayed(this, 16)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            loadWeather()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            this.width = width
            this.height = height
            spawn()
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
            spawn()
        }

        private fun spawn() {
            rain.clear(); snow.clear(); clouds.clear(); fog.clear(); sparks.clear(); wind.clear()
            boltPath = null; flashAlpha = 0f; boltAlpha = 0f
            val w = width.toFloat().coerceAtLeast(1f)
            val h = height.toFloat().coerceAtLeast(1f)

            when (weatherType) {
                WeatherType.CLEAR -> {
                    repeat(22) {
                        sparks.add(Spark(Random.nextFloat() * w, Random.nextFloat() * h * 0.65f,
                            0.2f + Random.nextFloat() * 0.45f, 8f + Random.nextFloat() * 28f, 20 + Random.nextInt(30)))
                    }
                    repeat(3) {
                        clouds.add(Cloud(Random.nextFloat() * w, h * 0.55f + Random.nextFloat() * h * 0.15f,
                            0.12f + Random.nextFloat() * 0.2f, 0.7f + Random.nextFloat() * 0.5f, 45 + Random.nextInt(25)))
                    }
                }
                WeatherType.CLOUDS -> {
                    repeat(9) {
                        clouds.add(Cloud(Random.nextFloat() * w * 1.4f - w * 0.2f,
                            50f + Random.nextFloat() * h * 0.4f,
                            0.2f + Random.nextFloat() * 0.55f, 0.9f + Random.nextFloat() * 1.2f, 120 + Random.nextInt(50)))
                    }
                }
                WeatherType.MIST -> {
                    repeat(7) {
                        fog.add(FogBand(Random.nextFloat() * w, h * 0.15f + Random.nextFloat() * h * 0.6f,
                            0.25f + Random.nextFloat() * 0.5f, w * 0.55f + Random.nextFloat() * w * 0.4f,
                            90f + Random.nextFloat() * 130f, 45 + Random.nextInt(45)))
                    }
                    repeat(4) {
                        clouds.add(Cloud(Random.nextFloat() * w, Random.nextFloat() * h * 0.35f, 0.15f, 1.2f, 70))
                    }
                }
                WeatherType.RAIN, WeatherType.DRIZZLE -> {
                    val n = if (weatherType == WeatherType.DRIZZLE) 70 else 140
                    repeat(n) {
                        rain.add(RainDrop(Random.nextFloat() * w, Random.nextFloat() * h,
                            12f + Random.nextFloat() * 20f, 12f + Random.nextFloat() * 20f,
                            150 + Random.nextInt(80), 1.5f + Random.nextFloat() * 2.2f))
                    }
                    repeat(5) {
                        clouds.add(Cloud(Random.nextFloat() * w, 20f + Random.nextFloat() * h * 0.28f,
                            0.25f + Random.nextFloat() * 0.4f, 1.0f + Random.nextFloat() * 0.9f, 140))
                    }
                }
                WeatherType.THUNDERSTORM -> {
                    repeat(160) {
                        rain.add(RainDrop(Random.nextFloat() * w, Random.nextFloat() * h,
                            16f + Random.nextFloat() * 26f, 16f + Random.nextFloat() * 24f, 190,
                            2f + Random.nextFloat() * 2.5f))
                    }
                    repeat(5) {
                        clouds.add(Cloud(Random.nextFloat() * w, 10f + Random.nextFloat() * h * 0.22f, 0.2f, 1.5f, 170))
                    }
                }
                WeatherType.SNOW -> {
                    repeat(90) {
                        snow.add(SnowFlake(Random.nextFloat() * w, Random.nextFloat() * h,
                            1.2f + Random.nextFloat() * 3.8f, 2.5f + Random.nextFloat() * 6.5f,
                            140 + Random.nextInt(100), Random.nextFloat() * 6.28f))
                    }
                    repeat(3) {
                        clouds.add(Cloud(Random.nextFloat() * w, 30f + Random.nextFloat() * h * 0.22f, 0.15f, 1.15f, 110))
                    }
                }
                else -> {
                    repeat(45) {
                        wind.add(WindP(Random.nextFloat() * w, Random.nextFloat() * h,
                            3.5f + Random.nextFloat() * 7f, 2f + Random.nextFloat() * 4f, 100 + Random.nextInt(80)))
                    }
                }
            }
        }

        private fun update() {
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0 || h <= 0) return
            time += 0.016f

            when (weatherType) {
                WeatherType.CLEAR -> {
                    sparks.forEach { it.x += it.vx; if (it.x > w + it.size) it.x = -it.size }
                    clouds.forEach { it.x += it.speed; if (it.x > w + 200) it.x = -250f }
                }
                WeatherType.CLOUDS, WeatherType.MIST -> {
                    clouds.forEach { it.x += it.speed; if (it.x > w + 280) it.x = -320f }
                    fog.forEach { it.x += it.speed; if (it.x > w + it.w) it.x = -it.w }
                }
                WeatherType.RAIN, WeatherType.DRIZZLE, WeatherType.THUNDERSTORM -> {
                    rain.forEach {
                        it.y += it.speed; it.x += it.speed * 0.12f
                        if (it.y > h + 30) { it.y = -30f; it.x = Random.nextFloat() * w }
                    }
                    clouds.forEach { it.x += it.speed; if (it.x > w + 250) it.x = -300f }
                    if (weatherType == WeatherType.THUNDERSTORM) {
                        val now = System.currentTimeMillis()
                        if (now - lastFlash > 3000 + Random.nextLong(5000)) {
                            flashAlpha = 0.9f; boltAlpha = 1f
                            boltPath = makeLightning(w, h); lastFlash = now
                        }
                        flashAlpha *= 0.87f; boltAlpha *= 0.91f
                    }
                }
                WeatherType.SNOW -> {
                    snow.forEach {
                        it.y += it.speed
                        it.x += sin(it.y * 0.02f + it.phase) * 1.7f
                        if (it.y > h + 10) { it.y = -10f; it.x = Random.nextFloat() * w }
                    }
                    clouds.forEach { it.x += it.speed; if (it.x > w + 200) it.x = -250f }
                }
                else -> {
                    wind.forEach {
                        it.x += it.speed
                        it.y += sin(it.x * 0.02f) * 1.5f
                        if (it.x > w + 20) { it.x = -20f; it.y = Random.nextFloat() * h }
                    }
                }
            }
        }

        private fun makeLightning(w: Float, h: Float): Path {
            val path = Path()
            var x = w * (0.25f + Random.nextFloat() * 0.5f)
            var y = 0f
            path.moveTo(x, y)
            while (y < h * 0.75f) {
                x += (Random.nextFloat() - 0.5f) * 90f
                y += 35f + Random.nextFloat() * 70f
                path.lineTo(x, y)
                if (Random.nextFloat() > 0.55f) {
                    path.moveTo(x, y)
                    path.lineTo(x + (Random.nextFloat() - 0.5f) * 110f, y + 25f + Random.nextFloat() * 50f)
                    path.moveTo(x, y)
                }
            }
            return path
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    update()
                    drawBg(canvas)
                    drawScene(canvas)
                    if (flashAlpha > 0.03f) {
                        canvas.drawColor(Color.argb((flashAlpha * 200).toInt(), 255, 255, 240))
                    }
                    if (boltAlpha > 0.05f && boltPath != null) {
                        p.color = Color.argb((boltAlpha * 255).toInt(), 255, 255, 220)
                        p.strokeWidth = 5f; p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
                        canvas.drawPath(boltPath!!, p)
                        p.style = Paint.Style.FILL
                    }
                }
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
                }
            }
        }

        private fun drawBg(canvas: Canvas) {
            val colors = when (weatherType) {
                WeatherType.CLEAR -> if (isDay)
                    intArrayOf(0xFF1E88E5.toInt(), 0xFF42A5F5.toInt(), 0xFF64B5F6.toInt(), 0xFF90CAF9.toInt())
                else
                    intArrayOf(0xFF020208.toInt(), 0xFF0A0E1C.toInt(), 0xFF12182C.toInt(), 0xFF1A2238.toInt())
                WeatherType.CLOUDS -> if (isDay)
                    intArrayOf(0xFF5C9BC7.toInt(), 0xFF7AAFD4.toInt(), 0xFF9BC4E0.toInt(), 0xFFB8D4E8.toInt())
                else
                    intArrayOf(0xFF0C1018.toInt(), 0xFF161C28.toInt(), 0xFF222A38.toInt(), 0xFF2E3648.toInt())
                WeatherType.MIST -> if (isDay)
                    intArrayOf(0xFF7EAECC.toInt(), 0xFF96BED6.toInt(), 0xFFB0CCE0.toInt(), 0xFFC8DCE8.toInt())
                else
                    intArrayOf(0xFF1A1E28.toInt(), 0xFF2A3040.toInt(), 0xFF3A4250.toInt(), 0xFF4A5260.toInt())
                WeatherType.RAIN, WeatherType.DRIZZLE -> if (isDay)
                    intArrayOf(0xFF4A7FA8.toInt(), 0xFF5A8FB8.toInt(), 0xFF6A9FC8.toInt(), 0xFF7AAFD0.toInt())
                else
                    intArrayOf(0xFF080C14.toInt(), 0xFF101820.toInt(), 0xFF182028.toInt(), 0xFF203040.toInt())
                WeatherType.THUNDERSTORM -> if (isDay)
                    intArrayOf(0xFF3A5A78.toInt(), 0xFF4A6A88.toInt(), 0xFF5A7A98.toInt(), 0xFF6A8AA8.toInt())
                else
                    intArrayOf(0xFF040408.toInt(), 0xFF0A0A10.toInt(), 0xFF121218.toInt(), 0xFF1A1A22.toInt())
                WeatherType.SNOW -> if (isDay)
                    intArrayOf(0xFF6A9AB8.toInt(), 0xFF82AEC8.toInt(), 0xFF9AC0D4.toInt(), 0xFFB2D0E0.toInt())
                else
                    intArrayOf(0xFF0C1420.toInt(), 0xFF182430.toInt(), 0xFF283848.toInt(), 0xFF384858.toInt())
                else -> if (isDay)
                    intArrayOf(0xFF3A8FD0.toInt(), 0xFF5AA5DC.toInt(), 0xFF7ABBE8.toInt(), 0xFF9ACFF0.toInt())
                else
                    intArrayOf(0xFF080C18.toInt(), 0xFF101828.toInt(), 0xFF1A2438.toInt(), 0xFF243048.toInt())
            }
            p.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), colors,
                floatArrayOf(0f, 0.3f, 0.65f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
            p.shader = null

            if (!isDay && (weatherType == WeatherType.CLEAR || weatherType == WeatherType.UNKNOWN)) {
                drawStarsAndMoon(canvas)
            }
        }

        private fun drawStarsAndMoon(canvas: Canvas) {
            val rnd = java.util.Random(42)
            val w = width.toFloat()
            val h = height.toFloat() * 0.75f
            for (i in 0 until 100) {
                val x = rnd.nextFloat() * w
                val y = rnd.nextFloat() * h
                val size = 0.8f + rnd.nextFloat() * 2f
                val tw = 0.5f + 0.5f * sin(time * (2f + rnd.nextFloat() * 3f) + i)
                p.color = Color.argb((120 + 100 * tw).toInt().coerceIn(40, 230), 255, 255, 255)
                canvas.drawCircle(x, y, size, p)
            }
            if (weatherType == WeatherType.CLEAR) {
                val mx = width * 0.72f
                val my = height * 0.2f
                val mr = width * 0.11f
                p.shader = RadialGradient(mx, my, mr * 2.4f,
                    intArrayOf(Color.argb(45, 200, 210, 255), Color.TRANSPARENT),
                    floatArrayOf(0.3f, 1f), Shader.TileMode.CLAMP)
                canvas.drawCircle(mx, my, mr * 2.4f, p)
                p.shader = RadialGradient(mx - mr * 0.25f, my - mr * 0.25f, mr,
                    intArrayOf(0xFFF5F5FF.toInt(), 0xFFD0D0E8.toInt(), 0xFFA8A8C0.toInt()),
                    floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
                canvas.drawCircle(mx, my, mr, p)
                p.shader = null
                p.color = Color.argb(35, 80, 80, 100)
                canvas.drawCircle(mx + mr * 0.2f, my + mr * 0.15f, mr * 0.18f, p)
                canvas.drawCircle(mx - mr * 0.15f, my + mr * 0.3f, mr * 0.12f, p)
            }
        }

        private fun drawScene(canvas: Canvas) {
            when (weatherType) {
                WeatherType.CLEAR -> {
                    if (isDay) drawSun(canvas)
                    sparks.forEach {
                        p.color = Color.argb(it.alpha, 255, 235, 180)
                        canvas.drawCircle(it.x, it.y, it.size, p)
                    }
                    clouds.forEach { drawCloud(canvas, it) }
                }
                WeatherType.CLOUDS -> clouds.forEach { drawCloud(canvas, it) }
                WeatherType.MIST -> {
                    clouds.forEach { drawCloud(canvas, it) }
                    fog.forEach {
                        p.shader = LinearGradient(it.x, it.y, it.x + it.w, it.y + it.h,
                            intArrayOf(Color.TRANSPARENT, Color.argb(it.alpha, 200, 205, 215), Color.TRANSPARENT),
                            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                        canvas.drawRect(it.x, it.y, it.x + it.w, it.y + it.h, p)
                        p.shader = null
                    }
                }
                WeatherType.RAIN, WeatherType.DRIZZLE, WeatherType.THUNDERSTORM -> {
                    clouds.forEach { drawCloud(canvas, it) }
                    rain.forEach {
                        p.color = Color.argb(it.alpha, if (isDay) 100 else 150, if (isDay) 160 else 200, if (isDay) 220 else 255)
                        p.strokeWidth = it.thick; p.strokeCap = Paint.Cap.ROUND
                        canvas.drawLine(it.x, it.y, it.x - it.len * 0.2f, it.y + it.len, p)
                    }
                }
                WeatherType.SNOW -> {
                    clouds.forEach { drawCloud(canvas, it) }
                    snow.forEach {
                        p.color = Color.argb(it.alpha, 255, 255, 255)
                        canvas.drawCircle(it.x, it.y, it.size, p)
                    }
                }
                else -> {
                    wind.forEach {
                        p.color = Color.argb(it.alpha, 200, 210, 220)
                        canvas.drawCircle(it.x, it.y, it.size, p)
                    }
                }
            }
        }

        private fun drawSun(canvas: Canvas) {
            val cx = width * 0.72f
            val cy = height * 0.28f
            val r = width * 0.14f * (1f + 0.03f * sin(time * 1.5f))
            for (i in 5 downTo 1) {
                val gr = r * (1.3f + i * 0.4f)
                p.shader = RadialGradient(cx, cy, gr,
                    intArrayOf(Color.argb(12 + i * 4, 255, 200, 90), Color.TRANSPARENT),
                    floatArrayOf(0.2f, 1f), Shader.TileMode.CLAMP)
                canvas.drawCircle(cx, cy, gr, p)
            }
            p.shader = null
            rayPaint.strokeWidth = 4.5f
            rayPaint.color = Color.argb(150, 255, 220, 130)
            for (i in 0 until 14) {
                val a = Math.toRadians(i * (360.0 / 14) + time * 12)
                val s = r * 1.3f; val e = r * (1.8f + 0.1f * sin(time * 2 + i))
                canvas.drawLine(
                    cx + (cos(a) * s).toFloat(), cy + (sin(a) * s).toFloat(),
                    cx + (cos(a) * e).toFloat(), cy + (sin(a) * e).toFloat(), rayPaint)
            }
            p.shader = RadialGradient(cx - r * 0.2f, cy - r * 0.2f, r,
                intArrayOf(0xFFFFF59D.toInt(), 0xFFFFD54F.toInt(), 0xFFFFA000.toInt()),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, r, p)
            p.shader = null
        }

        private fun drawCloud(canvas: Canvas, c: Cloud) {
            if (isDay) p.color = Color.argb(c.alpha.coerceAtMost(200), 245, 248, 252)
            else p.color = Color.argb(c.alpha.coerceAtMost(160), 80, 90, 110)
            val s = c.scale; val cx = c.x; val cy = c.y
            canvas.drawCircle(cx, cy, 52f * s, p)
            canvas.drawCircle(cx + 44f * s, cy - 10f * s, 44f * s, p)
            canvas.drawCircle(cx - 38f * s, cy + 6f * s, 40f * s, p)
            canvas.drawCircle(cx + 14f * s, cy + 22f * s, 46f * s, p)
            canvas.drawCircle(cx - 12f * s, cy - 18f * s, 38f * s, p)
            canvas.drawCircle(cx + 58f * s, cy + 8f * s, 32f * s, p)
        }
    }
}
