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
 * Full-screen animated background that changes with the weather.
 * - Clear / Sunny → soft gradient + floating light particles
 * - Clouds → moving cloud shapes
 * - Rain / Drizzle → falling rain drops
 * - Thunder → dark + occasional flashes
 * - Snow → falling snowflakes
 * - Mist → soft fog layers
 */
class WeatherBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var weatherType: WeatherType = WeatherType.CLEAR
    private var isDay = true

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 220, 220, 230)
        style = Paint.Style.FILL
    }

    private val particles = mutableListOf<Particle>()
    private val clouds = mutableListOf<Cloud>()
    private var flashAlpha = 0f
    private var lastFlashTime = 0L

    private val frameRunnable = object : Runnable {
        override fun run() {
            updateParticles()
            invalidate()
            postDelayed(this, 16) // ~60 fps
        }
    }

    data class Particle(
        var x: Float,
        var y: Float,
        var speed: Float,
        var size: Float,
        var alpha: Int = 200
    )

    data class Cloud(
        var x: Float,
        var y: Float,
        var speed: Float,
        var scale: Float
    )

    fun setWeather(type: WeatherType, day: Boolean = true) {
        weatherType = type
        isDay = day
        particles.clear()
        clouds.clear()
        initParticles()
        invalidate()
    }

    private fun initParticles() {
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)

        when (weatherType) {
            WeatherType.RAIN, WeatherType.DRIZZLE -> {
                repeat(80) {
                    particles.add(
                        Particle(
                            x = Random.nextFloat() * w,
                            y = Random.nextFloat() * h,
                            speed = 12f + Random.nextFloat() * 18f,
                            size = 1.5f + Random.nextFloat() * 2.5f
                        )
                    )
                }
            }
            WeatherType.SNOW -> {
                repeat(60) {
                    particles.add(
                        Particle(
                            x = Random.nextFloat() * w,
                            y = Random.nextFloat() * h,
                            speed = 2f + Random.nextFloat() * 4f,
                            size = 3f + Random.nextFloat() * 5f,
                            alpha = 150 + Random.nextInt(100)
                        )
                    )
                }
            }
            WeatherType.CLEAR -> {
                // Soft light orbs
                repeat(12) {
                    particles.add(
                        Particle(
                            x = Random.nextFloat() * w,
                            y = Random.nextFloat() * h * 0.6f,
                            speed = 0.3f + Random.nextFloat() * 0.6f,
                            size = 20f + Random.nextFloat() * 40f,
                            alpha = 30 + Random.nextInt(40)
                        )
                    )
                }
            }
            WeatherType.CLOUDS, WeatherType.MIST -> {
                repeat(6) {
                    clouds.add(
                        Cloud(
                            x = Random.nextFloat() * w * 1.5f - w * 0.25f,
                            y = 40f + Random.nextFloat() * h * 0.4f,
                            speed = 0.4f + Random.nextFloat() * 0.8f,
                            scale = 0.7f + Random.nextFloat() * 0.8f
                        )
                    )
                }
            }
            WeatherType.THUNDERSTORM -> {
                repeat(70) {
                    particles.add(
                        Particle(
                            x = Random.nextFloat() * w,
                            y = Random.nextFloat() * h,
                            speed = 15f + Random.nextFloat() * 25f,
                            size = 2f + Random.nextFloat() * 3f
                        )
                    )
                }
            }
            else -> {}
        }
    }

    private fun updateParticles() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        when (weatherType) {
            WeatherType.RAIN, WeatherType.DRIZZLE, WeatherType.THUNDERSTORM -> {
                particles.forEach {
                    it.y += it.speed
                    it.x += it.speed * 0.15f // slight wind
                    if (it.y > h) {
                        it.y = -10f
                        it.x = Random.nextFloat() * w
                    }
                }
                if (weatherType == WeatherType.THUNDERSTORM) {
                    val now = System.currentTimeMillis()
                    if (now - lastFlashTime > 4000 + Random.nextLong(5000)) {
                        flashAlpha = 0.7f
                        lastFlashTime = now
                    }
                    flashAlpha *= 0.92f
                }
            }
            WeatherType.SNOW -> {
                particles.forEach {
                    it.y += it.speed
                    it.x += sin(it.y * 0.02f) * 1.2f
                    if (it.y > h) {
                        it.y = -10f
                        it.x = Random.nextFloat() * w
                    }
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
                    if (it.x > w + 200) it.x = -300f
                }
            }
            else -> {}
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initParticles()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(frameRunnable)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(frameRunnable)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)
        drawWeatherElements(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        val shader = when (weatherType) {
            WeatherType.CLEAR -> {
                if (isDay) {
                    LinearGradient(
                        0f, 0f, 0f, height.toFloat(),
                        intArrayOf(Color.parseColor("#1A237E"), Color.parseColor("#0D47A1"), Color.parseColor("#01579B")),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.CLAMP
                    )
                } else {
                    LinearGradient(
                        0f, 0f, 0f, height.toFloat(),
                        intArrayOf(Color.parseColor("#0A0A1A"), Color.parseColor("#12122A"), Color.parseColor("#1A1A3A")),
                        null, Shader.TileMode.CLAMP
                    )
                }
            }
            WeatherType.CLOUDS -> LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(Color.parseColor("#263238"), Color.parseColor("#37474F"), Color.parseColor("#455A64")),
                null, Shader.TileMode.CLAMP
            )
            WeatherType.RAIN, WeatherType.DRIZZLE -> LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(Color.parseColor("#1A237E"), Color.parseColor("#283593"), Color.parseColor("#303F9F")),
                null, Shader.TileMode.CLAMP
            )
            WeatherType.THUNDERSTORM -> LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(Color.parseColor("#0D0D15"), Color.parseColor("#1A1A2E"), Color.parseColor("#212121")),
                null, Shader.TileMode.CLAMP
            )
            WeatherType.SNOW -> LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(Color.parseColor("#37474F"), Color.parseColor("#546E7A"), Color.parseColor("#78909C")),
                null, Shader.TileMode.CLAMP
            )
            WeatherType.MIST -> LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(Color.parseColor("#424242"), Color.parseColor("#616161"), Color.parseColor("#757575")),
                null, Shader.TileMode.CLAMP
            )
            else -> LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(Color.parseColor("#0D0D15"), Color.parseColor("#1A1A2E")),
                null, Shader.TileMode.CLAMP
            )
        }
        bgPaint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Lightning flash
        if (flashAlpha > 0.05f) {
            canvas.drawColor(Color.argb((flashAlpha * 180).toInt(), 255, 255, 220))
        }
    }

    private fun drawWeatherElements(canvas: Canvas) {
        when (weatherType) {
            WeatherType.RAIN, WeatherType.DRIZZLE, WeatherType.THUNDERSTORM -> {
                particlePaint.color = Color.argb(180, 180, 210, 255)
                particlePaint.strokeWidth = 2.5f
                particlePaint.strokeCap = Paint.Cap.ROUND
                particles.forEach {
                    canvas.drawLine(it.x, it.y, it.x - it.size * 0.3f, it.y + it.size * 4, particlePaint)
                }
            }
            WeatherType.SNOW -> {
                particles.forEach {
                    particlePaint.color = Color.argb(it.alpha, 255, 255, 255)
                    canvas.drawCircle(it.x, it.y, it.size, particlePaint)
                }
            }
            WeatherType.CLEAR -> {
                particles.forEach {
                    particlePaint.color = Color.argb(it.alpha, 255, 240, 180)
                    canvas.drawCircle(it.x, it.y, it.size, particlePaint)
                }
            }
            WeatherType.CLOUDS, WeatherType.MIST -> {
                clouds.forEach { drawCloud(canvas, it) }
            }
            else -> {}
        }
    }

    private fun drawCloud(canvas: Canvas, cloud: Cloud) {
        val s = cloud.scale
        val cx = cloud.x
        val cy = cloud.y
        cloudPaint.alpha = if (weatherType == WeatherType.MIST) 90 else 160
        canvas.drawCircle(cx, cy, 50f * s, cloudPaint)
        canvas.drawCircle(cx + 40f * s, cy - 10f * s, 40f * s, cloudPaint)
        canvas.drawCircle(cx - 35f * s, cy + 5f * s, 35f * s, cloudPaint)
        canvas.drawCircle(cx + 15f * s, cy + 20f * s, 45f * s, cloudPaint)
    }
}
