package com.htcweather.flipclock.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import java.util.*
import kotlin.math.abs

/**
 * HTC-style Flip Clock – smooth vertical flip animation.
 */
class FlipClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var hours = -1
    private var minutes = -1

    private val digitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#212121")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isFakeBoldText = true
    }

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F8F8F8")
        style = Paint.Style.FILL
    }

    private val cardTopPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        style = Paint.Style.FILL
    }

    private val cardBottomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EEEEEE")
        style = Paint.Style.FILL
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40000000")
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }

    private val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BDBDBD")
        strokeWidth = 2.5f
    }

    // 4 digits: h1 h2 m1 m2
    private val flipProgress = FloatArray(4) { 0f }
    private val oldDigits = IntArray(4) { 0 }
    private val newDigits = IntArray(4) { 0 }
    private val isFlipping = BooleanArray(4) { false }
    private val animators = arrayOfNulls<ValueAnimator>(4)

    private var digitWidth = 0f
    private var digitHeight = 0f
    private var cardRadius = 18f
    private var gap = 10f

    private val timer = Timer("FlipClock", true)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // for blur shadow
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { post { tick() } }
        }, 0, 500)
    }

    private fun tick() {
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        if (h != hours || m != minutes) {
            setTime(h, m, animate = hours >= 0)
        }
    }

    fun setTime(h: Int, m: Int, animate: Boolean = true) {
        val targets = intArrayOf(h / 10, h % 10, m / 10, m % 10)
        val currents = if (hours < 0) targets.clone() else
            intArrayOf(hours / 10, hours % 10, minutes / 10, minutes % 10)

        hours = h
        minutes = m

        for (i in 0..3) {
            if (targets[i] != currents[i]) {
                if (animate && !isFlipping[i]) {
                    startFlip(i, currents[i], targets[i])
                } else if (!isFlipping[i]) {
                    oldDigits[i] = targets[i]
                    newDigits[i] = targets[i]
                    flipProgress[i] = 0f
                }
            } else if (!isFlipping[i]) {
                oldDigits[i] = targets[i]
                newDigits[i] = targets[i]
            }
        }
        invalidate()
    }

    private fun startFlip(index: Int, from: Int, to: Int) {
        animators[index]?.cancel()
        isFlipping[index] = true
        oldDigits[index] = from
        newDigits[index] = to
        flipProgress[index] = 0f

        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener {
                flipProgress[index] = it.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isFlipping[index] = false
                    oldDigits[index] = to
                    flipProgress[index] = 0f
                    invalidate()
                }
            })
        }
        animators[index] = anim
        anim.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (width / 3.0f).toInt().coerceAtLeast(120)
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val totalGaps = gap * 3.5f
        digitWidth = (w - totalGaps) / 4.3f
        digitHeight = h * 0.88f
        cardRadius = digitWidth * 0.14f
        digitPaint.textSize = digitHeight * 0.68f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val totalW = digitWidth * 4 + gap * 3.2f
        val startX = (width - totalW) / 2f
        val startY = (height - digitHeight) / 2f

        drawDigitCard(canvas, startX, startY, 0)
        drawDigitCard(canvas, startX + digitWidth + gap, startY, 1)

        // Colon
        val sepX = startX + digitWidth * 2 + gap * 1.6f
        val cy = startY + digitHeight / 2
        canvas.drawCircle(sepX, cy - digitHeight * 0.16f, 7f, separatorPaint)
        canvas.drawCircle(sepX, cy + digitHeight * 0.16f, 7f, separatorPaint)

        drawDigitCard(canvas, startX + digitWidth * 2 + gap * 2.2f, startY, 2)
        drawDigitCard(canvas, startX + digitWidth * 3 + gap * 3.2f, startY, 3)
    }

    private fun drawDigitCard(canvas: Canvas, x: Float, y: Float, index: Int) {
        val progress = flipProgress[index]
        val midY = y + digitHeight / 2
        val r = cardRadius

        // Soft shadow
        canvas.drawRoundRect(x + 2, y + 5, x + digitWidth + 2, y + digitHeight + 5, r, r, shadowPaint)

        if (!isFlipping[index] || progress <= 0.01f) {
            // Static card – top half slightly lighter
            canvas.drawRoundRect(x, y, x + digitWidth, y + digitHeight, r, r, cardPaint)
            canvas.drawRoundRect(x, y, x + digitWidth, midY, r, r, cardTopPaint)
            // redraw bottom corners
            canvas.drawRect(x, midY - r, x + digitWidth, midY, cardTopPaint)
            canvas.drawLine(x + 6, midY, x + digitWidth - 6, midY, linePaint)
            val text = oldDigits[index].toString()
            val textY = midY - (digitPaint.descent() + digitPaint.ascent()) / 2
            canvas.drawText(text, x + digitWidth / 2, textY, digitPaint)
            return
        }

        // === Animated flip ===
        // Bottom half always shows NEW digit (static)
        canvas.save()
        canvas.clipRect(x, midY, x + digitWidth, y + digitHeight)
        canvas.drawRoundRect(x, y, x + digitWidth, y + digitHeight, r, r, cardBottomPaint)
        canvas.drawLine(x + 6, midY, x + digitWidth - 6, midY, linePaint)
        val newText = newDigits[index].toString()
        val textY = midY - (digitPaint.descent() + digitPaint.ascent()) / 2
        canvas.drawText(newText, x + digitWidth / 2, textY, digitPaint)
        canvas.restore()

        // Top flap: folds down (0 → 0.5) then bottom of new rises (0.5 → 1)
        if (progress < 0.5f) {
            // Top half of OLD digit flipping down
            val t = progress * 2f // 0→1
            val scaleY = 1f - t
            canvas.save()
            canvas.clipRect(x, y, x + digitWidth, midY)
            canvas.scale(1f, scaleY.coerceAtLeast(0.01f), x + digitWidth / 2, midY)
            canvas.drawRoundRect(x, y, x + digitWidth, y + digitHeight, r, r, cardTopPaint)
            canvas.drawLine(x + 6, midY, x + digitWidth - 6, midY, linePaint)
            val oldText = oldDigits[index].toString()
            // Darken as it flips
            digitPaint.alpha = (255 * (1f - t * 0.4f)).toInt()
            canvas.drawText(oldText, x + digitWidth / 2, textY, digitPaint)
            digitPaint.alpha = 255
            canvas.restore()
        } else {
            // Bottom half of NEW digit flipping up into place
            val t = (progress - 0.5f) * 2f // 0→1
            val scaleY = t
            canvas.save()
            canvas.clipRect(x, midY, x + digitWidth, y + digitHeight)
            canvas.scale(1f, scaleY.coerceAtLeast(0.01f), x + digitWidth / 2, midY)
            canvas.drawRoundRect(x, y, x + digitWidth, y + digitHeight, r, r, cardBottomPaint)
            canvas.drawLine(x + 6, midY, x + digitWidth - 6, midY, linePaint)
            digitPaint.alpha = (255 * (0.6f + t * 0.4f)).toInt()
            canvas.drawText(newText, x + digitWidth / 2, textY, digitPaint)
            digitPaint.alpha = 255
            canvas.restore()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        timer.cancel()
        animators.forEach { it?.cancel() }
    }
}
