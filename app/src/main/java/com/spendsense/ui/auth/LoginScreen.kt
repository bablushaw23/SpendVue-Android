package com.spendsense.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.ui.theme.PrimaryIndigo
import com.spendsense.ui.theme.SecondaryEmerald

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
            viewModel.resetState()
        }
        if (uiState is LoginUiState.Error) {
            snackbarHostState.showSnackbar((uiState as LoginUiState.Error).message)
            viewModel.resetState()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(72.dp))

                    // ── Logo area ─────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(PrimaryIndigo, SecondaryEmerald)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💰", fontSize = 38.sp)
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = buildAnnotatedString {
                            append("Spend")
                            withStyle(SpanStyle(color = SecondaryEmerald)) { append("Sense") }
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Track 11 accounts. Stay in budget.\nYour data never leaves your phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(Modifier.height(48.dp))

                    // ── Form card ─────────────────────────────────────────────
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Get Started",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Your Name") },
                                placeholder = { Text("e.g. Raj Kumar") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                placeholder = { Text("you@example.com") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(Modifier.height(4.dp))

                            Button(
                                onClick = { viewModel.loginDummy(displayName.trim(), email.trim()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                enabled = uiState !is LoginUiState.Loading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryIndigo
                                )
                            ) {
                                if (uiState is LoginUiState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        "Continue  →",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Privacy note ──────────────────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🔒 ", fontSize = 14.sp)
                        Text(
                            "All financial data stored on your device only",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}
