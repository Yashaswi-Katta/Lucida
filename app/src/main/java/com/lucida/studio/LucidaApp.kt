package com.lucida.studio

import android.app.Application
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder

class LucidaApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(SvgDecoder.Factory()) }
        .allowHardware(false)
        .bitmapConfig(Bitmap.Config.ARGB_8888)
        .crossfade(true)
        .build()
}
