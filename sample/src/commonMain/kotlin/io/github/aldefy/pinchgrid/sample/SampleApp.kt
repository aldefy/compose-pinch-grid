package io.github.aldefy.pinchgrid.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.aldefy.pinchgrid.PinchGrid
import io.github.aldefy.pinchgrid.PinchGridDefaults
import io.github.aldefy.pinchgrid.rememberPinchGridState

private val photoUrls = (1..50).map { index ->
    "https://picsum.photos/seed/grid$index/400/400"
}

private val placeholderColors = listOf(
    Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6),
    Color(0xFFFFD54F), Color(0xFFBA68C8), Color(0xFF4DD0E1),
    Color(0xFFFF8A65), Color(0xFFA1887F), Color(0xFF90A4AE),
    Color(0xFFF06292),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleApp() {
    MaterialTheme {
        val gridState = rememberPinchGridState(
            initialColumnCount = 3,
            minColumns = 1,
            maxColumns = 5,
        )
        var threshold by remember { mutableFloatStateOf(PinchGridDefaults.ThresholdFraction) }
        val fpsInfo = rememberFpsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pinch Grid") },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Columns: ${gridState.columnCount}  |  Progress: ${
                                    (gridState.scaleProgress * 100).toInt()
                                }%",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "${fpsInfo.fps} FPS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    fpsInfo.fps >= 55 -> Color(0xFF4CAF50) // green
                                    fpsInfo.fps >= 40 -> Color(0xFFFF9800) // orange
                                    else -> Color(0xFFF44336) // red
                                },
                            )
                        }
                        Text(
                            text = "Threshold: ${((threshold * 100).toInt() / 100f)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Slider(
                            value = threshold,
                            onValueChange = { threshold = it },
                            valueRange = 0.1f..0.8f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row {
                            FilledTonalButton(
                                onClick = { gridState.snapToColumn(gridState.columnCount - 1) },
                                enabled = gridState.columnCount > gridState.minColumns,
                            ) {
                                Text("- Zoom In")
                            }
                            Spacer(Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = { gridState.snapToColumn(gridState.columnCount + 1) },
                                enabled = gridState.columnCount < gridState.maxColumns,
                            ) {
                                Text("+ Zoom Out")
                            }
                        }
                    }
                }

                PinchGrid(
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    thresholdFraction = threshold,
                ) {
                    items(
                        items = photoUrls,
                        key = { it },
                    ) { url ->
                        PhotoCell(url = url, index = photoUrls.indexOf(url))
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoCell(url: String, index: Int) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(placeholderColors[index % placeholderColors.size]),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = "Photo $index",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = "${index + 1}",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
