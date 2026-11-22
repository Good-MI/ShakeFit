package com.example.shakefit.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shakefit.R
import com.example.shakefit.model.DistanceSource
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.HistoryEvent
import com.example.shakefit.model.HistoryUiState
import com.example.shakefit.model.SessionMode
import com.example.shakefit.model.SessionSummary
import com.example.shakefit.ui.components.SessionSummaryContent
import com.example.shakefit.ui.theme.ShakeFitTheme
import com.example.shakefit.ui.theme.getExerciseAccentColor
import com.example.shakefit.util.formatCalories
import com.example.shakefit.util.formatDistanceKm
import com.example.shakefit.util.formatDuration
import com.example.shakefit.util.formatPace
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Route tipis HistoryScreen: meng-hoist HistoryViewModel.
 */
@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-refresh saat pertama kali tab Riwayat dibuka (silent)
    LaunchedEffect(Unit) {
        viewModel.refresh(silent = true)
    }

    // Auto-refresh saat kembali aktif ke tab Riwayat (silent)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh(silent = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    HistoryScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

/**
 * HistoryScreen murni stateless (DesignShakefit.md 7.6).
 * Menampilkan daftar riwayat latihan dengan filter chip, serta detail sesi jika `selected != null`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onEvent: (HistoryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Mode Detail: jika item riwayat sedang dipilih
    if (state.selected != null) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.history_detail_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { onEvent(HistoryEvent.CloseDetail) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back)
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            SessionSummaryContent(
                summary = state.selected,
                onDismiss = { onEvent(HistoryEvent.CloseDetail) },
                onUpdateNote = { note ->
                    onEvent(HistoryEvent.UpdateNote(state.selected.sessionId, note))
                },
                onDelete = {
                    val id = state.selected.sessionId
                    onEvent(HistoryEvent.CloseDetail)
                    onEvent(HistoryEvent.Delete(id))
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
        return
    }

    // Mode List Normal
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp)
    ) {
        // Filter Chips Row: Semua, Squat, Push-up, Running
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = state.filter == null,
                    onClick = { onEvent(HistoryEvent.SetFilter(null)) },
                    label = { Text(text = stringResource(R.string.history_filter_all)) }
                )
            }
            item {
                FilterChip(
                    selected = state.filter == ExerciseType.SQUAT,
                    onClick = { onEvent(HistoryEvent.SetFilter(ExerciseType.SQUAT)) },
                    label = { Text(text = "Squat") }
                )
            }
            item {
                FilterChip(
                    selected = state.filter == ExerciseType.PUSHUP,
                    onClick = { onEvent(HistoryEvent.SetFilter(ExerciseType.PUSHUP)) },
                    label = { Text(text = "Push-up") }
                )
            }
            item {
                FilterChip(
                    selected = state.filter == ExerciseType.RUNNING,
                    onClick = { onEvent(HistoryEvent.SetFilter(ExerciseType.RUNNING)) },
                    label = { Text(text = "Running") }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Pesan Error jika ada
        state.error?.let { err ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = err.message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Konten: Loading, Kosong, atau Daftar Sesi
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            state.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.history_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.history_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.items, key = { it.sessionId }) { session ->
                        HistoryItemCard(
                            session = session,
                            onClick = { onEvent(HistoryEvent.Open(session.sessionId)) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kartu ringkasan sesi pada daftar riwayat.
 */
@Composable
private fun HistoryItemCard(
    session: SessionSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val accentColor = getExerciseAccentColor(session.exercise, isDark)

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()) }
    val dateString = remember(session.startedAtEpochMs) {
        dateFormat.format(Date(session.startedAtEpochMs))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Baris Atas: Indikator Latihan, Mode, Rekor Baru Badge, & Tanggal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Text(
                        text = session.exercise.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = session.mode.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (session.isNewRecord) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = stringResource(R.string.history_item_record_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            // Baris Metrik Utama
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (session.exercise == ExerciseType.RUNNING) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formatDistanceKm(session.distanceMeters ?: 0f),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (session.distanceSource == DistanceSource.STEPS_ESTIMATE) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = stringResource(R.string.estimate_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Pace: ${formatPace(session.avgPaceSecPerKm)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column {
                        Text(
                            text = "${session.reps ?: 0} reps",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        session.qualityScore?.let { qs ->
                            Text(
                                text = "Kualitas: $qs%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Kolom Kanan: Durasi & Kalori
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDuration(session.durationSec),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatCalories(session.caloriesEst),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Baris Bawah: Tanggal & Indikator Catatan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (session.note.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Catatan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// PREVIEWS (Wajib: Normal, Kosong, Detail, Loading)
// ==========================================

@Preview(name = "HistoryScreen - Normal", showBackground = true)
@Composable
private fun HistoryScreenNormalPreview() {
    val sampleSummary = SessionSummary(
        sessionId = 1,
        exercise = ExerciseType.SQUAT,
        mode = SessionMode.FREE,
        startedAtEpochMs = System.currentTimeMillis() - 3600000,
        durationSec = 240,
        reps = 45,
        avgTempoMs = 1800,
        qualityScore = 88,
        distanceMeters = null,
        avgPaceSecPerKm = null,
        bestPaceSecPerKm = null,
        avgCadenceSpm = null,
        distanceSource = null,
        splits = emptyList(),
        caloriesEst = 32f,
        note = "Latihan sore terasa ringan",
        newBadges = emptyList(),
        isNewRecord = true
    )

    ShakeFitTheme {
        HistoryScreen(
            state = HistoryUiState(
                filter = null,
                items = listOf(sampleSummary),
                selected = null,
                isLoading = false,
                error = null
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "HistoryScreen - Detail Terbuka", showBackground = true)
@Composable
private fun HistoryScreenDetailPreview() {
    val sampleSummary = SessionSummary(
        sessionId = 2,
        exercise = ExerciseType.RUNNING,
        mode = SessionMode.FREE,
        startedAtEpochMs = System.currentTimeMillis() - 7200000,
        durationSec = 1350,
        reps = null,
        avgTempoMs = null,
        qualityScore = null,
        distanceMeters = 3500f,
        avgPaceSecPerKm = 385,
        bestPaceSecPerKm = 360,
        avgCadenceSpm = 165,
        distanceSource = DistanceSource.GPS,
        splits = emptyList(),
        caloriesEst = 240f,
        note = "Cuaca cerah, rute taman kota",
        newBadges = emptyList(),
        isNewRecord = false
    )

    ShakeFitTheme {
        HistoryScreen(
            state = HistoryUiState(
                filter = null,
                items = listOf(sampleSummary),
                selected = sampleSummary,
                isLoading = false,
                error = null
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "HistoryScreen - Kosong", showBackground = true)
@Composable
private fun HistoryScreenEmptyPreview() {
    ShakeFitTheme {
        HistoryScreen(
            state = HistoryUiState(
                filter = null,
                items = emptyList(),
                selected = null,
                isLoading = false,
                error = null
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "HistoryScreen - Loading", showBackground = true)
@Composable
private fun HistoryScreenLoadingPreview() {
    ShakeFitTheme {
        HistoryScreen(
            state = HistoryUiState(
                filter = null,
                items = emptyList(),
                selected = null,
                isLoading = true,
                error = null
            ),
            onEvent = {}
        )
    }
}
