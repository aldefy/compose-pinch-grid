package io.github.aldefy.pinchresizegrid.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos

/**
 * Measures real-time FPS using [withFrameNanos].
 * Returns a state holding the current FPS (updated every ~500ms).
 * Pure Compose, no platform APIs — works on all KMP targets.
 */
@Composable
fun rememberFpsState(): FpsInfo {
    var fps by remember { mutableIntStateOf(0) }
    var frameTimeMs by remember { mutableStateOf(0f) }
    var lastFrameNanos by remember { mutableLongStateOf(0L) }
    var frameCount by remember { mutableIntStateOf(0) }
    var accumNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            val currentNanos = withFrameNanos { it }
            if (lastFrameNanos != 0L) {
                val deltaNanos = currentNanos - lastFrameNanos
                frameTimeMs = deltaNanos / 1_000_000f
                accumNanos += deltaNanos
                frameCount++

                // Update FPS display every ~500ms
                if (accumNanos >= 500_000_000L) {
                    fps = ((frameCount.toLong() * 1_000_000_000L) / accumNanos).toInt()
                    frameCount = 0
                    accumNanos = 0L
                }
            }
            lastFrameNanos = currentNanos
        }
    }

    return remember { FpsInfo() }.also {
        it.fps = fps
        it.frameTimeMs = frameTimeMs
    }
}

class FpsInfo {
    var fps by mutableIntStateOf(0)
    var frameTimeMs by mutableStateOf(0f)
}
