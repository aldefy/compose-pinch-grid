package io.github.aldefy.pinchgrid

import androidx.compose.runtime.Composable

/**
 * Provides a platform-specific haptic feedback trigger for column snap events.
 * Returns a lambda that triggers a short tick haptic when invoked.
 */
@Composable
internal expect fun rememberHapticFeedback(): () -> Unit
