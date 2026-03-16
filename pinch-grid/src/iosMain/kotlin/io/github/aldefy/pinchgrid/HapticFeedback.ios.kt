package io.github.aldefy.pinchgrid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UISelectionFeedbackGenerator

@Composable
internal actual fun rememberHapticFeedback(): () -> Unit {
    val generator = remember { UISelectionFeedbackGenerator() }
    return remember(generator) {
        {
            generator.prepare()
            generator.selectionChanged()
        }
    }
}
