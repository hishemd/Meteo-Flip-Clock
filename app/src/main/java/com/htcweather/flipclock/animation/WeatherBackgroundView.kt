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
                repeat(18) {
                    sparks.add(Spark(Random.nextFloat() * w, Random.nextFloat() * h * 0.55f,
                        0.15f + Random.nextFloat() * 0.35f, 0f, 1f, 10f + Random.nextFloat() * 28f))
                }
                // Nuages hauts réalistes
                repeat(6) {
                    clouds.add(Cloud(Random.nextFloat() * w * 1.3f - w * 0.15f,
                        20f + Random.nextFloat() * h * 0.3f,
                        0.06f + Random.nextFloat() * 0.16f, 1.2f + Random.nextFloat() * 1.0f, 120 + Random.nextInt(50)))
                }
            }
            WeatherType.CLOUDS -> {
                repeat(11) {
                    clouds.add(Cloud(Random.nextFloat() * w * 1.5f - w * 0.25f,
                        15f + Random.nextFloat() * h * 0.42f,
                        0.1f + Random.nextFloat() * 0.3f, 1.1f + Random.nextFloat() * 1.3f, 140 + Random.nextInt(55)))
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
                val count = if (weatherType == WeatherType.DRIZZLE) 90 else 160
                repeat(count) {
                    val kind = Random.nextFloat()
                    val thick = when {
                        kind < 0.45f -> 1.2f
                        kind < 0.85f -> 2.0f
                        else -> 2.8f
                    }
                    val alpha = when {
                        kind < 0.45f -> 140
                        kind < 0.85f -> 190
                        else -> 230
                    }
                    rain.add(RainDrop(
                        Random.nextFloat() * w, Random.nextFloat() * h,
                        14f + Random.nextFloat() * 22f,
                        14f + Random.nextFloat() * 20f,
                        alpha, thick
                    ))
                }
                // Dark heavy rain clouds at top
                repeat(6) {
                    clouds.add(Cloud(Random.nextFloat() * w * 1.2f - w * 0.1f,
                        5f + Random.nextFloat() * h * 0.22f,
                        0.15f + Random.nextFloat() * 0.3f, 1.3f + Random.nextFloat() * 0.9f, 180 + Random.nextInt(40)))
                }
            }
            WeatherType.THUNDERSTORM -> {
                repeat(200) {
                    val kind = Random.nextFloat()
                    val thick = if (kind < 0.5f) 1.5f else 2.8f
                    rain.add(RainDrop(Random.nextFloat() * w, Random.nextFloat() * h,
                        18f + Random.nextFloat() * 28f, 18f + Random.nextFloat() * 26f,
                        200 + Random.nextInt(50), thick))
                }
                repeat(6) {
                    clouds.add(Cloud(Random.nextFloat() * w, 0f + Random.nextFloat() * h * 0.2f,
                        0.2f, 1.5f + Random.nextFloat() * 0.5f, 200))
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
        // Realistic day / night sky gradients
        val colors = when (weatherType) {
            WeatherType.CLEAR -> if (isDay)
                // HTML-like sunny: #1e90ff → #87ceeb → soft warm
                intArrayOf(0xFF1E90FF.toInt(), 0xFF4AA8FF.toInt(), 0xFF87CEEB.toInt(), 0xFFB8DCE8.toInt())
            else
                intArrayOf(0xFF0D1B2A.toInt(), 0xFF1B263B.toInt(), 0xFF2A3A55.toInt(), 0xFF415A77.toInt())
            WeatherType.CLOUDS -> if (isDay)
                intArrayOf(0xFF607D8B.toInt(), 0xFF78909C.toInt(), 0xFF90A4AE.toInt(), 0xFFB0BEC5.toInt())
            else
                intArrayOf(0xFF1A252F.toInt(), 0xFF2C3E50.toInt(), 0xFF34495E.toInt(), 0xFF3D566E.toInt())
            WeatherType.MIST -> if (isDay)
                intArrayOf(0xFF7EAECC.toInt(), 0xFF96BED6.toInt(), 0xFFB0CCE0.toInt(), 0xFFC8DCE8.toInt())
            else
                intArrayOf(0xFF1A1E28.toInt(), 0xFF2A3040.toInt(), 0xFF3A4250.toInt(), 0xFF4A5260.toInt())
            WeatherType.RAIN, WeatherType.DRIZZLE -> if (isDay)
                intArrayOf(0xFF1C2833.toInt(), 0xFF2C3E50.toInt(), 0xFF34495E.toInt(), 0xFF3D566E.toInt())
            else
                intArrayOf(0xFF0F1A24.toInt(), 0xFF1C2833.toInt(), 0xFF2C3E50.toInt(), 0xFF34495E.toInt())
            WeatherType.THUNDERSTORM -> if (isDay)
                intArrayOf(0xFF1A237E.toInt(), 0xFF283593.toInt(), 0xFF303F9F.toInt(), 0xFF3949AB.toInt())
            else
                intArrayOf(0xFF0D1440.toInt(), 0xFF1A237E.toInt(), 0xFF283593.toInt(), 0xFF303F9F.toInt())
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

        // Night: draw stars for clear / partly clear
        if (!isDay && (weatherType == WeatherType.CLEAR || weatherType == WeatherType.UNKNOWN)) {
            drawStars(canvas)
        }
    }

    private val starSeed = 42
    private fun drawStars(canvas: Canvas) {
        val rnd = java.util.Random(starSeed.toLong())
        val w = width.toFloat()
        val h = height.toFloat() * 0.75f
        for (i in 0 until 80) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            val size = 0.8f + rnd.nextFloat() * 1.8f
            val twinkle = 0.5f + 0.5f * kotlin.math.sin(time * (2f + rnd.nextFloat() * 3f) + i)
            val alpha = (120 + 100 * twinkle).toInt().coerceIn(40, 220)
            p.color = android.graphics.Color.argb(alpha, 255, 255, 255)
            canvas.drawCircle(x, y, size, p)
        }
        // moon for clear night
        if (weatherType == WeatherType.CLEAR) {
            val mx = width * 0.75f
            val my = height * 0.22f
            val mr = width * 0.10f
            // moon glow
            p.shader = RadialGradient(mx, my, mr * 2.2f,
                intArrayOf(android.graphics.Color.argb(40, 200, 210, 255), android.graphics.Color.TRANSPARENT),
                floatArrayOf(0.3f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(mx, my, mr * 2.2f, p)
            p.shader = null
            // moon body
            p.shader = RadialGradient(mx - mr * 0.25f, my - mr * 0.25f, mr,
                intArrayOf(0xFFF5F5FF.toInt(), 0xFFD0D0E8.toInt(), 0xFFA8A8C0.toInt()),
                floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(mx, my, mr, p)
            p.shader = null
            // simple crater shadows
            p.color = android.graphics.Color.argb(30, 80, 80, 100)
            canvas.drawCircle(mx + mr * 0.2f, my + mr * 0.15f, mr * 0.18f, p)
            canvas.drawCircle(mx - mr * 0.15f, my + mr * 0.3f, mr * 0.12f, p)
        }
    }

    private fun drawScene(canvas: Canvas) {
        when (weatherType) {
            WeatherType.CLEAR -> {
                if (isDay) {
                    drawSun(canvas)
                    // warm daylight haze particles
                    sparks.forEach {
                        p.color = Color.argb(22, 255, 240, 200)
                        canvas.drawCircle(it.x, it.y, it.size, p)
                    }
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
                    p.strokeWidth = it.thick
                    p.strokeCap = Paint.Cap.ROUND
                    val x2 = it.x - it.len * 0.25f
                    val y2 = it.y + it.len
                    p.shader = LinearGradient(
                        it.x, it.y, x2, y2,
                        intArrayOf(
                            Color.argb(0, 174, 194, 224),
                            Color.argb((it.alpha * 0.45f).toInt(), 174, 194, 224),
                            Color.argb(it.alpha, 200, 220, 255),
                            Color.argb(it.alpha, 255, 255, 255)
                        ),
                        floatArrayOf(0f, 0.2f, 0.75f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawLine(it.x, it.y, x2, y2, p)
                    p.shader = null
                }
                // Brouillard bas (comme HTML rain-mist)
                p.shader = LinearGradient(
                    0f, height * 0.65f, 0f, height.toFloat(),
                    intArrayOf(Color.TRANSPARENT, Color.argb(75, 80, 110, 140)),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, height * 0.65f, width.toFloat(), height.toFloat(), p)
                p.shader = null
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

    /** Nuages doux type photo (comme capture météo) – volumes flous animés */
    private fun drawCloud(canvas: Canvas, c: Cloud) {
        val s = c.scale
        val cx = c.x
        val cy = c.y + sin(time * 0.6f + c.x * 0.01f) * 4f // léger flottement vertical

        // Couleurs jour / nuit réalistes
        val (r1, g1, b1, aBase) = if (isDay) {
            // Blanc-gris lumineux
            listOf(250, 252, 255, c.alpha.coerceIn(90, 210))
        } else {
            // Bleu-gris nuit comme la capture
            listOf(95, 110, 140, c.alpha.coerceIn(100, 200))
        }
        val (r2, g2, b2) = if (isDay) listOf(220, 228, 240) else listOf(55, 65, 90)

        // Corps principal – plusieurs blobs flous (RadialGradient)
        data class Blob(val ox: Float, val oy: Float, val rad: Float, val alphaMul: Float)
        val blobs = listOf(
            Blob(0f, 0f, 58f * s, 1f),
            Blob(48f * s, -14f * s, 50f * s, 0.95f),
            Blob(-42f * s, 6f * s, 46f * s, 0.9f),
            Blob(18f * s, 22f * s, 52f * s, 0.92f),
            Blob(-14f * s, -20f * s, 42f * s, 0.88f),
            Blob(68f * s, 8f * s, 36f * s, 0.8f),
            Blob(-55f * s, -8f * s, 34f * s, 0.75f),
            Blob(30f * s, -28f * s, 38f * s, 0.7f)
        )
        for (b in blobs) {
            val rad = b.rad
            val a = (aBase * b.alphaMul).toInt().coerceIn(20, 230)
            p.shader = RadialGradient(
                cx + b.ox, cy + b.oy, rad,
                intArrayOf(
                    Color.argb(a, r1, g1, b1),
                    Color.argb((a * 0.55f).toInt(), r2, g2, b2),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx + b.ox, cy + b.oy, rad, p)
        }
        p.shader = null
    }
}
