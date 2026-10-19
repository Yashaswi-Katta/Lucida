package com.lucida.studio.domain.math

import kotlin.math.PI
import kotlin.math.abs

/** Stateful 1D One-Euro filter. Timestamps are seconds; use [filterNanos] for monotonic nanoseconds. */
class OneEuroFilter1D(
    var minCutoff: Float = 1.2f,
    var beta: Float = 0.007f,
    var dCutoff: Float = 1.0f
) {
    private var initialized = false
    private var previousTime = 0.0
    private var previousRaw = 0f
    private var filteredValue = 0f
    private var filteredDerivative = 0f

    fun filter(value: Float, timestampSeconds: Double): Float {
        if (!value.isFinite() || !timestampSeconds.isFinite()) return if (initialized) filteredValue else value
        if (!initialized) {
            initialized = true
            previousTime = timestampSeconds
            previousRaw = value
            filteredValue = value
            filteredDerivative = 0f
            return value
        }
        val dt = timestampSeconds - previousTime
        if (dt <= 0.0) return filteredValue
        val safeMin = minCutoff.coerceAtLeast(0.0001f).toDouble()
        val safeDerivativeCutoff = dCutoff.coerceAtLeast(0.0001f).toDouble()
        val derivative = ((value - previousRaw) / dt).toFloat()
        val derivativeAlpha = smoothingAlpha(dt, safeDerivativeCutoff)
        filteredDerivative = lowPass(derivative, filteredDerivative, derivativeAlpha)
        val cutoff = (safeMin + beta.coerceAtLeast(0f) * abs(filteredDerivative)).toDouble()
        val alpha = smoothingAlpha(dt, cutoff.coerceAtLeast(0.0001))
        filteredValue = lowPass(value, filteredValue, alpha)
        previousRaw = value
        previousTime = timestampSeconds
        return filteredValue
    }

    fun filterNanos(value: Float, timestampNanos: Long): Float = filter(value, timestampNanos / 1_000_000_000.0)

    fun reset() {
        initialized = false
        previousTime = 0.0
        previousRaw = 0f
        filteredValue = 0f
        filteredDerivative = 0f
    }

    private fun smoothingAlpha(dt: Double, cutoffHz: Double): Double {
        val tau = 1.0 / (2.0 * PI * cutoffHz)
        return (1.0 / (1.0 + tau / dt)).coerceIn(0.0, 1.0)
    }

    private fun lowPass(input: Float, previous: Float, alpha: Double): Float =
        (alpha * input + (1.0 - alpha) * previous).toFloat()
}

data class FilteredPoint(val x: Float, val y: Float)

/** Independent One-Euro filters for horizontal and vertical touch coordinates. */
class OneEuroFilter2D(
    minCutoff: Float = 1.2f,
    beta: Float = 0.007f,
    dCutoff: Float = 1.0f
) {
    private val xFilter = OneEuroFilter1D(minCutoff, beta, dCutoff)
    private val yFilter = OneEuroFilter1D(minCutoff, beta, dCutoff)

    fun filter(x: Float, y: Float, timestampSeconds: Double): Pair<Float, Float> =
        xFilter.filter(x, timestampSeconds) to yFilter.filter(y, timestampSeconds)

    fun filterNanos(x: Float, y: Float, timestampNanos: Long): Pair<Float, Float> =
        xFilter.filterNanos(x, timestampNanos) to yFilter.filterNanos(y, timestampNanos)

    fun reset() {
        xFilter.reset()
        yFilter.reset()
    }
}
