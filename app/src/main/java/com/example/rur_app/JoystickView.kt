package com.example.rur_app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var hatRadius = 0f

    private var hatX = 0f
    private var hatY = 0f

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#33FFFFFF".toColorInt() // Semi-transparent white
        style = Paint.Style.FILL
    }

    private val hatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FFC700".toColorInt() // Brand yellow
        style = Paint.Style.FILL
        setShadowLayer(8f, 0f, 0f, Color.BLACK)
    }

    private var listener: OnJoystickMoveListener? = null

    interface OnJoystickMoveListener {
        fun onValueChanged(x: Float, y: Float)
    }

    fun setOnJoystickMoveListener(listener: OnJoystickMoveListener?) {
        this.listener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = min(w, h) / 3f
        hatRadius = baseRadius / 2f
        hatX = centerX
        hatY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw base
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)
        // Draw hat
        canvas.drawCircle(hatX, hatY, hatRadius, hatPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val distance = sqrt(dx * dx + dy * dy)

                if (distance < baseRadius) {
                    hatX = event.x
                    hatY = event.y
                } else {
                    val angle = atan2(dy, dx)
                    hatX = centerX + cos(angle) * baseRadius
                    hatY = centerY + sin(angle) * baseRadius
                }

                val normalizedX = (hatX - centerX) / baseRadius
                val normalizedY = -(hatY - centerY) / baseRadius // Invert Y so up is positive

                listener?.onValueChanged(normalizedX, normalizedY)
                invalidate()
                
                if (event.action == MotionEvent.ACTION_DOWN) {
                    performClick()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                hatX = centerX
                hatY = centerY
                listener?.onValueChanged(0f, 0f)
                invalidate()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
