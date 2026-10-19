package com.lucida.studio.ui.workspace.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lucida.studio.ui.theme.AmberGold
import com.lucida.studio.ui.theme.CharcoalBorder
import com.lucida.studio.ui.theme.CharcoalSurface
import com.lucida.studio.ui.theme.CrimsonAlert
import com.lucida.studio.ui.theme.SlateMuted
import com.lucida.studio.ui.theme.StudioWhite
import com.lucida.studio.ui.workspace.GridOverlayType
import com.lucida.studio.ui.workspace.RenderFilterMode
import com.lucida.studio.ui.workspace.StencilTint
import com.lucida.studio.ui.workspace.TonalBandSolo
import com.lucida.studio.ui.workspace.WorkspaceIntent
import com.lucida.studio.ui.workspace.WorkspaceState

@Composable
fun StudioHudBar(
    state: WorkspaceState,
    onIntent: (WorkspaceIntent) -> Unit,
    isRecording: Boolean,
    frameCount: Int,
    intervalSeconds: Long,
    captureBusy: Boolean,
    onIntervalChange: (Long) -> Unit,
    onSnapshot: () -> Unit,
    onRecordToggle: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = CharcoalSurface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, CharcoalBorder)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Close controls ▾" else "Studio controls ▴") }
                TextButton(onClick = { onIntent(WorkspaceIntent.ToggleTouchLock) }) { Text("Ghost lock", color = CrimsonAlert) }
            }
            if (expanded) Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("REFERENCE OPACITY", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                    Text("${(state.opacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = AmberGold)
                }
                Slider(value = state.opacity, onValueChange = { onIntent(WorkspaceIntent.SetOpacity(it)) }, enabled = state.bitmap != null)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (mode in RenderFilterMode.entries) FilterChip(selected = state.filterMode == mode,
                        onClick = { onIntent(WorkspaceIntent.SetFilter(mode)) }, label = { Text(mode.title) })
                }
                if (state.filterMode != RenderFilterMode.RAW_IMAGE) {
                    Text("LINE / SOLO TINT", style = MaterialTheme.typography.labelSmall, color = SlateMuted,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (tint in StencilTint.entries) {
                            Box(Modifier.size(48.dp).border(if (state.tint == tint) 3.dp else 1.dp,
                                if (state.tint == tint) AmberGold else SlateMuted, CircleShape)
                                .padding(7.dp).background(Color(0xFF000000L or tint.rgb.toLong()), CircleShape)
                                .selectable(selected = state.tint == tint, role = Role.RadioButton,
                                    onClick = { onIntent(WorkspaceIntent.SetTint(tint)) })
                                .semantics { contentDescription = "${tint.title} stencil tint" })
                        }
                    }
                }
                if (state.filterMode == RenderFilterMode.STENCIL_EDGES) {
                    Text("EDGE THRESHOLD  ${state.threshold}", style = MaterialTheme.typography.labelSmall,
                        color = SlateMuted, modifier = Modifier.padding(top = 10.dp))
                    Slider(value = state.threshold.toFloat(), valueRange = 10f..120f,
                        onValueChange = { onIntent(WorkspaceIntent.SetThreshold(it.toInt())) })
                }
                if (state.filterMode == RenderFilterMode.VALUE_POSTERIZE) {
                    StudioMenu("Value layer", state.tonalSolo.title,
                        TonalBandSolo.entries.map { it.title to { onIntent(WorkspaceIntent.SetTonalSolo(it)) } })
                }
                StudioMenu("Composition", state.grid.title,
                    GridOverlayType.entries.map { it.title to { onIntent(WorkspaceIntent.SetGrid(it)) } })
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.isCalibrating, enabled = state.bitmap != null,
                        onClick = { onIntent(WorkspaceIntent.SetCalibration(!state.isCalibrating)) }, label = { Text("Paper pins") })
                    FilterChip(selected = state.opticsLocked, onClick = { onIntent(WorkspaceIntent.ToggleOpticsLock) }, label = { Text("Optics lock") })
                    FilterChip(selected = state.torchEnabled, onClick = { onIntent(WorkspaceIntent.ToggleTorch) }, label = { Text("Torch") })
                }
                Text("CLEAN DRAWING CAPTURE", style = MaterialTheme.typography.labelSmall,
                    color = SlateMuted, modifier = Modifier.padding(top = 12.dp))
                StudioMenu("Interval", "$intervalSeconds seconds", listOf(5L, 10L, 30L, 60L).map { seconds ->
                    "$seconds seconds" to { onIntervalChange(seconds) }
                }, enabled = !isRecording)
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    TextButton(onClick = onSnapshot, enabled = !captureBusy && !isRecording) { Text("Snapshot") }
                    TextButton(onClick = onRecordToggle, enabled = !captureBusy) {
                        Text(if (isRecording) "Stop · $frameCount frames" else "Start sequence", color = if (isRecording) CrimsonAlert else AmberGold)
                    }
                    TextButton(onClick = onExport, enabled = frameCount > 0 && !isRecording && !captureBusy) { Text("Export ZIP") }
                }
                Text("Volume Down toggles Ghost lock. Recording stops when Lucida leaves the foreground.",
                    style = MaterialTheme.typography.bodySmall, color = SlateMuted)
                Text("Keep the phone and paper fixed after calibration. Pins do not track camera movement.",
                    style = MaterialTheme.typography.bodySmall, color = StudioWhite, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StudioMenu(label: String, value: String, options: List<Pair<String, () -> Unit>>, enabled: Boolean = true) {
    var open by rememberSaveable { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SlateMuted, modifier = Modifier.weight(1f).padding(top = 14.dp))
        Box {
            TextButton(onClick = { open = true }, enabled = enabled) { Text(value) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                for ((title, action) in options) DropdownMenuItem(text = { Text(title) },
                    onClick = { open = false; action() })
            }
        }
    }
}
