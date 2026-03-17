package io.github.aldefy.pinchgrid

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged

/**
 * Attaches pinch-to-resize gesture handling to a composable.
 *
 * Uses raw pointer input with [PointerEventPass.Initial] to intercept two-finger
 * pinch gestures **before** the child LazyVerticalGrid processes them for scroll.
 * This is critical: the default [PointerEventPass.Main] goes child→parent, meaning
 * the grid's scroll handler would see events first and start scrolling before we
 * can detect the second finger.
 *
 * With [PointerEventPass.Initial] (parent→child), we see both fingers arrive first,
 * consume the zoom events, and the grid's scroll never activates during a pinch.
 * Single-finger scroll still works because we only consume when 2+ pointers are down.
 *
 * Also supports double-tap-to-zoom: toggles between current column count and
 * [PinchGridState.minColumns] (fully zoomed in), like Google Photos.
 *
 * @param state The [PinchGridState] to drive.
 * @param thresholdFraction Scale change fraction required to trigger column snap.
 * @param deadZone Minimum scale delta to register.
 * @param pinchOutMultiplier Asymmetric threshold multiplier for pinch-out.
 * @param doubleTapEnabled Whether double-tap-to-zoom is enabled.
 * @param enabled Whether gesture detection is enabled.
 */
@Composable
internal fun Modifier.pinchGridGesture(
    state: PinchGridState,
    thresholdFraction: Float = PinchGridDefaults.ThresholdFraction,
    deadZone: Float = PinchGridDefaults.DeadZone,
    pinchOutMultiplier: Float = PinchGridDefaults.PinchOutThresholdMultiplier,
    doubleTapEnabled: Boolean = true,
    enabled: Boolean = true,
): Modifier {
    if (!enabled) return this

    return this
        .pointerInput(state, thresholdFraction, deadZone, pinchOutMultiplier) {
            awaitEachGesture {
                // Wait for first finger — don't require unconsumed so we coexist with scroll
                awaitFirstDown(requireUnconsumed = false)

                var isPinching = false

                do {
                    // Initial pass: parent sees events BEFORE child (grid scroll)
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val pointers = event.changes

                    if (pointers.size >= 2) {
                        val zoom = event.calculateZoom()

                        if (zoom != 1f) {
                            isPinching = true
                            // Consume all pointer changes — grid scroll won't see them
                            pointers.forEach { change ->
                                if (change.positionChanged()) {
                                    change.consume()
                                }
                            }
                            state.onScale(
                                scaleFactor = zoom,
                                thresholdFraction = thresholdFraction,
                                deadZone = deadZone,
                                pinchOutMultiplier = pinchOutMultiplier,
                            )
                        }
                    }
                } while (pointers.any { it.pressed })

                if (isPinching) {
                    state.onGestureEnd()
                }
            }
        }
        .then(
            if (doubleTapEnabled) {
                Modifier.pointerInput(state) {
                    detectTapGestures(
                        onDoubleTap = {
                            state.toggleZoom()
                        },
                    )
                }
            } else {
                Modifier
            },
        )
}
