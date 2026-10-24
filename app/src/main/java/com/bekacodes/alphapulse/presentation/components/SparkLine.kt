package com.bekacodes.alphapulse.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bekacodes.alphapulse.ui.theme.Negative
import com.bekacodes.alphapulse.ui.theme.Positive

@Composable
fun Sparkline(points: List<Float>, rising: Boolean, modifier: Modifier = Modifier) {
    val path = remember { Path() }
    val direction = if (rising) "Upward" else "Downward"

    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Price sparkline trend ($direction)"
        }
    ) {
        if (points.size < 2) return@Canvas
        val min = points.min()
        val max = points.max()
        val range = (max - min).let { if (it == 0f) 1f else it }

        path.reset()
        points.forEachIndexed { i, value ->
            val point = Offset(
                x = i * size.width / points.lastIndex,
                y = size.height - ((value - min) / range) * size.height
            )
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(
            path = path,
            color = if (rising) Positive else Negative,
            style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}