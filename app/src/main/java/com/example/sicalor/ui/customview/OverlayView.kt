package com.example.sicalor.ui.customview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.example.sicalor.ui.data.BoundingBox

class OverlayView @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var results = listOf<BoundingBox>()
    private val boxPaint = Paint().apply {
        color = Color.rgb(160, 200, 120)
        strokeWidth = 8F
        style = Paint.Style.STROKE
    }
    private val textBackgroundPaint = Paint().apply {
        color = Color.parseColor("#80A0C878")
        style = Paint.Style.FILL
        isAntiAlias = true
        textSize = 50f
    }
    private val textPaint = Paint().apply {
        color = Color.parseColor("#DDEB9D")
        textSize = 48f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
        setShadowLayer(6f, 2f, 2f, Color.argb(150, 221, 235, 100))
    }

    fun clear() {
        results = emptyList()
        invalidate()
    }

    private val bounds = Rect()

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        results.forEach {
            val left = it.x1 * width
            val top = it.y1 * height
            val right = it.x2 * width
            val bottom = it.y2 * height

            canvas.drawRoundRect(
                RectF(left, top, right, bottom),
                25f,
                25f,
                boxPaint
            )

            setLayerType(LAYER_TYPE_SOFTWARE, boxPaint)

            val drawableText = it.clsName

            textPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()

            val backgroundRect = RectF(
                left,
                top,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING * 2,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING * 2
            )
            canvas.drawRoundRect(backgroundRect, 20f, 20f, textBackgroundPaint)

            canvas.drawText(
                drawableText,
                left + BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textPaint
            )
        }
    }

    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes
        invalidate()
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}