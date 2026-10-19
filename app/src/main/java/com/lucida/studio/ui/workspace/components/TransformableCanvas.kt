package com.lucida.studio.ui.workspace.components

import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.lucida.studio.domain.math.MatrixHomographySolver
import com.lucida.studio.ui.workspace.WorkspaceIntent
import com.lucida.studio.ui.workspace.WorkspaceState

@Composable
fun TransformableCanvas(
    state: WorkspaceState,
    onIntent: (WorkspaceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val outline = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFB300.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(12f, 8f), 0f)
    } }
    val input = if (state.isTouchLocked || state.isCalibrating || state.bitmap == null) Modifier else
        Modifier.pointerInput(state.imageUri) {
            detectTransformGestures { centroid, pan, zoom, rotation ->
                onIntent(WorkspaceIntent.Transform(centroid.x, centroid.y, pan.x, pan.y, zoom, rotation))
            }
        }
    Canvas(modifier.then(input)) {
        val bitmap = state.bitmap ?: return@Canvas
        val matrix = MatrixHomographySolver.toMatrix(state.matrix)
        val canvas = drawContext.canvas.nativeCanvas
        paint.alpha = (state.opacity * 255f).toInt().coerceIn(0, 255)
        canvas.save()
        try {
            canvas.concat(matrix)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        } finally {
            canvas.restore()
        }
        if (!state.isTouchLocked && state.corners.size == 8) {
            val path = Path().apply {
                moveTo(state.corners[0], state.corners[1])
                for (i in 1..3) lineTo(state.corners[i * 2], state.corners[i * 2 + 1])
                close()
            }
            canvas.drawPath(path, outline)
        }
    }
}
