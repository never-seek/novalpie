package com.novalpie.nativeapp.ui

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.abs

/**
 * Mobile Chromium (which powers novalpie.cc in web browsers) uses an exponential velocity decay
 * model with light friction, allowing a thumb flick to glide effortlessly across multiple result
 * rows with natural physical momentum.
 *
 * Android Compose's default [androidx.compose.foundation.gestures.ScrollableDefaults.flingBehavior]
 * uses platform spline decay with high friction (0.015), halting abruptly after ~1 screenful.
 * This behavior provides the web browser's continuous, fluid gliding flow.
 */
internal const val SMOOTH_FLOW_FLING_DEFAULT_FRICTION = 0.28f
internal const val SMOOTH_FLOW_FLING_VELOCITY_THRESHOLD = 15f

@Composable
internal fun rememberSmoothFlowFlingBehavior(
    frictionMultiplier: Float = SMOOTH_FLOW_FLING_DEFAULT_FRICTION,
    absVelocityThreshold: Float = SMOOTH_FLOW_FLING_VELOCITY_THRESHOLD,
): FlingBehavior {
    val decay = remember(frictionMultiplier, absVelocityThreshold) {
        exponentialDecay<Float>(
            frictionMultiplier = frictionMultiplier,
            absVelocityThreshold = absVelocityThreshold,
        )
    }
    return remember(decay) {
        SmoothFlowFlingBehavior(decay)
    }
}

internal class SmoothFlowFlingBehavior(
    private val decay: DecayAnimationSpec<Float>,
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (abs(initialVelocity) < 1f) {
            return 0f
        }
        var lastValue = 0f
        var remainingVelocity = initialVelocity
        AnimationState(
            initialValue = 0f,
            initialVelocity = initialVelocity,
        ).animateDecay(decay) {
            val delta = value - lastValue
            val consumed = scrollBy(delta)
            lastValue = value
            remainingVelocity = velocity
            if (abs(delta - consumed) > 0.5f) {
                cancelAnimation()
            }
        }
        return remainingVelocity
    }
}
