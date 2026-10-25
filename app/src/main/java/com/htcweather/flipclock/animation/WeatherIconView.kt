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
 * Icône météo animée style HTC Diamond (soleil brillant, nuage + pluie…).
 */
class WeatherIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var weatherType: WeatherType = WeatherType.CLEAR
    private var rotationAngle = 0f
    private var pulse = 0f
    private var pulseDir = 1f
    private val drops = mutableListOf<Pair<Float, Float>>()

    private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#ECEFF1")
    }
    private val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64B5F6")
        strokeWidth = 3.5f
        strokeCap = Paint.Cap.ROUND
    }
    private val snowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }

    private val animRunnable = object : Runnable {
        override fun run() {
            rotationAngle = (rotationAngle + 0.6f) % 360f
            pulse += 0.035f * pulseDir
            if (pulse > 1f) pulseDir = -1f
            if (pulse < 0f) pulseDir = 1f
            for (i in drops.indices) {
                val (x, y) = drops[i]
                var ny = y + 3.5f
                if (ny > height) ny = -8f
                drops[i] = x to ny
            }
            invalidate()
            postDelayed(this, 16)
        }
    }

    fun setWeather(type: WeatherType) {
        weatherType = type
        drops.clear()
        if (type == WeatherType.RAIN || type == WeatherType.DRIZZLE || type == WeatherType.THUNDERSTORM) {
            repeat(10) { drops.add((12f + it * 9f) to (Random.nextFloat() * 50f)) }
        } else if (type == WeatherType.SNOW) {
            repeat(12) { drops.add((10f + it * 8f) to (Random.nextFloat() * 55f)) }
        }
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(animRunnable)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(animRunnable)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) * 0.30f

        when (weatherType) {
            WeatherType.CLEAR -> drawSun(canvas, cx, cy, radius)
            WeatherType.CLOUDS -> drawCloud(canvas, cx, cy, radius * 1.25f)
            WeatherType.RAIN, WeatherType.DRIZZLE -> {
                drawCloud(canvas, cx, cy - 12f, radius * 1.05f)
                drawRain(canvas)
            }
            WeatherType.THUNDERSTORM -> {
                drawCloud(canvas, cx, cy - 12f, radius * 1.05f)
                drawRain(canvas)
                // lightning bolt
                val bolt = Path().apply {
                    moveTo(cx + 8, cy - 18)
                    lineTo(cx - 6, cy + 2)
                    lineTo(cx + 6, cy + 2)
                    lineTo(cx - 8, cy + 28)
                }
                sunPaint.color = Color.parseColor("#FFEE58")
                sunPaint.shader = null
                canvas.drawPath(bolt, sunPaint)
            }
            WeatherType.SNOW -> {
                drawCloud(canvas, cx, cy - 14f, radius * 1.05f)
                drops.forEach { (x, y) ->
                    snowPaint.color = Color.WHITE
                    canvas.drawCircle(x, y, 3.5f, snowPaint)
                }
            }
            WeatherType.MIST -> {
                cloudPaint.alpha = 110
                drawCloud(canvas, cx, cy, radius * 1.35f)
                cloudPaint.alpha = 255
            }
            else -> drawSun(canvas, cx, cy, radius)
        }
    }

    private fun drawSun(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        // Glow
        for (i in 3 downTo 1) {
            sunPaint.shader = RadialGradient(
                cx, cy, radius * (1.3f + i * 0.4f),
                intArrayOf(Color.argb(25 + i * 8, 255, 200, 80), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, radius * (1.3f + i * 0.4f), sunPaint)
        }
        // Rays
        rayPaint.color = Color.argb(180, 255, 220, 130)
        canvas.save()
        canvas.rotate(rotationAngle, cx, cy)
        for (i in 0 until 10) {
            val a = Math.toRadians((i * 36).toDouble())
            val s = radius * 1.28f
            val e = radius * (1.65f + pulse * 0.12f)
            canvas.drawLine(
                cx + (cos(a) * s).toFloat(), cy + (sin(a) * s).toFloat(),
                cx + (cos(a) * e).toFloat(), cy + (sin(a) * e).toFloat(),
                rayPaint
            )
        }
        canvas.restore()
        // Core
        sunPaint.shader = RadialGradient(
            cx - radius * 0.2f, cy - radius * 0.2f, radius,
            intArrayOf(Color.parseColor("#FFF59D"), Color.parseColor("#FFD54F"), Color.parseColor("#FFA000")),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius, sunPaint)
        sunPaint.shader = null
    }

    private fun drawCloud(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        canvas.drawCircle(cx - size * 0.32f, cy, size * 0.42f, cloudPaint)
        canvas.drawCircle(cx + size * 0.28f, cy - size * 0.08f, size * 0.38f, cloudPaint)
        canvas.drawCircle(cx, cy - size * 0.22f, size * 0.48f, cloudPaint)
        canvas.drawCircle(cx + size * 0.08f, cy + size * 0.14f, size * 0.40f, cloudPaint)
        canvas.drawCircle(cx - size * 0.08f, cy + size * 0.1f, size * 0.36f, cloudPaint)
    }

    private fun drawRain(canvas: Canvas) {
        drops.forEach { (x, y) ->
            canvas.drawLine(x, y, x - 1.5f, y + 12f, rainPaint)
        }
    }
}
