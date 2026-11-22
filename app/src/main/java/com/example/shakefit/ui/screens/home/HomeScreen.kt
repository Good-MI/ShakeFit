package com.example.shakefit.ui.screens.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shakefit.R
import com.example.shakefit.model.ExerciseCard
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.GoalProgress
import com.example.shakefit.model.HomeEvent
import com.example.shakefit.model.HomeUiState
import com.example.shakefit.model.UiError
import com.example.shakefit.ui.theme.ShakeFitTheme
import com.example.shakefit.ui.theme.getExerciseAccentColor

/**
 * HomeRoute: Wrapper rute tipis sesuai DesignShakefit.md dan ui-contract.md.
 * Memanggil collectAsStateWithLifecycle(), auto-refresh saat kembali ke beranda, dan meneruskan navigasi.
 */
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onNavigateToExercise: (ExerciseType) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-refresh saat pertama kali masuk tab Beranda (silent agar tidak memunculkan spinner)
    LaunchedEffect(Unit) {
        viewModel.onEvent(HomeEvent.SilentRefresh)
    }

    // Auto-refresh saat kembali aktif ke tab Beranda (silent refresh tanpa spinner)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(HomeEvent.SilentRefresh)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    HomeScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToExercise = onNavigateToExercise,
        modifier = modifier
    )
}

/**
 * HomeScreen: 100% Stateless Composable untuk Layar Beranda ShakeFit.
 * Merender header sapaan, streak, kartu target hari ini, dan daftar latihan aktif/nonaktif.
 * Dilengkapi Pull-to-Refresh bawaan Material 3 (PullToRefreshBox).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    onNavigateToExercise: (ExerciseType) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { onEvent(HomeEvent.Refresh) },
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Header: Sapaan Personal & Streak Pill
            HomeHeaderSection(
                greetingName = state.greetingName,
                streakDays = state.streakDays
            )

            // 2. Banner Error (jika state.error != null)
            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.error?.let { err ->
                    ErrorBannerCard(
                        message = err.message,
                        onRetry = { onEvent(HomeEvent.Refresh) }
                    )
                }
            }

            // 3. Section Target Hari Ini
            TodayGoalsSection(todayGoals = state.todayGoals)

            // 5. Section Mulai Latihan (Squat, Push-up, Running)
            ExerciseListSection(
                exercises = state.exercises,
                onExerciseClick = { type ->
                    onEvent(HomeEvent.SelectExercise(type))
                    onNavigateToExercise(type)
                }
            )
        }
    }
}

/**
 * Header Sapaan & Streak Pill gamified.
 */
@Composable
private fun HomeHeaderSection(
    greetingName: String?,
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val greetingText = greetingName?.let {
                stringResource(R.string.greeting_user, it)
            } ?: stringResource(R.string.greeting_default)

            Text(
                text = greetingText,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Streak Pill Gamified
        val isStreakActive = streakDays > 0
        Surface(
            shape = RoundedCornerShape(50),
            color = if (isStreakActive) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            tonalElevation = if (isStreakActive) 2.dp else 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = stringResource(R.string.streak_title),
                    tint = if (isStreakActive) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.streak_label, streakDays),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isStreakActive) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Section Target Hari Ini (List atau Empty State).
 */
@Composable
private fun TodayGoalsSection(
    todayGoals: List<GoalProgress>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.section_today_goals),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )

        if (todayGoals.isEmpty()) {
            // Empty State
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.empty_goals_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.empty_goals_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Horizontal Row kartu progres target
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(todayGoals, key = { it.goalId }) { goal ->
                    GoalProgressCard(goal = goal)
                }
            }
        }
    }
}

/**
 * Kartu item progres target harian.
 */
@Composable
private fun GoalProgressCard(
    goal: GoalProgress,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val accentColor = getExerciseAccentColor(goal.type, isDark)
    val isCompleted = goal.fraction >= 1.0f

    OutlinedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.width(170.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.type.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                if (isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = stringResource(R.string.goal_completed),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Visual Progress Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = goal.fraction.coerceIn(0f, 1f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accentColor)
                )
            }

            // Nilai metrik (Current / Target + Unit)
            val currentInt = goal.current.toInt()
            val targetInt = goal.target.toInt()
            Text(
                text = "$currentInt / $targetInt ${goal.unit}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Section Daftar 3 Latihan Utama (Squat, Push-up, Running).
 */
@Composable
private fun ExerciseListSection(
    exercises: List<ExerciseCard>,
    onExerciseClick: (ExerciseType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.section_exercises),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )

        exercises.forEach { card ->
            ExerciseItemCard(
                card = card,
                onClick = { onExerciseClick(card.type) }
            )
        }
    }
}

/**
 * Kartu latihan individual dengan dukungan state aktif dan nonaktif (sensor unavailable).
 */
@Composable
private fun ExerciseItemCard(
    card: ExerciseCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val accentColor = getExerciseAccentColor(card.type, isDark)
    val isAvailable = card.sensorAvailable

    val cardAlpha = if (isAvailable) 1.0f else 0.55f

    Card(
        onClick = onClick,
        enabled = isAvailable,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isAvailable) 2.dp else 0.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ikon bulat dengan warna aksen latihan
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = card.name,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val helperText = when (card.type) {
                        ExerciseType.SQUAT -> stringResource(R.string.squat_desc)
                        ExerciseType.PUSHUP -> stringResource(R.string.pushup_desc)
                        ExerciseType.RUNNING -> stringResource(R.string.running_desc)
                    }
                    Text(
                        text = helperText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isAvailable) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.tap_to_start),
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Banner peringatan inline jika sensor tidak tersedia di perangkat
            if (!isAvailable) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = card.unavailableReason ?: stringResource(R.string.sensor_unavailable_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Banner Error jika state.error != null.
 */
@Composable
private fun ErrorBannerCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(50)
            ) {
                Text(text = stringResource(R.string.btn_try_again))
            }
        }
    }
}

// =========================================================================
// @Preview Lengkap (Data Dummy Khusus di Fungsi Preview Sesuai Aturan Mutlak)
// =========================================================================

private val dummyExercises = listOf(
    ExerciseCard(
        type = ExerciseType.SQUAT,
        name = "Squat",
        sensorAvailable = true,
        unavailableReason = null
    ),
    ExerciseCard(
        type = ExerciseType.PUSHUP,
        name = "Push-up",
        sensorAvailable = true,
        unavailableReason = null
    ),
    ExerciseCard(
        type = ExerciseType.RUNNING,
        name = "Lari (Outdoor / Treadmill)",
        sensorAvailable = true,
        unavailableReason = null
    )
)

private val dummyGoals = listOf(
    GoalProgress(
        goalId = 1L,
        type = ExerciseType.SQUAT,
        current = 35f,
        target = 50f,
        unit = "REPS",
        fraction = 0.70f
    ),
    GoalProgress(
        goalId = 2L,
        type = ExerciseType.RUNNING,
        current = 5.2f,
        target = 5.0f,
        unit = "KM",
        fraction = 1.0f
    )
)

@Preview(name = "Normal - Light Theme", showBackground = true)
@Composable
private fun PreviewHomeScreenNormalLight() {
    ShakeFitTheme(darkTheme = false) {
        HomeScreen(
            state = HomeUiState(
                greetingName = "Budi",
                exercises = dummyExercises,
                todayGoals = dummyGoals,
                streakDays = 5,
                isLoading = false,
                error = null
            ),
            onEvent = {},
            onNavigateToExercise = {}
        )
    }
}

@Preview(name = "Normal - Dark Theme", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewHomeScreenNormalDark() {
    ShakeFitTheme(darkTheme = true) {
        HomeScreen(
            state = HomeUiState(
                greetingName = "Budi",
                exercises = dummyExercises,
                todayGoals = dummyGoals,
                streakDays = 5,
                isLoading = false,
                error = null
            ),
            onEvent = {},
            onNavigateToExercise = {}
        )
    }
}

@Preview(name = "Loading State", showBackground = true)
@Composable
private fun PreviewHomeScreenLoading() {
    ShakeFitTheme {
        HomeScreen(
            state = HomeUiState(
                greetingName = null,
                exercises = dummyExercises,
                todayGoals = emptyList(),
                streakDays = 0,
                isLoading = true,
                error = null
            ),
            onEvent = {},
            onNavigateToExercise = {}
        )
    }
}

@Preview(name = "Empty Goals State", showBackground = true)
@Composable
private fun PreviewHomeScreenEmptyGoals() {
    ShakeFitTheme {
        HomeScreen(
            state = HomeUiState(
                greetingName = null,
                exercises = dummyExercises,
                todayGoals = emptyList(),
                streakDays = 0,
                isLoading = false,
                error = null
            ),
            onEvent = {},
            onNavigateToExercise = {}
        )
    }
}

@Preview(name = "Sensor Unavailable State", showBackground = true)
@Composable
private fun PreviewHomeScreenSensorUnavailable() {
    val exercisesWithDisabled = dummyExercises.map {
        if (it.type == ExerciseType.PUSHUP) {
            it.copy(
                sensorAvailable = false,
                unavailableReason = "Sensor Proximity tidak ditemukan di HP ini"
            )
        } else it
    }

    ShakeFitTheme {
        HomeScreen(
            state = HomeUiState(
                greetingName = "Pongo",
                exercises = exercisesWithDisabled,
                todayGoals = dummyGoals,
                streakDays = 2,
                isLoading = false,
                error = null
            ),
            onEvent = {},
            onNavigateToExercise = {}
        )
    }
}

@Preview(name = "Error State", showBackground = true)
@Composable
private fun PreviewHomeScreenError() {
    ShakeFitTheme {
        HomeScreen(
            state = HomeUiState(
                greetingName = null,
                exercises = dummyExercises,
                todayGoals = emptyList(),
                streakDays = 0,
                isLoading = false,
                error = UiError(code = "DB_ERROR", message = "Gagal membaca database SQLite")
            ),
            onEvent = {},
            onNavigateToExercise = {}
        )
    }
}
