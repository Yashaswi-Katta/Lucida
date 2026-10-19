package com.lucida.studio.ui.workspace.components

import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import com.lucida.studio.domain.math.MatrixHomographySolver
import com.lucida.studio.ui.workspace.GridOverlayType

@Composable
fun GridOverlayDrawer(
    type: GridOverlayType,
    matrix: List<Float>,
    imageWidth: Int,
    imageHeight: Int,
    modifier: Modifier = Modifier
) {
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xBFFFB300.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    } }
    Canvas(modifier) {
        if (type == GridOverlayType.NONE || imageWidth <= 0 || imageHeight <= 0) return@Canvas
        val w = imageWidth.toFloat()
        val h = imageHeight.toFloat()
        val path = Path()
        fun line(x0: Float, y0: Float, x1: Float, y1: Float) {
            val start = MatrixHomographySolver.map(matrix, x0, y0) ?: return
            val end = MatrixHomographySolver.map(matrix, x1, y1) ?: return
            path.moveTo(start.first, start.second)
            path.lineTo(end.first, end.second)
        }
        when (type) {
            GridOverlayType.NONE -> Unit
            GridOverlayType.RULE_OF_THIRDS, GridOverlayType.GRID_4X4 -> {
                val divisions = if (type == GridOverlayType.RULE_OF_THIRDS) 3 else 4
                for (i in 1 until divisions) {
                    val f = i.toFloat() / divisions
                    line(w * f, 0f, w * f, h)
                    line(0f, h * f, w, h * f)
                }
            }
            GridOverlayType.DIAGONAL_CROSS -> {
                line(0f, 0f, w, h)
                line(w, 0f, 0f, h)
                line(w / 2f, 0f, w / 2f, h)
                line(0f, h / 2f, w, h / 2f)
            }
            GridOverlayType.LOOMIS_PROPORTION -> {
                for (i in 1..7) line(0f, h * i / 8f, w, h * i / 8f)
                line(w / 2f, 0f, w / 2f, h)
                line(w / 4f, 0f, w / 4f, h)
                line(w * 3f / 4f, 0f, w * 3f / 4f, h)
            }
        }
        drawContext.canvas.nativeCanvas.drawPath(path, paint)
    }
}
