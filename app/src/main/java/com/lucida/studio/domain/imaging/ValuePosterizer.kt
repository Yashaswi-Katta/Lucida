package com.lucida.studio.domain.imaging

import android.graphics.Bitmap
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Posterized tint overlay with a selectable shadow/midtone/highlight band. */
object ValuePosterizer {
    const val BAND_COUNT = 4

    /** A null [soloBand] keeps all four bands; a value in 0..3 isolates one band. */
    suspend fun process(source: Bitmap, soloBand: Int?, tintRgb: Int): Bitmap = withContext(Dispatchers.Default) {
        val width = source.width
        val height = source.height
        val sourceRow = IntArray(width)
        val outputRow = IntArray(width)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val selectedBand = soloBand?.coerceIn(0, BAND_COUNT - 1)
        val tintRed = (tintRgb ushr 16) and 0xFF
        val tintGreen = (tintRgb ushr 8) and 0xFF
        val tintBlue = tintRgb and 0xFF
        for (y in 0 until height) {
            coroutineContext.ensureActive()
            source.getPixels(sourceRow, 0, width, 0, y, width, 1)
            outputRow.fill(0)
            for (x in 0 until width) {
                val pixel = sourceRow[x]
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha < 16) continue
                val red = (pixel ushr 16) and 0xFF
                val green = (pixel ushr 8) and 0xFF
                val blue = pixel and 0xFF
                val luminance = (54 * red + 183 * green + 19 * blue) ushr 8
                val band = ((luminance * BAND_COUNT) / 256).coerceAtMost(BAND_COUNT - 1)
                if (selectedBand == null || selectedBand == band) {
                    val level = (band + 1) * 255 / BAND_COUNT
                    val r = tintRed * level / 255
                    val g = tintGreen * level / 255
                    val b = tintBlue * level / 255
                    outputRow[x] = (alpha shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
            output.setPixels(outputRow, 0, width, 0, y, width, 1)
        }
        output
    }
}
