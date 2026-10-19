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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
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
    cameraController: StudioCameraController,
    onIntent: (WorkspaceIntent) -> Unit,
    isRecording: Boolean,
    frameCount: Int,
    intervalSeconds: Long,
    captureBusy: Boolean,
    onIntervalChange: (Long) -> Unit,
    onSnapshot: () -> Unit,
    onRecordToggle: () -> Unit,
    onExport: () -> Unit,
    onReplayTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val cameraCaps by cameraController.capabilities.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (expanded) {
            // Expanded Drawer (Modal-like Floating Card)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = CharcoalSurface.copy(alpha = 0.98f),
                border = BorderStroke(1.dp, CharcoalBorder),
                shadowElevation = 8.dp
            ) {
                Column(Modifier.padding(16.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "STUDIO CONTROLS",
                            color = AmberGold,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    expanded = false
                                    onReplayTutorial()
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("📖 Guide", color = AmberGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(
                                onClick = { expanded = false },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("✕ Close", color = SlateMuted, fontSize = 13.sp)
                            }
                        }
                    }

                    // Tabs
                    val tabs = listOf("Style & Filter", "Grid & Paper", "Optics & Timelapse")
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = AmberGold,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = AmberGold
                            )
                        },
                        divider = { }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        title,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTab == index) AmberGold else SlateMuted
                                    )
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Tab Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                // Tab 0: Style & Filters
                                Text("REFERENCE RENDER MODE", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RenderFilterMode.entries.forEach { mode ->
                                        FilterChip(
                                            selected = state.filterMode == mode,
                                            onClick = { onIntent(WorkspaceIntent.SetFilter(mode)) },
                                            label = { Text(mode.title, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AmberGold,
                                                selectedLabelColor = Color(0xFF121417)
                                            )
                                        )
                                    }
                                }

                                if (state.filterMode == RenderFilterMode.STENCIL_EDGES) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("EDGE SENSITIVITY", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                                        Text("${state.threshold}", style = MaterialTheme.typography.labelSmall, color = AmberGold)
                                    }
                                    Slider(
                                        value = state.threshold.toFloat(),
                                        valueRange = 10f..120f,
                                        onValueChange = { onIntent(WorkspaceIntent.SetThreshold(it.toInt())) },
                                        colors = SliderDefaults.colors(thumbColor = AmberGold, activeTrackColor = AmberGold)
                                    )
                                }

                                if (state.filterMode == RenderFilterMode.VALUE_POSTERIZE) {
                                    Text("TONAL BAND SOLO", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TonalBandSolo.entries.forEach { solo ->
                                            FilterChip(
                                                selected = state.tonalSolo == solo,
                                                onClick = { onIntent(WorkspaceIntent.SetTonalSolo(solo)) },
                                                label = { Text(solo.title, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = AmberGold,
                                                    selectedLabelColor = Color(0xFF121417)
                                                )
                                            )
                                        }
                                    }
                                }

                                if (state.filterMode != RenderFilterMode.RAW_IMAGE) {
                                    Text("LINE TINT COLOR", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        StencilTint.entries.forEach { tint ->
                                            val isSelected = state.tint == tint
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .border(
                                                        if (isSelected) 3.dp else 1.dp,
                                                        if (isSelected) AmberGold else SlateMuted,
                                                        CircleShape
                                                    )
                                                    .padding(5.dp)
                                                    .background(Color(0xFF000000L or (tint.rgb.toLong() and 0xFFFFFFL)), CircleShape)
                                                    .selectable(
                                                        selected = isSelected,
                                                        role = Role.RadioButton,
                                                        onClick = { onIntent(WorkspaceIntent.SetTint(tint)) }
                                                    )
                                            )
                                        }
                                    }
                                }
                            }

                            1 -> {
                                // Tab 1: Grid & Paper Calibration
                                Text("COMPOSITION GUIDES", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GridOverlayType.entries.forEach { grid ->
                                        FilterChip(
                                            selected = state.grid == grid,
                                            onClick = { onIntent(WorkspaceIntent.SetGrid(grid)) },
                                            label = { Text(grid.title, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AmberGold,
                                                selectedLabelColor = Color(0xFF121417)
                                            )
                                        )
                                    }
                                }

                                Text("PERSPECTIVE ALIGNMENT", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                                Button(
                                    onClick = { onIntent(WorkspaceIntent.SetCalibration(!state.isCalibrating)) },
                                    enabled = state.bitmap != null,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (state.isCalibrating) CrimsonAlert else AmberGold,
                                        contentColor = if (state.isCalibrating) StudioWhite else Color(0xFF121417)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (state.isCalibrating) "Done Calibrating (Hide Pins)" else "Calibrate Paper Corners (4 Pins)")
                                }
                                Text(
                                    "Align the 4 pins to the 4 corners of the paper on your desk. The reference will warp to match the perspective of your drawing surface.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateMuted
                                )
                            }

                            2 -> {
                                // Tab 2: Optics & Timelapse
                                Text("CAMERA OPTICS", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = state.opticsLocked,
                                        onClick = { onIntent(WorkspaceIntent.ToggleOpticsLock) },
                                        label = { Text(if (state.opticsLocked) "Optics Locked" else "Lock Focus/AE", fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AmberGold,
                                            selectedLabelColor = Color(0xFF121417)
                                        )
                                    )
                                    FilterChip(
                                        selected = state.torchEnabled,
                                        onClick = { onIntent(WorkspaceIntent.ToggleTorch) },
                                        label = { Text(if (state.torchEnabled) "Torch ON" else "Torch", fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AmberGold,
                                            selectedLabelColor = Color(0xFF121417)
                                        )
                                    )
                                }

                                if (cameraCaps.supportsExposureCompensation && cameraCaps.isReady) {
                                    val ev = cameraCaps.exposureIndex * cameraCaps.exposureStepEv
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("EXPOSURE", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                                        Text("${if (ev > 0f) "+" else ""}${"%.1f".format(ev)} EV", style = MaterialTheme.typography.labelSmall, color = AmberGold)
                                    }
                                    Slider(
                                        value = cameraCaps.exposureIndex.toFloat(),
                                        valueRange = cameraCaps.exposureMinimum.toFloat()..cameraCaps.exposureMaximum.toFloat(),
                                        onValueChange = { cameraController.setExposureCompensationIndex(it.roundToInt()) },
                                        colors = SliderDefaults.colors(thumbColor = AmberGold, activeTrackColor = AmberGold)
                                    )
                                }

                                Text("CLEAN TIMELAPSE SEQUENCE", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Interval", fontSize = 12.sp, color = StudioWhite)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(5L, 10L, 30L, 60L).forEach { secs ->
                                            FilterChip(
                                                selected = intervalSeconds == secs,
                                                enabled = !isRecording,
                                                onClick = { onIntervalChange(secs) },
                                                label = { Text("${secs}s", fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = onRecordToggle,
                                        enabled = !captureBusy,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isRecording) CrimsonAlert else AmberGold,
                                            contentColor = if (isRecording) StudioWhite else Color(0xFF121417)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (isRecording) "Stop ($frameCount frames)" else "Start Timelapse", fontSize = 12.sp)
                                    }
                                    if (frameCount > 0 && !isRecording) {
                                        Button(
                                            onClick = onExport,
                                            enabled = !captureBusy,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CharcoalBorder,
                                                contentColor = StudioWhite
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Export ZIP", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Collapsed Bottom Floating Dock (Always Visible during drawing)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = CharcoalSurface.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, CharcoalBorder),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Opacity Slider (Immediate thumb control for artists)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${(state.opacity * 100).toInt()}%",
                        color = AmberGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp)
                    )
                    Slider(
                        value = state.opacity,
                        onValueChange = { onIntent(WorkspaceIntent.SetOpacity(it)) },
                        enabled = state.bitmap != null,
                        colors = SliderDefaults.colors(thumbColor = AmberGold, activeTrackColor = AmberGold),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quick Still Snapshot Button
                    TextButton(
                        onClick = onSnapshot,
                        enabled = !captureBusy && !isRecording,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("📸 Snap", fontSize = 12.sp, color = StudioWhite)
                    }

                    // Tools Drawer Toggle
                    Button(
                        onClick = { expanded = !expanded },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (expanded) AmberGold else CharcoalBorder,
                            contentColor = if (expanded) Color(0xFF121417) else StudioWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(if (expanded) "Tools ▾" else "Tools ▴", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Ghost Lock Toggle
                    TextButton(
                        onClick = { onIntent(WorkspaceIntent.ToggleTouchLock) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("🔒 Lock", fontSize = 12.sp, color = CrimsonAlert, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
