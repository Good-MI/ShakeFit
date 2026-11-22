package com.example.shakefit.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shakefit.R
import com.example.shakefit.model.BadgeUi
import com.example.shakefit.model.DayStat
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.StatsUiState
import com.example.shakefit.ui.theme.ShakeFitTheme
import com.example.shakefit.util.formatDistanceKm
import com.example.shakefit.util.formatPace
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Route tipis StatsScreen: meng-hoist StatsViewModel.
 */
@Composable
fun StatsRoute(
    viewModel: StatsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-refresh saat tab Statistik dibuka (silent)
    LaunchedEffect(Unit) {
        viewModel.loadStats(silent = true)
    }

    // Auto-refresh saat kembali aktif ke tab Statistik (silent)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadStats(silent = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    StatsScreen(
        state = state,
        modifier = modifier
    )
}

/**
 * StatsScreen murni stateless (DesignShakefit.md 7.7).
 * Menampilkan ringkasan total, grafik aktivitas 7 hari berbasis Canvas, rekor pribadi, dan galeri lencana.
 */
@Composable
fun StatsScreen(
    state: StatsUiState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    if (state.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Pesan Error jika ada
        state.error?.let { err ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = err.message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // 1. Banner Streak (jika streak > 0)
        if (state.streakDays > 0) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.stats_streak_banner, state.streakDays),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Pertahankan konsistensi latihan harianmu!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 2. Ringkasan Total
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.stats_section_totals),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TotalMetricCard(
                    title = stringResource(R.string.stats_total_sessions),
                    value = "${state.totalSessions}",
                    modifier = Modifier.weight(1f)
                )
                TotalMetricCard(
                    title = stringResource(R.string.stats_total_reps),
                    value = "${state.totalReps}",
                    modifier = Modifier.weight(1f)
                )
                TotalMetricCard(
                    title = stringResource(R.string.stats_total_km),
                    value = formatDistanceKm(state.totalKm * 1000f),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Grafik Batang 7 Hari Terakhir (Canvas Compose murni)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.stats_section_chart),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ActivityBarChart(
                    last7Days = state.last7Days,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                )
            }
        }

        // 4. Rekor Pribadi
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.stats_section_records),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RecordItem(
                            label = stringResource(R.string.stats_record_squat),
                            value = "${state.bestRepsByExercise[ExerciseType.SQUAT] ?: 0} reps",
                            modifier = Modifier.weight(1f)
                        )
                        RecordItem(
                            label = stringResource(R.string.stats_record_pushup),
                            value = "${state.bestRepsByExercise[ExerciseType.PUSHUP] ?: 0} reps",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RecordItem(
                            label = stringResource(R.string.stats_record_pace),
                            value = formatPace(state.bestPaceSecPerKm),
                            modifier = Modifier.weight(1f)
                        )
                        RecordItem(
                            label = stringResource(R.string.stats_record_longest_run),
                            value = state.longestRunMeters?.let { formatDistanceKm(it) } ?: "--",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 5. Galeri Lencana
        val unlockedCount = state.badges.count { it.unlockedAtEpochMs != null }
        val totalBadges = state.badges.size

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.stats_section_badges, unlockedCount, totalBadges),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.badges.forEach { badge ->
                    BadgeCard(badge = badge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Kartu ringkasan metrik total (Sesi, Reps, Km).
 */
@Composable
private fun TotalMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Grafik Batang 7 Hari Terakhir (Canvas Compose murni).
 */
@Composable
private fun ActivityBarChart(
    last7Days: List<DayStat>,
    modifier: Modifier = Modifier
) {
    if (last7Days.isEmpty() || last7Days.all { it.sessions == 0 }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.stats_chart_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    // Hitung intensitas aktivitas (sesi + reps/15 + km*2)
    val maxScore = remember(last7Days) {
        last7Days.maxOfOrNull { day ->
            day.sessions.toFloat() + (day.reps / 15f) + (day.km * 2f)
        }?.coerceAtLeast(1f) ?: 1f
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barCount = last7Days.size
                if (barCount == 0) return@Canvas

                val totalWidth = size.width
                val totalHeight = size.height
                val barWidth = (totalWidth / barCount) * 0.55f
                val stepWidth = totalWidth / barCount

                last7Days.forEachIndexed { index, day ->
                    val score = day.sessions.toFloat() + (day.reps / 15f) + (day.km * 2f)
                    val fraction = (score / maxScore).coerceIn(0f, 1f)
                    val barHeight = if (score > 0) {
                        (totalHeight * fraction).coerceAtLeast(12f)
                    } else {
                        6f // Baseline dot/line
                    }

                    val left = index * stepWidth + (stepWidth - barWidth) / 2f
                    val top = totalHeight - barHeight

                    // Gambar background track bar
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, totalHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )

                    // Gambar bar aktivitas terisi
                    drawRoundRect(
                        color = if (score > 0) primaryColor else trackColor.copy(alpha = 0.5f),
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Baris Label Tanggal di Bawah Grafik
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            last7Days.forEach { day ->
                val label = if (day.dateIso.length >= 5) day.dateIso.takeLast(5) else day.dateIso
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Item tampilan rekor pribadi (mis. Squat Terbanyak, Pace Terbaik).
 */
@Composable
private fun RecordItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
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
 * Kartu lencana dengan status Terbuka (emas/vibrant) atau Terkunci (redup).
 */
@Composable
private fun BadgeCard(
    badge: BadgeUi,
    modifier: Modifier = Modifier
) {
    val isUnlocked = badge.unlockedAtEpochMs != null
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val unlockedDate = remember(badge.unlockedAtEpochMs) {
        badge.unlockedAtEpochMs?.let { dateFormat.format(Date(it)) }
    }

    OutlinedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isUnlocked) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(if (!isUnlocked) Modifier.alpha(0.55f) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Ikon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Filled.Star else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (isUnlocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Info Judul & Deskripsi
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = badge.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isUnlocked) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = if (isUnlocked) {
                                unlockedDate ?: stringResource(R.string.badge_unlocked_label)
                            } else {
                                stringResource(R.string.badge_locked_label)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isUnlocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==========================================
// PREVIEWS (Wajib: Normal, Kosong, Loading)
// ==========================================

@Preview(name = "StatsScreen - Normal", showBackground = true)
@Composable
private fun StatsScreenNormalPreview() {
    val sampleDays = listOf(
        DayStat("2026-11-15", reps = 20, km = 0f, sessions = 1),
        DayStat("2026-11-16", reps = 45, km = 0f, sessions = 2),
        DayStat("2026-11-17", reps = 0, km = 3.2f, sessions = 1),
        DayStat("2026-11-18", reps = 50, km = 0f, sessions = 1),
        DayStat("2026-11-19", reps = 0, km = 5.0f, sessions = 1),
        DayStat("2026-11-20", reps = 60, km = 0f, sessions = 2),
        DayStat("2026-11-21", reps = 30, km = 2.5f, sessions = 1)
    )

    val sampleBadges = listOf(
        BadgeUi("FIRST_SESSION", "Langkah Pertama", "Menyelesaikan sesi latihan pertama", System.currentTimeMillis()),
        BadgeUi("FIRST_10_REPS", "10 Repetisi", "Mencapai 10 repetisi dalam satu sesi", System.currentTimeMillis()),
        BadgeUi("STREAK_3", "Konsisten 3 Hari", "Latihan 3 hari berturut-turut", System.currentTimeMillis()),
        BadgeUi("STREAK_7", "Prajurit 7 Hari", "Latihan 7 hari berturut-turut", null),
        BadgeUi("REPS_100_DAY", "Centurion", "Menyelesaikan 100 repetisi dalam 1 hari", null)
    )

    ShakeFitTheme {
        StatsScreen(
            state = StatsUiState(
                totalSessions = 12,
                totalReps = 320,
                totalKm = 15.5f,
                bestRepsByExercise = mapOf(ExerciseType.SQUAT to 60, ExerciseType.PUSHUP to 35),
                bestPaceSecPerKm = 345,
                longestRunMeters = 5200f,
                last7Days = sampleDays,
                streakDays = 5,
                badges = sampleBadges,
                isLoading = false,
                error = null
            )
        )
    }
}

@Preview(name = "StatsScreen - Kosong", showBackground = true)
@Composable
private fun StatsScreenEmptyPreview() {
    ShakeFitTheme {
        StatsScreen(
            state = StatsUiState(
                totalSessions = 0,
                totalReps = 0,
                totalKm = 0f,
                bestRepsByExercise = emptyMap(),
                bestPaceSecPerKm = null,
                longestRunMeters = null,
                last7Days = emptyList(),
                streakDays = 0,
                badges = listOf(
                    BadgeUi("FIRST_SESSION", "Langkah Pertama", "Menyelesaikan sesi latihan pertama", null),
                    BadgeUi("STREAK_3", "Konsisten 3 Hari", "Latihan 3 hari berturut-turut", null)
                ),
                isLoading = false,
                error = null
            )
        )
    }
}

@Preview(name = "StatsScreen - Loading", showBackground = true)
@Composable
private fun StatsScreenLoadingPreview() {
    ShakeFitTheme {
        StatsScreen(
            state = StatsUiState(
                totalSessions = 0,
                totalReps = 0,
                totalKm = 0f,
                bestRepsByExercise = emptyMap(),
                bestPaceSecPerKm = null,
                longestRunMeters = null,
                last7Days = emptyList(),
                streakDays = 0,
                badges = emptyList(),
                isLoading = true,
                error = null
            )
        )
    }
}
