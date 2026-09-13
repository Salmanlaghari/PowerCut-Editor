package com.powercut.editor.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.ui.theme.PremiumGold
import kotlin.math.roundToInt

@Composable
fun PowerCutSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = PremiumGold,
    steps: Int = 0,
    valueFormatter: (Float) -> String = { v -> "${(v * 100).roundToInt()}%" }
) {
    val rangeStart = valueRange.start
    val rangeEnd = valueRange.endInclusive
    val range = rangeEnd - rangeStart
    val coercedValue = value.coerceIn(rangeStart, rangeEnd)
    val fraction = if (range > 0f) (coercedValue - rangeStart) / range else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            androidx.compose.material3.Text(
                text = valueFormatter(coercedValue),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
        val density = LocalDensity.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val dx = dragAmount.x
                                val trackWidth = size.width - with(density) { 36.dp.toPx() }
                                val newFraction = (fraction + dx / trackWidth).coerceIn(0f, 1f)
                                val newValue = rangeStart + newFraction * range
                                onValueChange(newValue)
                            }
                        )
                    }
            ) {
                val trackWidth = size.width - with(density) { 36.dp.toPx() }
                val trackY = size.height / 2f
                val thumbRadius = with(density) { 9.dp.toPx() }
                val thumbX = with(density) { 18.dp.toPx() } + fraction * trackWidth
                val activeWidth = fraction * trackWidth

                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(with(density) { 18.dp.toPx() }, trackY),
                    end = Offset(with(density) { 18.dp.toPx() } + trackWidth, trackY),
                    strokeWidth = with(density) { 4.dp.toPx() },
                    cap = StrokeCap.Round
                )
                if (activeWidth > 0f) {
                    drawLine(
                        color = accentColor,
                        start = Offset(with(density) { 18.dp.toPx() }, trackY),
                        end = Offset(with(density) { 18.dp.toPx() } + activeWidth, trackY),
                        strokeWidth = with(density) { 4.dp.toPx() },
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(
                    color = accentColor,
                    radius = thumbRadius,
                    center = Offset(thumbX, trackY)
                )
            }
        }
    }
}
