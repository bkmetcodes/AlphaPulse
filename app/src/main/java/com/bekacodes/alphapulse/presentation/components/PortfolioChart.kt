package com.bekacodes.alphapulse.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bekacodes.alphapulse.presentation.formatCurrency
import com.bekacodes.alphapulse.presentation.models.ChartPoint
import com.bekacodes.alphapulse.ui.theme.Border
import com.bekacodes.alphapulse.ui.theme.Negative
import com.bekacodes.alphapulse.ui.theme.Positive
import com.bekacodes.alphapulse.ui.theme.SurfaceColor
import com.bekacodes.alphapulse.ui.theme.TextPrimary
import com.bekacodes.alphapulse.ui.theme.TextSecondary
import kotlin.math.roundToInt

/**
 * Interactive Google Finance-style Area Chart with horizontal drag/tap scrubbing,
 * zero-allocation cached Path objects across draw passes, and haptic feedback ticks.
 */
@Composable
fun PortfolioChart(
    points: List<ChartPoint>,
    previousClose: Double,
    scrubbedPoint: ChartPoint?,
    isPositive: Boolean,
    onScrub: (ChartPoint?) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val activeColor = if (isPositive) Positive else Negative
    val gradientColor = if (isPositive) Color(0x6610B981) else Color(0x66F43F5E)

    // Reusable cached Path objects to prevent GC allocation churn on continuous draw passes
    val cachedLinePath = remember { Path() }
    val cachedAreaPath = remember { Path() }

    Box(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "Interactive portfolio chart. Drag across chart to scrub balance values."
                }
                .pointerInput(points) {
                    if (points.size < 2) return@pointerInput
                    detectTapGestures(
                        onPress = { offset ->
                            val idx = ((offset.x / size.width) * points.lastIndex)
                                .roundToInt()
                                .coerceIn(0, points.lastIndex)
                            onScrub(points[idx])
                            tryAwaitRelease()
                            onScrub(null)
                        }
                    )
                }
                .pointerInput(points) {
                    if (points.size < 2) return@pointerInput
                    var lastIndex = -1
                    detectHorizontalDragGestures(
                        onDragEnd = { onScrub(null) },
                        onDragCancel = { onScrub(null) },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val idx = ((change.position.x / size.width) * points.lastIndex)
                                .roundToInt()
                                .coerceIn(0, points.lastIndex)
                            if (idx != lastIndex) {
                                lastIndex = idx
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onScrub(points[idx])
                            }
                        }
                    )
                }
        ) {
            val chartTop = 36.dp.toPx()
            val chartBottom = size.height - 20.dp.toPx()
            val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)

            // Horizontal reference lines
            repeat(4) { i ->
                val y = chartTop + chartHeight * (i + 1) / 4f
                drawLine(Border, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }

            if (points.size < 2) return@Canvas

            val rawMin = points.minOf { it.value }.coerceAtMost(previousClose)
            val rawMax = points.maxOf { it.value }.coerceAtLeast(previousClose)
            val range = (rawMax - rawMin).let { if (it == 0.0) 1.0 else it }

            fun valueToY(v: Double): Float {
                val normalized = ((v - rawMin) / range).toFloat()
                return chartBottom - (normalized * chartHeight)
            }

            cachedLinePath.reset()
            points.forEachIndexed { index, pt ->
                val x = index * size.width / points.lastIndex
                val y = valueToY(pt.value)
                if (index == 0) cachedLinePath.moveTo(x, y) else cachedLinePath.lineTo(x, y)
            }

            cachedAreaPath.reset()
            cachedAreaPath.addPath(cachedLinePath)
            cachedAreaPath.lineTo(size.width, chartBottom)
            cachedAreaPath.lineTo(0f, chartBottom)
            cachedAreaPath.close()

            drawPath(
                path = cachedAreaPath,
                brush = Brush.verticalGradient(
                    listOf(gradientColor, Color.Transparent),
                    chartTop,
                    chartBottom
                )
            )
            drawPath(
                path = cachedLinePath,
                color = activeColor,
                style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Dashed "Prev Close" baseline
            val closeY = valueToY(previousClose).coerceIn(chartTop, chartBottom)
            drawLine(
                color = TextSecondary,
                start = Offset(0f, closeY),
                end = Offset(size.width, closeY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )

            // Active Scrub Crosshair & Glowing Dot on the exact curve point
            val targetPoint = scrubbedPoint ?: points.last()
            val targetIndex = points.indexOf(targetPoint).coerceAtLeast(0)
            val cursorX = targetIndex * size.width / points.lastIndex
            val cursorY = valueToY(targetPoint.value)

            drawLine(
                color = TextPrimary.copy(alpha = 0.75f),
                start = Offset(cursorX, chartTop),
                end = Offset(cursorX, chartBottom),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
            drawCircle(SurfaceColor, 8.dp.toPx(), Offset(cursorX, cursorY))
            drawCircle(activeColor, 4.5.dp.toPx(), Offset(cursorX, cursorY))
            drawCircle(TextPrimary, 2.dp.toPx(), Offset(cursorX, cursorY))
        }

        // Top-right floating scrub tooltip
        val activePoint = scrubbedPoint ?: points.lastOrNull()
        if (activePoint != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .height(28.dp),
                shape = RoundedCornerShape(8.dp),
                color = SurfaceColor,
                border = BorderStroke(1.dp, Border)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Text(
                        text = "${formatCurrency(activePoint.value)} · ${activePoint.timestampLabel}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Text(
            text = "Prev Close",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 26.dp, end = 8.dp),
            color = TextSecondary,
            fontSize = 10.sp
        )
        Text(
            text = points.firstOrNull()?.timestampLabel ?: "9:30 AM",
            modifier = Modifier.align(Alignment.BottomStart),
            color = TextSecondary,
            fontSize = 10.sp
        )
        Text(
            text = points.lastOrNull()?.timestampLabel ?: "4:00 PM",
            modifier = Modifier.align(Alignment.BottomEnd),
            color = TextSecondary,
            fontSize = 10.sp
        )
    }
}