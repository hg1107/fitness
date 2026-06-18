package com.example.fitnesstracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ProgressChartView(
    points: List<Pair<Long, Double>>, // timestamp to value
    valueSuffix: String = "",
    lineColor: Color = StravaOrange,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CardGray)
                .border(1.dp, BorderGray, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No history logs yet",
                color = MediumGray,
                fontSize = 14.sp
            )
        }
        return
    }

    // Sort points chronologically
    val sortedPoints = remember(points) { points.sortedBy { it.first } }

    val textMeasurer = rememberTextMeasurer()
    var selectedIndex by remember(sortedPoints) { mutableStateOf<Int?>(null) }
    var touchX by remember { mutableStateOf<Float?>(null) }

    val minVal = remember(sortedPoints) { sortedPoints.minOf { it.second } }
    val maxVal = remember(sortedPoints) { sortedPoints.maxOf { it.second } }
    val valRange = remember(minVal, maxVal) { (maxVal - minVal).coerceAtLeast(1.0) }

    // Pad the vertical range slightly for aesthetics
    val yMin = remember(minVal, valRange) { minVal - valRange * 0.1 }
    val yMax = remember(maxVal, valRange) { maxVal + valRange * 0.1 }
    val yRange = remember(yMin, yMax) { yMax - yMin }

    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CardGray)
            .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        // Selection Detail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            val index = selectedIndex
            if (index != null && index in sortedPoints.indices) {
                val point = sortedPoints[index]
                val dateStr = dateFormat.format(Date(point.first))
                val formattedVal = if (point.second % 1.0 == 0.0) {
                    point.second.toInt().toString()
                } else {
                    String.format(Locale.US, "%.1f", point.second)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$formattedVal $valueSuffix",
                        color = lineColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = dateStr,
                        color = LightGray,
                        fontSize = 13.sp
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val latestPoint = sortedPoints.last()
                    val formattedVal = if (latestPoint.second % 1.0 == 0.0) {
                        latestPoint.second.toInt().toString()
                    } else {
                        String.format(Locale.US, "%.1f", latestPoint.second)
                    }
                    Text(
                        text = "Latest: $formattedVal $valueSuffix",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Swipe to view history",
                        color = MediumGray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(sortedPoints) {
                    detectTapGestures(
                        onTap = { offset ->
                            val width = size.width
                            val xSpace = width - 80f // 60f padding right for labels, 20f padding left
                            val stepX = if (sortedPoints.size > 1) xSpace / (sortedPoints.size - 1) else xSpace
                            val relX = (offset.x - 20f).coerceIn(0f, xSpace)
                            val idx = (relX / stepX).roundToInt().coerceIn(0, sortedPoints.size - 1)
                            selectedIndex = idx
                            touchX = 20f + idx * stepX
                        }
                    )
                }
                .pointerInput(sortedPoints) {
                    detectDragGestures(
                        onDragEnd = {
                            selectedIndex = null
                            touchX = null
                        },
                        onDragCancel = {
                            selectedIndex = null
                            touchX = null
                        },
                        onDrag = { change, _ ->
                            val width = size.width
                            val xSpace = width - 80f
                            val stepX = if (sortedPoints.size > 1) xSpace / (sortedPoints.size - 1) else xSpace
                            val relX = (change.position.x - 20f).coerceIn(0f, xSpace)
                            val idx = (relX / stepX).roundToInt().coerceIn(0, sortedPoints.size - 1)
                            selectedIndex = idx
                            touchX = 20f + idx * stepX
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val chartRight = width - 60f // 60f margin for Y-axis labels
            val chartLeft = 20f
            val chartBottom = height - 20f
            val chartTop = 10f

            val chartWidth = chartRight - chartLeft
            val chartHeight = chartBottom - chartTop

            // 1. Draw Gridlines and Y labels
            val gridLinesCount = 3
            val yLabelStyle = TextStyle(color = MediumGray, fontSize = 10.sp)
            for (i in 0..gridLinesCount) {
                val ratio = i.toFloat() / gridLinesCount
                val gridY = chartBottom - ratio * chartHeight
                val gridVal = yMin + ratio * yRange

                // Draw line
                drawLine(
                    color = BorderGray,
                    start = Offset(chartLeft, gridY),
                    end = Offset(chartRight, gridY),
                    strokeWidth = 1f
                )

                // Draw label
                val labelText = if (gridVal % 1.0 == 0.0) {
                    gridVal.toInt().toString()
                } else {
                    String.format(Locale.US, "%.1f", gridVal)
                }
                val textLayout = textMeasurer.measure(labelText, yLabelStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = labelText,
                    topLeft = Offset(chartRight + 8f, gridY - textLayout.size.height / 2f),
                    style = yLabelStyle
                )
            }

            // Map points to screen coordinates
            val coords = sortedPoints.mapIndexed { index, pair ->
                val x = if (sortedPoints.size > 1) {
                    chartLeft + (index.toFloat() / (sortedPoints.size - 1)) * chartWidth
                } else {
                    chartLeft + chartWidth / 2f
                }
                val y = chartBottom - ((pair.second - yMin) / yRange).toFloat() * chartHeight
                Offset(x, y)
            }

            // 2. Draw under-line gradient fill
            if (coords.size > 1) {
                val fillPath = Path().apply {
                    moveTo(coords.first().x, chartBottom)
                    coords.forEach { lineTo(it.x, it.y) }
                    lineTo(coords.last().x, chartBottom)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        startY = chartTop,
                        endY = chartBottom
                    )
                )
            }

            // 3. Draw connection line
            if (coords.size > 1) {
                val strokePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        lineTo(coords[i].x, coords[i].y)
                    }
                }
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 4. Draw data dots
            coords.forEachIndexed { index, offset ->
                drawCircle(
                    color = CardGray,
                    radius = 4.dp.toPx(),
                    center = offset
                )
                drawCircle(
                    color = lineColor,
                    radius = 2.5.dp.toPx(),
                    center = offset,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // 5. Draw selection indicator (vertical dashed line and selected dot overlay)
            val currentTouchX = touchX
            val currentIdx = selectedIndex
            if (currentTouchX != null && currentIdx != null && currentIdx in coords.indices) {
                val selectedCoord = coords[currentIdx]

                // Vertical line
                drawLine(
                    color = LightGray.copy(alpha = 0.5f),
                    start = Offset(selectedCoord.x, chartTop),
                    end = Offset(selectedCoord.x, chartBottom),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Highlighted dot
                drawCircle(
                    color = lineColor,
                    radius = 6.dp.toPx(),
                    center = selectedCoord
                )
                drawCircle(
                    color = White,
                    radius = 3.dp.toPx(),
                    center = selectedCoord
                )
            }
        }
    }
}
