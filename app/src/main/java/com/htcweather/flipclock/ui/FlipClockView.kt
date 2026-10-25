package com.htcweather.flipclock.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import java.util.*
import kotlin.math.min

/**
 * Classic HTC-style Flip Clock (two digits for hours + two for minutes).
 * Each digit is a card that flips vertically when the number changes.
 */
class FlipClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var hours = 0
    private var minutes = 0

    private val digitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#212121")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F5F5F5")
        style = Paint.Style.FILL
    }

    private val cardShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33000000")
        style = Paint.Style.FILL
    }

    private val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BDBDBD")
        strokeWidth = 2f
    }

    // Flip animation state for each digit position (0=h1, 1=h2, 2=m1, 3=m2)
    private val flipProgress = FloatArray(4) { 0f }
    private val oldDigits = IntArray(4) { 0 }
    private val newDigits = IntArray(4) { 0 }
    private val isFlipping = BooleanArray(4) { false }

    private var digitWidth = 0f
    private var digitHeight = 0f
    private var cardRadius = 16f
    private var gap = 12f

    private val timer = Timer()
    private var lastMinute = -1

    init {
        // Update every second so we catch the minute change
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                post { updateTime() }
            }
        }, 0, 1000)
    }

    private fun updateTime() {
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)

        if (h != hours || m != minutes) {
            setTime(h, m, animate = true)
        }
    }

    fun setTime(h: Int, m: Int, animate: Boolean = true) {
        val newH1 = h / 10
        val newH2 = h % 10
        val newM1 = m / 10
        val newM2 = m % 10

        val targets = intArrayOf(newH1, newH2, newM1, newM2)
        val currents = intArrayOf(hours / 10, hours % 10, minutes / 10, minutes % 10)

        hours = h
        minutes = m

        for (i in 0..3) {
            if (targets[i] != currents[i]) {
                if (animate && !isFlipping[i]) {
                    startFlip(i, currents[i], targets[i])
                } else {
                    oldDigits[i] = targets[i]
                    newDigits[i] = targets[i]
                    flipProgress[i] = 0f
                }
            }
        }
        invalidate()
    }

    private fun startFlip(index: Int, from: Int, to: Int) {
        isFlipping[index] = true
        oldDigits[index] = from
        newDigits[index] = to
        flipProgress[index] = 0f

        val animator = ObjectAnimator.ofFloat(0f, 1f).apply {
            duration = 450
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                flipProgress[index] = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isFlipping[index] = false
                    oldDigits[index] = to
                    flipProgress[index] = 0f
                    invalidate()
                }
            })
        }
        animator.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        // Aspect roughly 3.2 : 1
        val height = (width / 3.2f).toInt()
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 4 cards + 1 separator space
        val totalGaps = gap * 3
        digitWidth = (w - totalGaps) / 4.2f
        digitHeight = h * 0.92f
        cardRadius = digitWidth * 0.12f
        digitPaint.textSize = digitHeight * 0.72f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val startX = (width - (digitWidth * 4 + gap * 3)) / 2f
        val startY = (height - digitHeight) / 2f

        // Hours
        drawDigitCard(canvas, startX, startY, 0)
        drawDigitCard(canvas, startX + digitWidth + gap, startY, 1)

        // Separator (two dots)
        val sepX = startX + digitWidth * 2 + gap * 1.5f
        val centerY = startY + digitHeight / 2
        canvas.drawCircle(sepX, centerY - digitHeight * 0.15f, 6f, separatorPaint)
        canvas.drawCircle(sepX, centerY + digitHeight * 0.15f, 6f, separatorPaint)

        // Minutes
        drawDigitCard(canvas, startX + digitWidth * 2 + gap * 2 + gap * 0.4f, startY, 2)
        drawDigitCard(canvas, startX + digitWidth * 3 + gap * 3 + gap * 0.4f, startY, 3)
    }

    private fun drawDigitCard(canvas: Canvas, x: Float, y: Float, index: Int) {
        val progress = flipProgress[index]
        val radius = cardRadius

        // Shadow
        canvas.drawRoundRect(
            x + 3, y + 4, x + digitWidth + 3, y + digitHeight + 4,
            radius, radius, cardShadowPaint
        )

        if (!isFlipping[index] || progress == 0f) {
            // Static card
            canvas.drawRoundRect(x, y, x + digitWidth, y + digitHeight, radius, radius, cardPaint)
            // Horizontal middle line
            canvas.drawLine(x + 4, y + digitHeight / 2, x + digitWidth - 4, y + digitHeight / 2, linePaint)
            // Digit
            val text = oldDigits[index].toString()
            val textY = y + digitHeight / 2 - (digitPaint.descent() + digitPaint.ascent()) / 2
            canvas.drawText(text, x + digitWidth / 2, textY, digitPaint)
            return
        }

        // Animated flip
        val midY = y + digitHeight / 2

        // Bottom half (new digit rising) – drawn first
        canvas.save()
        canvas.clipRect(x, midY, x + digitWidth, y + digitHeight)
        canvas.drawRoundRect(x, y, x + digitWidth, y + digitHeight, radius, radius, cardPaint)
        canvas.drawLine(x + 4, midY, x + digitWidth - 4, midY, linePaint)
        val newText = newDigits[index].toString()
        val textY = midY - (digitPaint.descent() + digitPaint.ascent()) / 2
        canvas.drawText(newText, x + digitWidth / 2, textY, digitPaint)
        canvas.restore()

        // Top half (old digit flipping down)
        canvas.save()
        val pivotY = midY
        // Simple scale-Y simulation of flip
        val scale = 1f - progress * 2f  // goes from 1 → -1
        if (scale > 0) {
            // Still showing top of old digit
            canvas.clipRect(x, y, x + digitWidth, midY)
            canvas.scale(1f, scale, x + digitWidth / 2, pivotY)
            canvas.drawRoundRect(x, y, x + digitWidth, y + digitHeight, radius, radius, cardPaint)
            canvas.drawLine(x + 4, midY, x + digitWidth - 4, midY, linePaint)
            val oldText = oldDigits[index].toString()
            canvas.drawText(oldText, x + digitWidth / 2, textY, digitPaint)
        } else {
            // Showing bottom of new digit (flipped)
            canvas.clipRect(x, midY, x + digitWidth, y + digitHeight)
            canvas.scale(1f, -scale, x + digitWidth / 2, pivotY)
            canvas.drawRoundRect(x, y, x + digitWidth, y + digitHeight, radius, radius, cardPaint)
            canvas.drawLine(x + 4, midY, x + digitWidth - 4, midY, linePaint)
            canvas.drawText(newText, x + digitWidth / 2, textY, digitPaint)
        }
        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        timer.cancel()
    }
}
