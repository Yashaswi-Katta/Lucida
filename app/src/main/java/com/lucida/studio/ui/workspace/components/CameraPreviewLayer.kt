package com.lucida.studio.ui.workspace.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.lucida.studio.ui.theme.AmberGold
import com.lucida.studio.ui.theme.CharcoalBorder
import com.lucida.studio.ui.theme.CharcoalSurface
import com.lucida.studio.ui.theme.StudioWhite

@Composable
fun CameraPreviewLayer(
    controller: StudioCameraController,
    opticsLocked: Boolean,
    torchEnabled: Boolean,
    modifier: Modifier = Modifier,
    onPermissionResult: (Boolean) -> Unit = {},
    onStatus: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val capabilities by controller.capabilities.collectAsState()
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        onPermissionResult(granted)
        if (!granted) onStatus("Camera permission denied; live preview is unavailable")
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            onPermissionResult(true)
        }
    }

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
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(28.dp),
                color = CharcoalSurface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, CharcoalBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "CAMERA PERMISSION REQUIRED",
                        color = AmberGold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Lucida requires camera access to project your paper and canvas under your reference artwork.",
                        color = StudioWhite,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor = Color(0xFF121417)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Camera Permission", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
