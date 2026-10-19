package com.lucida.studio.domain.imaging

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

/** Row-buffered CPU Sobel stencil. Transparent source pixels remain transparent. */
object StencilEdgeProcessor {
    suspend fun process(source: Bitmap, threshold: Int, tintRgb: Int): Bitmap = withContext(Dispatchers.Default) {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if (width < 3 || height < 3) return@withContext output
        var north = IntArray(width)
        var middle = IntArray(width)
        var south = IntArray(width)
        var lumNorth = IntArray(width)
        var lumMiddle = IntArray(width)
        var lumSouth = IntArray(width)
        val rendered = IntArray(width)
        val t = threshold.coerceIn(10, 120)
        val rgb = tintRgb and 0x00FFFFFF
        fun luminanceRow(pixels: IntArray, destination: IntArray) {
            for (x in 0 until width) {
                val pixel = pixels[x]
                destination[x] = if ((pixel ushr 24) < 16) 0 else {
                    (54 * ((pixel ushr 16) and 0xFF) + 183 * ((pixel ushr 8) and 0xFF) + 19 * (pixel and 0xFF)) ushr 8
                }
            }
        }
        source.getPixels(north, 0, width, 0, 0, width, 1)
        source.getPixels(middle, 0, width, 0, 1, width, 1)
        luminanceRow(north, lumNorth)
        luminanceRow(middle, lumMiddle)
        for (y in 1 until height - 1) {
            coroutineContext.ensureActive()
            source.getPixels(south, 0, width, 0, y + 1, width, 1)
            luminanceRow(south, lumSouth)
            rendered.fill(0)
            for (x in 1 until width - 1) {
                if ((middle[x] ushr 24) < 16) continue
                val gx = -lumNorth[x - 1] + lumNorth[x + 1] - 2 * lumMiddle[x - 1] + 2 * lumMiddle[x + 1] - lumSouth[x - 1] + lumSouth[x + 1]
                val gy = -lumNorth[x - 1] - 2 * lumNorth[x] - lumNorth[x + 1] + lumSouth[x - 1] + 2 * lumSouth[x] + lumSouth[x + 1]
                val magnitude = (abs(gx) + abs(gy)).coerceAtMost(255)
                if (magnitude >= t) {
                    val alpha = ((magnitude - t) * 2).coerceAtMost(255)
                    rendered[x] = (alpha shl 24) or rgb
                }
            }
            output.setPixels(rendered, 0, width, 0, y, width, 1)
            val oldPixels = north; north = middle; middle = south; south = oldPixels
            val oldLuminance = lumNorth; lumNorth = lumMiddle; lumMiddle = lumSouth; lumSouth = oldLuminance
        }
        output
    }
}
