package com.lucida.studio.ui.workspace.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucida.studio.ui.theme.AmberGold
import com.lucida.studio.ui.theme.CharcoalBorder
import com.lucida.studio.ui.theme.CharcoalSurface
import com.lucida.studio.ui.theme.CrimsonAlert
import com.lucida.studio.ui.theme.SlateMuted
import com.lucida.studio.ui.theme.StudioWhite
import com.lucida.studio.ui.workspace.WorkspaceIntent
import com.lucida.studio.ui.workspace.WorkspaceState

@Composable
fun TelemetryTopBar(
    state: WorkspaceState,
    onIntent: (WorkspaceIntent) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = CharcoalSurface.copy(alpha = 0.90f),
            border = BorderStroke(1.dp, CharcoalBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Logo and Status Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(
                                color = if (state.error != null) CrimsonAlert else if (state.bitmap != null) Color(0xFF22C55E) else AmberGold,
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = "LUCIDA",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = AmberGold
                    )
                    if (state.homographyActive) {
                        Surface(
                            color = AmberGold.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "WARP",
                                color = AmberGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (state.isCalibrating) {
                        Surface(
                            color = CrimsonAlert.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "PINS",
                                color = CrimsonAlert,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Right: Quick Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Flashlight toggle
                    TextButton(
                        onClick = { onIntent(WorkspaceIntent.ToggleTorch) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (state.torchEnabled) AmberGold else SlateMuted
                        )
                    ) {
                        Text(if (state.torchEnabled) "Torch ON" else "Torch", fontSize = 12.sp)
                    }

                    // Reset alignment/fit
                    if (state.bitmap != null) {
                        TextButton(
                            onClick = { onIntent(WorkspaceIntent.ResetTransform) },
                            colors = ButtonDefaults.textButtonColors(contentColor = SlateMuted)
                        ) {
                            Text("Fit", fontSize = 12.sp)
                        }
                    }

                    // Import image
                    Button(
                        onClick = onImport,
                        enabled = !state.isLoading && !state.isRestoring,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor = Color(0xFF121417)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("+ Import", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Status Subtitle (only when message is meaningful or active)
        if (state.cameraStatus.isNotBlank() && !state.cameraStatus.startsWith("Waiting")) {
            Surface(
                color = CharcoalSurface.copy(alpha = 0.75f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.5.dp, CharcoalBorder),
                modifier = Modifier.padding(start = 6.dp)
            ) {
                Text(
                    text = state.cameraStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
