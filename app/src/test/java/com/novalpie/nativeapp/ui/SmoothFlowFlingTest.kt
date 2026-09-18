package com.novalpie.nativeapp.ui

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SmoothFlowFlingTest {
    private class TestFrameClock : MonotonicFrameClock {
        private var timeNanos = 0L
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            timeNanos += 16_000_000L // 16ms per frame
            return onFrame(timeNanos)
        }
    }

    @Test
    fun zeroVelocityReturnsZeroImmediatelyWithoutScrolling() = runBlocking {
        var scrollCount = 0
        val scope = object : ScrollScope {
            override fun scrollBy(pixels: Float): Float {
                scrollCount++
                return pixels
            }
        }
        val fling = SmoothFlowFlingBehavior(exponentialDecay())
        with(fling) {
            val remaining = scope.performFling(0f)
            assertEquals(0f, remaining, 0.001f)
            assertEquals(0, scrollCount)
        }
    }

    @Test
    fun positiveVelocityGlidesAndConsumesDeltas() = runBlocking {
        withContext(TestFrameClock()) {
            var totalScrolled = 0f
            val scope = object : ScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    totalScrolled += pixels
                    return pixels
                }
            }
            val fling = SmoothFlowFlingBehavior(
                exponentialDecay(frictionMultiplier = 0.82f, absVelocityThreshold = 0.5f)
            )
            with(fling) {
                val remaining = scope.performFling(3000f)
                assertTrue("Total scrolled distance must be substantial: $totalScrolled", totalScrolled > 500f)
                assertTrue("Velocity should decay near zero: $remaining", abs(remaining) <= 1f)
            }
        }
    }

    @Test
    fun negativeVelocityGlidesUpwards() = runBlocking {
        withContext(TestFrameClock()) {
            var totalScrolled = 0f
            val scope = object : ScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    totalScrolled += pixels
                    return pixels
                }
            }
            val fling = SmoothFlowFlingBehavior(
                exponentialDecay(frictionMultiplier = 0.82f, absVelocityThreshold = 0.5f)
            )
            with(fling) {
                val remaining = scope.performFling(-3000f)
                assertTrue("Total scrolled distance must be negative: $totalScrolled", totalScrolled < -500f)
                assertTrue("Velocity should decay near zero: $remaining", abs(remaining) <= 1f)
            }
        }
    }

    @Test
    fun cancelsAndReturnsResidualVelocityWhenScrollHitsBoundary() = runBlocking {
        withContext(TestFrameClock()) {
            var scrolledPixels = 0f
            val scope = object : ScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    // Simulate hitting a boundary after consuming 100 pixels
                    return if (scrolledPixels < 100f) {
                        scrolledPixels += pixels
                        pixels
                    } else {
                        0f
                    }
                }
            }
            val fling = SmoothFlowFlingBehavior(
                exponentialDecay(frictionMultiplier = 0.82f, absVelocityThreshold = 0.5f)
            )
            with(fling) {
                val remaining = scope.performFling(5000f)
                assertTrue("Remaining velocity should be returned when boundary hits: $remaining", remaining > 100f)
            }
        }
    }
}

