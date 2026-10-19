package com.lucida.studio

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder

class LucidaApp : Application(), ImageLoaderFactory, CameraXConfig.Provider {

    companion object {
        @Volatile
        var lastCrash: String? = null
            private set

        fun recordCrash(context: Context, error: Throwable) {
            val report = buildString {
                appendLine("${error::class.java.name}: ${error.message ?: "No error message"}")
                for (element in error.stackTrace.take(25)) {
                    appendLine("  at $element")
                }
                var cause = error.cause
                var depth = 0
                while (cause != null && depth < 3) {
                    appendLine("Caused by ${cause::class.java.name}: ${cause.message ?: ""}")
                    for (element in cause.stackTrace.take(15)) {
                        appendLine("  at $element")
                    }
                    cause = cause.cause
                    depth++
                }
            }
            lastCrash = report
            runCatching {
                val prefs = context.getSharedPreferences("lucida_crash_log", Context.MODE_PRIVATE)
                prefs.edit().putString("last_fatal_crash", report).commit()
            }
        }

        fun getSavedCrash(context: Context): String? {
            return lastCrash ?: runCatching {
                val prefs = context.getSharedPreferences("lucida_crash_log", Context.MODE_PRIVATE)
                prefs.getString("last_fatal_crash", null)
            }.getOrNull()
        }

        fun clearSavedCrash(context: Context) {
            lastCrash = null
            runCatching {
                val prefs = context.getSharedPreferences("lucida_crash_log", Context.MODE_PRIVATE)
                prefs.edit().remove("last_fatal_crash").commit()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("LucidaApp", "FATAL CRASH on thread ${thread.name}: ${throwable.message}", throwable)
            recordCrash(this, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun getCameraXConfig(): CameraXConfig = Camera2Config.defaultConfig()

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(SvgDecoder.Factory()) }
        .allowHardware(false)
        .bitmapConfig(Bitmap.Config.ARGB_8888)
        .crossfade(true)
        .build()
}
