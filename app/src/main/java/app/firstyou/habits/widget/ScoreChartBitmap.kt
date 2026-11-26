package app.firstyou.habits.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.sqrt

private val monthFormat = DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("es"))

fun renderScoreChart(
    title: String,
    points: List<Pair<LocalDate, Double>>,
    widthPx: Int,
    heightPx: Int,
    density: Float,
    densityDpi: Int,
    background: Int,
    textColor: Int,
    lineColor: Int,
    showTitle: Boolean,
    showAxis: Boolean,
    titleSizeSp: Int,
): Bitmap {
    val srcW = widthPx.coerceAtLeast(1)
    val srcH = heightPx.coerceAtLeast(1)
    val maxPixels = 160_000
    val down = if (srcW.toLong() * srcH > maxPixels) {
        sqrt(srcW.toDouble() * srcH / maxPixels).toFloat()
    } else {
        1f
    }
    val width = (srcW / down).toInt().coerceAtLeast(1)
    val height = (srcH / down).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.density = densityDpi
    val canvas = Canvas(bitmap)
    canvas.drawColor(background)
    fun d(dp: Float) = dp * density / down

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = Paint.Align.CENTER
        textSize = d(titleSizeSp.coerceIn(14, 28).toFloat())
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        alpha = 150
        textAlign = Paint.Align.RIGHT
        textSize = d(11f)
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    val yearPaint = Paint(labelPaint).apply {
        textSize = d(10f)
        alpha = 120
    }
    val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        alpha = 36
        strokeWidth = d(1f).coerceAtLeast(1f)
    }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        strokeWidth = d(2f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        style = Paint.Style.FILL
    }

    val heightDp = srcH / density.coerceAtLeast(1f)
    val showYear = showAxis && heightDp >= 160f
    val pad = d(if (heightDp < 160f) 8f else 12f)
    var top = pad
    if (showTitle) {
        val maxTitle = (width - pad * 2 - d(56f)).coerceAtLeast(d(40f))
        val shown = ellipsize(titlePaint, title, maxTitle)
        val baseline = top - titlePaint.fontMetrics.ascent
        canvas.drawText(shown, width / 2f, baseline, titlePaint)
        top = baseline + titlePaint.fontMetrics.descent + d(if (heightDp < 160f) 6f else 10f)
    }

    val labelHeight = labelPaint.fontMetrics.descent - labelPaint.fontMetrics.ascent
    val yearHeight = if (showYear) yearPaint.fontMetrics.descent - yearPaint.fontMetrics.ascent + d(1f) else 0f
    val bottom = height - pad - labelHeight - yearHeight - d(4f)
    val axisGap = d(6f)
    val left = if (showAxis) pad + labelPaint.measureText("100%") + axisGap else pad
    val right = width - pad
    if (points.size < 2 || right <= left + d(8f) || bottom <= top + d(24f)) {
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.alpha = 170
        val message = "Todavía no hay suficientes días"
        canvas.drawText(ellipsize(labelPaint, message, width - pad * 2), width / 2f, height / 2f, labelPaint)
        return bitmap
    }

    fun yOf(score: Double): Float {
        val clamped = score.coerceIn(0.0, 1.0).toFloat()
        return bottom - (bottom - top) * clamped
    }
    if (showAxis) {
        val levels = if (bottom - top < labelHeight * 6f) {
            listOf(1f to "100%", 0.5f to "50%", 0f to "0%")
        } else {
            listOf(1f to "100%", 0.8f to "80%", 0.6f to "60%", 0.4f to "40%", 0.2f to "20%")
        }
        levels.forEach { (level, label) ->
            val y = bottom - (bottom - top) * level
            canvas.drawLine(left, y, right, y, gridPaint)
            val baseline = y - (labelPaint.fontMetrics.ascent + labelPaint.fontMetrics.descent) / 2f
            canvas.drawText(label, left - axisGap, baseline, labelPaint)
        }
    }
    fun xOf(index: Int) = left + (right - left) * index / points.lastIndex
    for (index in 0 until points.lastIndex) {
        canvas.drawLine(
            xOf(index),
            yOf(points[index].second),
            xOf(index + 1),
            yOf(points[index + 1].second),
            linePaint,
        )
    }
    val radius = d(3.2f)
    points.forEachIndexed { index, point ->
        canvas.drawCircle(xOf(index), yOf(point.second), radius, dotPaint)
    }
    if (showAxis) {
        labelPaint.textAlign = Paint.Align.CENTER
        yearPaint.textAlign = Paint.Align.CENTER
        var lastRight = left
        var lastMonth = -1
        points.forEachIndexed { index, (date, _) ->
            val x = xOf(index)
            val monthChanged = date.monthValue != lastMonth
            val month = date.format(monthFormat).trim('.').lowercase(Locale.forLanguageTag("es"))
            val label = if (index == 0 || monthChanged) month else date.dayOfMonth.toString()
            val half = labelPaint.measureText(label) / 2f
            if (x - half < lastRight + d(4f) && index != 0) return@forEachIndexed
            if (x + half > right + d(2f)) return@forEachIndexed
            val y = bottom + d(3f) - labelPaint.fontMetrics.ascent
            canvas.drawText(label, x, y, labelPaint)
            if (index == 0 && showYear) {
                val yearY = y + labelPaint.fontMetrics.descent - yearPaint.fontMetrics.ascent + d(1f)
                canvas.drawText(date.year.toString(), x, yearY, yearPaint)
            }
            lastRight = x + half
            lastMonth = date.monthValue
        }
    }
    return bitmap
}

private fun ellipsize(paint: Paint, text: String, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    val ellipsis = "…"
    var end = text.length
    while (end > 1 && paint.measureText(text.take(end) + ellipsis) > maxWidth) end--
    return text.take(end).trimEnd() + ellipsis
}
