package com.lucida.studio.ui.workspace.components

import android.content.Context
import android.graphics.Bitmap
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Live CameraX controller; stills are captured from ImageCapture, never from the HUD. */
class StudioCameraController(context: Context) {
    private val appContext = context.applicationContext
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(appContext)
    private val providerFuture: ListenableFuture<ProcessCameraProvider>? = runCatching {
        ProcessCameraProvider.getInstance(appContext)
    }.getOrNull()
    private val mutableCapabilities = MutableStateFlow(CameraCapabilities())
    val capabilities: StateFlow<CameraCapabilities> = mutableCapabilities.asStateFlow()

    @Volatile private var provider: ProcessCameraProvider? = null
    @Volatile private var camera: Camera? = null
    @Volatile private var imageCapture: ImageCapture? = null
    @Volatile private var boundPreview: Preview? = null
    @Volatile private var boundOwner: LifecycleOwner? = null
    @Volatile private var previewView: PreviewView? = null
    @Volatile private var bindPending = false
    @Volatile private var closed = false
    @Volatile private var requestedOpticsLock = false
    @Volatile private var requestedTorch = false

    data class CameraCapabilities(
        val isReady: Boolean = false,
        val status: String = "Waiting for camera permission",
        val supportsTorch: Boolean = false,
        val exposureMinimum: Int = 0,
        val exposureMaximum: Int = 0,
        val exposureIndex: Int = 0,
        val exposureStepEv: Float = 0f,
        val supportsExposureCompensation: Boolean = false
    )

    fun bind(owner: LifecycleOwner, view: PreviewView) {
        if (closed) return
        if (boundOwner === owner && previewView === view && (camera != null || bindPending)) return
        val future = providerFuture
        if (future == null) {
            mutableCapabilities.value = mutableCapabilities.value.copy(
                isReady = false,
                status = "Camera service unavailable on this device"
            )
            return
        }
        boundOwner = owner
        previewView = view
        bindPending = true
        mutableCapabilities.value = mutableCapabilities.value.copy(isReady = false, status = "Starting back camera...")
        future.addListener({
            bindPending = false
            if (closed || owner.lifecycle.currentState == androidx.lifecycle.Lifecycle.State.DESTROYED) return@addListener
            runCatching {
                val cameraProvider = future.get()
                provider = cameraProvider
                boundPreview?.let { oldPreview ->
                    imageCapture?.let { oldCapture -> cameraProvider.unbind(oldPreview, oldCapture) }
                }
                val rotation = view.display?.rotation ?: Surface.ROTATION_0
                val preview = Preview.Builder().setTargetRotation(rotation).build().also {
                    it.setSurfaceProvider(view.surfaceProvider)
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(rotation)
                    .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                    .build()
                val boundCamera = cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                boundPreview = preview
                imageCapture = capture
                camera = boundCamera
                val exposure = boundCamera.cameraInfo.exposureState
                val range = exposure.exposureCompensationRange
                mutableCapabilities.value = CameraCapabilities(
                    isReady = true,
                    status = "Back camera ready · clean still capture",
                    supportsTorch = boundCamera.cameraInfo.hasFlashUnit(),
                    exposureMinimum = range.lower,
                    exposureMaximum = range.upper,
                    exposureIndex = exposure.exposureCompensationIndex,
                    exposureStepEv = exposure.exposureCompensationStep.toFloat(),
                    supportsExposureCompensation = range.lower != range.upper
                )
                updateSettings(requestedOpticsLock, requestedTorch)
            }.onFailure { error ->
                camera = null
                imageCapture = null
                mutableCapabilities.value = mutableCapabilities.value.copy(
                    isReady = false,
                    status = "Camera unavailable: ${error.cause?.message ?: error.message ?: "device or permission error"}"
                )
            }
        }, mainExecutor)
    }

    fun updateSettings(opticsLocked: Boolean, torchEnabled: Boolean) {
        requestedOpticsLock = opticsLocked
        requestedTorch = torchEnabled
        val activeCamera = camera ?: return
        val hasTorch = activeCamera.cameraInfo.hasFlashUnit()
        if (torchEnabled && !hasTorch) {
            mutableCapabilities.value = mutableCapabilities.value.copy(status = "This camera has no flash/torch")
        } else {
            val future = activeCamera.cameraControl.enableTorch(torchEnabled && hasTorch)
            future.addListener({
                runCatching { future.get() }.onFailure { error ->
                    mutableCapabilities.value = mutableCapabilities.value.copy(
                        status = "Torch update failed: ${error.cause?.message ?: error.message}"
                    )
                }
            }, mainExecutor)
        }
        applyExposureHold(activeCamera, opticsLocked)
    }

    fun setExposureCompensationIndex(index: Int) {
        val active = camera ?: return
        val range = active.cameraInfo.exposureState.exposureCompensationRange
        val bounded = index.coerceIn(range.lower, range.upper)
        val future = active.cameraControl.setExposureCompensationIndex(bounded)
        future.addListener({
            runCatching { future.get() }.onSuccess { applied ->
                mutableCapabilities.value = mutableCapabilities.value.copy(exposureIndex = applied)
            }.onFailure { error ->
                mutableCapabilities.value = mutableCapabilities.value.copy(
                    status = "Exposure adjustment failed: ${error.cause?.message ?: error.message}"
                )
            }
        }, mainExecutor)
    }

    /** Focus/meter at a PreviewView-local point; available for touch-to-focus callers. */
    fun focusAt(point: MeteringPoint) {
        val active = camera ?: return
        val action = FocusMeteringAction.Builder(point)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        active.cameraControl.startFocusAndMetering(action).addListener({}, mainExecutor)
    }

    fun previewBitmap(): Bitmap? = runCatching { previewView?.bitmap }.getOrNull()

    suspend fun capture(target: File): File {
        val capture = imageCapture ?: throw IllegalStateException("Camera is not ready yet")
        target.parentFile?.mkdirs()
        return suspendCancellableCoroutine { continuation ->
            val output = ImageCapture.OutputFileOptions.Builder(target).build()
            capture.takePicture(output, mainExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    if (continuation.isActive) continuation.resume(target)
                    else target.delete()
                }

                override fun onError(exception: ImageCaptureException) {
                    target.delete()
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
            })
            continuation.invokeOnCancellation { target.delete() }
        }
    }

    fun close() {
        closed = true
        bindPending = false
        val oldPreview = boundPreview
        val oldCapture = imageCapture
        val oldProvider = provider
        if (oldPreview != null && oldCapture != null) oldProvider?.unbind(oldPreview, oldCapture)
        camera = null
        imageCapture = null
        boundPreview = null
        boundOwner = null
        previewView = null
        mutableCapabilities.value = mutableCapabilities.value.copy(isReady = false, status = "Camera closed")
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun applyExposureHold(activeCamera: Camera, locked: Boolean) {
        runCatching {
            val options = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, locked)
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    if (locked) CaptureRequest.CONTROL_AF_MODE_OFF else CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                .build()
            val future = Camera2CameraControl.from(activeCamera.cameraControl).setCaptureRequestOptions(options)
            future.addListener({
                runCatching { future.get() }.onFailure { error ->
                    mutableCapabilities.value = mutableCapabilities.value.copy(
                        status = "Optics lock unavailable: ${error.cause?.message ?: error.message}"
                    )
                }
            }, mainExecutor)
        }.onFailure { error ->
            mutableCapabilities.value = mutableCapabilities.value.copy(
                status = "Optics lock unavailable: ${error.message ?: "camera does not support AE/AF lock"}"
            )
        }
    }
}
