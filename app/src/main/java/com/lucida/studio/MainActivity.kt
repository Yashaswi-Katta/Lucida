package com.lucida.studio

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.lucida.studio.domain.imaging.TimelapseFrameRecorder
import com.lucida.studio.ui.theme.GunmetalBg
import com.lucida.studio.ui.theme.LucidaTheme
import com.lucida.studio.ui.theme.StudioWhite
import com.lucida.studio.ui.workspace.WorkspaceIntent
import com.lucida.studio.ui.workspace.WorkspaceScreen
import com.lucida.studio.ui.workspace.WorkspaceViewModel
import com.lucida.studio.ui.workspace.components.StudioCameraController

class MainActivity : ComponentActivity() {
    private var workspaceViewModel: WorkspaceViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = GunmetalBg.toArgb()
            window.navigationBarColor = GunmetalBg.toArgb()
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        val vm = runCatching {
            ViewModelProvider(this)[WorkspaceViewModel::class.java].also { workspaceViewModel = it }
        }.getOrNull()

        setContent {
            LucidaTheme {
                val cameraController = remember { StudioCameraController(applicationContext) }
                val recorder = remember(cameraController) {
                    TimelapseFrameRecorder(applicationContext, cameraController)
                }
                DisposableEffect(cameraController) {
                    onDispose { cameraController.close() }
                }
                if (vm != null) {
                    WorkspaceScreen(vm, cameraController, recorder)
                } else {
                    Surface(modifier = Modifier.fillMaxSize(), color = GunmetalBg) {
                        Text(
                            text = "Could not initialize studio workspace",
                            color = StudioWhite,
                            modifier = Modifier.padding(24.dp)
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
