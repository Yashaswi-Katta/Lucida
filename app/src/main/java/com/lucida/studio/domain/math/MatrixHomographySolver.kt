package com.lucida.studio.domain.math

import android.graphics.Matrix
import kotlin.math.cos
import kotlin.math.sin

/** Matrix helpers for bitmap-to-screen mapping. Android Matrix stores a projective 3x3 matrix. */
object MatrixHomographySolver {
    val identity: List<Float> = listOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)

    /** Center an image in the viewport without cropping (android.graphics.Matrix FIT semantics). */
    fun fit(width: Int, height: Int, viewportWidth: Float, viewportHeight: Float): List<Float> {
        if (width <= 0 || height <= 0 || viewportWidth <= 0f || viewportHeight <= 0f) return identity
        val scale = minOf(viewportWidth / width, viewportHeight / height)
        val tx = (viewportWidth - width * scale) / 2f
        val ty = (viewportHeight - height * scale) / 2f
        return listOf(scale, 0f, tx, 0f, scale, ty, 0f, 0f, 1f)
    }

    fun fit(width: Int, height: Int, viewportWidth: Int, viewportHeight: Int): List<Float> =
        fit(width, height, viewportWidth.toFloat(), viewportHeight.toFloat())

    /** Apply a screen-space centroid scale/rotation/pan delta to an existing bitmap transform. */
    fun gesture(
        values: List<Float>, centroidX: Float, centroidY: Float,
        panX: Float, panY: Float, zoom: Float, rotationDegrees: Float
    ): List<Float> {
        if (values.size != 9 || !values.all(Float::isFinite)) return identity
        val safeZoom = zoom.takeIf { it.isFinite() && it > 0f } ?: 1f
        val radians = Math.toRadians(rotationDegrees.toDouble())
        val a = safeZoom * cos(radians).toFloat()
        val b = -safeZoom * sin(radians).toFloat()
        val c = safeZoom * sin(radians).toFloat()
        val d = safeZoom * cos(radians).toFloat()
        val dx = centroidX + panX - (a * centroidX + b * centroidY)
        val dy = centroidY + panY - (c * centroidX + d * centroidY)
        val delta = listOf(a, b, dx, c, d, dy, 0f, 0f, 1f)
        return multiply(delta, values)
    }

    /** Source bitmap corners (TL, TR, BR, BL) to screen-space paper pins in the same order. */
    fun fromCorners(width: Int, height: Int, corners: List<Float>): List<Float>? {
        if (width <= 0 || height <= 0 || corners.size != 8 || !corners.all(Float::isFinite)) return null
        val source = floatArrayOf(0f, 0f, width.toFloat(), 0f, width.toFloat(), height.toFloat(), 0f, height.toFloat())
        val destination = corners.toFloatArray()
        val result = Matrix()
        if (!result.setPolyToPoly(source, 0, destination, 0, 4)) return null
        val out = FloatArray(9)
        result.getValues(out)
        if (!out.all(Float::isFinite)) return null
        return out.toList()
    }

    fun map(values: List<Float>, x: Float, y: Float): Pair<Float, Float>? {
        if (values.size != 9 || !values.all(Float::isFinite)) return null
        val point = floatArrayOf(x, y)
        toMatrix(values).mapPoints(point)
        if (!point.all(Float::isFinite)) return null
        return point[0] to point[1]
    }

    fun inverseMap(values: List<Float>, x: Float, y: Float): Pair<Float, Float>? {
        val inverse = Matrix()
        if (!toMatrix(values).invert(inverse)) return null
        val point = floatArrayOf(x, y)
        inverse.mapPoints(point)
        if (!point.all(Float::isFinite)) return null
        return point[0] to point[1]
    }

    fun toMatrix(values: List<Float>): Matrix = Matrix().apply {
        setValues(if (values.size == 9 && values.all(Float::isFinite)) values.toFloatArray() else identity.toFloatArray())
    }

    private fun multiply(left: List<Float>, right: List<Float>): List<Float> {
        val result = FloatArray(9)
        for (row in 0..2) for (column in 0..2) {
            var value = 0f
            for (k in 0..2) value += left[row * 3 + k] * right[k * 3 + column]
            result[row * 3 + column] = value
        }
        return if (result.all(Float::isFinite)) result.toList() else identity
    }
}
