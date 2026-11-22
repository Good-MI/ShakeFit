package com.example.shakefit.ui.screens.repsession

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shakefit.R
import com.example.shakefit.model.BadgeUi
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.RepSessionEvent
import com.example.shakefit.model.RepSessionUiState
import com.example.shakefit.model.SessionMode
import com.example.shakefit.model.SessionPhase
import com.example.shakefit.model.SessionSummary
import com.example.shakefit.model.UiError
import com.example.shakefit.ui.components.SessionSummaryContent
import com.example.shakefit.ui.theme.ShakeFitTheme
import com.example.shakefit.ui.theme.getExerciseAccentColor
import com.example.shakefit.util.formatDuration

/**
 * RepSessionRoute: Wrapper tipis sesuai DesignShakefit.md dan ui-contract.md.
 * Meng-hoist RepSessionViewModel dan mengalirkan navigasi.
 */
@Composable
fun RepSessionRoute(
    viewModel: RepSessionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RepSessionScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

/**
 * RepSessionScreen: 100% Stateless Composable untuk Sesi Latihan Repetisi (Squat & Push-up).
 * Mengelola transisi visual fase IDLE, COUNTDOWN, ACTIVE, PAUSED, dan FINISHED.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepSessionScreen(
    state: RepSessionUiState,
    onEvent: (RepSessionEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val accentColor = getExerciseAccentColor(state.exercise, isDark)

    // Jaga layar tetap menyala saat COUNTDOWN atau ACTIVE (PRD F-03 / DesignShakefit.md Bagian 6.3)
    val currentView = LocalView.current
    DisposableEffect(state.phase) {
        val keepOn = state.phase == SessionPhase.COUNTDOWN || state.phase == SessionPhase.ACTIVE
        currentView.keepScreenOn = keepOn
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    // Jika fase FINISHED dan summary tersedia, tampilkan SessionSummaryContent penuh
    if (state.phase == SessionPhase.FINISHED && state.summary != null) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            modifier = modifier.fillMaxSize()
        ) { padding ->
            SessionSummaryContent(
                summary = state.summary,
                onDismiss = {
                    onEvent(RepSessionEvent.DismissSummary)
                    onNavigateBack()
                },
                modifier = Modifier.padding(padding)
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.exercise.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )

                        // Phase Badge
                        val phaseBadgeText = when (state.phase) {
                            SessionPhase.IDLE -> "PERSIAPAN"
                            SessionPhase.COUNTDOWN -> "BERSIAP"
                            SessionPhase.ACTIVE -> "AKTIF"
                            SessionPhase.PAUSED -> stringResource(R.string.paused_pill)
                            SessionPhase.FINISHED -> "SELESAI"
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (state.phase == SessionPhase.PAUSED) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = phaseBadgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.phase == SessionPhase.PAUSED) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (state.phase == SessionPhase.IDLE || state.phase == SessionPhase.PAUSED) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Bagian Atas: PositionHintBanner (Expandable, Teks Terkunci Sesuai Bagian 6.1 & 9)
            PositionHintBanner(
                positionHint = state.positionHint,
                isPhaseIdle = state.phase == SessionPhase.IDLE
            )

            // Bagian Tengah: Tampilan Visual Sesuai Fase Sesi
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (state.phase) {
                    SessionPhase.IDLE -> {
                        IdlePhaseContent(
                            mode = state.mode,
                            targetReps = state.targetReps,
                            accentColor = accentColor,
                            onStartSession = { selectedMode ->
                                onEvent(RepSessionEvent.Start(state.exercise, selectedMode))
                            }
                        )
                    }
                    SessionPhase.COUNTDOWN -> {
                        CountdownPhaseContent(
                            countdownRemaining = state.countdownRemaining,
                            accentColor = accentColor,
                            onCancel = { onEvent(RepSessionEvent.Cancel) }
                        )
                    }
                    SessionPhase.ACTIVE, SessionPhase.PAUSED -> {
                        ActivePhaseContent(
                            state = state,
                            accentColor = accentColor,
                            onEvent = onEvent
                        )
                    }
                    SessionPhase.FINISHED -> {
                        // Di-handle di scaffold awal
                    }
                }
            }

            // Banner Error jika ada
            state.error?.let { err ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = err.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact Expandable Banner untuk Posisi HP Terkunci (DesignShakefit.md Bagian 6.1 & ui-contract.md Bagian 9).
 */
@Composable
private fun PositionHintBanner(
    positionHint: String,
    isPhaseIdle: Boolean,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember(isPhaseIdle) { mutableStateOf(isPhaseIdle) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.pos_hint_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = if (isExpanded) stringResource(R.string.pos_hint_collapse) else stringResource(R.string.pos_hint_expand),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Teks terkunci ditampilkan utuh tanpa pemotongan
            if (isExpanded) {
                Text(
                    text = positionHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = positionHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Tampilan Fase IDLE (Pemilihan mode latihan & tombol Mulai).
 */
@Composable
private fun IdlePhaseContent(
    mode: SessionMode,
    targetReps: Int?,
    accentColor: Color,
    onStartSession: (SessionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(mode) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.mode_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Pilihan Mode Latihan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            // Mode Bebas
            FilterChip(
                selected = selectedMode == SessionMode.FREE,
                onClick = { selectedMode = SessionMode.FREE },
                label = { Text(text = stringResource(R.string.mode_free)) },
                modifier = Modifier.padding(horizontal = 4.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor.copy(alpha = 0.2f),
                    selectedLabelColor = accentColor
                )
            )

            // Mode Tantangan 60 Detik
            FilterChip(
                selected = selectedMode == SessionMode.CHALLENGE_60S,
                onClick = { selectedMode = SessionMode.CHALLENGE_60S },
                label = { Text(text = stringResource(R.string.mode_challenge_60s)) },
                modifier = Modifier.padding(horizontal = 4.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor.copy(alpha = 0.2f),
                    selectedLabelColor = accentColor
                )
            )

            // Mode Target (jika targetReps disetel)
            if (targetReps != null && targetReps > 0) {
                FilterChip(
                    selected = selectedMode == SessionMode.GOAL,
                    onClick = { selectedMode = SessionMode.GOAL },
                    label = { Text(text = "${stringResource(R.string.mode_goal)} ($targetReps)") },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.2f),
                        selectedLabelColor = accentColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tombol CTA Besar "Mulai Latihan"
        Button(
            onClick = { onStartSession(selectedMode) },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.btn_start_workout),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Tampilan Fase COUNTDOWN (Hitung mundur tegas di tengah).
 */
@Composable
private fun CountdownPhaseContent(
    countdownRemaining: Int,
    accentColor: Color,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$countdownRemaining",
                style = MaterialTheme.typography.displayLarge,
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
        }

        Text(
            text = stringResource(R.string.countdown_get_ready),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(50),
            modifier = Modifier.width(160.dp)
        ) {
            Text(text = stringResource(R.string.btn_cancel))
        }
    }
}

/**
 * Tampilan Fase ACTIVE & PAUSED: VisualProgressRing, metrik sekunder, dan kontrol sesi.
 */
@Composable
private fun ActivePhaseContent(
    state: RepSessionUiState,
    accentColor: Color,
    onEvent: (RepSessionEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPaused = state.phase == SessionPhase.PAUSED

    // Hitung fraksi ring progress
    val targetFraction: Float = when (state.mode) {
        SessionMode.GOAL -> {
            val target = state.targetReps ?: 1
            if (target > 0) (state.reps.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 1f
        }
        SessionMode.CHALLENGE_60S -> {
            val rem = state.remainingSec ?: 60
            (rem.toFloat() / 60f).coerceIn(0f, 1f)
        }
        SessionMode.FREE -> 1f
    }
    val animatedFraction by animateFloatAsState(targetValue = targetFraction, label = "RingProgress")

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // VisualProgressRing (Canvas Compose)
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            val activeColor = if (state.targetReached) MaterialTheme.colorScheme.tertiary else accentColor

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 16.dp.toPx()
                // Track belakang
                drawCircle(
                    color = trackColor,
                    style = Stroke(width = strokeWidth)
                )
                // Progress Arc
                drawArc(
                    color = activeColor,
                    startAngle = -90f,
                    sweepAngle = animatedFraction * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Konten Tengah Cincin
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${state.reps}",
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (state.targetReached) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground
                )

                val subLabel = state.targetReps?.let {
                    stringResource(R.string.target_reps_format, it)
                } ?: stringResource(R.string.reps_label)

                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Chip Selebrasi Target Tercapai
                if (state.targetReached) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.target_reached_pill),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // Chip Status Dijeda
                if (isPaused) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = stringResource(R.string.paused_pill),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Bar Metrik Sekunder (Durasi / Sisa Waktu & Kualitas)
        OutlinedCard(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Metrik 1: Durasi / Sisa Waktu
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val is60s = state.mode == SessionMode.CHALLENGE_60S
                    val durLabel = if (is60s) stringResource(R.string.metric_remaining_time) else stringResource(R.string.metric_duration)
                    val durValue = if (is60s) formatDuration(state.remainingSec ?: 0) else formatDuration(state.elapsedSec)

                    Text(
                        text = durLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = durValue,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Metrik 2: Skor Kualitas Gerakan (F-12a)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.metric_quality),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val qualityText = state.liveQualityScore?.let { "$it%" } ?: "--"
                    val qualityColor = when {
                        state.liveQualityScore == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        state.liveQualityScore >= 80 -> MaterialTheme.colorScheme.primary
                        state.liveQualityScore >= 60 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }

                    Text(
                        text = qualityText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = qualityColor
                    )
                }
            }
        }

        // Kontrol Tombol Sesi Latihan
        if (state.phase == SessionPhase.ACTIVE) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onEvent(RepSessionEvent.Pause) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_pause),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { onEvent(RepSessionEvent.Finish) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_finish),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (isPaused) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onEvent(RepSessionEvent.Resume) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_resume),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onEvent(RepSessionEvent.Cancel) },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(text = stringResource(R.string.btn_cancel))
                    }

                    Button(
                        onClick = { onEvent(RepSessionEvent.Finish) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(text = stringResource(R.string.btn_finish))
                    }
                }
            }
        }
    }
}

// =========================================================================
// @Preview Lengkap (Data Dummy Khusus di Fungsi Preview)
// =========================================================================

@Preview(name = "Phase IDLE - Light", showBackground = true)
@Composable
private fun PreviewRepSessionIdle() {
    ShakeFitTheme {
        RepSessionScreen(
            state = RepSessionUiState(
                exercise = ExerciseType.SQUAT,
                phase = SessionPhase.IDLE,
                mode = SessionMode.FREE,
                countdownRemaining = 5,
                reps = 0,
                elapsedSec = 0,
                remainingSec = null,
                lastRepIntervalMs = null,
                liveQualityScore = null,
                targetReps = 30,
                targetReached = false,
                positionHint = "Saku depan celana, HP tegak, layar menghadap badan. Tidak dipegang",
                summary = null,
                error = null
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Phase COUNTDOWN", showBackground = true)
@Composable
private fun PreviewRepSessionCountdown() {
    ShakeFitTheme {
        RepSessionScreen(
            state = RepSessionUiState(
                exercise = ExerciseType.SQUAT,
                phase = SessionPhase.COUNTDOWN,
                mode = SessionMode.FREE,
                countdownRemaining = 3,
                reps = 0,
                elapsedSec = 0,
                remainingSec = null,
                lastRepIntervalMs = null,
                liveQualityScore = null,
                targetReps = null,
                targetReached = false,
                positionHint = "Saku depan celana, HP tegak, layar menghadap badan. Tidak dipegang",
                summary = null,
                error = null
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Phase ACTIVE - Pushup", showBackground = true)
@Composable
private fun PreviewRepSessionActive() {
    ShakeFitTheme {
        RepSessionScreen(
            state = RepSessionUiState(
                exercise = ExerciseType.PUSHUP,
                phase = SessionPhase.ACTIVE,
                mode = SessionMode.FREE,
                countdownRemaining = 0,
                reps = 24,
                elapsedSec = 45,
                remainingSec = null,
                lastRepIntervalMs = 1200L,
                liveQualityScore = 92,
                targetReps = null,
                targetReached = false,
                positionHint = "HP rata di lantai, layar menghadap atas, bagian atas HP tepat di bawah dada",
                summary = null,
                error = null
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Phase ACTIVE - Target Reached", showBackground = true)
@Composable
private fun PreviewRepSessionTargetReached() {
    ShakeFitTheme {
        RepSessionScreen(
            state = RepSessionUiState(
                exercise = ExerciseType.SQUAT,
                phase = SessionPhase.ACTIVE,
                mode = SessionMode.GOAL,
                countdownRemaining = 0,
                reps = 30,
                elapsedSec = 75,
                remainingSec = null,
                lastRepIntervalMs = 1100L,
                liveQualityScore = 88,
                targetReps = 30,
                targetReached = true,
                positionHint = "Saku depan celana, HP tegak, layar menghadap badan. Tidak dipegang",
                summary = null,
                error = null
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Phase PAUSED", showBackground = true)
@Composable
private fun PreviewRepSessionPaused() {
    ShakeFitTheme {
        RepSessionScreen(
            state = RepSessionUiState(
                exercise = ExerciseType.SQUAT,
                phase = SessionPhase.PAUSED,
                mode = SessionMode.FREE,
                countdownRemaining = 0,
                reps = 18,
                elapsedSec = 60,
                remainingSec = null,
                lastRepIntervalMs = 1350L,
                liveQualityScore = 78,
                targetReps = null,
                targetReached = false,
                positionHint = "Saku depan celana, HP tegak, layar menghadap badan. Tidak dipegang",
                summary = null,
                error = null
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Phase FINISHED - New Record & Badges", showBackground = true)
@Composable
private fun PreviewRepSessionFinished() {
    val dummySummary = SessionSummary(
        sessionId = 1L,
        exercise = ExerciseType.SQUAT,
        mode = SessionMode.FREE,
        startedAtEpochMs = System.currentTimeMillis() - 120000L,
        durationSec = 120,
        reps = 45,
        avgTempoMs = 1250,
        qualityScore = 90,
        distanceMeters = null,
        avgPaceSecPerKm = null,
        bestPaceSecPerKm = null,
        avgCadenceSpm = null,
        distanceSource = null,
        splits = emptyList(),
        caloriesEst = 14.4f,
        note = "Latihan squat sore hari",
        newBadges = listOf(
            BadgeUi("FIRST_SESSION", "Langkah Pertama", "Menyelesaikan sesi latihan pertama", System.currentTimeMillis()),
            BadgeUi("FIRST_10_REPS", "Pemanasan Bagus", "Mencapai 10 repetisi dalam satu sesi", System.currentTimeMillis())
        ),
        isNewRecord = true
    )

    ShakeFitTheme {
        RepSessionScreen(
            state = RepSessionUiState(
                exercise = ExerciseType.SQUAT,
                phase = SessionPhase.FINISHED,
                mode = SessionMode.FREE,
                countdownRemaining = 0,
                reps = 45,
                elapsedSec = 120,
                remainingSec = null,
                lastRepIntervalMs = 1250L,
                liveQualityScore = 90,
                targetReps = null,
                targetReached = false,
                positionHint = "Saku depan celana, HP tegak, layar menghadap badan. Tidak dipegang",
                summary = dummySummary,
                error = null
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
