package com.lucida.studio.ui.workspace.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lucida.studio.ui.theme.AmberGold
import com.lucida.studio.ui.theme.CharcoalBorder
import com.lucida.studio.ui.theme.CharcoalSurface
import com.lucida.studio.ui.theme.CrimsonAlert
import com.lucida.studio.ui.theme.SlateMuted
import com.lucida.studio.ui.theme.StudioWhite

data class TutorialStep(
    val badge: String,
    val title: String,
    val description: String,
    val proTip: String,
    val iconEmoji: String
)

@Composable
fun StudioTutorialDialog(
    onDismiss: () -> Unit
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    var interactiveOpacity by remember { mutableFloatStateOf(0.65f) }

    val steps = listOf(
        TutorialStep(
            badge = "OPTICAL SUPERPOSITION",
            title = "Welcome to Lucida",
            description = "Place your phone on a stand looking down at your paper. Lucida overlays your digital reference artwork directly over your live camera feed so you can trace and draw on physical paper!",
            proTip = "Look at the phone screen while your hand draws on the paper beneath.",
            iconEmoji = "✨"
        ),
        TutorialStep(
            badge = "HOMOGRAPHY CALIBRATION",
            title = "3D Paper Corner Pins",
            description = "Phone mounted at an angle? Open Tools > Grid & Paper > Calibrate Paper Corners. Drag the 4 corner pins (TL, TR, BR, BL) to your physical paper corners.",
            proTip = "The precision loupe zooms in under your finger. The reference automatically warps to match your paper's perspective!",
            iconEmoji = "📌"
        ),
        TutorialStep(
            badge = "FILTERS & TONAL SOLO",
            title = "Stencils & Opacity",
            description = "Extract clean outlines with 'Stencil Edges', or isolate values with 'Tonal Solo' (shadows, midtones, highlights). Adjust opacity anytime using the thumb slider on the bottom dock.",
            proTip = "Try the interactive opacity slider below to test how transparency works!",
            iconEmoji = "🎨"
        ),
        TutorialStep(
            badge = "DISTRACTION-FREE",
            title = "Optics & Ghost Lock",
            description = "Tap 'Lock Focus/AE' to freeze camera autofocus so your moving hand won't cause lens hunting. Tap 'Lock' or press Volume Down for Ghost Lock—all UI vanishes so your resting palm never disrupts your art!",
            proTip = "Tap '📸 Snap' to save a clean drawing photo directly into your device's Gallery (Pictures/Lucida).",
            iconEmoji = "🔒"
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(26.dp),
            color = CharcoalSurface.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, CharcoalBorder),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Step pill and Skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = AmberGold.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "STEP ${stepIndex + 1} OF ${steps.size}",
                            color = AmberGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Skip", color = SlateMuted, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Animated Body
                AnimatedContent(
                    targetState = stepIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut())
                        }
                    },
                    label = "TutorialSlide"
                ) { currentStep ->
                    val step = steps[currentStep]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Emoji icon bubble
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(AmberGold.copy(alpha = 0.12f), CircleShape)
                                .border(1.dp, AmberGold.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(step.iconEmoji, fontSize = 28.sp)
                        }

                        Text(
                            text = step.badge,
                            color = AmberGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )

                        Text(
                            text = step.title,
                            color = StudioWhite,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = step.description,
                            color = StudioWhite.copy(alpha = 0.90f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )

                        // Interactive Opacity Test (Step 2 / index 2)
                        if (currentStep == 2) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                color = Color(0xFF1C1F26),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(0.5.dp, CharcoalBorder)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("LIVE OPACITY PREVIEW", fontSize = 10.sp, color = SlateMuted, fontWeight = FontWeight.Bold)
                                        Text("${(interactiveOpacity * 100).toInt()}%", fontSize = 11.sp, color = AmberGold, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = interactiveOpacity,
                                        onValueChange = { interactiveOpacity = it },
                                        colors = SliderDefaults.colors(thumbColor = AmberGold, activeTrackColor = AmberGold)
                                    )
                                }
                            }
                        }

                        // Pro-Tip Callout
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF161920),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.5.dp, CharcoalBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("💡", fontSize = 14.sp)
                                Text(
                                    step.proTip,
                                    color = SlateMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Bottom: Dots indicator and Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Step Dots
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        steps.indices.forEach { index ->
                            Box(
                                modifier = Modifier
                                    .height(7.dp)
                                    .width(if (index == stepIndex) 18.dp else 7.dp)
                                    .background(
                                        if (index == stepIndex) AmberGold else SlateMuted.copy(alpha = 0.4f),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }

                    // Navigation Action Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (stepIndex > 0) {
                            TextButton(
                                onClick = { stepIndex-- },
                                colors = ButtonDefaults.textButtonColors(contentColor = SlateMuted)
                            ) {
                                Text("Back", fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                if (stepIndex < steps.size - 1) {
                                    stepIndex++
                                } else {
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (stepIndex == steps.size - 1) Color(0xFF22C55E) else AmberGold,
                                contentColor = Color(0xFF121417)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text(
                                if (stepIndex == steps.size - 1) "Start Drawing 🚀" else "Next →",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
