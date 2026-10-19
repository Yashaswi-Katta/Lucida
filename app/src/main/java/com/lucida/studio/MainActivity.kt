package com.lucida.studio

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.lucida.studio.domain.imaging.TimelapseFrameRecorder
import com.lucida.studio.ui.theme.GunmetalBg
import com.lucida.studio.ui.theme.LucidaTheme
import com.lucida.studio.ui.workspace.WorkspaceIntent
import com.lucida.studio.ui.workspace.WorkspaceScreen
import com.lucida.studio.ui.workspace.WorkspaceViewModel
import com.lucida.studio.ui.workspace.components.StudioCameraController
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb

class MainActivity : ComponentActivity() {
    private lateinit var workspaceViewModel: WorkspaceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workspaceViewModel = ViewModelProvider(this)[WorkspaceViewModel::class.java]
        window.statusBarColor = GunmetalBg.toArgb()
        window.navigationBarColor = GunmetalBg.toArgb()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            LucidaTheme {
                val cameraController = remember { StudioCameraController(applicationContext) }
                val recorder = remember(cameraController) {
                    TimelapseFrameRecorder(applicationContext, cameraController)
                }
                DisposableEffect(cameraController) {
                    onDispose { cameraController.close() }
                }
                WorkspaceScreen(workspaceViewModel, cameraController, recorder)
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (::workspaceViewModel.isInitialized && event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                workspaceViewModel.accept(WorkspaceIntent.ToggleTouchLock)
            }
            if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) return true
        }
        return super.dispatchKeyEvent(event)
    }
}
