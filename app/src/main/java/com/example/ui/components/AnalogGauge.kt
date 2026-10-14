package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogGauge(
    title: String,
    value: Float,
    maxValue: Float,
    modifier: Modifier = Modifier,
    size: Dp = 90.dp,
    unit: String = "",
    isWarning: Boolean = false,
    warningThreshold: Float = 0.8f
) {
    val clampedRatio = (value / maxValue).coerceIn(0f, 1f)
    // Angle span from -135 degrees to +135 degrees (270 degrees total)
    val startAngle = -135f
    val sweepTotal = 270f
    val needleAngle = startAngle + clampedRatio * sweepTotal

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.width / 2f - 4f

        // Outer Metallic Bezel
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFE2E8F0), Color(0xFF64748B), Color(0xFF1E293B)),
                center = center,
                radius = radius + 4f
            ),
            radius = radius + 4f,
            center = center
        )

        // Inner Bezel Border
        drawCircle(
            color = Color(0xFF0F172A),
            radius = radius - 2f,
            center = center
        )

        // Gauge Face Background
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1E293B), Color(0xFF0B132B)),
                center = center,
                radius = radius - 6f
            ),
            radius = radius - 6f,
            center = center
        )

        // Warning Zone Arc (e.g. Redline RPM or Max Boost)
        val warningStartAngle = startAngle + warningThreshold * sweepTotal
        val warningSweep = (1f - warningThreshold) * sweepTotal
        drawArc(
            color = Color(0xFFEF4444).copy(alpha = 0.75f),
            startAngle = warningStartAngle + 90f,
            sweepAngle = warningSweep,
            useCenter = false,
            topLeft = Offset(center.x - radius + 12f, center.y - radius + 12f),
            size = androidx.compose.ui.geometry.Size((radius - 12f) * 2f, (radius - 12f) * 2f),
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Main Scale Arc
        drawArc(
            color = Color(0xFF94A3B8).copy(alpha = 0.4f),
            startAngle = startAngle + 90f,
            sweepAngle = sweepTotal,
            useCenter = false,
            topLeft = Offset(center.x - radius + 12f, center.y - radius + 12f),
            size = androidx.compose.ui.geometry.Size((radius - 12f) * 2f, (radius - 12f) * 2f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Tick marks
        val tickCount = 9
        for (i in 0 until tickCount) {
            val tickRatio = i.toFloat() / (tickCount - 1)
            val tickAngleDeg = startAngle + tickRatio * sweepTotal
            val tickAngleRad = (tickAngleDeg + 90f) * (PI.toFloat() / 180f)

            val tickOuter = radius - 10f
            val tickInner = if (i % 2 == 0) radius - 18f else radius - 14f

            val p1 = Offset(
                center.x + cos(tickAngleRad) * tickInner,
                center.y + sin(tickAngleRad) * tickInner
            )
            val p2 = Offset(
                center.x + cos(tickAngleRad) * tickOuter,
                center.y + sin(tickAngleRad) * tickOuter
            )

            val tickColor = if (tickRatio >= warningThreshold) Color(0xFFEF4444) else Color(0xFFCBD5E1)
            drawLine(
                color = tickColor,
                start = p1,
                end = p2,
                strokeWidth = if (i % 2 == 0) 2.5f else 1.5f
            )
        }

        // Needle (drawn pointing upward at angle 0, then rotated)
        rotate(degrees = needleAngle, pivot = center) {
            val needlePath = Path().apply {
                moveTo(center.x - 3.5f, center.y + 10f)
                lineTo(center.x + 3.5f, center.y + 10f)
                lineTo(center.x + 0.8f, center.y - radius + 14f)
                lineTo(center.x - 0.8f, center.y - radius + 14f)
                close()
            }
            drawPath(
                path = needlePath,
                color = if (isWarning || clampedRatio >= warningThreshold) Color(0xFFFF3D00) else Color(0xFFEF4444)
            )
        }

        // Center Cap (chrome hub)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF1F5F9), Color(0xFF475569)),
                center = center,
                radius = 8f
            ),
            radius = 8f,
            center = center
        )

        // Labels using native canvas
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 22f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText(
            title,
            center.x,
            center.y + radius * 0.52f,
            paint
        )

        // Value readout below
        paint.textSize = 18f
        paint.color = android.graphics.Color.parseColor("#94A3B8")
        drawContext.canvas.nativeCanvas.drawText(
            "${value.toInt()}$unit",
            center.x,
            center.y + radius * 0.78f,
            paint
        )
    }
}
