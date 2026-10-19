package com.lucida.studio.domain.imaging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.OpenableColumns
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.min

class ImageFormatLoader(private val context: Context) {
    data class LoadedImage(
        val bitmap: Bitmap,
        val mimeType: String,
        val hasAlpha: Boolean,
        val originalName: String
    )

    suspend fun load(uri: Uri): LoadedImage = withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context.applicationContext)
            .data(uri)
            .allowHardware(false)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .size(MAX_DIMENSION, MAX_DIMENSION)
            .scale(Scale.FIT)
            .build()
        val result = Coil.imageLoader(context.applicationContext).execute(request)
        if (result !is SuccessResult) {
            val reason = (result as? coil.request.ErrorResult)?.throwable?.message ?: "Unsupported or unreadable image"
            throw IllegalArgumentException("Could not decode reference image: $reason")
        }

        val drawable = result.drawable
        val originalWidth = drawable.intrinsicWidth.coerceAtLeast(1)
        val originalHeight = drawable.intrinsicHeight.coerceAtLeast(1)
        val scale = min(1.0, min(MAX_DIMENSION.toDouble() / originalWidth, MAX_DIMENSION.toDouble() / originalHeight))
        val width = (originalWidth * scale).toInt().coerceAtLeast(1)
        val height = (originalHeight * scale).toInt().coerceAtLeast(1)
        val bitmap = (drawable as? BitmapDrawable)?.bitmap?.let { source ->
            if (source.config == Bitmap.Config.ARGB_8888 && source.width <= MAX_DIMENSION && source.height <= MAX_DIMENSION) source
            else Bitmap.createScaledBitmap(source, width, height, true).copy(Bitmap.Config.ARGB_8888, false)
        } ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { output ->
            val canvas = Canvas(output)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            output.setHasAlpha(true)
        }
        val mimeType = context.contentResolver.getType(uri)?.lowercase(Locale.ROOT) ?: mimeFromName(displayName(uri))
        LoadedImage(bitmap, mimeType, containsTransparency(bitmap), displayName(uri))
    }

    private fun displayName(uri: Uri): String {
        if (uri.scheme == "content") {
            runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) return cursor.getString(index).orEmpty()
                    }
                }
            }
        }
        return uri.lastPathSegment ?: "reference-image"
    }

    private fun mimeFromName(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (ext) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            "heic", "heif" -> "image/heic"
            "svg" -> "image/svg+xml"
            else -> "application/octet-stream"
        }
    }

    private fun containsTransparency(bitmap: Bitmap): Boolean {
        if (!bitmap.hasAlpha()) return false
        val row = IntArray(bitmap.width)
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            if (row.any { (it ushr 24) != 0xFF }) return true
        }
        return false
    }

    companion object {
        const val MAX_DIMENSION = 2048
    }
}
