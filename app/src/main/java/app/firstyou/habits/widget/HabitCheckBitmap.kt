package app.firstyou.habits.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

fun habitCheckBitmap(done: Boolean, accent: Int, sizePx: Int, densityDpi: Int): Bitmap {
    val size = sizePx.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bitmap.density = densityDpi
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val inset = size * 0.12f
    val rect = RectF(inset, inset, size - inset, size - inset)
    val radius = size * 0.22f
    if (done) {
        paint.style = Paint.Style.FILL
        paint.color = accent
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeWidth = size * 0.09f
        paint.color = if (isLight(accent)) 0xFF1C1B1A.toInt() else 0xFFFFFFFF.toInt()
        canvas.drawLine(size * 0.28f, size * 0.52f, size * 0.44f, size * 0.68f, paint)
        canvas.drawLine(size * 0.44f, size * 0.68f, size * 0.74f, size * 0.34f, paint)
    } else {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.075f
        paint.color = 0xFF8E8E8E.toInt()
        canvas.drawRoundRect(rect, radius, radius, paint)
    }
    return bitmap
}

private fun isLight(color: Int): Boolean {
    val red = (color shr 16) and 0xFF
    val green = (color shr 8) and 0xFF
    val blue = color and 0xFF
    return 0.299 * red + 0.587 * green + 0.114 * blue > 170
}
