package com.htcweather.flipclock.animation

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.htcweather.flipclock.data.model.WeatherType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated weather background inspired by HTC Sense 3.0 (18 weather scenes).
 * Canvas particle system: sun rays, rain, snow, fog, lightning, wind, clouds.
 */
class WeatherBackgroundView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var weatherType = WeatherType.CLEAR
    private var isDay = true
    private var time = 0f
    private var flashAlpha = 0f
    private var lastFlash = 0L
    private var boltPath: Path? = null
    private var boltAlpha = 0f

    private val rain = mutableListOf<RainDrop>()
    private val snow = mutableListOf<SnowFlake>()
    private val clouds = mutableListOf<Cloud>()
    private val fogLayers = mutableListOf<FogBand>()
    private val sparks = mutableListOf<Spark>()
    private val windLeaves = mutableListOf<WindParticle>()

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }

    data class RainDrop(var x: Float, var y: Float, var speed: Float, var len: Float, var alpha: Int, var thick: Float)
    data class SnowFlake(var x: Float, var y: Float, var speed: Float, var size: Float, var alpha: Int, var phase: Float)
    data class Cloud(var x: Float, var y: Float, var speed: Float, var scale: Float, var alpha: Int)
    data class FogBand(var x: Float, var y: Float, var speed: Float, var w: Float, var h: Float, var alpha: Int)
    data class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, var size: Float)
    data class WindParticle(var x: Float, var y: Float, var speed: Float, var size: Float, var alpha: Int)

    private val tick = object : Runnable {
        override fun run() {
            time += 0.016f
            update()
            invalidate()
            postDelayed(this, 16)
        }
    }

    fun setWeather(type: WeatherType, day: Boolean = true) {
        weatherType = type; isDay = day
        rain.clear(); snow.clear(); clouds.clear(); fogLayers.clear(); sparks.clear(); windLeaves.clear()
        boltPath = null; flashAlpha = 0f; boltAlpha = 0f
        spawn()
        invalidate()
    }

    private fun spawn() {
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        when (weatherType) {
            WeatherType.CLEAR -> {
                // light orbs + soft clouds
                repeat(20) {
                    sparks.add(Spark(Random.nextFloat() * w, Random.nextFloat() * h * 0.6f,
                        0.2f + Random.nextFloat() * 0.4f, 0f, 1f, 10f + Random.nextFloat() * 30f))
                }
                repeat(3) {
                    clouds.add(Cloud(Random.nextFloat() * w, h * 0.5f + Random.nextFloat() * h * 0.2f,
                        0.12f + Random.nextFloat() * 0.2f, 0.6f + Random.nextFloat() * 0.5f, 40 + Random.nextInt(30)))
                }
            }
            WeatherType.CLOUDS -> {
                repeat(8) {
                    clouds.add(Cloud(Random.nextFloat() * w * 1.4f - w * 0.2f,
                        40f + Random.nextFloat() * h * 0.45f,
                        0.2f + Random.nextFloat() * 0.5f, 0.9f + Random.nextFloat() * 1.2f, 120 + Random.nextInt(50)))
                }
            }
            WeatherType.MIST -> {
                repeat(6) {
                    fogLayers.add(FogBand(Random.nextFloat() * w, h * 0.2f + Random.nextFloat() * h * 0.6f,
                        0.3f + Random.nextFloat() * 0.5f, w * 0.6f + Random.nextFloat() * w * 0.4f,
                        80f + Random.nextFloat() * 120f, 50 + Random.nextInt(40)))
                }
                repeat(4) {
                    clouds.add(Cloud(Random.nextFloat() * w, Random.nextFloat() * h * 0.4f,
                        0.15f, 1.2f, 80))
                }
            }
            WeatherType.RAIN, WeatherType.DRIZZLE -> {
                val count = if (weatherType == WeatherType.DRIZZLE) 60 else 120
                repeat(count) {
                    rain.add(RainDrop(Random.nextFloat() * w, Random.nextFloat() * h,
                        12f + Random.nextFloat() * 18f, 12f + Random.nextFloat() * 18f,
                        140 + Random.nextInt(80), 1.5f + Random.nextFloat() * 2f))
                }
                repeat(5) {
                    clouds.add(Cloud(Random.nextFloat() * w, 20f + Random.nextFloat() * h * 0.3f,
                        0.25f + Random.nextFloat() * 0.4f, 1.0f + Random.nextFloat() * 0.8f, 130 + Random.nextInt(40)))
                }
            }
            WeatherType.THUNDERSTORM -> {
                repeat(140) {
                    rain.add(RainDrop(Random.nextFloat() * w, Random.nextFloat() * h,
                        16f + Random.nextFloat() * 24f, 16f + Random.nextFloat() * 22f,
                        180, 2f + Random.nextFloat() * 2.5f))
                }
                repeat(4) {
                    clouds.add(Cloud(Random.nextFloat() * w, 10f + Random.nextFloat() * h * 0.25f,
                        0.2f, 1.4f, 160))
                }
            }
            WeatherType.SNOW -> {
                repeat(80) {
                    snow.add(SnowFlake(Random.nextFloat() * w, Random.nextFloat() * h,
                        1.2f + Random.nextFloat() * 3.5f, 2.5f + Random.nextFloat() * 6f,
                        140 + Random.nextInt(100), Random.nextFloat() * 6.28f))
                }
                repeat(3) {
                    clouds.add(Cloud(Random.nextFloat() * w, 30f + Random.nextFloat() * h * 0.25f,
                        0.15f, 1.1f, 100))
                }
            }
            else -> {
                // wind-like drift
                repeat(40) {
                    windLeaves.add(WindParticle(Random.nextFloat() * w, Random.nextFloat() * h,
                        3f + Random.nextFloat() * 6f, 2f + Random.nextFloat() * 4f, 100 + Random.nextInt(80)))
                }
            }
        }
    }

    private fun update() {
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        when (weatherType) {
            WeatherType.CLEAR -> {
                sparks.forEach {
                    it.x += it.vx
                    if (it.x > w + it.size) it.x = -it.size
                }
                clouds.forEach { it.x += it.speed; if (it.x > w + 200) it.x = -250f }
            }
            WeatherType.CLOUDS, WeatherType.MIST -> {
                clouds.forEach { it.x += it.speed; if (it.x > w + 280) it.x = -320f }
                fogLayers.forEach {
                    it.x += it.speed
                    if (it.x > w + it.w) it.x = -it.w
                }
            }
            WeatherType.RAIN, WeatherType.DRIZZLE, WeatherType.THUNDERSTORM -> {
                rain.forEach {
                    it.y += it.speed
                    it.x += it.speed * 0.12f
                    if (it.y > h + 30) { it.y = -30f; it.x = Random.nextFloat() * w }
                }
                clouds.forEach { it.x += it.speed; if (it.x > w + 250) it.x = -300f }
                if (weatherType == WeatherType.THUNDERSTORM) {
                    val now = System.currentTimeMillis()
                    if (now - lastFlash > 3200 + Random.nextLong(4500)) {
                        flashAlpha = 0.85f
                        boltAlpha = 1f
                        boltPath = makeLightning(w, h)
                        lastFlash = now
                    }
                    flashAlpha *= 0.88f
                    boltAlpha *= 0.92f
                }
            }
            WeatherType.SNOW -> {
                snow.forEach {
                    it.y += it.speed
                    it.x += sin(it.y * 0.02f + it.phase) * 1.6f
                    if (it.y > h + 10) { it.y = -10f; it.x = Random.nextFloat() * w }
                }
                clouds.forEach { it.x += it.speed; if (it.x > w + 200) it.x = -250f }
            }
            else -> {
                windLeaves.forEach {
                    it.x += it.speed
                    it.y += sin(it.x * 0.02f) * 1.5f
                    if (it.x > w + 20) { it.x = -20f; it.y = Random.nextFloat() * h }
                }
            }
        }
    }

    private fun makeLightning(w: Float, h: Float): Path {
        val path = Path()
        var x = w * (0.3f + Random.nextFloat() * 0.4f)
        var y = 0f
        path.moveTo(x, y)
        while (y < h * 0.7f) {
            x += (Random.nextFloat() - 0.5f) * 80f
            y += 40f + Random.nextFloat() * 60f
            path.lineTo(x, y)
            if (Random.nextFloat() > 0.6f) {
                // branch
                path.moveTo(x, y)
                path.lineTo(x + (Random.nextFloat() - 0.5f) * 100f, y + 30f + Random.nextFloat() * 40f)
                path.moveTo(x, y)
            }
        }
        return path
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh); spawn()
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); post(tick) }
    override fun onDetachedFromWindow() { removeCallbacks(tick); super.onDetachedFromWindow() }

    override fun onDraw(canvas: Canvas) {
        drawBg(canvas)
        drawScene(canvas)
        if (flashAlpha > 0.03f) canvas.drawColor(Color.argb((flashAlpha * 200).toInt(), 255, 255, 240))
        if (boltAlpha > 0.05f && boltPath != null) {
            p.color = Color.argb((boltAlpha * 255).toInt(), 255, 255, 220)
            p.strokeWidth = 4f; p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
            canvas.drawPath(boltPath!!, p)
            p.style = Paint.Style.FILL
        }
    }

    private fun drawBg(canvas: Canvas) {
        val colors = when (weatherType) {
            WeatherType.CLEAR -> if (isDay)
                intArrayOf(0xFF071428.toInt(), 0xFF0C2A4A.toInt(), 0xFF154A72.toInt(), 0xFF1E6A8C.toInt())
            else
                intArrayOf(0xFF030308.toInt(), 0xFF0A0A18.toInt(), 0xFF12122A.toInt(), 0xFF1A1A38.toInt())
            WeatherType.CLOUDS -> intArrayOf(0xFF1A2030.toInt(), 0xFF283040.toInt(), 0xFF384050.toInt(), 0xFF485060.toInt())
            WeatherType.MIST -> intArrayOf(0xFF2A2A35.toInt(), 0xFF3A3A48.toInt(), 0xFF4A4A58.toInt(), 0xFF5A5A68.toInt())
            WeatherType.RAIN, WeatherType.DRIZZLE -> intArrayOf(0xFF0A1020.toInt(), 0xFF121C32.toInt(), 0xFF1A2A48.toInt(), 0xFF223858.toInt())
            WeatherType.THUNDERSTORM -> intArrayOf(0xFF06060C.toInt(), 0xFF0E0E16.toInt(), 0xFF16161E.toInt(), 0xFF1E1E28.toInt())
            WeatherType.SNOW -> intArrayOf(0xFF1A2430.toInt(), 0xFF2A3848.toInt(), 0xFF3A4C5C.toInt(), 0xFF4A6070.toInt())
            else -> intArrayOf(0xFF0D1520.toInt(), 0xFF152030.toInt(), 0xFF1A2A40.toInt(), 0xFF203050.toInt())
        }
        p.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), colors,
            floatArrayOf(0f, 0.35f, 0.7f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
        p.shader = null
    }

    private fun drawScene(canvas: Canvas) {
        when (weatherType) {
            WeatherType.CLEAR -> {
                drawSun(canvas)
                sparks.forEach {
                    p.color = Color.argb(28, 255, 230, 160)
                    canvas.drawCircle(it.x, it.y, it.size, p)
                }
                clouds.forEach { drawCloud(canvas, it) }
            }
            WeatherType.CLOUDS -> clouds.forEach { drawCloud(canvas, it) }
            WeatherType.MIST -> {
                clouds.forEach { drawCloud(canvas, it) }
                fogLayers.forEach {
                    p.shader = LinearGradient(it.x, it.y, it.x + it.w, it.y + it.h,
                        intArrayOf(Color.TRANSPARENT, Color.argb(it.alpha, 200, 200, 210), Color.TRANSPARENT),
                        floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                    canvas.drawRect(it.x, it.y, it.x + it.w, it.y + it.h, p)
                    p.shader = null
                }
            }
            WeatherType.RAIN, WeatherType.DRIZZLE, WeatherType.THUNDERSTORM -> {
                clouds.forEach { drawCloud(canvas, it) }
                rain.forEach {
                    p.color = Color.argb(it.alpha, 150, 200, 255)
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
                windLeaves.forEach {
                    p.color = Color.argb(it.alpha, 200, 210, 220)
                    canvas.drawCircle(it.x, it.y, it.size, p)
                }
            }
        }
    }

    private fun drawSun(canvas: Canvas) {
        val cx = width * 0.70f
        val cy = height * 0.32f
        val r = width * 0.15f * (1f + 0.03f * sin(time * 1.5f))

        // multi-layer glow (Sense style)
        for (i in 5 downTo 1) {
            val gr = r * (1.3f + i * 0.4f)
            p.shader = RadialGradient(cx, cy, gr,
                intArrayOf(Color.argb(12 + i * 4, 255, 200, 90), Color.TRANSPARENT),
                floatArrayOf(0.2f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, gr, p)
        }
        p.shader = null

        // rays
        rayPaint.strokeWidth = 4.5f
        rayPaint.color = Color.argb(150, 255, 220, 130)
        for (i in 0 until 14) {
            val a = Math.toRadians(i * (360.0 / 14) + time * 12)
            val s = r * 1.3f; val e = r * (1.8f + 0.1f * sin(time * 2 + i))
            canvas.drawLine(
                cx + (cos(a) * s).toFloat(), cy + (sin(a) * s).toFloat(),
                cx + (cos(a) * e).toFloat(), cy + (sin(a) * e).toFloat(), rayPaint)
        }

        // core
        p.shader = RadialGradient(cx - r * 0.2f, cy - r * 0.2f, r,
            intArrayOf(0xFFFFF59D.toInt(), 0xFFFFD54F.toInt(), 0xFFFFA000.toInt()),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r, p)
        p.shader = null
    }

    private fun drawCloud(canvas: Canvas, c: Cloud) {
        p.color = Color.argb(c.alpha, 225, 230, 240)
        val s = c.scale; val cx = c.x; val cy = c.y
        canvas.drawCircle(cx, cy, 50f * s, p)
        canvas.drawCircle(cx + 42f * s, cy - 10f * s, 42f * s, p)
        canvas.drawCircle(cx - 36f * s, cy + 6f * s, 38f * s, p)
        canvas.drawCircle(cx + 14f * s, cy + 20f * s, 44f * s, p)
        canvas.drawCircle(cx - 12f * s, cy - 16f * s, 36f * s, p)
        canvas.drawCircle(cx + 55f * s, cy + 8f * s, 30f * s, p)
    }
}
