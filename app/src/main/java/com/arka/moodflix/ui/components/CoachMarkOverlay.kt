package com.arka.moodflix.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

data class CoachStep(
    val title: String,
    val body: String,
    val targetBounds: Rect?,
    val spotlightRadius: Dp = 40.dp,
)

@Composable
fun CoachMarkOverlay(
    steps: List<CoachStep>,
    currentStep: Int,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    if (currentStep >= steps.size) return

    val step = steps[currentStep]
    val isLast = currentStep == steps.size - 1

    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(400),
        label = "coachAlpha"
    )

    val density = LocalDensity.current
    val spotlightPx = with(density) { step.spotlightRadius.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {}
            )
    ) {
        // Dark scrim with spotlight cutout
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                canvas.drawRect(
                    left = 0f, top = 0f,
                    right = size.width, bottom = size.height,
                    paint = Paint().apply { color = Color.Black.copy(alpha = 0.75f) }
                )

                step.targetBounds?.let { bounds ->
                    canvas.drawCircle(
                        center = Offset(bounds.center.x, bounds.center.y),
                        radius = spotlightPx,
                        paint = Paint().apply {
                            color = Color.Transparent
                            blendMode = BlendMode.Clear
                        }
                    )
                }
            }
        }

        // Card positioned below the spotlight
        val cardTopPx = step.targetBounds?.let { bounds ->
            val below = bounds.bottom + spotlightPx + 24f
            val screenHeight = with(density) { 800.dp.toPx() }
            if (below + with(density) { 160.dp.toPx() } > screenHeight) {
                bounds.top - spotlightPx - with(density) { 180.dp.toPx() }
            } else {
                below
            }
        } ?: with(density) { 200.dp.toPx() }

        Popup(
            offset = IntOffset(
                x = 24,
                y = cardTopPx.toInt()
            )
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = step.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = if (isLast) onFinish else onNext,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (isLast) "Got it!" else "Next →",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

fun Modifier.coachTarget(onBounds: (Rect) -> Unit): Modifier =
    this.onGloballyPositioned { coords: LayoutCoordinates ->
        onBounds(coords.boundsInWindow())
    }