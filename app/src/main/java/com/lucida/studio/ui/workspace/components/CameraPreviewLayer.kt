package com.lucida.studio.ui.workspace.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt
import com.lucida.studio.ui.theme.AmberGold
import com.lucida.studio.ui.theme.CharcoalSurface
import com.lucida.studio.ui.theme.StudioWhite

@Composable
fun CameraPreviewLayer(
    controller: StudioCameraController,
    opticsLocked: Boolean,
    torchEnabled: Boolean,
    modifier: Modifier = Modifier,
    onStatus: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val capabilities by controller.capabilities.collectAsState()
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var exposure by remember { mutableFloatStateOf(0f) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) onStatus("Camera permission denied; live preview and capture are unavailable")
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(capabilities.exposureIndex) { exposure = capabilities.exposureIndex.toFloat() }
    LaunchedEffect(capabilities.status) { onStatus(capabilities.status) }
    LaunchedEffect(opticsLocked, torchEnabled) { controller.updateSettings(opticsLocked, torchEnabled) }

    Box(modifier) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    PreviewView(viewContext).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        controller.bind(lifecycleOwner, this)
                    }
                },
                update = { controller.bind(lifecycleOwner, it) }
            )
        } else {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera permission is needed for the live drawing surface.", color = StudioWhite)
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Grant camera permission")
                }
            }
        }
        if (hasPermission && capabilities.supportsExposureCompensation && capabilities.isReady) {
            Surface(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                color = CharcoalSurface.copy(alpha = 0.90f),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    val ev = exposure * capabilities.exposureStepEv
                    Text("EXPOSURE ${if (ev > 0f) "+" else ""}${"%.1f".format(ev)} EV", color = AmberGold,
                        style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = exposure.coerceIn(capabilities.exposureMinimum.toFloat(), capabilities.exposureMaximum.toFloat()),
                        onValueChange = { value ->
                            exposure = value
                            controller.setExposureCompensationIndex(value.roundToInt())
                        },
                        valueRange = capabilities.exposureMinimum.toFloat()..capabilities.exposureMaximum.toFloat()
                    )
                }
            }
        }
    }
}
