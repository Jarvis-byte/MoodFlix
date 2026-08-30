package com.arka.moodflix.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arka.moodflix.R
import com.arka.moodflix.ui.theme.Amber300
import com.arka.moodflix.ui.theme.Amber400
import com.arka.moodflix.ui.theme.Amber700
import com.arka.moodflix.ui.theme.Cream100
import com.arka.moodflix.ui.theme.Ink600
import com.arka.moodflix.ui.theme.Ink700
import com.arka.moodflix.ui.theme.Ink800
import com.arka.moodflix.ui.theme.Ink900
import com.arka.moodflix.ui.theme.Muted
import com.arka.moodflix.ui.theme.Violet400
import com.arka.moodflix.ui.theme.Violet700

/* ---------------------------------------------------------------------------
 * Poster URLs  (TMDB w342)
 * ------------------------------------------------------------------------- */
private val Col1 = listOf(
    "https://image.tmdb.org/t/p/w342/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg",  // Inception
    "https://image.tmdb.org/t/p/w342/qJ2tW6WMUDux911r6m7haRef0WH.jpg",  // Dark Knight
    "https://image.tmdb.org/t/p/w342/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",  // Interstellar
    "https://image.tmdb.org/t/p/w342/d5NXSklXo0qyIYkgV94XAgMIckC.jpg",  // Dune
    "https://image.tmdb.org/t/p/w342/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg",  // Parasite
)
private val Col2 = listOf(
    "https://image.tmdb.org/t/p/w342/udDclJoHjfjb8Ekgsd4FDteOkCU.jpg",  // Joker
    "https://image.tmdb.org/t/p/w342/or06FN3Dka5tukK1e9sl16pB3iy.jpg",  // Endgame
    "https://image.tmdb.org/t/p/w342/1g0dhYtq4irTY1GPXvft6k4YLjm.jpg",  // Spider-Man NWH
    "https://image.tmdb.org/t/p/w342/cezWGskPY5x7GaglTTRN4Fugfb8.jpg",  // The Avengers
    "https://image.tmdb.org/t/p/w342/8tZYtuWezp8JbcsvHYO0O46tFbo.jpg",  // Mad Max
)
private val Col3 = listOf(
    "https://image.tmdb.org/t/p/w342/uDO8zWDhfWwoFdKS4fzkUJt0Rf0.jpg",  // La La Land
    "https://image.tmdb.org/t/p/w342/qdIMHd4sEfJSckfVJfKQvisL02a.jpg",  // The Revenant
    "https://image.tmdb.org/t/p/w342/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg",  // Inception
    "https://image.tmdb.org/t/p/w342/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg",  // Parasite
    "https://image.tmdb.org/t/p/w342/or06FN3Dka5tukK1e9sl16pB3iy.jpg",  // Endgame
)

/* ---------------------------------------------------------------------------
 * Placeholder gradients — use theme Ink + accent tints so a cache miss still
 * feels like MoodFlix, not a generic purple app.
 * ------------------------------------------------------------------------- */
private val Placeholders = listOf(
    Ink700 to Violet700,                        // deep ink → cinema violet
    Ink800 to Amber700,                         // dark ink → warm amber
    Ink700 to Violet400.copy(alpha = 0.55f),    // soft violet wash
    Ink700 to Ink600,                           // subtle neutral
    Ink800 to Amber700.copy(alpha = 0.70f),     // amber ember
)

/* ---------------------------------------------------------------------------
 * Screen
 * ------------------------------------------------------------------------- */

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state        by viewModel.uiState.collectAsStateWithLifecycle()
    val context      = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit)       { if (viewModel.isAlreadySignedIn) onLoggedIn() }
    LaunchedEffect(state.error){ state.error?.let { snackbarHost.showSnackbar(it) } }

    Scaffold(
        containerColor = Ink900,                    // darkest base from theme
        snackbarHost   = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            PosterMosaic(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp)
                    .align(Alignment.TopCenter)
            )

            BottomCard(
                isSigningIn   = state.isSigningIn,
                onSignIn      = { viewModel.signIn(context, onSuccess = onLoggedIn) },
                onEmailSignIn = { email, password ->
                    viewModel.signInWithEmail(email, password, onSuccess = onLoggedIn)
                },
                onEmailSignUp = { email, password ->
                    viewModel.signUpWithEmail(email, password, onSuccess = onLoggedIn)
                },
                modifier      = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/* ---------------------------------------------------------------------------
 * Poster mosaic — three staggered columns, tilted grid, scrim at the bottom
 * ------------------------------------------------------------------------- */

@Composable
private fun PosterMosaic(modifier: Modifier = Modifier) {
    Box(modifier.clipToBounds()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .graphicsLayer {
                    rotationZ       = -10f
                    scaleX          = 1.40f
                    scaleY          = 1.40f
                    transformOrigin = TransformOrigin(0.5f, 0.0f)
                }
                .offset(y = (-30).dp)
        ) {
            PosterColumn(urls = Col1, verticalOffsetDp = 0)
            PosterColumn(urls = Col2, verticalOffsetDp = 55)
            PosterColumn(urls = Col3, verticalOffsetDp = 20)
        }

        // Ink900 scrim: transparent at top, fully opaque at bottom.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.50f to Ink900.copy(alpha = 0.30f),
                        0.78f to Ink900.copy(alpha = 0.82f),
                        1.00f to Ink900
                    )
                )
        )
    }
}

@Composable
private fun PosterColumn(urls: List<String>, verticalOffsetDp: Int) {
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier            = Modifier.offset(y = verticalOffsetDp.dp)
    ) {
        urls.forEachIndexed { idx, url ->
            val (pTop, pBot) = Placeholders[idx % Placeholders.size]
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(162.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(pTop, pBot)))
            ) {
                AsyncImage(
                    model            = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(500)
                        .build(),
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/* ---------------------------------------------------------------------------
 * Bottom card
 * ------------------------------------------------------------------------- */

@Composable
private fun BottomCard(
    isSigningIn: Boolean,
    onSignIn: () -> Unit,
    onEmailSignIn: (email: String, password: String) -> Unit,
    onEmailSignUp: (email: String, password: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEmailForm by rememberSaveable { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            // Ink800 card sitting on Ink900 background — just one step lighter.
            .background(
                color = Ink800,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
            // Hairline amber border on the top edge to separate card from mosaic.
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        Amber400.copy(alpha = 0.40f),
                        Amber400.copy(alpha = 0.40f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
            .padding(start = 28.dp, end = 28.dp, top = 32.dp, bottom = 36.dp)
            // Lets the card's own content scroll into view above the keyboard,
            // instead of overflowing off-screen when the ime shrinks this Box.
            .verticalScroll(rememberScrollState())
    ) {
        // App name — Amber400 (primary) is the cinema-projector accent.
        Text(
            text  = "MoodFlix",
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 1.5.sp,
                fontWeight    = FontWeight.SemiBold
            ),
            color = Amber400
        )

        Spacer(Modifier.height(18.dp))

        // Headline — Cream100 (onBackground).
        Text(
            text  = stringResource(R.string.login_headline),
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight    = FontWeight.ExtraBold,
                lineHeight    = 42.sp,
                letterSpacing = (-0.5).sp
            ),
            color     = Cream100,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(14.dp))

        // Subtitle — Muted (onSurfaceVariant).
        Text(
            text       = stringResource(R.string.login_subtitle),
            style      = MaterialTheme.typography.bodyMedium,
            color      = Muted,
            textAlign  = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(32.dp))

        // Primary CTA.
        GoogleSignInButton(isSigningIn = isSigningIn, onClick = onSignIn)

        Spacer(Modifier.height(16.dp))

        if (showEmailForm) {
            EmailSignInForm(
                isSigningIn = isSigningIn,
                onSignIn    = onEmailSignIn,
                onSignUp    = onEmailSignUp
            )
        } else {
            TextButton(onClick = { showEmailForm = true }) {
                Text(
                    text  = stringResource(R.string.login_sign_in_with_email),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
            }
        }
    }
}

/* ---------------------------------------------------------------------------
 * Email / password sign-in form
 * ------------------------------------------------------------------------- */

@Composable
private fun EmailSignInForm(
    isSigningIn: Boolean,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (email: String, password: String) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isCreatingAccount by rememberSaveable { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor      = Cream100,
        unfocusedTextColor    = Cream100,
        focusedBorderColor    = Amber400,
        unfocusedBorderColor  = Ink600,
        cursorColor           = Amber400,
        focusedLabelColor     = Amber400,
        unfocusedLabelColor   = Muted
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value         = email,
            onValueChange = { email = it },
            label         = { Text(stringResource(R.string.login_email_label)) },
            singleLine    = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors        = fieldColors,
            modifier      = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value         = password,
            onValueChange = { password = it },
            label         = { Text(stringResource(R.string.login_password_label)) },
            singleLine    = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = stringResource(
                            if (passwordVisible) R.string.cd_hide_password else R.string.cd_show_password
                        ),
                        tint = Muted
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors        = fieldColors,
            modifier      = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick  = {
                if (isCreatingAccount) onSignUp(email, password) else onSignIn(email, password)
            },
            enabled  = !isSigningIn,
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Ink700),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (isSigningIn) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    color       = Cream100,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text  = stringResource(
                        if (isCreatingAccount) R.string.login_create_account else R.string.login_sign_in
                    ),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Cream100
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        TextButton(onClick = { isCreatingAccount = !isCreatingAccount }) {
            Text(
                text = stringResource(
                    if (isCreatingAccount) {
                        R.string.login_already_have_account
                    } else {
                        R.string.login_new_here
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
        }
    }
}

/* ---------------------------------------------------------------------------
 * Google Sign In button
 *
 * Gradient: Amber700 → Amber400 — warm projector-beam feel, on-brand.
 * Text + spinner: Ink900 (onPrimary from theme).
 * ------------------------------------------------------------------------- */

@Composable
private fun GoogleSignInButton(isSigningIn: Boolean, onClick: () -> Unit) {
    val source  = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()

    Button(
        onClick           = onClick,
        enabled           = !isSigningIn,
        interactionSource = source,
        shape             = RoundedCornerShape(16.dp),
        elevation         = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors            = ButtonDefaults.buttonColors(
            containerColor         = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer { alpha = if (pressed) 0.82f else 1f }
            // Amber700 → Amber400: dark warm amber to bright amber.
            .background(
                brush = Brush.horizontalGradient(listOf(Amber700, Amber400)),
                shape = RoundedCornerShape(16.dp)
            )
            // Top-catch highlight: very faint Amber300 line.
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Amber300.copy(alpha = 0.45f),
                        Amber700.copy(alpha = 0.10f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        if (isSigningIn) {
            CircularProgressIndicator(
                modifier    = Modifier.size(22.dp),
                color       = Ink900,                   // onPrimary
                strokeWidth = 2.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // White circle for the Google G — must stay white (Google brand).
                Box(
                    modifier         = Modifier
                        .size(28.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painterResource(com.arka.moodflix.R.drawable.google_logo),
                        null,
//                        Modifier.size(15.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text  = stringResource(R.string.login_continue_with_google),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp
                    ),
                    color = Ink900                      // onPrimary — dark on amber
                )
            }
        }
    }
}

