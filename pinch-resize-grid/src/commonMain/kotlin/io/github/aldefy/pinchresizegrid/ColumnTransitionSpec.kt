package io.github.aldefy.pinchresizegrid

import androidx.compose.animation.core.Spring

/**
 * Defines how the grid transitions when the column count changes.
 */
public sealed class ColumnTransitionSpec {

    /** No transition animation — column count changes instantly (Google Photos style). */
    public data object None : ColumnTransitionSpec()

    /**
     * Crossfade between the old and new column layouts.
     *
     * @param durationMillis Duration of the crossfade animation.
     */
    public data class Crossfade(
        val durationMillis: Int = 200,
    ) : ColumnTransitionSpec()
}
