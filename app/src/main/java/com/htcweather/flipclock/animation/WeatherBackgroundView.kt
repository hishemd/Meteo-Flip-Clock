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
 * Fond animé style HTC Sense / Diamond :
 * dégradés sombres, soleil lumineux, pluie, nuages doux, particules.
 */
class WeatherBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var weatherType: WeatherType = WeatherType.CLEAR
    private var isDay = true
    private var sunAngle = 0f
    private var pulse = 0f
    private var pulseDir = 1f
    private var flashAlpha = 0f
    private var lastFlash = 0L

    private val particles = mutableListOf<Particle>()
    private val clouds = mutableListOf<Cloud>()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    data class Particle(var x: Float, var y: Float, var speed: Float, var size: Float, var alpha: Int = 200, var drift: Float = 0f)
    data class Cloud(var x: Float, var y: Float, var speed: Float, var scale: Float, var alpha: Int = 160)

    private val frameRunnable = object : Runnable {
        override fun run() {
            update()
            invalidate()
            postDelayed(this, 16)
        }
    }

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
                repeat(90) {
                    particles.add(
                        Particle(
                            Random.nextFloat() * w,
                            Random.nextFloat() * h,
                            10f + Random.nextFloat() * 16f,
                            1.5f + Random.nextFloat() * 2.5f,
                            160 + Random.nextInt(60),
                            0.1f + Random.nextFloat() * 0.2f
                        )
                    )
                }
                // soft clouds
                repeat(4) {
                    clouds.add(
                        Cloud(
                            Random.nextFloat() * w,
                            40f + Random.nextFloat() * h * 0.35f,
                            0.2f + Random.nextFloat() * 0.4f,
                            0.9f + Random.nextFloat() * 0.8f,
                            100 + Random.nextInt(50)
                        )
                    )
                }
            }
            WeatherType.THUNDERSTORM -> {
                repeat(100) {
                    particles.add(
                        Particle(
                            Random.nextFloat() * w, Random.nextFloat() * h,
                            14f + Random.nextFloat() * 22f, 2f + Random.nextFloat() * 3f,
                            180, 0.15f
                        )
                    )
                }
                repeat(3) {
                    clouds.add(Cloud(Random.nextFloat() * w, 30f + Random.nextFloat() * h * 0.3f, 0.15f, 1.2f, 140))
                }
            }
            WeatherType.SNOW -> {
                repeat(65) {
                    particles.add(
                        Particle(
                            Random.nextFloat() * w, Random.nextFloat() * h,
                            1.5f + Random.nextFloat() * 3.5f, 3f + Random.nextFloat() * 5f,
                            150 + Random.nextInt(90), Random.nextFloat() * 0.02f
                        )
                    )
                }
            }
            WeatherType.CLEAR -> {
                // floating light orbs (HTC glow particles)
                repeat(18) {
                    particles.add(
                        Particle(
                            Random.nextFloat() * w,
                            Random.nextFloat() * h * 0.7f,
                            0.15f + Random.nextFloat() * 0.35f,
                            8f + Random.nextFloat() * 28f,
                            20 + Random.nextInt(35)
                        )
                    )
                }
                // soft distant clouds
                if (isDay) {
                    repeat(3) {
                        clouds.add(
                            Cloud(
                                Random.nextFloat() * w,
                                h * 0.45f + Random.nextFloat() * h * 0.25f,
                                0.15f + Random.nextFloat() * 0.25f,
                                0.7f + Random.nextFloat() * 0.5f,
                                50 + Random.nextInt(40)
                            )
                        )
                    }
                }
            }
            WeatherType.CLOUDS, WeatherType.MIST -> {
                repeat(7) {
                    clouds.add(
                        Cloud(
                            Random.nextFloat() * w * 1.3f - w * 0.15f,
                            50f + Random.nextFloat() * h * 0.5f,
                            0.25f + Random.nextFloat() * 0.55f,
                            0.8f + Random.nextFloat() * 1.1f,
                            if (weatherType == WeatherType.MIST) 70 else 130
                        )
                    )
                }
            }
            else -> {}
        }
    }

    private fun update() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        sunAngle = (sunAngle + 0.35f) % 360f
        pulse += 0.025f * pulseDir
        if (pulse > 1f) pulseDir = -1f
        if (pulse < 0f) pulseDir = 1f

        when (weatherType) {
            WeatherType.RAIN, WeatherType.DRIZZLE, WeatherType.THUNDERSTORM -> {
                particles.forEach {
                    it.y += it.speed
                    it.x += it.speed * it.drift
                    if (it.y > h + 20) {
                        it.y = -20f
                        it.x = Random.nextFloat() * w
                    }
                }
                clouds.forEach {
                    it.x += it.speed
                    if (it.x > w + 200) it.x = -280f
                }
                if (weatherType == WeatherType.THUNDERSTORM) {
                    val now = System.currentTimeMillis()
                    if (now - lastFlash > 4000 + Random.nextLong(5000)) {
                        flashAlpha = 0.65f
                        lastFlash = now
                    }
                    flashAlpha *= 0.91f
                }
            }
            WeatherType.SNOW -> {
                particles.forEach {
                    it.y += it.speed
                    it.x += sin(it.y * 0.02f + it.drift * 50) * 1.3f
                    if (it.y > h) {
                        it.y = -10f
                        it.x = Random.nextFloat() * w
                    }
                }
            }
            WeatherType.CLEAR -> {
                particles.forEach {
                    it.x += it.speed
                    it.y += sin(it.x * 0.01f) * 0.15f
                    if (it.x > w + it.size) it.x = -it.size
                }
                clouds.forEach {
                    it.x += it.speed
                    if (it.x > w + 200) it.x = -250f
                }
            }
            WeatherType.CLOUDS, WeatherType.MIST -> {
                clouds.forEach {
                    it.x += it.speed
                    if (it.x > w + 250) it.x = -300f
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
        drawGradient(canvas)
        drawWeather(canvas)
        if (flashAlpha > 0.04f) {
            canvas.drawColor(Color.argb((flashAlpha * 180).toInt(), 255, 255, 230))
        }
    }

    private fun drawGradient(canvas: Canvas) {
        val colors = when (weatherType) {
            WeatherType.CLEAR -> if (isDay)
                // HTC day: deep blue to softer blue
                intArrayOf(
                    Color.parseColor("#0A1628"),
                    Color.parseColor("#0D2137"),
                    Color.parseColor("#123A5C"),
                    Color.parseColor("#1A4A6E")
                )
            else
                intArrayOf(
                    Color.parseColor("#050510"),
                    Color.parseColor("#0A0A1A"),
                    Color.parseColor("#12122A"),
                    Color.parseColor("#1A1A35")
                )
            WeatherType.CLOUDS -> intArrayOf(
                Color.parseColor("#1A1F28"),
                Color.parseColor("#252B36"),
                Color.parseColor("#2E3642"),
                Color.parseColor("#3A4452")
            )
            WeatherType.RAIN, WeatherType.DRIZZLE -> intArrayOf(
                Color.parseColor("#0C1220"),
                Color.parseColor("#121C30"),
                Color.parseColor("#1A2840"),
                Color.parseColor("#223550")
            )
            WeatherType.THUNDERSTORM -> intArrayOf(
                Color.parseColor("#08080E"),
                Color.parseColor("#101018"),
                Color.parseColor("#181820"),
                Color.parseColor("#222228")
            )
            WeatherType.SNOW -> intArrayOf(
                Color.parseColor("#1C2430"),
                Color.parseColor("#2A3440"),
                Color.parseColor("#3A4654"),
                Color.parseColor("#4A5868")
            )
            WeatherType.MIST -> intArrayOf(
                Color.parseColor("#2A2A30"),
                Color.parseColor("#3A3A42"),
                Color.parseColor("#4A4A52"),
                Color.parseColor("#5A5A62")
            )
            else -> intArrayOf(
                Color.parseColor("#0D0D15"),
                Color.parseColor("#151520"),
                Color.parseColor("#1A1A2E")
            )
        }
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            colors, floatArrayOf(0f, 0.35f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
    }

    private fun drawWeather(canvas: Canvas) {
        when (weatherType) {
            WeatherType.CLEAR -> {
                drawHtcSun(canvas)
                // soft orbs
                particles.forEach {
                    particlePaint.color = Color.argb(it.alpha, 255, 235, 180)
                    canvas.drawCircle(it.x, it.y, it.size, particlePaint)
                }
                clouds.forEach { drawSoftCloud(canvas, it) }
            }
            WeatherType.RAIN, WeatherType.DRIZZLE, WeatherType.THUNDERSTORM -> {
                clouds.forEach { drawSoftCloud(canvas, it) }
                // rain streaks – HTC style thin blue lines
                particlePaint.strokeWidth = 2.2f
                particlePaint.strokeCap = Paint.Cap.ROUND
                particles.forEach {
                    particlePaint.color = Color.argb(it.alpha, 140, 190, 255)
                    canvas.drawLine(
                        it.x, it.y,
                        it.x - it.size * 0.35f, it.y + it.size * 6f,
                        particlePaint
                    )
                }
            }
            WeatherType.SNOW -> {
                particles.forEach {
                    particlePaint.color = Color.argb(it.alpha, 255, 255, 255)
                    canvas.drawCircle(it.x, it.y, it.size, particlePaint)
                }
            }
            WeatherType.CLOUDS, WeatherType.MIST -> {
                clouds.forEach { drawSoftCloud(canvas, it) }
            }
            else -> {}
        }
    }

    /** Soleil style HTC Diamond : gros, brillant, rayons doux */
    private fun drawHtcSun(canvas: Canvas) {
        val cx = width * 0.72f
        val cy = height * 0.38f
        val r = width * 0.16f * (1f + pulse * 0.04f)

        // Outer glow (multiple layers like HTC)
        for (i in 4 downTo 1) {
            val gr = r * (1.4f + i * 0.35f)
            val alpha = (18 - i * 3).coerceAtLeast(4)
            sunPaint.shader = RadialGradient(
                cx, cy, gr,
                intArrayOf(
                    Color.argb(alpha + 10, 255, 200, 80),
                    Color.argb(0, 255, 200, 80)
                ),
                floatArrayOf(0.3f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, gr, sunPaint)
        }
        sunPaint.shader = null

        // Rays
        rayPaint.strokeWidth = 5f
        rayPaint.color = Color.argb(160, 255, 220, 120)
        for (i in 0 until 12) {
            val a = Math.toRadians((i * 30.0 + sunAngle))
            val start = r * 1.25f
            val end = r * (1.75f + pulse * 0.12f)
            canvas.drawLine(
                cx + (cos(a) * start).toFloat(),
                cy + (sin(a) * start).toFloat(),
                cx + (cos(a) * end).toFloat(),
                cy + (sin(a) * end).toFloat(),
                rayPaint
            )
        }

        // Core – bright yellow like HTC
        sunPaint.shader = RadialGradient(
            cx - r * 0.15f, cy - r * 0.15f, r,
            intArrayOf(
                Color.parseColor("#FFF59D"),
                Color.parseColor("#FFD54F"),
                Color.parseColor("#FFB300")
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, sunPaint)
        sunPaint.shader = null
    }

    private fun drawSoftCloud(canvas: Canvas, c: Cloud) {
        cloudPaint.color = Color.argb(c.alpha, 230, 235, 245)
        val s = c.scale
        val cx = c.x
        val cy = c.y
        // HTC-style fluffy cloud (overlapping circles)
        canvas.drawCircle(cx, cy, 48f * s, cloudPaint)
        canvas.drawCircle(cx + 38f * s, cy - 8f * s, 40f * s, cloudPaint)
        canvas.drawCircle(cx - 32f * s, cy + 4f * s, 36f * s, cloudPaint)
        canvas.drawCircle(cx + 12f * s, cy + 18f * s, 42f * s, cloudPaint)
        canvas.drawCircle(cx - 10f * s, cy - 18f * s, 34f * s, cloudPaint)
    }
}
