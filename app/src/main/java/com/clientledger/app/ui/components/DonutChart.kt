package com.clientledger.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Donut chart component for displaying proportional data (2 segments max)
 * 
 * @param segments List of DonutSegment data (label, value, color)
 * @param centerText Text to display in the center of the donut
 * @param selectedIndex Index of selected segment (-1 for none)
 * @param onSegmentClick Callback when segment is clicked (index)
 * @param modifier Modifier for the chart container
 * @param size Size of the donut chart
 * @param strokeWidth Width of the donut stroke
 * @param gapAngle Gap angle between segments in degrees
 */
@Composable
fun DonutChart(
    segments: List<DonutSegment>,
    centerText: String,
    selectedIndex: Int = -1,
    onSegmentClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 40.dp,
    gapAngle: Float = 2f // Gap between segments in degrees
) {
    if (segments.isEmpty()) {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    val total = segments.sumOf { it.value }.toFloat()
    if (total == 0f) {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onSegmentClick != null) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val center = Offset(size.toPx() / 2, size.toPx() / 2)
                                val radius = (size.toPx() - strokeWidth.toPx()) / 2
                                val distance = sqrt(
                                    (tapOffset.x - center.x).pow(2) + (tapOffset.y - center.y).pow(2)
                                )
                                
                                // Check if tap is within donut ring
                                val innerRadius = radius - strokeWidth.toPx()
                                if (distance >= innerRadius && distance <= radius + strokeWidth.toPx() / 2) {
                                    // Calculate angle from center (0 = right, 90 = top, 180 = left, 270 = bottom)
                                    val angleRad = atan2(
                                        (tapOffset.y - center.y).toDouble(),
                                        (tapOffset.x - center.x).toDouble()
                                    )
                                    // Convert to degrees and adjust to start from top (-90)
                                    var angleDeg = Math.toDegrees(angleRad).toFloat() + 90f
                                    if (angleDeg < 0) angleDeg += 360f
                                    
                                    // Find which segment was clicked
                                    var currentAngle = 0f // Start from top (0 = top after adjustment)
                                    segments.forEachIndexed { index, segment ->
                                        val segmentAngle = (segment.value / total) * 360f
                                        val segmentStart = currentAngle
                                        val segmentEnd = currentAngle + segmentAngle
                                        
                                        // Check if angle is within segment (handle wrap-around)
                                        val isInSegment = if (segmentEnd > 360f) {
                                            // Segment wraps around
                                            angleDeg >= segmentStart || angleDeg <= (segmentEnd - 360f)
                                        } else {
                                            angleDeg >= segmentStart && angleDeg <= segmentEnd
                                        }
                                        
                                        if (isInSegment) {
                                            onSegmentClick(index)
                                            return@detectTapGestures
                                        }
                                        
                                        currentAngle += segmentAngle + gapAngle
                                        if (currentAngle >= 360f) currentAngle -= 360f
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            val center = Offset(size.toPx() / 2, size.toPx() / 2)
            val radius = (size.toPx() - strokeWidth.toPx()) / 2
            val innerRadius = radius - strokeWidth.toPx()
            
            var startAngle = -90f // Start from top
            
            segments.forEachIndexed { index, segment ->
                val segmentValue = segment.value.toFloat()
                val sweepAngle = (segmentValue / total) * 360f
                
                // Draw segment
                val color = if (selectedIndex == index) {
                    segment.color.copy(alpha = 1f)
                } else if (selectedIndex >= 0) {
                    segment.color.copy(alpha = 0.4f)
                } else {
                    segment.color
                }
                
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth.toPx())
                )
                
                startAngle += sweepAngle + gapAngle
            }
        }
        
        // Center text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

data class DonutSegment(
    val label: String,
    val value: Long,
    val color: Color
)

