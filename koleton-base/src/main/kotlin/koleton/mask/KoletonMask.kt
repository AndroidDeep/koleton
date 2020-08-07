package koleton.mask

import android.graphics.*
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import koleton.custom.Attributes
import koleton.util.*
import kotlin.math.absoluteValue

internal class KoletonMask(
    private val view: View,
    private val attrs: Attributes
) {

    private val paint: Paint by lazy { Paint().apply { color = attrs.color } }
    private val bitmap: Bitmap by lazy { Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ALPHA_8) }
    private val canvas: Canvas by lazy { Canvas(bitmap) }
    private val lineSpacingPerLine: Float by lazy { attrs.lineSpacing / 2 }

    init {
        val paint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
            isAntiAlias = attrs.cornerRadius > NUMBER_ZERO
        }
        mask(view, view as ViewGroup, paint)
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }

    private fun mask(view: View, root: ViewGroup, paint: Paint) {
        when (view) {
            is ViewGroup -> view.children().forEach { v -> mask(v, root, paint) }
            is TextView -> maskTextView(view, root)
            else -> maskView(view, root, paint)
        }
    }

    private fun maskView(view: View, root: ViewGroup, paint: Paint) {
        val rect = Rect().also {
            view.getDrawingRect(it)
            root.offsetDescendantRectToMyCoords(view, it)
        }
        canvas.drawRoundRect(RectF(rect), attrs.cornerRadius, attrs.cornerRadius, paint)
    }

    private fun maskTextView(
        view: TextView,
        root: ViewGroup
    ) {
        val rect = Rect().also {
            view.getDrawingRect(it)
            root.offsetDescendantRectToMyCoords(view, it)
        }
        val textPaint = view.paint.apply { isAntiAlias = attrs.cornerRadius > NUMBER_ZERO }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            maskStaticLayout(view, rect, textPaint)
        } else {
            maskLineWrapping(view, rect, textPaint)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun maskStaticLayout(
        view: TextView,
        rect: Rect,
        textPaint: TextPaint
    ) {
        val spannable = spannable { background(attrs.color, attrs.cornerRadius, lineSpacingPerLine, view.validatedText(attrs)) }
        val staticLayout = StaticLayout.Builder
            .obtain(spannable, 0, spannable.length, textPaint.apply { color = Color.TRANSPARENT }, view.width)
            .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
            .setIncludePad(view.includeFontPadding)
            .setMaxLines(view.lineCount)
            .build()
        canvas.save()
        canvas.translate(rect.left.toFloat(), rect.top.toFloat())
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun maskLineWrapping(
        view: TextView,
        rect: Rect,
        textPaint: TextPaint
    ) {
        if (view.lineCount.isZero()) return
        val measuredWidth = floatArrayOf(0F)
        var startIndex = 0
        var count: Int
        var lineIndex = 0
        val validatedText = view.validatedText(attrs)
        while (startIndex < validatedText.length) {
            count = textPaint.breakText(
                validatedText,
                startIndex,
                validatedText.length,
                true,
                view.width.toFloat(),
                measuredWidth
            )
            val topOffset = view.height * lineIndex / view.lineCount
            val bottomOffset =
                view.height * (lineIndex - (view.lineCount - NUMBER_ONE)).absoluteValue / view.lineCount
            val top = rect.top.toFloat() + (topOffset + lineSpacingPerLine)
            val bottom = rect.bottom.toFloat() - (bottomOffset + lineSpacingPerLine)
            val right = rect.left.toFloat() + measuredWidth[0]
            val rectF = RectF(
                rect.left.toFloat(),
                top,
                if (right > rect.right * WRAPPING_LIMIT) rect.right.toFloat() else right,
                bottom
            )
            canvas.drawRoundRect(rectF, attrs.cornerRadius, attrs.cornerRadius, textPaint)
            startIndex += count
            lineIndex++
        }
    }
}