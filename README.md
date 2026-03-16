# ComposePinchGrid

A **Google Photos-style** pinch-to-resize grid for Compose Multiplatform. Pinch to change column count with haptic feedback, breathing scale animation, and smooth transitions. Built on Compose Foundation — no Material dependency.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.aldefy/pinch-grid?color=blue)](https://central.sonatype.com/artifact/io.github.aldefy/pinch-grid)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-purple.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.8.0-blue)](https://www.jetbrains.com/compose-multiplatform/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

| Android | iOS | Desktop (JVM) | Web (Wasm) |
|:-------:|:---:|:-------------:|:----------:|
|    ✓    |  ✓  |       ✓       |     ✓      |

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.aldefy:pinch-grid:1.0.0-alpha01")
}
```

## Quick Start

```kotlin
@Composable
fun PhotoGrid(photos: List<Photo>) {
    val state = rememberPinchGridState()

    PinchGrid(state = state) {
        items(photos, key = { it.id }) { photo ->
            AsyncImage(
                model = photo.url,
                modifier = Modifier.aspectRatio(1f),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
```

That's it. Pinch to resize, haptic on snap, scroll position preserved.

## API

### PinchGrid

```kotlin
@Composable
fun PinchGrid(
    state: PinchGridState,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(0.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(0.dp),
    thresholdFraction: Float = PinchGridDefaults.ThresholdFraction,
    transitionSpec: ColumnTransitionSpec = PinchGridDefaults.TransitionSpec,
    gestureEnabled: Boolean = true,
    onColumnChanged: ((newCount: Int) -> Unit)? = null,
    content: LazyGridScope.() -> Unit,
)
```

### State

```kotlin
val state = rememberPinchGridState(
    initialColumnCount = 3,  // start with 3 columns
    minColumns = 1,          // full-width single item (zoom in limit)
    maxColumns = 5,          // dense grid (zoom out limit)
)

// Read current state
state.columnCount      // current column count
state.scaleProgress    // 0f–1f, how close to next snap (for custom item scaling)
state.isZoomingIn      // true = spreading fingers, false = pinching, null = idle
state.previousColumnCount  // for transition animation

// Programmatic control
state.snapToColumn(2)  // change columns from code (keyboard, buttons, accessibility)
```

## Gesture Configuration

The gesture feel is highly configurable. All parameters have tuned defaults:

### Threshold Fraction

Controls how much pinch is needed to trigger a column change. Lower = more sensitive.

```kotlin
PinchGrid(
    state = state,
    thresholdFraction = 0.45f,  // default — responsive but not accidental
    // thresholdFraction = 0.2f,  // very sensitive — small pinch triggers change
    // thresholdFraction = 0.7f,  // conservative — requires deliberate pinch
) { /* content */ }
```

### Defaults Reference

| Parameter | Default | What it does |
|-----------|---------|-------------|
| `ThresholdFraction` | `0.45f` | Scale change needed to snap. Lower = more sensitive |
| `DeadZone` | `0.01f` | Micro-movement filter. Prevents jitter from small finger tremors |
| `PinchOutThresholdMultiplier` | `0.85f` | Makes pinch-out 15% easier than pinch-in (compensates natural finger asymmetry) |
| `InitialColumnCount` | `3` | Starting columns |
| `MinColumns` | `1` | Zoom-in limit (single item full width) |
| `MaxColumns` | `5` | Zoom-out limit (dense grid) |

### Asymmetric Thresholds

Pinch-out (spreading fingers) naturally produces less scale change than pinch-in. The `PinchOutThresholdMultiplier` compensates — at `0.85f`, zooming in requires 15% less finger movement than zooming out, making both directions feel equally responsive.

### Dead Zone

The `0.01f` dead zone filters micro-movements. Without it, tiny finger tremors while holding a pinch cause the grid to jitter. You shouldn't need to change this.

## Transition Specs

```kotlin
// Google Photos style — instant reflow, no animation (default)
PinchGrid(
    state = state,
    transitionSpec = ColumnTransitionSpec.None,
) { /* content */ }

// Crossfade — smooth opacity transition between layouts
PinchGrid(
    state = state,
    transitionSpec = ColumnTransitionSpec.Crossfade(durationMillis = 200),
) { /* content */ }
```

## Breathing Scale

During a pinch gesture, the grid subtly scales up (zooming in) or down (zooming out) following your fingers. This provides real-time visual feedback before the column count snaps. The effect uses `graphicsLayer` — **zero recompositions**, pure GPU transform at 60fps.

You can use `state.scaleProgress` and `state.isZoomingIn` to apply custom per-item transforms:

```kotlin
items(photos, key = { it.id }) { photo ->
    val itemScale = when (state.isZoomingIn) {
        true -> 1f + (state.scaleProgress * 0.1f)
        false -> 1f - (state.scaleProgress * 0.1f)
        null -> 1f
    }
    AsyncImage(
        model = photo.url,
        modifier = Modifier
            .graphicsLayer { scaleX = itemScale; scaleY = itemScale }
            .aspectRatio(1f),
    )
}
```

## Haptic Feedback

Fires automatically on every column snap:

| Platform | Implementation |
|----------|---------------|
| Android | `View.performHapticFeedback(CLOCK_TICK)` |
| iOS | `UISelectionFeedbackGenerator.selectionChanged()` |
| Desktop | No-op |
| Web | No-op |

## Programmatic Control

```kotlin
val state = rememberPinchGridState()

// Buttons
Button(onClick = { state.snapToColumn(state.columnCount - 1) }) { Text("Zoom In") }
Button(onClick = { state.snapToColumn(state.columnCount + 1) }) { Text("Zoom Out") }

// Respond to changes
PinchGrid(
    state = state,
    onColumnChanged = { newCount -> analytics.log("columns_changed", newCount) },
) { /* content */ }
```

## Scroll Position Preservation

When the column count changes, the grid maintains the user's scroll position by snapshotting `firstVisibleItemIndex` before the change and restoring it after. For best results, provide stable `key` values to your items:

```kotlin
items(photos, key = { it.id }) { photo -> /* ... */ }
```

## Sample App

The included sample app demonstrates all features with 50 random photos, a live FPS counter, and an interactive threshold tuning slider.

```bash
# Run on connected Android device
./gradlew :sample:installDebug

# Run on desktop (buttons only, no pinch)
./gradlew :sample:run
```

## Building

```bash
# Build library for all targets
./gradlew :pinch-grid:build

# Generate API dump (after adding public API)
./gradlew :pinch-grid:apiDump

# Publish to local staging (for Maven Central upload)
./gradlew :pinch-grid:publishAllPublicationsToLocalStagingRepository \
    -Psigning.gnupg.keyName=F30A3C2E
```

## License

```
Copyright 2026 Adit Lal

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
