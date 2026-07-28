package com.kaz.midnight

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class CircleColorWheel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var wheelBitmap: Bitmap? = null
    private var centerX = 0f
    private var centerY = 0f
    private var wheelRadius = 0f
    private var cursorX = 0f
    private var cursorY = 0f
    private var currentHue = 0f
    private var currentSat = 0f
    private var currentValue = 1f
    private var currentColor = Color.RED

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.argb(80, 0, 0, 0)
    }

    var onColorChanged: ((Int) -> Unit)? = null

    var selectedColor: Int
        get() = currentColor
        set(color) {
            currentColor = color
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)
            currentHue = hsv[0]
            currentSat = hsv[1]
            currentValue = hsv[2]
            updateCursorPos()
            invalidate()
        }

    private fun updateCursorPos() {
        val angle = Math.toRadians(currentHue.toDouble()).toFloat()
        val dist = currentSat * wheelRadius
        cursorX = centerX + dist * cos(angle)
        cursorY = centerY - dist * sin(angle)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        wheelRadius = min(centerX, centerY) * 0.92f
        generateWheelBitmap()
        updateCursorPos()
    }

    private fun generateWheelBitmap() {
        val size = (wheelRadius * 2).toInt()
        if (size <= 0) return
        val cx = wheelRadius
        val cy = wheelRadius
        val pixels = IntArray(size * size)

        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = x - cx
                val dy = y - cy
                val dist = sqrt(dx * dx + dy * dy)
                if (dist <= wheelRadius) {
                    val angle = atan2(-dy.toDouble(), dx.toDouble())
                    val hue = ((Math.toDegrees(angle) + 360) % 360).toFloat()
                    val sat = (dist / wheelRadius).coerceAtMost(1f)
                    pixels[y * size + x] = Color.HSVToColor(floatArrayOf(hue, sat, 1f))
                } else {
                    pixels[y * size + x] = 0
                }
            }
        }
        wheelBitmap?.recycle()
        wheelBitmap = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        wheelBitmap?.let { canvas.drawBitmap(it, centerX - wheelRadius, centerY - wheelRadius, null) }

        val cr = max(10f, wheelRadius * 0.05f + 8f)
        fillPaint.color = currentColor
        canvas.drawCircle(cursorX, cursorY, cr + 2, shadowPaint)
        canvas.drawCircle(cursorX, cursorY, cr, fillPaint)
        canvas.drawCircle(cursorX, cursorY, cr, strokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> pickColorAt(event.x, event.y)
        }
        return true
    }

    private fun pickColorAt(x: Float, y: Float) {
        val dx = x - centerX
        val dy = y - centerY
        val dist = sqrt(dx * dx + dy * dy)

        if (dist > wheelRadius) return

        if (dist > 0f) {
            val angle = atan2(-dy.toDouble(), dx.toDouble())
            currentHue = ((Math.toDegrees(angle) + 360) % 360).toFloat()
            currentSat = (dist / wheelRadius).coerceIn(0f, 1f)
        } else {
            currentSat = 0f
        }
        cursorX = x
        cursorY = y
        currentValue = 1f
        currentColor = Color.HSVToColor(floatArrayOf(currentHue, currentSat, currentValue))
        onColorChanged?.invoke(currentColor)
        invalidate()
    }

    fun setRGB(r: Int, g: Int, b: Int) {
        selectedColor = Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }
}
