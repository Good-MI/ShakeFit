package com.example.shakefit.ui.screens.runsession

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shakefit.R
import com.example.shakefit.model.DistanceSource
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.GpsStatus
import com.example.shakefit.model.RunEnvironment
import com.example.shakefit.model.RunSessionEvent
import com.example.shakefit.model.RunSessionUiState
import com.example.shakefit.model.RunSplit
import com.example.shakefit.model.SessionMode
import com.example.shakefit.model.SessionPhase
import com.example.shakefit.model.SessionSummary
import com.example.shakefit.ui.components.SessionSummaryContent
import com.example.shakefit.ui.theme.ErrorDark
import com.example.shakefit.ui.theme.ErrorLight
import com.example.shakefit.ui.theme.RunningAccentDark
import com.example.shakefit.ui.theme.RunningAccentLight
import com.example.shakefit.ui.theme.ShakeFitTheme
import com.example.shakefit.ui.theme.SuccessDark
import com.example.shakefit.ui.theme.SuccessLight
import com.example.shakefit.ui.theme.WarningDark
import com.example.shakefit.ui.theme.WarningLight
import com.example.shakefit.util.formatCalories
import com.example.shakefit.util.formatDistanceKm
import com.example.shakefit.util.formatDuration
import com.example.shakefit.util.formatPace

/**
 * RunSessionRoute: Wrapper tipis sesuai DesignShakefit.md dan ui-contract.md.
 * Meng-handle izin lokasi just-in-time dan izin notifikasi Android 13+ tanpa logika domain.
 */
@Composable
fun RunSessionRoute(
    viewModel: RunSessionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Activity Result Launcher untuk izin runtime lokasi & notifikasi
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.onEvent(RunSessionEvent.PermissionResultChanged)
    }

    // Memicu permintaan izin just-in-time saat flag di uiState aktif
    LaunchedEffect(uiState.needsLocationPermission, uiState.needsNotificationPermission) {
        val permissionsToRequest = mutableListOf<String>()
        if (uiState.needsLocationPermission) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (uiState.needsNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    RunSessionScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onRequestPermissions = {
            val permissionsToRequest = mutableListOf<String>()
            if (uiState.needsLocationPermission) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (uiState.needsNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (permissionsToRequest.isNotEmpty()) {
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        },
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

/**
 * RunSessionScreen: 100% Stateless Composable untuk Sesi Lari (Outdoor GPS & Treadmill).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSessionScreen(
    state: RunSessionUiState,
    onEvent: (RunSessionEvent) -> Unit,
    onRequestPermissions: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val accentColor = if (isDark) RunningAccentDark else RunningAccentLight
    var showCalibrateDialog by remember { mutableStateOf(false) }

    // Dialog Kalibrasi Jarak Treadmill
    if (showCalibrateDialog) {
        TreadmillCalibrateDialog(
            onDismiss = { showCalibrateDialog = false },
            onConfirm = { actualMeters ->
                onEvent(RunSessionEvent.CalibrateTreadmill(actualMeters))
                showCalibrateDialog = false
            }
        )
    }

    // Jika fase FINISHED dan summary tersedia, tampilkan SessionSummaryContent penuh
    if (state.phase == SessionPhase.FINISHED && state.summary != null) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            modifier = modifier.fillMaxSize()
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Opsi Tambahan Khusus Treadmill: Kalibrasi Jarak
                if (state.environment == RunEnvironment.TREADMILL) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.btn_calibrate_treadmill),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { showCalibrateDialog = true },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text(
                                    text = "Kalibrasi",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                SessionSummaryContent(
                    summary = state.summary,
                    onDismiss = {
                        onEvent(RunSessionEvent.DismissSummary)
                        onNavigateBack()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
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
                            text = "Lari",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )

                        // Badge Lingkungan Lari
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = accentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (state.environment == RunEnvironment.OUTDOOR) "Outdoor" else "Treadmill",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        // Badge Status GPS (khusus Outdoor)
                        if (state.environment == RunEnvironment.OUTDOOR) {
                            GpsStatusBadge(gpsStatus = state.gpsStatus)
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
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Banner Tracking Background Aktif (F-07)
                if (state.isBackgroundTracking) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.bg_tracking_active),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Banner Peringatan Izin Lokasi & Notifikasi Just-in-Time
                if (state.needsLocationPermission && state.environment == RunEnvironment.OUTDOOR) {
                    PermissionNoticeBanner(
                        message = stringResource(R.string.permission_location_needed),
                        onRequest = onRequestPermissions
                    )
                }
                if (state.needsNotificationPermission) {
                    PermissionNoticeBanner(
                        message = stringResource(R.string.permission_notification_needed),
                        onRequest = onRequestPermissions
                    )
                }

                // Bagian Tengah Sesuai Fase Sesi
                when (state.phase) {
                    SessionPhase.IDLE -> {
                        RunIdlePhaseContent(
                            currentEnv = state.environment,
                            accentColor = accentColor,
                            onStartRun = { env -> onEvent(RunSessionEvent.Start(env)) }
                        )
                    }
                    SessionPhase.COUNTDOWN -> {
                        RunCountdownPhaseContent(
                            countdownRemaining = state.countdownRemaining,
                            accentColor = accentColor,
                            onCancel = { onEvent(RunSessionEvent.Cancel) }
                        )
                    }
                    SessionPhase.ACTIVE, SessionPhase.PAUSED -> {
                        RunActivePhaseContent(
                            state = state,
                            accentColor = accentColor,
                            onEvent = onEvent
                        )
                    }
                    SessionPhase.FINISHED -> {
                        // Handled above
                    }
                }
            }

            // Banner Error jika ada
            state.error?.let { err ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
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
 * Badge status GPS dengan warna semantik (DesignShakefit.md).
 */
@Composable
private fun GpsStatusBadge(
    gpsStatus: GpsStatus,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val (statusText, badgeColor, textColor) = when (gpsStatus) {
        GpsStatus.READY -> Triple(
            stringResource(R.string.gps_status_ready),
            if (isDark) SuccessDark else SuccessLight,
            Color.White
        )
        GpsStatus.SEARCHING -> Triple(
            stringResource(R.string.gps_status_searching),
            if (isDark) WarningDark else WarningLight,
            Color.Black
        )
        GpsStatus.WEAK -> Triple(
            stringResource(R.string.gps_status_weak),
            if (isDark) WarningDark else WarningLight,
            Color.Black
        )
        GpsStatus.DISABLED -> Triple(
            stringResource(R.string.gps_status_disabled),
            if (isDark) ErrorDark else ErrorLight,
            Color.White
        )
        GpsStatus.PERMISSION_DENIED -> Triple(
            stringResource(R.string.gps_status_permission_denied),
            if (isDark) ErrorDark else ErrorLight,
            Color.White
        )
        GpsStatus.OFF -> Triple(
            stringResource(R.string.gps_status_off),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = badgeColor,
        modifier = modifier
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * Banner Peringatan Izin Just-in-Time.
 */
@Composable
private fun PermissionNoticeBanner(
    message: String,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequest,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(
                    text = stringResource(R.string.btn_request_permission),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}

/**
 * Tampilan Fase IDLE (Pemilihan mode lari Outdoor GPS vs Treadmill).
 */
@Composable
private fun RunIdlePhaseContent(
    currentEnv: RunEnvironment,
    accentColor: Color,
    onStartRun: (RunEnvironment) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedEnv by remember { mutableStateOf(currentEnv) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(R.string.env_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = selectedEnv == RunEnvironment.OUTDOOR,
                onClick = { selectedEnv = RunEnvironment.OUTDOOR },
                label = { Text(text = stringResource(R.string.env_outdoor)) },
                modifier = Modifier.padding(horizontal = 4.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor.copy(alpha = 0.2f),
                    selectedLabelColor = accentColor
                )
            )

            FilterChip(
                selected = selectedEnv == RunEnvironment.TREADMILL,
                onClick = { selectedEnv = RunEnvironment.TREADMILL },
                label = { Text(text = stringResource(R.string.env_treadmill)) },
                modifier = Modifier.padding(horizontal = 4.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor.copy(alpha = 0.2f),
                    selectedLabelColor = accentColor
                )
            )
        }

        // Kartu Penjelasan Mode Terpilih
        OutlinedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (selectedEnv == RunEnvironment.OUTDOOR) stringResource(R.string.env_outdoor) else stringResource(R.string.env_treadmill),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = if (selectedEnv == RunEnvironment.OUTDOOR) stringResource(R.string.env_outdoor_desc) else stringResource(R.string.env_treadmill_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tombol CTA Besar "Mulai Lari"
        Button(
            onClick = { onStartRun(selectedEnv) },
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
                    text = stringResource(R.string.btn_start_run),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Tampilan Fase COUNTDOWN.
 */
@Composable
private fun RunCountdownPhaseContent(
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
 * Tampilan Fase ACTIVE & PAUSED: Jarak, Grid Metrik, Split 1 km, dan Kontrol Sesi.
 */
@Composable
private fun RunActivePhaseContent(
    state: RunSessionUiState,
    accentColor: Color,
    onEvent: (RunSessionEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPaused = state.phase == SessionPhase.PAUSED
    val isTreadmillEstimate = state.distanceSource == DistanceSource.STEPS_ESTIMATE

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Kartu Utama Jarak Tempuh
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.metric_distance),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Label Perkiraan wajib tampil jika mode treadmill (F-12e)
                    if (isTreadmillEstimate) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.estimate_label),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isPaused) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = stringResource(R.string.paused_pill),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Angka Jarak Format Besar & Tegas (tabular tnum)
                Text(
                    text = formatDistanceKm(state.distanceMeters),
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
            }
        }

        // Grid Metrik Sekunder
        OutlinedCard(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Baris 1: Durasi & Pace Saat Ini
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RunMetricCell(
                        label = stringResource(R.string.metric_duration),
                        value = formatDuration(state.elapsedSec),
                        modifier = Modifier.weight(1f)
                    )
                    RunMetricCell(
                        label = stringResource(R.string.metric_current_pace),
                        value = formatPace(state.currentPaceSecPerKm),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Baris 2: Pace Rata-rata & Cadence
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RunMetricCell(
                        label = stringResource(R.string.metric_avg_pace),
                        value = formatPace(state.avgPaceSecPerKm),
                        modifier = Modifier.weight(1f)
                    )
                    RunMetricCell(
                        label = stringResource(R.string.metric_cadence),
                        value = "${state.cadenceSpm ?: "--"} spm",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Baris 3: Kalori
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    RunMetricCell(
                        label = stringResource(R.string.metric_calories),
                        value = formatCalories(state.caloriesEst),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Daftar Split per 1 km (jika ada)
        if (state.splits.isNotEmpty()) {
            RunSplitsTable(splits = state.splits)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Kontrol Tombol Sesi Lari
        if (state.phase == SessionPhase.ACTIVE) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onEvent(RunSessionEvent.Pause) },
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
                    onClick = { onEvent(RunSessionEvent.Finish) },
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
                    onClick = { onEvent(RunSessionEvent.Resume) },
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
                        onClick = { onEvent(RunSessionEvent.Cancel) },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(text = stringResource(R.string.btn_cancel))
                    }

                    Button(
                        onClick = { onEvent(RunSessionEvent.Finish) },
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

@Composable
private fun RunMetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Tabel Daftar Split 1 km.
 */
@Composable
private fun RunSplitsTable(
    splits: List<RunSplit>,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.section_splits),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Header Tabel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.split_km_header),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.split_duration_header),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.split_pace_header),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            // Baris-baris Split
            splits.forEach { split ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${split.kmIndex}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatDuration(split.durationSec),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatPace(split.paceSecPerKm),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Dialog Kalibrasi Jarak Treadmill.
 */
@Composable
private fun TreadmillCalibrateDialog(
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_calibrate_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.dialog_calibrate_msg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = textInput,
                    onValueChange = {
                        textInput = it
                        hasError = false
                    },
                    label = { Text(stringResource(R.string.dialog_calibrate_hint)) },
                    isError = hasError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = textInput.replace(',', '.').toFloatOrNull()
                    if (parsed != null && parsed > 0f) {
                        // Jika user menginput nilai kecil (misal 2.5), anggap satuan km -> konversi ke meter
                        val meters = if (parsed < 50f) parsed * 1000f else parsed
                        onConfirm(meters)
                    } else {
                        hasError = true
                    }
                }
            ) {
                Text(text = stringResource(R.string.dialog_calibrate_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.btn_cancel))
            }
        }
    )
}

// =========================================================================
// @Preview Lengkap
// =========================================================================

@Preview(name = "Phase IDLE - Outdoor", showBackground = true)
@Composable
private fun PreviewRunSessionIdle() {
    ShakeFitTheme {
        RunSessionScreen(
            state = RunSessionUiState(
                phase = SessionPhase.IDLE,
                environment = RunEnvironment.OUTDOOR,
                countdownRemaining = 5,
                elapsedSec = 0,
                distanceMeters = 0f,
                distanceSource = DistanceSource.GPS,
                currentPaceSecPerKm = null,
                avgPaceSecPerKm = null,
                cadenceSpm = null,
                caloriesEst = 0f,
                splits = emptyList(),
                gpsStatus = GpsStatus.SEARCHING,
                needsLocationPermission = false,
                needsNotificationPermission = false,
                isBackgroundTracking = false,
                summary = null,
                error = null
            ),
            onEvent = {},
            onRequestPermissions = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Phase ACTIVE - Outdoor GPS", showBackground = true)
@Composable
private fun PreviewRunSessionActiveOutdoor() {
    ShakeFitTheme {
        RunSessionScreen(
            state = RunSessionUiState(
                phase = SessionPhase.ACTIVE,
                environment = RunEnvironment.OUTDOOR,
                countdownRemaining = 0,
                elapsedSec = 750,
                distanceMeters = 2450f,
                distanceSource = DistanceSource.GPS,
                currentPaceSecPerKm = 310,
                avgPaceSecPerKm = 306,
                cadenceSpm = 168,
                caloriesEst = 175f,
                splits = listOf(
                    RunSplit(kmIndex = 1, durationSec = 305, paceSecPerKm = 305),
                    RunSplit(kmIndex = 2, durationSec = 308, paceSecPerKm = 308)
                ),
                gpsStatus = GpsStatus.READY,
                needsLocationPermission = false,
                needsNotificationPermission = false,
                isBackgroundTracking = true,
                summary = null,
                error = null
            ),
            onEvent = {},
            onRequestPermissions = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Phase ACTIVE - Treadmill (Perkiraan)", showBackground = true)
@Composable
private fun PreviewRunSessionActiveTreadmill() {
    ShakeFitTheme {
        RunSessionScreen(
            state = RunSessionUiState(
                phase = SessionPhase.ACTIVE,
                environment = RunEnvironment.TREADMILL,
                countdownRemaining = 0,
                elapsedSec = 520,
                distanceMeters = 1500f,
                distanceSource = DistanceSource.STEPS_ESTIMATE,
                currentPaceSecPerKm = 345,
                avgPaceSecPerKm = 346,
                cadenceSpm = 160,
                caloriesEst = 108f,
                splits = listOf(
                    RunSplit(kmIndex = 1, durationSec = 346, paceSecPerKm = 346)
                ),
                gpsStatus = GpsStatus.OFF,
                needsLocationPermission = false,
                needsNotificationPermission = false,
                isBackgroundTracking = true,
                summary = null,
                error = null
            ),
            onEvent = {},
            onRequestPermissions = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Phase PAUSED", showBackground = true)
@Composable
private fun PreviewRunSessionPaused() {
    ShakeFitTheme {
        RunSessionScreen(
            state = RunSessionUiState(
                phase = SessionPhase.PAUSED,
                environment = RunEnvironment.OUTDOOR,
                countdownRemaining = 0,
                elapsedSec = 900,
                distanceMeters = 3000f,
                distanceSource = DistanceSource.GPS,
                currentPaceSecPerKm = null,
                avgPaceSecPerKm = 300,
                cadenceSpm = null,
                caloriesEst = 215f,
                splits = emptyList(),
                gpsStatus = GpsStatus.READY,
                needsLocationPermission = false,
                needsNotificationPermission = false,
                isBackgroundTracking = false,
                summary = null,
                error = null
            ),
            onEvent = {},
            onRequestPermissions = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Phase FINISHED - Summary", showBackground = true)
@Composable
private fun PreviewRunSessionFinished() {
    val dummySummary = SessionSummary(
        sessionId = 2L,
        exercise = ExerciseType.RUNNING,
        mode = SessionMode.FREE,
        startedAtEpochMs = System.currentTimeMillis() - 1500000L,
        durationSec = 1500,
        reps = null,
        avgTempoMs = null,
        qualityScore = null,
        distanceMeters = 5020f,
        avgPaceSecPerKm = 298,
        bestPaceSecPerKm = 285,
        avgCadenceSpm = 172,
        distanceSource = DistanceSource.GPS,
        splits = listOf(
            RunSplit(kmIndex = 1, durationSec = 302, paceSecPerKm = 302),
            RunSplit(kmIndex = 2, durationSec = 298, paceSecPerKm = 298),
            RunSplit(kmIndex = 3, durationSec = 295, paceSecPerKm = 295),
            RunSplit(kmIndex = 4, durationSec = 290, paceSecPerKm = 290),
            RunSplit(kmIndex = 5, durationSec = 285, paceSecPerKm = 285)
        ),
        caloriesEst = 360f,
        note = "Lari pagi 5K",
        newBadges = listOf(),
        isNewRecord = true
    )

    ShakeFitTheme {
        RunSessionScreen(
            state = RunSessionUiState(
                phase = SessionPhase.FINISHED,
                environment = RunEnvironment.OUTDOOR,
                countdownRemaining = 0,
                elapsedSec = 1500,
                distanceMeters = 5020f,
                distanceSource = DistanceSource.GPS,
                currentPaceSecPerKm = null,
                avgPaceSecPerKm = 298,
                cadenceSpm = 172,
                caloriesEst = 360f,
                splits = emptyList(),
                gpsStatus = GpsStatus.OFF,
                needsLocationPermission = false,
                needsNotificationPermission = false,
                isBackgroundTracking = false,
                summary = dummySummary,
                error = null
            ),
            onEvent = {},
            onRequestPermissions = {},
            onNavigateBack = {}
        )
    }
}
