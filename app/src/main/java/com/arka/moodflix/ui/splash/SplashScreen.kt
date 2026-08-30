package com.arka.moodflix.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arka.moodflix.R
import com.arka.moodflix.ui.theme.Amber400
import com.arka.moodflix.ui.theme.Ink900
import com.arka.moodflix.ui.theme.Muted
import kotlinx.coroutines.delay

/**
 * Held just long enough for the entrance animation to read as intentional,
 * not for a real load - auth state is already known synchronously from
 * Firebase's cached [FirebaseAuth.currentUser], so this is a pure UI beat.
 */
private const val SPLASH_HOLD_MS = 1400L

@Composable
fun SplashScreen(
    onNavigateToDiscover: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        delay(SPLASH_HOLD_MS)
        if (viewModel.isLoggedIn) onNavigateToDiscover() else onNavigateToLogin()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val reelRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reelRotation"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(120)
        revealed = true
    }
    val textScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "textScale"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(650),
        label = "textAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink900),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .graphicsLayer {
                            scaleX = glowPulse
                            scaleY = glowPulse
                            alpha = 0.35f
                        }
                        .background(
                            brush = Brush.radialGradient(
                                listOf(Amber400, Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                Icon(
                    imageVector = Icons.Filled.Theaters,
                    contentDescription = null,
                    tint = Amber400,
                    modifier = Modifier
                        .size(84.dp)
                        .graphicsLayer { rotationZ = reelRotation }
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "MoodFlix",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    letterSpacing = 0.5.sp
                ),
                color = Amber400,
                modifier = Modifier.graphicsLayer {
                    scaleX = textScale
                    scaleY = textScale
                    alpha = textAlpha
                }
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                modifier = Modifier.graphicsLayer { alpha = textAlpha }
            )
        }
    }
}
