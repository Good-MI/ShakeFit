package com.example.shakefit.ui.screens.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shakefit.R
import com.example.shakefit.model.Gender
import com.example.shakefit.model.ProfileEvent
import com.example.shakefit.model.ProfileUiState
import com.example.shakefit.ui.theme.ShakeFitTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Route tipis ProfileScreen: menangani izin notifikasi, snackbar, dan hoisting ProfileViewModel.
 */
@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.snackbar_profile_saved)

    // Launcher izin notifikasi just-in-time (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // Hasil dipantau via state viewmodel saat reload
    }

    // Tampilkan Snackbar saat profil berhasil disimpan
    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.onEvent(ProfileEvent.ConsumeSaved)
            snackbarHostState.currentSnackbarData?.dismiss()
            try {
                snackbarHostState.showSnackbar(savedMessage)
            } catch (_: Exception) {
                // Ignore cancellation
            }
        }
    }

    ProfileScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onOpenSettings = onOpenSettings,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

/**
 * ProfileScreen murni stateless (DesignShakefit.md 7.8).
 * Menampilkan header pengguna, izin notifikasi ulang tahun, dan formulir data fisik.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    // State lokal untuk form input (diinisialisasi dari state backend)
    var nicknameInput by remember(state.nickname) { mutableStateOf(state.nickname) }
    var selectedGender by remember(state.gender) { mutableStateOf(state.gender ?: Gender.MALE) }
    var heightInput by remember(state.heightCm) {
        mutableStateOf(state.heightCm?.let { String.format(Locale.US, "%.0f", it) } ?: "")
    }
    var weightInput by remember(state.weightKg) {
        mutableStateOf(state.weightKg?.let { String.format(Locale.US, "%.1f", it) } ?: "")
    }
    var birthDateInput by remember(state.birthDateIso) { mutableStateOf(state.birthDateIso ?: "") }

    var showDatePickerDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.btn_open_settings)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Banner Izin Notifikasi Ulang Tahun (jika dibutuhkan)
            if (state.notificationPermissionNeeded) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.profile_notif_banner_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Text(
                            text = stringResource(R.string.profile_notif_banner_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )

                        Button(
                            onClick = onRequestNotificationPermission,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = stringResource(R.string.btn_allow_notification),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 2. Header Profil Pengguna
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar Inisial
                    val initial = if (state.nickname.isNotBlank()) {
                        state.nickname.trim().take(1).uppercase()
                    } else {
                        "?"
                    }

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.nickname.isNotBlank()) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (state.nickname.isNotBlank()) {
                                state.nickname
                            } else {
                                stringResource(R.string.profile_header_unnamed)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Subtitle Usia & Gender
                        val ageText = state.ageYears?.let {
                            stringResource(R.string.profile_age_format, it)
                        }
                        val genderText = when (state.gender) {
                            Gender.MALE -> stringResource(R.string.gender_male)
                            Gender.FEMALE -> stringResource(R.string.gender_female)
                            null -> null
                        }

                        val infoSub = listOfNotNull(genderText, ageText).joinToString(" • ")
                        if (infoSub.isNotBlank()) {
                            Text(
                                text = infoSub,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (state.isProfileComplete) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = if (state.isProfileComplete) {
                                    stringResource(R.string.profile_complete_badge)
                                } else {
                                    stringResource(R.string.profile_incomplete_badge)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isProfileComplete) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // 3. Catatan Ramah Jika Profil Belum Lengkap (PRD F-01)
            if (!state.isProfileComplete) {
                OutlinedCard(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.profile_incomplete_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Pesan Error Umum jika ada
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

            // 4. Formulir Data Profil
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Field Nickname
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = nicknameInput,
                            onValueChange = { nicknameInput = it },
                            label = { Text(text = stringResource(R.string.label_nickname)) },
                            placeholder = { Text(text = stringResource(R.string.hint_nickname)) },
                            isError = state.fieldErrors.containsKey("nickname"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        state.fieldErrors["nickname"]?.let { errMsg ->
                            Text(
                                text = errMsg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    // Field Gender
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.label_gender),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = selectedGender == Gender.MALE,
                                onClick = { selectedGender = Gender.MALE },
                                label = { Text(text = stringResource(R.string.gender_male)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedGender == Gender.FEMALE,
                                onClick = { selectedGender = Gender.FEMALE },
                                label = { Text(text = stringResource(R.string.gender_female)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Field Tinggi Badan (cm)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = { heightInput = it },
                            label = { Text(text = "${stringResource(R.string.label_height)} (cm)") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            isError = state.fieldErrors.containsKey("height"),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        state.fieldErrors["height"]?.let { errMsg ->
                            Text(
                                text = errMsg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    // Field Berat Badan (kg)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text(text = "${stringResource(R.string.label_weight)} (kg)") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            isError = state.fieldErrors.containsKey("weight"),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        state.fieldErrors["weight"]?.let { errMsg ->
                            Text(
                                text = errMsg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    // Field Tanggal Lahir (yyyy-MM-dd)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = birthDateInput,
                            onValueChange = { birthDateInput = it },
                            label = { Text(text = stringResource(R.string.label_birth_date)) },
                            placeholder = { Text(text = stringResource(R.string.hint_birth_date)) },
                            trailingIcon = {
                                IconButton(onClick = { showDatePickerDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.DateRange,
                                        contentDescription = null
                                    )
                                }
                            },
                            isError = state.fieldErrors.containsKey("birthDate"),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        state.fieldErrors["birthDate"]?.let { errMsg ->
                            Text(
                                text = errMsg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tombol Simpan Profil
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val heightVal = heightInput.toFloatOrNull() ?: 0f
                            val weightVal = weightInput.toFloatOrNull() ?: 0f
                            onEvent(
                                ProfileEvent.Save(
                                    nickname = nicknameInput.trim(),
                                    gender = selectedGender,
                                    heightCm = heightVal,
                                    weightKg = weightVal,
                                    birthDateIso = birthDateInput.trim()
                                )
                            )
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_save_profile),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // DatePickerDialog Material 3
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            birthDateInput = formatter.format(Date(millis))
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text(text = stringResource(R.string.btn_save), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(text = stringResource(R.string.btn_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ==========================================
// PREVIEWS (Wajib: Lengkap, Kosong, Error)
// ==========================================

@Preview(name = "ProfileScreen - Profil Lengkap", showBackground = true)
@Composable
private fun ProfileScreenCompletePreview() {
    ShakeFitTheme {
        ProfileScreen(
            state = ProfileUiState(
                nickname = "Budi Santoso",
                gender = Gender.MALE,
                heightCm = 175f,
                weightKg = 70f,
                birthDateIso = "1998-05-12",
                ageYears = 28,
                isProfileComplete = true,
                fieldErrors = emptyMap(),
                saved = false,
                notificationPermissionNeeded = false,
                error = null
            ),
            onEvent = {},
            onRequestNotificationPermission = {},
            onOpenSettings = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "ProfileScreen - Profil Kosong", showBackground = true)
@Composable
private fun ProfileScreenEmptyPreview() {
    ShakeFitTheme {
        ProfileScreen(
            state = ProfileUiState(
                nickname = "",
                gender = null,
                heightCm = null,
                weightKg = null,
                birthDateIso = null,
                ageYears = null,
                isProfileComplete = false,
                fieldErrors = emptyMap(),
                saved = false,
                notificationPermissionNeeded = true,
                error = null
            ),
            onEvent = {},
            onRequestNotificationPermission = {},
            onOpenSettings = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "ProfileScreen - Validation Errors", showBackground = true)
@Composable
private fun ProfileScreenErrorsPreview() {
    ShakeFitTheme {
        ProfileScreen(
            state = ProfileUiState(
                nickname = "Nama Terlalu Panjang Sekali Melebihi Tiga Puluh Karakter",
                gender = Gender.FEMALE,
                heightCm = 300f,
                weightKg = 10f,
                birthDateIso = "2099-01-01",
                ageYears = null,
                isProfileComplete = false,
                fieldErrors = mapOf(
                    "nickname" to "Nama panggilan maksimal 30 karakter",
                    "height" to "Tinggi badan harus antara 50 - 250 cm",
                    "weight" to "Berat badan harus antara 20 - 300 kg",
                    "birthDate" to "Format tanggal lahir salah atau berada di masa depan"
                ),
                saved = false,
                notificationPermissionNeeded = false,
                error = null
            ),
            onEvent = {},
            onRequestNotificationPermission = {},
            onOpenSettings = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
