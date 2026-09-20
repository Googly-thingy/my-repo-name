package com.example.animation

enum class MovementPreset(
    val title: String,
    val description: String,
    val defaultDurationSec: Float
) {
    TURNTABLE_360(
        title = "360° Turntable Showcase",
        description = "Smooth 360-degree continuous spin around the vertical axis. Ideal for showcasing photogrammetry scans.",
        defaultDurationSec = 4.0f
    ),
    HOVER_LEVITATE(
        title = "Floating Levitation",
        description = "Gentle anti-gravity hovering with organic bobbing and subtle tilt angles.",
        defaultDurationSec = 3.0f
    ),
    CINEMATIC_ZOOM(
        title = "Dramatic Push & Reveal",
        description = "Dynamic cinematic camera push-in with accelerated spin and smooth deceleration settle.",
        defaultDurationSec = 4.0f
    ),
    PLAYFUL_BOUNCE(
        title = "Squash & Bounce",
        description = "Energetic bouncing rhythm on the studio floor with natural squash and stretch response.",
        defaultDurationSec = 2.5f
    ),
    HEARTBEAT_PULSE(
        title = "Breathing Pulse",
        description = "Organic rhythm that pulses scale and slight elevation like a living artifact.",
        defaultDurationSec = 2.0f
    ),
    HELIX_SPIRAL(
        title = "Helix Ascent",
        description = "Corkscrew motion rising smoothly upwards while rotating, then descending back to base.",
        defaultDurationSec = 4.0f
    ),
    WOBBLE_GREETING(
        title = "Wave & Wobble",
        description = "Playful side-to-side greeting tilt for characters and robotic models.",
        defaultDurationSec = 2.0f
    );

    fun generateKeyframes(): List<Keyframe> {
        return when (this) {
            TURNTABLE_360 -> listOf(
                Keyframe(timeSec = 0.0f, rotY = 0f, easing = EasingType.LINEAR),
                Keyframe(timeSec = 1.0f, rotY = 90f, easing = EasingType.LINEAR),
                Keyframe(timeSec = 2.0f, rotY = 180f, easing = EasingType.LINEAR),
                Keyframe(timeSec = 3.0f, rotY = 270f, easing = EasingType.LINEAR),
                Keyframe(timeSec = 4.0f, rotY = 360f, easing = EasingType.LINEAR)
            )

            HOVER_LEVITATE -> listOf(
                Keyframe(timeSec = 0.0f, posY = 0.0f, rotZ = 0f, rotX = 0f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 0.75f, posY = 0.28f, rotZ = 4f, rotX = -3f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 1.5f, posY = 0.0f, rotZ = 0f, rotX = 0f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 2.25f, posY = -0.2f, rotZ = -4f, rotX = 3f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 3.0f, posY = 0.0f, rotZ = 0f, rotX = 0f, easing = EasingType.SMOOTH)
            )

            CINEMATIC_ZOOM -> listOf(
                Keyframe(timeSec = 0.0f, posZ = -1.2f, rotY = -120f, scale = 0.6f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 1.8f, posZ = 0.3f, rotY = 40f, scale = 1.25f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 2.8f, posZ = 0.0f, rotY = -10f, scale = 1.0f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 4.0f, posZ = 0.0f, rotY = 0f, scale = 1.0f, easing = EasingType.SMOOTH)
            )

            PLAYFUL_BOUNCE -> listOf(
                Keyframe(timeSec = 0.0f, posY = -0.1f, scale = 1.15f, rotX = 0f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 0.6f, posY = 0.65f, scale = 0.9f, rotX = 12f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 1.25f, posY = -0.1f, scale = 1.15f, rotX = 0f, easing = EasingType.BOUNCE),
                Keyframe(timeSec = 1.85f, posY = 0.4f, scale = 0.95f, rotX = -8f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 2.5f, posY = -0.1f, scale = 1.15f, rotX = 0f, easing = EasingType.BOUNCE)
            )

            HEARTBEAT_PULSE -> listOf(
                Keyframe(timeSec = 0.0f, scale = 1.0f, posY = 0f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 0.35f, scale = 1.22f, posY = 0.08f, easing = EasingType.ELASTIC),
                Keyframe(timeSec = 0.65f, scale = 0.96f, posY = -0.02f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 1.0f, scale = 1.12f, posY = 0.04f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 1.35f, scale = 0.98f, posY = 0.0f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 2.0f, scale = 1.0f, posY = 0f, easing = EasingType.SMOOTH)
            )

            HELIX_SPIRAL -> listOf(
                Keyframe(timeSec = 0.0f, posY = -0.5f, rotY = 0f, posX = 0f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 1.0f, posY = -0.1f, rotY = 90f, posX = 0.35f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 2.0f, posY = 0.5f, rotY = 180f, posX = 0f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 3.0f, posY = -0.1f, rotY = 270f, posX = -0.35f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 4.0f, posY = -0.5f, rotY = 360f, posX = 0f, easing = EasingType.SMOOTH)
            )

            WOBBLE_GREETING -> listOf(
                Keyframe(timeSec = 0.0f, rotZ = 0f, rotY = 0f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 0.5f, rotZ = -18f, rotY = 15f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 1.0f, rotZ = 18f, rotY = -15f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 1.5f, rotZ = -10f, rotY = 8f, easing = EasingType.SMOOTH),
                Keyframe(timeSec = 2.0f, rotZ = 0f, rotY = 0f, easing = EasingType.SMOOTH)
            )
        }
    }
}
