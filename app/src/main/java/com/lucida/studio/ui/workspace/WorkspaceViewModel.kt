package com.lucida.studio.ui.workspace

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lucida.studio.data.SavedSession
import com.lucida.studio.data.SessionPreferencesRepo
import com.lucida.studio.domain.imaging.ImageFormatLoader
import com.lucida.studio.domain.imaging.StencilEdgeProcessor
import com.lucida.studio.domain.imaging.ValuePosterizer
import com.lucida.studio.domain.math.MatrixHomographySolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {
    private val mutable = MutableStateFlow(WorkspaceState())
    val state = mutable.asStateFlow()
    private val loader = ImageFormatLoader(application)
    private val preferences = SessionPreferencesRepo(application)
    private var original: Bitmap? = null
    private var loadJob: Job? = null
    private var renderJob: Job? = null
    private var sourceRevision = 0L
    private var pendingRestore: SavedSession? = null
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    init {
        viewModelScope.launch {
            try {
                val saved = preferences.sessions.first()
                pendingRestore = saved
                mutable.update { s -> s.copy(
                    imageUri = saved.uri,
                    opacity = saved.opacity,
                    filterMode = enumValue(saved.filter, RenderFilterMode.RAW_IMAGE),
                    tint = enumValue(saved.tint, StencilTint.AMBER_GOLD),
                    threshold = saved.threshold,
                    tonalSolo = enumValue(saved.solo, TonalBandSolo.ALL_BANDS),
                    grid = enumValue(saved.grid, GridOverlayType.NONE),
                    isRestoring = false
                ) }
                if (saved.uri != null) load(Uri.parse(saved.uri), restoring = true)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutable.update { it.copy(isRestoring = false, error = "Session could not be restored: ${error.message}") }
            }
            launch {
                state.filter { !it.isRestoring && !it.isLoading }.map { s ->
                    SavedSession(s.imageUri, s.matrix, s.corners, s.viewportWidth, s.viewportHeight,
                        s.opacity, s.filterMode.name, s.tint.name, s.threshold,
                        s.tonalSolo.name, s.grid.name, s.homographyActive)
                }.distinctUntilChanged().debounce(500).collect { snapshot ->
                    try {
                        preferences.save(snapshot)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        mutable.update { it.copy(error = "Session save failed: ${error.message}") }
                    }
                }
            }
        }
    }

    fun accept(intent: WorkspaceIntent) {
        if (state.value.isTouchLocked && intent !is WorkspaceIntent.ToggleTouchLock &&
            intent !is WorkspaceIntent.ViewportChanged && intent !is WorkspaceIntent.CameraStatus &&
            intent !is WorkspaceIntent.ReportError) return
        when (intent) {
            is WorkspaceIntent.ImportImage -> load(intent.uri, restoring = false)
            is WorkspaceIntent.ViewportChanged -> resize(intent.width, intent.height)
            is WorkspaceIntent.Transform -> transform(intent)
            is WorkspaceIntent.MoveCorner -> moveCorner(intent)
            is WorkspaceIntent.SetOpacity -> mutable.update { it.copy(opacity = intent.value.coerceIn(0f, 1f)) }
            is WorkspaceIntent.SetFilter -> { mutable.update { it.copy(filterMode = intent.value) }; render() }
            is WorkspaceIntent.SetTint -> { mutable.update { it.copy(tint = intent.value) }; render() }
            is WorkspaceIntent.SetThreshold -> { mutable.update { it.copy(threshold = intent.value.coerceIn(10, 120)) }; render(debounce = true) }
            is WorkspaceIntent.SetTonalSolo -> { mutable.update { it.copy(tonalSolo = intent.value) }; render() }
            is WorkspaceIntent.SetGrid -> mutable.update { it.copy(grid = intent.value) }
            is WorkspaceIntent.SetCalibration -> mutable.update { it.copy(isCalibrating = intent.enabled && it.bitmap != null) }
            is WorkspaceIntent.CameraStatus -> mutable.update { it.copy(cameraStatus = intent.message) }
            is WorkspaceIntent.ReportError -> mutable.update { it.copy(error = intent.message) }
            WorkspaceIntent.ToggleTouchLock -> mutable.update { it.copy(isTouchLocked = !it.isTouchLocked, isCalibrating = false) }
            WorkspaceIntent.ToggleOpticsLock -> mutable.update { it.copy(opticsLocked = !it.opticsLocked) }
            WorkspaceIntent.ToggleTorch -> mutable.update { it.copy(torchEnabled = !it.torchEnabled) }
            WorkspaceIntent.ResetTransform -> fit()
            WorkspaceIntent.DismissError -> mutable.update { it.copy(error = null) }
        }
    }

    private fun load(uri: Uri, restoring: Boolean) {
        loadJob?.cancel()
        renderJob?.cancel()
        if (!restoring) pendingRestore = null
        mutable.update { it.copy(isLoading = true, isProcessing = false, error = null) }
        loadJob = viewModelScope.launch {
            try {
                val loaded = loader.load(uri)
                original = loaded.bitmap
                sourceRevision++
                cache.evictAll()
                mutable.update { it.copy(
                    bitmap = loaded.bitmap, imageUri = uri.toString(),
                    imageName = loaded.originalName ?: "Reference image",
                    imageFormat = loaded.mimeType, hasAlpha = loaded.hasAlpha,
                    imageWidth = loaded.bitmap.width, imageHeight = loaded.bitmap.height,
                    isLoading = false, isCalibrating = false
                ) }
                val saved = pendingRestore
                if (saved != null && saved.uri == uri.toString() && saved.matrix.size == 9) {
                    restoreAlignment(saved)
                } else fit()
                render()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutable.update { it.copy(isLoading = false, error = "Cannot import image. Select it again if access expired. ${error.message ?: "Unsupported or damaged image"}") }
            }
        }
    }

    private fun restoreAlignment(saved: SavedSession) {
        val s = state.value
        if (s.viewportWidth <= 0 || s.viewportHeight <= 0) return
        if (saved.viewportWidth <= 0 || saved.viewportHeight <= 0) {
            pendingRestore = null
            fit()
            return
        }
        val sx = s.viewportWidth.toFloat() / saved.viewportWidth
        val sy = s.viewportHeight.toFloat() / saved.viewportHeight
        val values = scaleScreen(saved.matrix, sx, sy)
        if (MatrixHomographySolver.inverseMap(values, 0f, 0f) == null) {
            pendingRestore = null
            fit()
            return
        }
        mutable.update { it.copy(matrix = values, corners = corners(values, it.imageWidth, it.imageHeight), homographyActive = saved.homography) }
        pendingRestore = null
    }

    private fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val s = state.value
        if (s.viewportWidth == width && s.viewportHeight == height) return
        mutable.update { it.copy(viewportWidth = width, viewportHeight = height) }
        val saved = pendingRestore
        if (original != null && saved != null) {
            restoreAlignment(saved)
        } else if (original != null && s.viewportWidth > 0 && s.viewportHeight > 0) {
            val adjusted = scaleScreen(s.matrix, width.toFloat() / s.viewportWidth, height.toFloat() / s.viewportHeight)
            mutable.update { it.copy(matrix = adjusted, corners = corners(adjusted, it.imageWidth, it.imageHeight)) }
        } else if (original != null) fit()
    }

    private fun fit() {
        val s = state.value
        if (s.imageWidth <= 0 || s.viewportWidth <= 0 || s.viewportHeight <= 0) return
        val matrix = MatrixHomographySolver.fit(s.imageWidth, s.imageHeight, s.viewportWidth.toFloat(), s.viewportHeight.toFloat())
        mutable.update { it.copy(matrix = matrix, corners = corners(matrix, it.imageWidth, it.imageHeight), homographyActive = false) }
    }

    private fun transform(intent: WorkspaceIntent.Transform) {
        val s = state.value
        if (s.bitmap == null || s.isCalibrating) return
        val matrix = MatrixHomographySolver.gesture(s.matrix, intent.centroidX, intent.centroidY,
            intent.panX, intent.panY, intent.zoom, intent.rotationDegrees)
        val points = corners(matrix, s.imageWidth, s.imageHeight)
        if (points.size == 8 && points.all { it.isFinite() && kotlin.math.abs(it) < 1_000_000f }) {
            mutable.update { it.copy(matrix = matrix, corners = points) }
        }
    }

    private fun moveCorner(intent: WorkspaceIntent.MoveCorner) {
        val s = state.value
        if (!s.isCalibrating || intent.index !in 0..3 || s.corners.size != 8) return
        val updated = s.corners.toMutableList().apply {
            this[intent.index * 2] = intent.x.coerceIn(0f, s.viewportWidth.toFloat())
            this[intent.index * 2 + 1] = intent.y.coerceIn(0f, s.viewportHeight.toFloat())
        }
        val matrix = MatrixHomographySolver.fromCorners(s.imageWidth, s.imageHeight, updated) ?: return
        mutable.update { it.copy(matrix = matrix, corners = updated, homographyActive = true) }
    }

    private fun render(debounce: Boolean = false) {
        renderJob?.cancel()
        val source = original ?: return
        val requested = state.value
        val revision = sourceRevision
        val key = "$revision:${requested.filterMode}:${requested.threshold}:${requested.tint}:${requested.tonalSolo}"
        renderJob = viewModelScope.launch {
            try {
                if (debounce) delay(65)
                mutable.update { it.copy(isProcessing = true) }
                val start = SystemClock.elapsedRealtime()
                val bitmap = if (requested.filterMode == RenderFilterMode.RAW_IMAGE) source else cache.get(key) ?: when (requested.filterMode) {
                    RenderFilterMode.RAW_IMAGE -> source
                    RenderFilterMode.STENCIL_EDGES -> StencilEdgeProcessor.process(source, requested.threshold, requested.tint.rgb)
                    RenderFilterMode.VALUE_POSTERIZE -> ValuePosterizer.process(source, requested.tonalSolo.band, requested.tint.rgb)
                }.also { cache.put(key, it) }
                if (revision == sourceRevision) mutable.update { it.copy(bitmap = bitmap, isProcessing = false, processingMs = SystemClock.elapsedRealtime() - start) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutable.update { it.copy(isProcessing = false, error = "Image processing failed: ${error.message}") }
            }
        }
    }

    override fun onCleared() {
        cache.evictAll()
        original = null
        super.onCleared()
    }

    private fun corners(values: List<Float>, width: Int, height: Int): List<Float> {
        val result = ArrayList<Float>(8)
        for ((x, y) in listOf(0f to 0f, width.toFloat() to 0f, width.toFloat() to height.toFloat(), 0f to height.toFloat())) {
            val point = MatrixHomographySolver.map(values, x, y) ?: return emptyList()
            result.add(point.first)
            result.add(point.second)
        }
        return result
    }

    private fun scaleScreen(values: List<Float>, sx: Float, sy: Float): List<Float> = values.mapIndexed { index, value ->
        when (index) { in 0..2 -> value * sx; in 3..5 -> value * sy; else -> value }
    }

    private inline fun <reified T : Enum<T>> enumValue(name: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
