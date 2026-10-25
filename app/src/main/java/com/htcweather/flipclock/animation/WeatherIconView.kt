package com.htcweather.flipclock.animation

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.htcweather.flipclock.data.model.WeatherType
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated weather icon in the style of classic HTC Sense (sun with rays, rain cloud, etc.)
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

    private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")
        style = Paint.Style.FILL
    }
    private val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFECB3")
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#ECEFF1")
        style = Paint.Style.FILL
    }
    private val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64B5F6")
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }
    private val snowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val drops = mutableListOf<Pair<Float, Float>>()

    private val animRunnable = object : Runnable {
        override fun run() {
            rotationAngle = (rotationAngle + 0.8f) % 360f
            pulse += 0.04f * pulseDir
            if (pulse > 1f) pulseDir = -1f
            if (pulse < 0f) pulseDir = 1f

            // animate rain/snow positions
            for (i in drops.indices) {
                val (x, y) = drops[i]
                drops[i] = x to (y + 4f)
                if (y > height) drops[i] = x to -10f
            }
            invalidate()
            postDelayed(this, 16)
        }
    }

    fun setWeather(type: WeatherType) {
        weatherType = type
        drops.clear()
        if (type == WeatherType.RAIN || type == WeatherType.DRIZZLE || type == WeatherType.THUNDERSTORM) {
            repeat(8) {
                drops.add((20f + it * 12f) to (Random.nextFloat() * 40f))
            }
        } else if (type == WeatherType.SNOW) {
            repeat(10) {
                drops.add((15f + it * 10f) to (Random.nextFloat() * 50f))
            }
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
        val radius = minOf(width, height) * 0.28f

        when (weatherType) {
            WeatherType.CLEAR -> drawSun(canvas, cx, cy, radius)
            WeatherType.CLOUDS -> drawCloud(canvas, cx, cy, radius * 1.3f)
            WeatherType.RAIN, WeatherType.DRIZZLE -> {
                drawCloud(canvas, cx, cy - 10f, radius * 1.1f)
                drawRain(canvas)
            }
            WeatherType.THUNDERSTORM -> {
                drawCloud(canvas, cx, cy - 10f, radius * 1.1f)
                drawRain(canvas)
                // simple lightning bolt
                val bolt = Path().apply {
                    moveTo(cx + 10, cy - 20)
                    lineTo(cx - 5, cy + 5)
                    lineTo(cx + 8, cy + 5)
                    lineTo(cx - 10, cy + 30)
                }
                sunPaint.color = Color.parseColor("#FFEB3B")
                canvas.drawPath(bolt, sunPaint)
                sunPaint.color = Color.parseColor("#FFD54F")
            }
            WeatherType.SNOW -> {
                drawCloud(canvas, cx, cy - 15f, radius * 1.1f)
                drawSnow(canvas)
            }
            WeatherType.MIST -> {
                cloudPaint.alpha = 120
                drawCloud(canvas, cx, cy, radius * 1.4f)
                cloudPaint.alpha = 255
            }
            else -> drawSun(canvas, cx, cy, radius)
        }
    }

    private fun drawSun(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        // Rays
        canvas.save()
        canvas.rotate(rotationAngle, cx, cy)
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val startR = radius * 1.3f
            val endR = radius * (1.7f + pulse * 0.15f)
            canvas.drawLine(
                cx + (cos(angle) * startR).toFloat(),
                cy + (sin(angle) * startR).toFloat(),
                cx + (cos(angle) * endR).toFloat(),
                cy + (sin(angle) * endR).toFloat(),
                rayPaint
            )
        }
        canvas.restore()

        // Glow
        val glow = RadialGradient(
            cx, cy, radius * 1.6f,
            intArrayOf(Color.argb(80, 255, 213, 79), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = glow }
        canvas.drawCircle(cx, cy, radius * 1.6f, glowPaint)

        // Core
        canvas.drawCircle(cx, cy, radius, sunPaint)
    }

    private fun drawCloud(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        canvas.drawCircle(cx - size * 0.35f, cy, size * 0.45f, cloudPaint)
        canvas.drawCircle(cx + size * 0.3f, cy - size * 0.1f, size * 0.4f, cloudPaint)
        canvas.drawCircle(cx, cy - size * 0.25f, size * 0.5f, cloudPaint)
        canvas.drawCircle(cx + size * 0.1f, cy + size * 0.15f, size * 0.42f, cloudPaint)
    }

    private fun drawRain(canvas: Canvas) {
        drops.forEach { (x, y) ->
            canvas.drawLine(x, y, x - 2f, y + 14f, rainPaint)
        }
    }

    private fun drawSnow(canvas: Canvas) {
        drops.forEach { (x, y) ->
            canvas.drawCircle(x, y, 4f, snowPaint)
        }
    }

    private object Random {
        private val r = java.util.Random()
        fun nextFloat() = r.nextFloat()
    }
}
