package com.example.ui.components

import android.view.MotionEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class PedalType {
    BRAKE,
    GAS
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PedalButton(
    type: PedalType,
    isPressedExternal: Boolean = false,
    onPressChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = if (type == PedalType.BRAKE) 100.dp else 90.dp,
    height: Dp = 120.dp
) {
    var isTouching by remember { mutableStateOf(false) }
    val effectivePressed = isTouching || isPressedExternal

    val pressScale by animateFloatAsState(
        targetValue = if (effectivePressed) 0.92f else 1.0f,
        animationSpec = tween(durationMillis = 60),
        label = "pedal_scale"
    )

    val pedalElevation by animateFloatAsState(
        targetValue = if (effectivePressed) 2f else 8f,
        animationSpec = tween(durationMillis = 60),
        label = "pedal_elev"
    )

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .testTag(if (type == PedalType.BRAKE) "brake_pedal" else "gas_pedal")
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val isDown = event.changes.any { it.pressed }
                        if (isDown != isTouching) {
                            isTouching = isDown
                            onPressChanged(isDown)
                        }
                    }
                }
            }
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                translationY = if (effectivePressed) 6f else 0f
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Hydraulic Stem / Mounting Arm at the bottom
            val stemWidth = 16.dp.toPx()
            val stemHeight = 24.dp.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF334155), Color(0xFF64748B), Color(0xFF1E293B)),
                    startX = (w - stemWidth) / 2f,
                    endX = (w + stemWidth) / 2f
                ),
                topLeft = Offset((w - stemWidth) / 2f, h - stemHeight),
                size = androidx.compose.ui.geometry.Size(stemWidth, stemHeight)
            )

            // 2. Drop Shadow of Pedal Plate
            val plateMargin = 6.dp.toPx()
            val plateW = w - plateMargin * 2f
            val plateH = h - stemHeight - 4.dp.toPx()
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(plateMargin + 2f, plateMargin + pedalElevation),
                size = androidx.compose.ui.geometry.Size(plateW, plateH),
                cornerRadius = CornerRadius(14f, 14f)
            )

            // 3. Pedal Base Plate (Metallic Gradient)
            val plateGradient = if (effectivePressed) {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF475569), Color(0xFF334155), Color(0xFF1E293B))
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF94A3B8), Color(0xFF64748B), Color(0xFF334155))
                )
            }
            drawRoundRect(
                brush = plateGradient,
                topLeft = Offset(plateMargin, plateMargin),
                size = androidx.compose.ui.geometry.Size(plateW, plateH),
                cornerRadius = CornerRadius(14f, 14f)
            )

            // 4. Pedal Outer Chrome Rim
            drawRoundRect(
                color = if (effectivePressed) Color(0xFFCBD5E1) else Color(0xFFF1F5F9),
                topLeft = Offset(plateMargin, plateMargin),
                size = androidx.compose.ui.geometry.Size(plateW, plateH),
                cornerRadius = CornerRadius(14f, 14f),
                style = Stroke(width = 3.dp.toPx())
            )

            // 5. Texture & Tread Pattern
            if (type == PedalType.BRAKE) {
                // Ribbed horizontal tread grips (classic brake pattern)
                val ribCount = 5
                val ribH = 6.dp.toPx()
                val ribSpacing = (plateH - 45.dp.toPx()) / ribCount
                for (i in 0 until ribCount) {
                    val ry = plateMargin + 16.dp.toPx() + i * ribSpacing
                    // Shadow groove
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(plateMargin + 10.dp.toPx(), ry),
                        size = androidx.compose.ui.geometry.Size(plateW - 20.dp.toPx(), ribH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    // High-grip rubber inset
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(plateMargin + 11.dp.toPx(), ry + 1.5f),
                        size = androidx.compose.ui.geometry.Size(plateW - 22.dp.toPx(), ribH - 2f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            } else {
                // Perforated circular grip holes (sport gas pedal pattern: 2 columns of 4 holes)
                val rows = 4
                val holeRadius = 6.dp.toPx()
                val colOffset = plateW * 0.28f
                val rowSpacing = (plateH - 42.dp.toPx()) / rows

                for (row in 0 until rows) {
                    val cy = plateMargin + 16.dp.toPx() + row * rowSpacing
                    // Left hole
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = holeRadius,
                        center = Offset(w / 2f - colOffset, cy)
                    )
                    drawCircle(
                        color = Color(0xFF334155),
                        radius = holeRadius - 1.5f,
                        center = Offset(w / 2f - colOffset, cy)
                    )

                    // Right hole
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = holeRadius,
                        center = Offset(w / 2f + colOffset, cy)
                    )
                    drawCircle(
                        color = Color(0xFF334155),
                        radius = holeRadius - 1.5f,
                        center = Offset(w / 2f + colOffset, cy)
                    )
                }
            }

            // 6. Pedal Label Text
            val labelText = if (type == PedalType.BRAKE) "BRAKE" else "GAS"
            val paint = android.graphics.Paint().apply {
                color = if (effectivePressed) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#E2E8F0")
                textSize = 28f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                isAntiAlias = true
                setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
            }

            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                w / 2f,
                plateMargin + plateH - 12.dp.toPx(),
                paint
            )
        }
    }
}
