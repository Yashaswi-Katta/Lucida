package com.lucida.studio

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder

class LucidaApp : Application(), ImageLoaderFactory, CameraXConfig.Provider {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("LucidaApp", "FATAL CRASH on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(SvgDecoder.Factory()) }
        .allowHardware(false)
        .bitmapConfig(Bitmap.Config.ARGB_8888)
        .crossfade(true)
        .build()

    override fun getCameraXConfig(): CameraXConfig = Camera2Config.defaultConfig()
}
