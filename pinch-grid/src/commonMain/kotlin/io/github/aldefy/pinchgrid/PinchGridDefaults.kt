package io.github.aldefy.pinchgrid

/**
 * Default values for [PinchGrid].
 */
public object PinchGridDefaults {

    /** Default initial column count. */
    public const val InitialColumnCount: Int = 3

    /** Minimum number of columns (fully zoomed in). */
    public const val MinColumns: Int = 1

    /** Maximum number of columns (fully zoomed out). */
    public const val MaxColumns: Int = 5

    /**
     * Fraction of scale change required to trigger a column count change.
     * Lower values = more sensitive. Tuned for responsive-but-not-accidental feel.
     */
    public const val ThresholdFraction: Float = 0.45f

    /**
     * Dead zone for scale changes to prevent micro-jitter from small finger movements.
     */
    public const val DeadZone: Float = 0.01f

    /**
     * Asymmetric threshold multiplier for pinch-out (zoom in → fewer columns).
     * Pinch-out naturally produces less scale change than pinch-in,
     * so we use a lower threshold to compensate.
     */
    public const val PinchOutThresholdMultiplier: Float = 0.85f

    /** Default transition spec for column count changes. Google Photos uses None. */
    public val TransitionSpec: ColumnTransitionSpec = ColumnTransitionSpec.None
}
