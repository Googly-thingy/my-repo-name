package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.StudioAccent
import com.example.ui.theme.StudioAccentMuted
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioEmeraldBg
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSurfaceMuted
import com.example.ui.theme.StudioSurfaceSubtle
import com.example.ui.theme.StudioSurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

data class TutorialChapter(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBgColor: Color,
    val conceptExplanation: String,
    val keyActions: List<String>,
    val proTip: String
)

val tutorialChapters = listOf(
    TutorialChapter(
        title = "3D Keyframing Fundamentals",
        subtitle = "Capture spatial poses and interpolate motion effortlessly",
        icon = Icons.Default.Timeline,
        iconColor = StudioAccent,
        iconBgColor = StudioAccentMuted,
        conceptExplanation = "A Keyframe records the complete 3D state (Position X/Y/Z, Rotation, Scale, and Deformations) at a specific point in time. The animation engine computes mathematically smooth transitions between any two keyframes.",
        keyActions = listOf(
            "Scrub the timeline to the exact second where you want a pose.",
            "Tap '+ Keyframe' to snapshot the current 3D transform.",
            "Tap any keyframe point on the scrubber bar to jump back and review it."
        ),
        proTip = "Establish your 'Golden Poses' first (the start, apex, and end) before fine-tuning intermediate frames."
    ),
    TutorialChapter(
        title = "FlipaClip 3D Onion Skinning",
        subtitle = "Ghost overlays showing previous and future silhouettes",
        icon = Icons.Default.Layers,
        iconColor = Color(0xFFE11D48),
        iconBgColor = Color(0xFFFFE4E6),
        conceptExplanation = "Just like traditional cel animation in FlipaClip, 3D Onion Skinning projects ghosted silhouettes of adjacent frames directly onto your 3D viewport so you can visually judge spacing, arcs, and overlapping action.",
        keyActions = listOf(
            "Toggle 'Onion Skin' on the timeline bar dock.",
            "Pause playback and step between frames: the Previous Pose appears in Coral Red and the Next Pose appears in Electric Cyan.",
            "Use the ghosts to avoid jittery movements and ensure smooth spatial arcs."
        ),
        proTip = "If your 3D model appears too fast, look at the onion skin gap. Smaller spacing produces slower, deliberate motion."
    ),
    TutorialChapter(
        title = "Filmstrip & Keyframe Dock",
        subtitle = "Horizontal visual management for every pose in your timeline",
        icon = Icons.Default.Movie,
        iconColor = Color(0xFF7C3AED),
        iconBgColor = Color(0xFFEDE9FE),
        conceptExplanation = "The bottom filmstrip displays numbered frame cells (#1, #2, #3...) representing every saved keyframe chronologically. It provides instantaneous pose navigation without guessing timestamps.",
        keyActions = listOf(
            "Tap any frame thumbnail to inspect that exact pose in the 3D viewport.",
            "Tap 'Clone' to duplicate the selected pose 0.5s forward in time.",
            "Tap 'Delete' to discard unneeded poses while keeping your timeline balanced."
        ),
        proTip = "Cloning a pose and adjusting just one parameter (like squash or elevation) is the fastest way to animate rhythmic steps."
    ),
    TutorialChapter(
        title = "Squash & Stretch (Physics)",
        subtitle = "Preserve 3D mass while giving character and impact to objects",
        icon = Icons.Default.Animation,
        iconColor = Color(0xFFD97706),
        iconBgColor = Color(0xFFFFFBEB),
        conceptExplanation = "Squash & Stretch is the fundamental rule of cartoon physics. When an object accelerates or strikes the ground, it compresses or elongates while preserving constant volume (expanding radially in X/Z as it squashes in Y).",
        keyActions = listOf(
            "Tap 'Edit Pose' on any selected keyframe.",
            "Open the 'Squash & Bend' tab and adjust the Squash & Stretch slider.",
            "Slide negative (-0.5) for heavy impact compression; slide positive (+0.8) for fast vertical elongation."
        ),
        proTip = "Always squash on the contact frame, then immediately snap back to neutral or stretch on the rebound!"
    ),
    TutorialChapter(
        title = "Curvature Bend & Twist",
        subtitle = "Organic vertex deformation for spines, coils, and natural waving",
        icon = Icons.Default.Tune,
        iconColor = StudioEmerald,
        iconBgColor = StudioEmeraldBg,
        conceptExplanation = "Real-world objects are not rigid blocks. Bend curves the model geometry along its height, and Twist rotates top vertices relative to the base, giving natural flex to organic scans and structures.",
        keyActions = listOf(
            "In the Pose Inspector, drag 'Twist' (-180° to +180°) for spiraling deformation.",
            "Drag 'Bend Curvature' (-0.8 to +0.8) to arch the model forward or backward.",
            "Combine Bend with rotation to simulate wind-blown structures or dynamic gestures."
        ),
        proTip = "Use a gentle Bend cycle of -0.2 to +0.2 across 2 seconds to make static scans feel like they are breathing."
    ),
    TutorialChapter(
        title = "Spatial Posing & Transform",
        subtitle = "Precise Yaw rotation, height elevation, and 3-axis scale",
        icon = Icons.Default.ViewInAr,
        iconColor = StudioPrimary,
        iconBgColor = StudioSurfaceMuted,
        conceptExplanation = "The Transform tab gives you numeric slider precision over Yaw orientation (-360° to +360°), elevation height (-1.5 to +1.5 units), and proportional scaling.",
        keyActions = listOf(
            "Select any keyframe and open 'Transform'.",
            "Fine-tune the height slider to synchronize jumps, landings, or hover elevation.",
            "Set rotation angles to smoothly spin models around their vertical axis."
        ),
        proTip = "For a seamless 360° spin, set Frame 1 at 0° and the final Frame at 360°, using Linear or Smooth easing."
    ),
    TutorialChapter(
        title = "Motion Easing & Timing Curves",
        subtitle = "Choose how poses transition: smooth, elastic, bounce, or stop-motion",
        icon = Icons.Default.Grain,
        iconColor = Color(0xFF2563EB),
        iconBgColor = Color(0xFFDBEAFE),
        conceptExplanation = "Easing dictates how movement accelerates between keyframes. Linear motion feels robotic, while customized acceleration curves bring biological realism and comedic snap.",
        keyActions = listOf(
            "Tap the 'Easing' tab in the Pose Inspector.",
            "Choose 'Smooth' for organic ease-in-out transitions.",
            "Choose 'Step' for FlipaClip traditional hand-drawn/stop-motion hard cuts.",
            "Choose 'Bounce' for impact collisions, or 'Elastic' for rubber-band snaps."
        ),
        proTip = "Use 'Step' easing if you want a classic claymation or flipbook stop-motion aesthetic!"
    ),
    TutorialChapter(
        title = "1-Tap Movement Presets",
        subtitle = "Instant choreography designed by professional 3D animators",
        icon = Icons.Default.AutoAwesome,
        iconColor = Color(0xFFD97706),
        iconBgColor = Color(0xFFFFFBEB),
        conceptExplanation = "If you don't want to keyframe manually from scratch, Presets instantly generate complete multi-keyframe choreography with tailored curves, squash/stretch, and rotational cycles.",
        keyActions = listOf(
            "Tap 'Presets' in the top studio bar.",
            "Choose from Turntable 360, Playful Bounce, Heartbeat Pulse, Helix Spiral, Levitation Hover, or Dramatic Camera Swing.",
            "The preset automatically populates the filmstrip and starts looping."
        ),
        proTip = "Apply a preset first, then edit individual keyframes in the filmstrip to customize it to your liking!"
    ),
    TutorialChapter(
        title = "Framerates & Multi-Format Export",
        subtitle = "From 12 FPS anime timing to 60 FPS fluid rendering and 3D files",
        icon = Icons.Default.Speed,
        iconColor = StudioPrimary,
        iconBgColor = StudioSurfaceMuted,
        conceptExplanation = "Customizable framerates allow you to target traditional 12 FPS animation, 24 FPS cinema, or 60 FPS ultra-fluidity. Once finished, you can export both ready-to-share videos and 3D mesh files.",
        keyActions = listOf(
            "Tap the 'FPS' pill to switch between 12, 24, 30, and 60 frames per second.",
            "Use the Step Forward/Backward buttons on the timeline for frame-accurate review.",
            "Tap 'Export' to generate MP4 videos, WebM clips, Animated GIFs, or export modified 3D meshes (OBJ, STL, PLY)."
        ),
        proTip = "12 FPS gives your 3D animation a distinct hand-animated anime or Lego stop-motion personality."
    )
)

@Composable
fun AnimationTutorialDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val chapter = tutorialChapters[currentStep]
    val totalSteps = tutorialChapters.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StudioSurfaceWhite,
            shadowElevation = 16.dp,
            modifier = modifier
                .fillMaxWidth(0.94f)
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Step Pill Indicator
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioSurfaceMuted
                    ) {
                        Text(
                            text = "Feature ${currentStep + 1} of $totalSteps",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Tutorial",
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Progress Bar Dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 0 until totalSteps) {
                        val isCurrent = i == currentStep
                        val isPassed = i < currentStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    when {
                                        isCurrent -> StudioPrimary
                                        isPassed -> StudioPrimary.copy(alpha = 0.4f)
                                        else -> StudioSurfaceMuted
                                    }
                                )
                                .clickable { currentStep = i }
                        )
                    }
                }

                // Chapter Content (Smooth Animated Transition)
                AnimatedContent(
                    targetState = chapter,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tutorial_page"
                ) { currentChapter ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Icon + Title Block
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(currentChapter.iconBgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = currentChapter.icon,
                                    contentDescription = null,
                                    tint = currentChapter.iconColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = currentChapter.title,
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentChapter.subtitle,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Concept Explanation Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = StudioSurfaceSubtle,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = currentChapter.conceptExplanation,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        // Key Action Steps
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "How to use this feature:",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            currentChapter.keyActions.forEachIndexed { idx, action ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(StudioPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = action,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }

                        // Pro Tip Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StudioEmeraldBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = StudioEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = "Pro Animator Tip",
                                        color = StudioEmerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = currentChapter.proTip,
                                        color = Color(0xFF065F46),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Previous", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Spacer(Modifier.width(10.dp))
                    }

                    if (currentStep < totalSteps - 1) {
                        Button(
                            onClick = { currentStep++ },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("tutorial_next_button")
                        ) {
                            Text("Next Feature", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioEmerald,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("tutorial_finish_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Start Animating", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
