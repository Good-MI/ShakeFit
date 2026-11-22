package com.example.shakefit.ui.screens.goals

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shakefit.R
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.model.GoalItem
import com.example.shakefit.model.GoalsEvent
import com.example.shakefit.model.GoalsUiState
import com.example.shakefit.ui.theme.ShakeFitTheme
import com.example.shakefit.ui.theme.getExerciseAccentColor
import java.util.Locale

/**
 * Route tipis GoalsScreen: meng-hoist ViewModel dan meneruskan event/state.
 */
@Composable
fun GoalsRoute(
    viewModel: GoalsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-refresh saat tab Target dibuka (silent)
    LaunchedEffect(Unit) {
        viewModel.refresh(silent = true)
    }

    // Auto-refresh saat kembali aktif ke tab Target dari layar lain (silent)
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

    GoalsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

/**
 * GoalsScreen murni stateless (DesignShakefit.md 7.5).
 * Menampilkan daftar target harian, progres hari ini, toggle aktif/nonaktif, ubah, dan hapus.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    state: GoalsUiState,
    onEvent: (GoalsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<GoalItem?>(null) }
    var goalToDelete by remember { mutableStateOf<GoalItem?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(text = stringResource(R.string.btn_add_goal), fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Layar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.goals_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.goals_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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

            // Konten: Loading, Kosong, atau Daftar Target
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
                state.goals.isEmpty() -> {
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
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.goals_empty_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.goals_empty_desc),
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
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.goals, key = { it.id }) { goal ->
                            GoalCard(
                                goal = goal,
                                onToggleActive = { active ->
                                    onEvent(GoalsEvent.SetActive(goal.id, active))
                                },
                                onEdit = { goalToEdit = goal },
                                onDelete = { goalToDelete = goal }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog Tambah Target
    if (showAddDialog) {
        AddGoalDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { type, target ->
                showAddDialog = false
                onEvent(GoalsEvent.Add(type, target))
            }
        )
    }

    // Dialog Ubah Target
    goalToEdit?.let { goal ->
        EditGoalDialog(
            goal = goal,
            onDismiss = { goalToEdit = null },
            onConfirm = { newTarget ->
                val id = goal.id
                goalToEdit = null
                onEvent(GoalsEvent.Update(id, newTarget))
            }
        )
    }

    // Dialog Konfirmasi Hapus Target
    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text(text = stringResource(R.string.dialog_delete_goal_title)) },
            text = { Text(text = stringResource(R.string.dialog_delete_goal_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = goal.id
                        goalToDelete = null
                        onEvent(GoalsEvent.Delete(id))
                    }
                ) {
                    Text(
                        text = stringResource(R.string.btn_delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text(text = stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

/**
 * Kartu item target harian dengan visual progress bar, switch aktif, dan aksi edit/delete.
 */
@Composable
private fun GoalCard(
    goal: GoalItem,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val accentColor = getExerciseAccentColor(goal.type, isDark)
    val exerciseName = when (goal.type) {
        ExerciseType.SQUAT -> "Squat"
        ExerciseType.PUSHUP -> "Push-up"
        ExerciseType.RUNNING -> "Running"
    }

    val targetDisplay = if (goal.type == ExerciseType.RUNNING) {
        String.format(Locale.US, "%.1f %s", goal.target, goal.unit)
    } else {
        String.format(Locale.US, "%d %s", goal.target.toInt(), goal.unit.lowercase())
    }

    val progressPercent = (goal.progress * 100).toInt()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Baris Atas: Nama Latihan, Switch Aktif
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (goal.active) {
                            stringResource(R.string.goal_status_active)
                        } else {
                            stringResource(R.string.goal_status_inactive)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (goal.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = goal.active,
                        onCheckedChange = onToggleActive
                    )
                }
            }

            // Target & Capaian Progres
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.label_target_value),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = targetDisplay,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = stringResource(R.string.goal_progress_percent, progressPercent),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (progressPercent >= 100) MaterialTheme.colorScheme.primary else accentColor
                )
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { goal.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (progressPercent >= 100) MaterialTheme.colorScheme.primary else accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Baris Aksi Bawah: Edit & Hapus
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.btn_edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.btn_delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dialog Tambah Target Baru (Pilih jenis latihan & isi angka).
 */
@Composable
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (ExerciseType, Float) -> Unit
) {
    var selectedType by remember { mutableStateOf(ExerciseType.SQUAT) }
    var targetText by remember { mutableStateOf("50") }

    val defaultTarget = when (selectedType) {
        ExerciseType.SQUAT -> "50"
        ExerciseType.PUSHUP -> "30"
        ExerciseType.RUNNING -> "3.0"
    }

    val unit = if (selectedType == ExerciseType.RUNNING) "km" else "reps"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.dialog_add_goal_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_select_exercise),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExerciseType.values().forEach { type ->
                        val isSelected = type == selectedType
                        val name = when (type) {
                            ExerciseType.SQUAT -> "Squat"
                            ExerciseType.PUSHUP -> "Push-up"
                            ExerciseType.RUNNING -> "Running"
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedType = type
                                targetText = defaultTarget
                            },
                            label = { Text(text = name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text(text = "${stringResource(R.string.label_target_value)} ($unit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = targetText.toFloatOrNull()
                    if (value != null && value > 0f) {
                        onConfirm(selectedType, value)
                    }
                }
            ) {
                Text(text = stringResource(R.string.btn_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.btn_cancel))
            }
        }
    )
}

/**
 * Dialog Ubah Target untuk target yang sudah ada.
 */
@Composable
private fun EditGoalDialog(
    goal: GoalItem,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var targetText by remember {
        mutableStateOf(
            if (goal.type == ExerciseType.RUNNING) {
                String.format(Locale.US, "%.1f", goal.target)
            } else {
                goal.target.toInt().toString()
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.dialog_edit_goal_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text(text = "${stringResource(R.string.label_target_value)} (${goal.unit.lowercase()})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = targetText.toFloatOrNull()
                    if (value != null && value > 0f) {
                        onConfirm(value)
                    }
                }
            ) {
                Text(text = stringResource(R.string.btn_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.btn_cancel))
            }
        }
    )
}

// ==========================================
// PREVIEWS (Wajib: Normal, Kosong, Loading)
// ==========================================

@Preview(name = "GoalsScreen - Normal", showBackground = true)
@Composable
private fun GoalsScreenNormalPreview() {
    ShakeFitTheme {
        GoalsScreen(
            state = GoalsUiState(
                goals = listOf(
                    GoalItem(id = 1, type = ExerciseType.SQUAT, target = 50f, unit = "REPS", active = true, progress = 0.8f),
                    GoalItem(id = 2, type = ExerciseType.PUSHUP, target = 30f, unit = "REPS", active = true, progress = 0.35f),
                    GoalItem(id = 3, type = ExerciseType.RUNNING, target = 5f, unit = "KM", active = false, progress = 0f)
                ),
                isLoading = false,
                error = null
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "GoalsScreen - Kosong", showBackground = true)
@Composable
private fun GoalsScreenEmptyPreview() {
    ShakeFitTheme {
        GoalsScreen(
            state = GoalsUiState(
                goals = emptyList(),
                isLoading = false,
                error = null
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "GoalsScreen - Loading", showBackground = true)
@Composable
private fun GoalsScreenLoadingPreview() {
    ShakeFitTheme {
        GoalsScreen(
            state = GoalsUiState(
                goals = emptyList(),
                isLoading = true,
                error = null
            ),
            onEvent = {}
        )
    }
}
