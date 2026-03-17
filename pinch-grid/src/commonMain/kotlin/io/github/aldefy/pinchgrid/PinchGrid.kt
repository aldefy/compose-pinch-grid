package io.github.aldefy.pinchgrid

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * A lazy vertical grid that supports pinch-to-resize column count,
 * inspired by the Google Photos gallery pattern.
 *
 * During the pinch gesture, the grid "breathes" — scaling up when zooming in
 * and scaling down when zooming out, providing real-time visual feedback
 * before the column count snaps. This uses [graphicsLayer] (draw-phase only,
 * no recomposition).
 */
@Composable
public fun PinchGrid(
    state: PinchGridState,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(0.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(0.dp),
    thresholdFraction: Float = PinchGridDefaults.ThresholdFraction,
    deadZone: Float = PinchGridDefaults.DeadZone,
    pinchOutThresholdMultiplier: Float = PinchGridDefaults.PinchOutThresholdMultiplier,
    breathingScaleIntensity: Float = PinchGridDefaults.BreathingScaleIntensity,
    breathingReturnDuration: Int = PinchGridDefaults.BreathingReturnDuration,
    hapticEnabled: Boolean = PinchGridDefaults.HapticEnabled,
    doubleTapEnabled: Boolean = PinchGridDefaults.DoubleTapEnabled,
    transitionSpec: ColumnTransitionSpec = PinchGridDefaults.TransitionSpec,
    gestureEnabled: Boolean = true,
    onColumnChanged: ((newCount: Int) -> Unit)? = null,
    content: LazyGridScope.() -> Unit,
) {
    val haptic = rememberHapticFeedback()
    state.hapticFeedback = if (hapticEnabled) haptic else null
    state.onColumnChanged = onColumnChanged
    state.gridStateRef = gridState

    // Restore scroll position after column count change.
    // We anchor on the CENTER visible item (not first-visible) so zooming in at the
    // bottom of the list doesn't "crawl to top" — same strategy as Google Photos.
    if (state.pendingScrollRestore) {
        LaunchedEffect(state.columnCount) {
            gridState.scrollToItem(state.savedAnchorItemIndex)
            state.pendingScrollRestore = false
        }
    }

    // Breathing scale: raw target from gesture, animated back to 1.0 on release
    val breathingTarget = if (breathingScaleIntensity > 0f) {
        when (state.isZoomingIn) {
            true -> 1f + (state.scaleProgress * breathingScaleIntensity)
            false -> 1f - (state.scaleProgress * breathingScaleIntensity)
            null -> 1f // gesture ended — animate back
        }
    } else {
        1f
    }
    val breathingScale by animateFloatAsState(
        targetValue = breathingTarget,
        animationSpec = if (state.isZoomingIn != null) {
            tween(durationMillis = 0) // instant during gesture
        } else {
            tween(durationMillis = breathingReturnDuration) // smooth return on release
        },
        label = "breathingScale",
    )

    Box(
        modifier = modifier.pinchGridGesture(
            state = state,
            thresholdFraction = thresholdFraction,
            deadZone = deadZone,
            pinchOutMultiplier = pinchOutThresholdMultiplier,
            doubleTapEnabled = doubleTapEnabled,
            enabled = gestureEnabled,
        ),
    ) {
        when (transitionSpec) {
            is ColumnTransitionSpec.None -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(state.columnCount),
                    state = gridState,
                    contentPadding = contentPadding,
                    verticalArrangement = verticalArrangement,
                    horizontalArrangement = horizontalArrangement,
                    modifier = Modifier.graphicsLayer {
                        scaleX = breathingScale
                        scaleY = breathingScale
                    },
                    content = content,
                )
            }

            is ColumnTransitionSpec.Crossfade -> {
                AnimatedContent(
                    targetState = state.columnCount,
                    transitionSpec = {
                        fadeIn(tween(transitionSpec.durationMillis)) togetherWith
                            fadeOut(tween(transitionSpec.durationMillis))
                    },
                    label = "PinchGridCrossfade",
                ) { columnCount ->
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnCount),
                        state = if (columnCount == state.columnCount) gridState else rememberLazyGridState(),
                        contentPadding = contentPadding,
                        verticalArrangement = verticalArrangement,
                        horizontalArrangement = horizontalArrangement,
                        modifier = Modifier.graphicsLayer {
                            scaleX = breathingScale
                            scaleY = breathingScale
                        },
                        content = content,
                    )
                }
            }
        }
    }
}
