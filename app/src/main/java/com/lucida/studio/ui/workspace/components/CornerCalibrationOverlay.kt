package com.lucida.studio.ui.workspace.components

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.lucida.studio.domain.math.OneEuroFilter2D
import kotlin.math.hypot

@Composable
fun CornerCalibrationOverlay(
    corners: List<Float>,
    onMove: (index: Int, x: Float, y: Float) -> Unit,
    previewBitmap: () -> Bitmap?,
    modifier: Modifier = Modifier
) {
    val latestCorners by rememberUpdatedState(corners)
    val latestMove by rememberUpdatedState(onMove)
    val latestPreview by rememberUpdatedState(previewBitmap)
    val density = LocalDensity.current.density
    val hitRadius = with(LocalDensity.current) { 40.dp.toPx() }
    val filter = remember { OneEuroFilter2D() }
    var grabbed by remember { mutableIntStateOf(-1) }
    var raw by remember { mutableStateOf(Offset.Zero) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var lastPreviewTime by remember { mutableStateOf(0L) }
    val stroke = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFB300.toInt(); style = Paint.Style.STROKE
    } }
    val text = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF8F9FA.toInt(); typeface = android.graphics.Typeface.MONOSPACE
    } }
    val imagePaint = remember { Paint(Paint.FILTER_BITMAP_FLAG) }
    Canvas(modifier.pointerInput(hitRadius) {
        detectDragGestures(
            onDragStart = { position ->
                val points = latestCorners
                grabbed = if (points.size == 8) (0..3).minByOrNull { i ->
                    hypot(position.x - points[i * 2], position.y - points[i * 2 + 1])
                }?.takeIf { i -> hypot(position.x - points[i * 2], position.y - points[i * 2 + 1]) <= hitRadius } ?: -1 else -1
                if (grabbed >= 0) {
                    raw = Offset(points[grabbed * 2], points[grabbed * 2 + 1])
                    filter.reset()
                    filter.filter(raw.x, raw.y, System.nanoTime() / 1_000_000_000.0)
                    preview = latestPreview()
                    lastPreviewTime = android.os.SystemClock.elapsedRealtime()
                }
            },
            onDragEnd = { grabbed = -1; preview = null },
            onDragCancel = { grabbed = -1; preview = null },
            onDrag = { change, delta ->
                if (grabbed >= 0) {
                    change.consume()
                    raw += delta
                    val filtered = filter.filter(raw.x, raw.y, System.nanoTime() / 1_000_000_000.0)
                    latestMove(grabbed, filtered.first, filtered.second)
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (now - lastPreviewTime > 120) {
                        preview = latestPreview()
                        lastPreviewTime = now
                    }
                }
            }
        )
    }) {
        if (corners.size != 8) return@Canvas
        val canvas = drawContext.canvas.nativeCanvas
        stroke.strokeWidth = 1.5f * density
        text.textSize = 12f * density
        for (i in 0..3) {
            val x = corners[i * 2]
            val y = corners[i * 2 + 1]
            val radius = if (i == grabbed) 16f * density else 12f * density
            canvas.drawCircle(x, y, radius, stroke)
            canvas.drawLine(x - radius - 7f * density, y, x + radius + 7f * density, y, stroke)
            canvas.drawLine(x, y - radius - 7f * density, x, y + radius + 7f * density, stroke)
            canvas.drawText(listOf("TL", "TR", "BR", "BL")[i], x + 16f * density, y - 16f * density, text)
        }
        if (grabbed >= 0) {
            val pointX = corners[grabbed * 2]
            val pointY = corners[grabbed * 2 + 1]
            val radius = 52f * density
            val lx = (pointX + 80f * density).coerceIn(radius, (size.width - radius).coerceAtLeast(radius))
            val candidateY = if (pointY > 160f * density) pointY - 90f * density else pointY + 90f * density
            val ly = candidateY.coerceIn(radius, (size.height - radius).coerceAtLeast(radius))
            val clip = Path().apply { addCircle(lx, ly, radius, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(clip)
            canvas.drawColor(0xFF121417.toInt())
            val bitmap = preview
            if (bitmap != null && !bitmap.isRecycled && size.width > 0 && size.height > 0) {
                val cx = pointX / size.width * bitmap.width
                val cy = pointY / size.height * bitmap.height
                val half = radius / 2.5f * bitmap.width / size.width
                val left = (cx - half).toInt().coerceIn(0, bitmap.width - 1)
                val top = (cy - half).toInt().coerceIn(0, bitmap.height - 1)
                val right = (cx + half).toInt().coerceIn(left + 1, bitmap.width)
                val bottom = (cy + half).toInt().coerceIn(top + 1, bitmap.height)
                canvas.drawBitmap(bitmap, Rect(left, top, right, bottom), RectF(lx - radius, ly - radius, lx + radius, ly + radius), imagePaint)
            } else canvas.drawText("PAPER", lx - 18f * density, ly + 24f * density, text)
            canvas.restore()
            canvas.drawCircle(lx, ly, radius, stroke)
            canvas.drawLine(lx - 15f * density, ly, lx + 15f * density, ly, stroke)
            canvas.drawLine(lx, ly - 15f * density, lx, ly + 15f * density, stroke)
        }
    }
}
