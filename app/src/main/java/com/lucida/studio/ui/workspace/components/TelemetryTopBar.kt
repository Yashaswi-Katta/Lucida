package com.lucida.studio.ui.workspace.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lucida.studio.ui.theme.AmberGold
import com.lucida.studio.ui.theme.CharcoalBorder
import com.lucida.studio.ui.theme.CharcoalSurface
import com.lucida.studio.ui.theme.SlateMuted
import com.lucida.studio.ui.workspace.WorkspaceIntent
import com.lucida.studio.ui.workspace.WorkspaceState

@Composable
fun TelemetryTopBar(
    state: WorkspaceState,
    onIntent: (WorkspaceIntent) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        color = CharcoalSurface.copy(alpha = 0.88f), border = BorderStroke(1.dp, CharcoalBorder)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f).padding(top = 7.dp)) {
                    Text("L U C I D A", style = MaterialTheme.typography.titleMedium, color = AmberGold)
                    Text("DIGITAL CAMERA LUCIDA", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                }
                TextButton(onClick = onImport, enabled = !state.isLoading && !state.isRestoring) { Text("Import") }
                TextButton(onClick = { onIntent(WorkspaceIntent.ResetTransform) }, enabled = state.bitmap != null) { Text("Reset") }
            }
            Text(state.cameraStatus, style = MaterialTheme.typography.labelSmall, color = SlateMuted, maxLines = 2)
            val badges = buildList {
                if (state.isLoading) add("DECODING")
                if (state.isProcessing) add("PROCESSING") else if (state.bitmap != null) add("${state.processingMs} ms / filter")
                if (state.opticsLocked) add("OPTICS REQUESTED")
                if (state.homographyActive) add("PAPER WARP")
                if (state.isCalibrating) add("PIN MODE · TL → TR → BR → BL")
            }
            if (badges.isNotEmpty()) Text(badges.joinToString("  ·  "), style = MaterialTheme.typography.labelSmall,
                color = AmberGold, modifier = Modifier.padding(top = 5.dp))
        }
    }
}
