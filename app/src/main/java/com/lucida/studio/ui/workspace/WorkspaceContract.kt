package com.lucida.studio.ui.workspace

import android.graphics.Bitmap
import android.net.Uri

enum class RenderFilterMode(val title: String) {
    RAW_IMAGE("Raw image"), STENCIL_EDGES("Stencil edges"), VALUE_POSTERIZE("Tonal values")
}

enum class StencilTint(val title: String, val rgb: Int) {
    INK_BLACK("Ink", 0x000000), AMBER_GOLD("Amber", 0xFFB300),
    CRIMSON_RED("Crimson", 0xE63946), CYAN_BLUEPRINT("Cyan", 0x00E5FF)
}

enum class TonalBandSolo(val title: String, val band: Int?) {
    ALL_BANDS("All values", null), SHADOWS_ONLY("Deep shadows", 0),
    DARK_MIDS_ONLY("Dark midtones", 1), LIGHT_MIDS_ONLY("Light midtones", 2),
    HIGHLIGHTS_ONLY("Highlights", 3)
}

enum class GridOverlayType(val title: String) {
    NONE("No grid"), RULE_OF_THIRDS("Rule of thirds"), GRID_4X4("4 × 4"),
    DIAGONAL_CROSS("Diagonal cross"), LOOMIS_PROPORTION("Loomis proportions")
}

data class WorkspaceState(
    val isRestoring: Boolean = true,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val imageUri: String? = null,
    val imageName: String? = null,
    val imageFormat: String? = null,
    val hasAlpha: Boolean = false,
    val bitmap: Bitmap? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val viewportWidth: Int = 0,
    val viewportHeight: Int = 0,
    val matrix: List<Float> = listOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f),
    val corners: List<Float> = emptyList(),
    val opacity: Float = 0.65f,
    val filterMode: RenderFilterMode = RenderFilterMode.RAW_IMAGE,
    val tint: StencilTint = StencilTint.AMBER_GOLD,
    val threshold: Int = 45,
    val tonalSolo: TonalBandSolo = TonalBandSolo.ALL_BANDS,
    val grid: GridOverlayType = GridOverlayType.NONE,
    val isTouchLocked: Boolean = false,
    val opticsLocked: Boolean = false,
    val torchEnabled: Boolean = false,
    val isCalibrating: Boolean = false,
    val homographyActive: Boolean = false,
    val processingMs: Long = 0,
    val cameraStatus: String = "Camera starting",
    val error: String? = null
)

sealed interface WorkspaceIntent {
    data class ImportImage(val uri: Uri) : WorkspaceIntent
    data class ViewportChanged(val width: Int, val height: Int) : WorkspaceIntent
    data class Transform(
        val centroidX: Float, val centroidY: Float,
        val panX: Float, val panY: Float,
        val zoom: Float, val rotationDegrees: Float
    ) : WorkspaceIntent
    data class MoveCorner(val index: Int, val x: Float, val y: Float) : WorkspaceIntent
    data class SetOpacity(val value: Float) : WorkspaceIntent
    data class SetFilter(val value: RenderFilterMode) : WorkspaceIntent
    data class SetTint(val value: StencilTint) : WorkspaceIntent
    data class SetThreshold(val value: Int) : WorkspaceIntent
    data class SetTonalSolo(val value: TonalBandSolo) : WorkspaceIntent
    data class SetGrid(val value: GridOverlayType) : WorkspaceIntent
    data class SetCalibration(val enabled: Boolean) : WorkspaceIntent
    data class CameraStatus(val message: String) : WorkspaceIntent
    data class ReportError(val message: String) : WorkspaceIntent
    data object ToggleTouchLock : WorkspaceIntent
    data object ToggleOpticsLock : WorkspaceIntent
    data object ToggleTorch : WorkspaceIntent
    data object ResetTransform : WorkspaceIntent
    data object DismissError : WorkspaceIntent
}
