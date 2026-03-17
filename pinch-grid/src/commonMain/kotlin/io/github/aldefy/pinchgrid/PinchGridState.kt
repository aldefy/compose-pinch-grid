package io.github.aldefy.pinchgrid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.grid.LazyGridState
import kotlin.math.abs

/**
 * State holder for [PinchGrid].
 *
 * Tracks the current column count, raw pinch scale accumulator, and progress
 * toward the next column snap point. Gesture logic is driven by [onScale].
 *
 * @param initialColumnCount Starting number of columns.
 * @param minColumns Minimum columns allowed (fully zoomed in).
 * @param maxColumns Maximum columns allowed (fully zoomed out).
 */
@Stable
public class PinchGridState(
    initialColumnCount: Int = PinchGridDefaults.InitialColumnCount,
    public val minColumns: Int = PinchGridDefaults.MinColumns,
    public val maxColumns: Int = PinchGridDefaults.MaxColumns,
) {
    init {
        require(minColumns >= 1) { "minColumns must be >= 1, was $minColumns" }
        require(maxColumns >= minColumns) { "maxColumns ($maxColumns) must be >= minColumns ($minColumns)" }
        require(initialColumnCount in minColumns..maxColumns) {
            "initialColumnCount ($initialColumnCount) must be in $minColumns..$maxColumns"
        }
    }

    /** Current committed column count. */
    public var columnCount: Int by mutableIntStateOf(initialColumnCount)
        internal set

    /** Column count before the last change (for transition animation). */
    public var previousColumnCount: Int by mutableIntStateOf(initialColumnCount)
        internal set

    /**
     * Progress toward the next column snap, in range 0f..1f.
     * Callers can use this to scale grid items during a pinch gesture
     * (like Google Photos' progressive zoom preview).
     */
    public var scaleProgress: Float by mutableFloatStateOf(0f)
        internal set

    /**
     * Whether the current gesture is zooming in (spreading fingers → fewer columns).
     * True = zoom in, false = zoom out, null = no gesture active.
     */
    public var isZoomingIn: Boolean? by mutableStateOf(null)
        internal set

    /** Accumulated raw scale delta since last snap. */
    internal var scaleAccumulator: Float by mutableFloatStateOf(0f)

    /**
     * Saved anchor item index (center of viewport), snapshotted before each column change.
     * Using center anchor instead of first-visible prevents the "crawl to top" effect
     * when zooming in at the bottom of the list — same approach as Google Photos.
     */
    internal var savedAnchorItemIndex: Int by mutableIntStateOf(0)

    /** Whether a scroll restoration is pending. */
    internal var pendingScrollRestore: Boolean by mutableStateOf(false)

    /** Callback invoked when column count changes. Set by the composable. */
    internal var onColumnChanged: ((Int) -> Unit)? = null

    /** Haptic feedback trigger. Set by the composable via platform expect/actual. */
    internal var hapticFeedback: (() -> Unit)? = null

    /** Grid state reference for snapshotting scroll position. Set by the composable. */
    internal var gridStateRef: LazyGridState? = null

    /**
     * Snapshot the center visible item index from the grid.
     * Center-anchoring keeps the user's focal point stable across column changes.
     */
    private fun snapshotCenterItem() {
        gridStateRef?.let { grid ->
            val visibleItems = grid.layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                val centerItem = visibleItems[visibleItems.size / 2]
                savedAnchorItemIndex = centerItem.index
            } else {
                savedAnchorItemIndex = grid.firstVisibleItemIndex
            }
        }
    }

    /**
     * Called by the gesture modifier on each scale event.
     *
     * @param scaleFactor The multiplicative scale factor (1.0 = no change).
     * @param thresholdFraction Fraction of scale change required to trigger snap.
     * @param deadZone Minimum scale delta to register (prevents micro-jitter).
     * @param pinchOutMultiplier Asymmetric threshold for pinch-out.
     */
    internal fun onScale(
        scaleFactor: Float,
        thresholdFraction: Float,
        deadZone: Float,
        pinchOutMultiplier: Float,
    ) {
        // Convert multiplicative scale to additive delta: >1 means spread (zoom in), <1 means pinch (zoom out)
        val delta = scaleFactor - 1f

        // Dead zone — ignore micro movements
        if (abs(delta) < deadZone) return

        scaleAccumulator += delta

        // Determine direction and effective threshold
        // Positive accumulator = spreading fingers = zoom in = fewer columns
        // Negative accumulator = pinching fingers = zoom out = more columns
        val isSpread = scaleAccumulator > 0f
        val effectiveThreshold = if (isSpread) {
            thresholdFraction * pinchOutMultiplier
        } else {
            thresholdFraction
        }

        // Track direction for visual feedback
        isZoomingIn = isSpread

        // Update progress for callers who want progressive preview
        scaleProgress = (abs(scaleAccumulator) / effectiveThreshold).coerceIn(0f, 1f)

        // Check if we've crossed the threshold
        if (abs(scaleAccumulator) >= effectiveThreshold) {
            val newCount = if (isSpread) {
                // Spread → zoom in → fewer columns
                (columnCount - 1).coerceAtLeast(minColumns)
            } else {
                // Pinch → zoom out → more columns
                (columnCount + 1).coerceAtMost(maxColumns)
            }

            if (newCount != columnCount) {
                // Snapshot center item BEFORE mutating columnCount
                snapshotCenterItem()
                pendingScrollRestore = true
                previousColumnCount = columnCount
                columnCount = newCount
                hapticFeedback?.invoke()
                onColumnChanged?.invoke(newCount)
            }

            // Reset accumulator after snap
            scaleAccumulator = 0f
            scaleProgress = 0f
        }
    }

    /**
     * Programmatically snap to a specific column count.
     * Useful for Desktop/keyboard controls or accessibility.
     */
    public fun snapToColumn(target: Int) {
        val clamped = target.coerceIn(minColumns, maxColumns)
        if (clamped != columnCount) {
            // Snapshot center item BEFORE mutating columnCount
            snapshotCenterItem()
            pendingScrollRestore = true
            previousColumnCount = columnCount
            columnCount = clamped
            scaleAccumulator = 0f
            scaleProgress = 0f
            hapticFeedback?.invoke()
            onColumnChanged?.invoke(clamped)
        }
    }

    /** Column count to return to after double-tap zoom out. */
    internal var columnCountBeforeDoubleTap: Int? = null

    /**
     * Toggle zoom on double-tap — like Google Photos.
     * First tap: zoom to [minColumns] (fully zoomed in).
     * Second tap: return to previous column count.
     */
    internal fun toggleZoom() {
        if (columnCount == minColumns && columnCountBeforeDoubleTap != null) {
            // Already zoomed in — zoom back out to previous
            snapToColumn(columnCountBeforeDoubleTap!!)
            columnCountBeforeDoubleTap = null
        } else {
            // Zoom in to single column
            columnCountBeforeDoubleTap = columnCount
            snapToColumn(minColumns)
        }
    }

    /** Reset accumulator when gesture ends. */
    internal fun onGestureEnd() {
        scaleAccumulator = 0f
        scaleProgress = 0f
        isZoomingIn = null
    }

    public companion object {
        /**
         * [Saver] for [PinchGridState] to survive configuration changes.
         */
        public fun Saver(minColumns: Int, maxColumns: Int): Saver<PinchGridState, Int> =
            Saver(
                save = { it.columnCount },
                restore = { saved ->
                    PinchGridState(
                        initialColumnCount = saved.coerceIn(minColumns, maxColumns),
                        minColumns = minColumns,
                        maxColumns = maxColumns,
                    )
                },
            )
    }
}

/**
 * Creates and remembers a [PinchGridState] that survives configuration changes.
 */
@Composable
public fun rememberPinchGridState(
    initialColumnCount: Int = PinchGridDefaults.InitialColumnCount,
    minColumns: Int = PinchGridDefaults.MinColumns,
    maxColumns: Int = PinchGridDefaults.MaxColumns,
): PinchGridState {
    return rememberSaveable(
        saver = PinchGridState.Saver(minColumns, maxColumns),
    ) {
        PinchGridState(
            initialColumnCount = initialColumnCount,
            minColumns = minColumns,
            maxColumns = maxColumns,
        )
    }
}
