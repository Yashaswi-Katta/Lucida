package com.lucida.studio.ui.workspace

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucida.studio.domain.imaging.TimelapseFrameRecorder
import com.lucida.studio.ui.theme.AmberGold
import com.lucida.studio.ui.theme.CharcoalSurface
import com.lucida.studio.ui.theme.CrimsonAlert
import com.lucida.studio.ui.theme.StudioWhite
import com.lucida.studio.ui.workspace.components.CameraPreviewLayer
import com.lucida.studio.ui.workspace.components.CornerCalibrationOverlay
import com.lucida.studio.ui.workspace.components.GridOverlayDrawer
import com.lucida.studio.ui.workspace.components.StudioCameraController
import com.lucida.studio.ui.workspace.components.StudioHudBar
import com.lucida.studio.ui.workspace.components.TelemetryTopBar
import com.lucida.studio.ui.workspace.components.TransformableCanvas
import kotlinx.coroutines.launch

@Composable
fun WorkspaceScreen(
    viewModel: WorkspaceViewModel,
    cameraController: StudioCameraController,
    recorder: TimelapseFrameRecorder
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recording by recorder.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var intervalSeconds by remember { mutableLongStateOf(10L) }
    var captureBusy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // Some document providers return only transient access; import still runs now.
            } catch (_: IllegalArgumentException) {
                // A provider may not support persisted permissions.
            }
            viewModel.accept(WorkspaceIntent.ImportImage(uri))
        }
    }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: error("Android could not open the selected export destination")
                output.use { recorder.exportSequence(it) }
            }.onSuccess {
                viewModel.accept(WorkspaceIntent.CameraStatus("ZIP sequence exported to the selected destination"))
            }.onFailure { error ->
                viewModel.accept(WorkspaceIntent.ReportError("Export failed: ${error.message ?: "destination unavailable"}"))
            }
        }
    }

    DisposableEffect(lifecycleOwner, recorder) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && recorder.state.value.isRecording) {
                scope.launch { recorder.stop() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { viewModel.accept(WorkspaceIntent.ViewportChanged(it.width, it.height)) }
    ) {
        CameraPreviewLayer(
            controller = cameraController,
            opticsLocked = state.opticsLocked,
            torchEnabled = state.torchEnabled,
            modifier = Modifier.fillMaxSize(),
            onStatus = { viewModel.accept(WorkspaceIntent.CameraStatus(it)) }
        )
        TransformableCanvas(
            state = state,
            onIntent = viewModel::accept,
            modifier = Modifier.fillMaxSize()
        )
        GridOverlayDrawer(
            type = state.grid,
            matrix = state.matrix,
            imageWidth = state.imageWidth,
            imageHeight = state.imageHeight,
            modifier = Modifier.fillMaxSize()
        )
        if (state.isCalibrating && state.corners.size == 8) {
            CornerCalibrationOverlay(
                corners = state.corners,
                onMove = { index, x, y -> viewModel.accept(WorkspaceIntent.MoveCorner(index, x, y)) },
                previewBitmap = cameraController::previewBitmap,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (state.isTouchLocked) GhostLockFrame(Modifier.fillMaxSize())

        Column(
            Modifier.align(Alignment.TopCenter).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelemetryTopBar(
                state = state,
                onIntent = viewModel::accept,
                onImport = { picker.launch(arrayOf("image/*", "image/svg+xml")) }
            )
        }

        StudioHudBar(
            state = state,
            onIntent = viewModel::accept,
            isRecording = recording.isRecording,
            frameCount = recording.frames,
            intervalSeconds = intervalSeconds,
            captureBusy = captureBusy,
            onIntervalChange = { intervalSeconds = it },
            onSnapshot = {
                scope.launch {
                    captureBusy = true
                    runCatching { recorder.snapshot() }
                        .onSuccess { file -> viewModel.accept(WorkspaceIntent.CameraStatus("Clean frame saved · ${file.name}")) }
                        .onFailure { error -> viewModel.accept(WorkspaceIntent.ReportError("Snapshot failed: ${error.message}")) }
                    captureBusy = false
                }
            },
            onRecordToggle = {
                scope.launch {
                    if (recording.isRecording) recorder.stop() else recorder.start(scope, intervalSeconds)
                }
            },
            onExport = { exportPicker.launch("lucida-sequence.zip") },
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 10.dp)
        )

        if (state.error != null) ErrorCard(
            message = state.error!!,
            onDismiss = { viewModel.accept(WorkspaceIntent.DismissError) },
            modifier = Modifier.align(Alignment.Center).padding(24.dp)
        )
        if (state.bitmap == null && !state.isLoading && state.error == null) EmptyHint(
            isRestoring = state.isRestoring,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 28.dp)
        )
    }
}

@Composable
private fun GhostLockFrame(modifier: Modifier = Modifier) {
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val paint = remember(density) { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE63946.toInt()
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3.dp.value * density
    } }
    Canvas(modifier) {
        drawContext.canvas.nativeCanvas.drawRect(
            2f, 2f, size.width - 2f, size.height - 2f, paint
        )
    }
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier, color = CharcoalSurface.copy(alpha = 0.97f), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("Studio notice", color = AmberGold, style = MaterialTheme.typography.titleMedium)
            Text(message, color = StudioWhite, modifier = Modifier.padding(top = 8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Dismiss") }
        }
    }
}

@Composable
private fun EmptyHint(isRestoring: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier, color = CharcoalSurface.copy(alpha = 0.88f), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("LUCIDA", color = AmberGold, style = MaterialTheme.typography.headlineSmall)
            Text(if (isRestoring) "Restoring your studio..." else "Place your phone above the paper, then import a reference.",
                color = StudioWhite, modifier = Modifier.padding(top = 8.dp))
            Text("Keep the camera stationary when aligning the four paper corners.",
                color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 5.dp))
        }
    }
}
