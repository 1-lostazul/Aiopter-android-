package io.alopter.companion.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate

@Composable
fun RotorLogo(modifier: Modifier = Modifier, spinning: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "rotor")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )
    Canvas(modifier) {
        val r = size.minDimension / 2
        rotate(if (spinning) angle else 0f) {
            repeat(3) { index ->
                rotate(index * 120f) {
                    translate(center.x, center.y) {
                        drawRoundRect(
                            Brush.linearGradient(
                                listOf(Color(0xFF55E7FF), Color(0xFF256BFF)),
                            ),
                            Offset(-r * .17f, -r * .78f),
                            Size(r * .34f, r * .72f),
                            androidx.compose.ui.geometry.CornerRadius(r * .2f),
                        )
                    }
                }
            }
        }
        drawCircle(Color(0xFFE9FCFF), r * .13f)
        drawCircle(Color(0xFF269DFF), r * .07f)
    }
}
