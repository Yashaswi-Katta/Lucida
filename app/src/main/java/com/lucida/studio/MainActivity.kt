package com.lucida.studio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner as PlatformLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner as ComposeLifecycleOwner
import com.lucida.studio.data.SavedSession
import com.lucida.studio.data.SessionPreferencesRepo
import com.lucida.studio.domain.imaging.TimelapseFrameRecorder
import com.lucida.studio.ui.theme.AmberGold
import com.lucida.studio.ui.theme.CharcoalBorder
import com.lucida.studio.ui.theme.CharcoalSurface
import com.lucida.studio.ui.theme.CrimsonAlert
import com.lucida.studio.ui.theme.GunmetalBg
import com.lucida.studio.ui.theme.LucidaTheme
import com.lucida.studio.ui.theme.SlateMuted
import com.lucida.studio.ui.theme.StudioWhite
import com.lucida.studio.ui.workspace.WorkspaceIntent
import com.lucida.studio.ui.workspace.WorkspaceScreen
import com.lucida.studio.ui.workspace.WorkspaceViewModel
import com.lucida.studio.ui.workspace.components.StudioCameraController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var workspaceViewModel: WorkspaceViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        var vmError: String? = null
        val vm = runCatching {
            ViewModelProvider(this)[WorkspaceViewModel::class.java].also { workspaceViewModel = it }
        }.getOrElse { error ->
            val trace = buildString {
                appendLine("ViewModel initialization error: ${error::class.java.name}: ${error.message}")
                for (el in error.stackTrace.take(20)) appendLine("  at $el")
            }
            vmError = trace
            null
        }

        setContent {
            CompositionLocalProvider(
                ComposeLifecycleOwner provides this,
                PlatformLifecycleOwner provides this
            ) {
                LucidaTheme {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    var diagnosticError by remember {
                        mutableStateOf(LucidaApp.getSavedCrash(context) ?: vmError)
                    }

                    val cameraController = remember { StudioCameraController(applicationContext) }
                    val recorder = remember(cameraController) {
                        TimelapseFrameRecorder(applicationContext, cameraController)
                    }
                    DisposableEffect(cameraController) {
                        onDispose { cameraController.close() }
                    }

                    if (diagnosticError != null) {
                        DiagnosticsScreen(
                            errorText = diagnosticError!!,
                            onDismiss = {
                                LucidaApp.clearSavedCrash(context)
                                diagnosticError = null
                            },
                            onResetSession = {
                                scope.launch {
                                    runCatching {
                                        SessionPreferencesRepo(context).save(SavedSession())
                                    }
                                    LucidaApp.clearSavedCrash(context)
                                    diagnosticError = null
                                }
                            }
                        )
                    } else if (vm != null) {
                        WorkspaceScreen(vm, cameraController, recorder)
                    } else {
                        DiagnosticsScreen(
                            errorText = vmError ?: "Could not initialize studio workspace.",
                            onDismiss = { },
                            onResetSession = {
                                scope.launch {
                                    runCatching {
                                        SessionPreferencesRepo(context).save(SavedSession())
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val vm = workspaceViewModel
        if (vm != null && event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                vm.accept(WorkspaceIntent.ToggleTouchLock)
            }
            if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) return true
        }
        return super.dispatchKeyEvent(event)
    }
}

@Composable
private fun DiagnosticsScreen(
    errorText: String,
    onDismiss: () -> Unit,
    onResetSession: () -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = GunmetalBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "LUCIDA DIAGNOSTICS",
                color = CrimsonAlert,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "A startup error or previous crash was detected. You can review the details below, copy them for support, or clear data and continue.",
                color = StudioWhite,
                style = MaterialTheme.typography.bodyMedium
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 320.dp),
                color = CharcoalSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CharcoalBorder)
            ) {
                Box(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = errorText,
                        color = SlateMuted,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("Lucida Crash Log", errorText)
                        clipboard?.setPrimaryClip(clip)
                        copied = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CharcoalBorder,
                        contentColor = StudioWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (copied) "Copied!" else "Copy Log")
                }
                Button(
                    onClick = onResetSession,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonAlert,
                        contentColor = StudioWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reset Session")
                }
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberGold,
                    contentColor = Color(0xFF121417)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Continue to Workspace")
            }
        }
    }
}
