package com.clientledger.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Pie chart component for displaying proportional data with multiple segments
 * 
 * @param segments List of PieSegment data (label, value, color)
 * @param modifier Modifier for the chart container
 * @param size Size of the pie chart
 * @param strokeWidth Width of the pie chart stroke (0 for filled)
 * @param gapAngle Gap angle between segments in degrees
 */
@Composable
fun PieChart(
    segments: List<PieSegment>,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 0.dp,
    gapAngle: Float = 2f // Gap between segments in degrees
) {
    if (segments.isEmpty()) {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет данных",
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
            modifier = Modifier.fillMaxSize()
        ) {
            val center = Offset(size.toPx() / 2, size.toPx() / 2)
            val radius = (size.toPx() - strokeWidth.toPx()) / 2
            val innerRadius = if (strokeWidth > 0.dp) radius - strokeWidth.toPx() else 0f
            
            var startAngle = -90f // Start from top
            
            segments.forEach { segment ->
                val segmentValue = segment.value.toFloat()
                val sweepAngle = (segmentValue / total) * 360f
                
                // Draw segment
                if (strokeWidth > 0.dp && innerRadius > 0) {
                    // Donut chart (with hole)
                    drawArc(
                        color = segment.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth.toPx())
                    )
                } else {
                    // Full pie chart
                    drawArc(
                        color = segment.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2)
                    )
                }
                
                startAngle += sweepAngle + gapAngle
            }
        }
    }
}

data class PieSegment(
    val label: String,
    val value: Long,
    val color: Color
)
