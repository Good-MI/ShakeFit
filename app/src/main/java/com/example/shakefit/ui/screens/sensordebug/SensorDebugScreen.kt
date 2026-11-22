package com.example.shakefit.ui.screens.sensordebug

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shakefit.model.GpsStatus
import com.example.shakefit.model.SensorDebugUiState
import java.util.Locale

/**
 * Route wrapper tipis untuk SensorDebugScreen.
 * Memisahkan pemanggilan ViewModel dari UI murni.
 */
@Composable
fun SensorDebugRoute(
    viewModel: SensorDebugViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gpsState by viewModel.gpsDebugState.collectAsStateWithLifecycle()

    // Listener otomatis aktif saat layar masuk, dan wajib dilepas saat layar keluar
    DisposableEffect(viewModel) {
        viewModel.startSensorMonitoring()
        onDispose {
            viewModel.stopSensorMonitoring()
            viewModel.stopGpsTest()
        }
    }

    SensorDebugScreen(
        uiState = uiState,
        gpsState = gpsState,
        onStartGpsTest = { viewModel.startGpsTest() },
        onStopGpsTest = { viewModel.stopGpsTest() },
        onPermissionResult = { viewModel.onPermissionResult(it) },
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

/**
 * Layar uji sensor mentah (Accelerometer, Gyroscope, Proximity) dan GPS.
 * UI murni stateless.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDebugScreen(
    uiState: SensorDebugUiState,
    gpsState: GpsDebugUiState,
    onStartGpsTest: () -> Unit,
    onStopGpsTest: () -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Launcher izin lokasi Just-in-Time saat tombol uji GPS ditekan
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        onPermissionResult(granted)
        if (granted) {
            onStartGpsTest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Uji Sensor & GPS (Debug)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Target Uji: Infinix Hot 70 (Sensor Asli)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            // --- 1. ACCELEROMETER ---
            SensorCard(
                title = "1. Accelerometer (Motion)",
                isAvailable = uiState.accelAvailable,
                unavailableReason = "Sensor Accelerometer tidak terdeteksi di perangkat ini."
            ) {
                if (uiState.accel != null) {
                    val (x, y, z) = uiState.accel
                    RawMetricRow("X (m/s²)", String.format(Locale.US, "%.3f", x))
                    RawMetricRow("Y (m/s²)", String.format(Locale.US, "%.3f", y))
                    RawMetricRow("Z (m/s²)", String.format(Locale.US, "%.3f", z))
                } else {
                    Text("Menunggu pembacaan data...", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
            }

            // --- 2. GYROSCOPE ---
            SensorCard(
                title = "2. Gyroscope (Motion)",
                isAvailable = uiState.gyroAvailable,
                unavailableReason = "Sensor Gyroscope tidak terdeteksi (opsional, fallback aktif)."
            ) {
                if (uiState.gyro != null) {
                    val (x, y, z) = uiState.gyro
                    RawMetricRow("X (rad/s)", String.format(Locale.US, "%.3f", x))
                    RawMetricRow("Y (rad/s)", String.format(Locale.US, "%.3f", y))
                    RawMetricRow("Z (rad/s)", String.format(Locale.US, "%.3f", z))
                } else {
                    Text("Menunggu pembacaan data...", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
            }

            // --- 3. PROXIMITY ---
            SensorCard(
                title = "3. Proximity (Position - Push-up)",
                isAvailable = uiState.proximityAvailable,
                unavailableReason = "Sensor Proximity tidak terdeteksi di perangkat ini."
            ) {
                RawMetricRow(
                    "Maks. Range",
                    uiState.proximityMaxRange?.let { String.format(Locale.US, "%.1f cm", it) } ?: "--"
                )
                RawMetricRow(
                    "Jarak Terbaca",
                    uiState.proximityValue?.let { String.format(Locale.US, "%.1f cm", it) } ?: "--"
                )

                Spacer(modifier = Modifier.height(4.dp))
                // Indikator visual dekat / jauh
                val isNear = uiState.proximityValue != null &&
                        uiState.proximityMaxRange != null &&
                        uiState.proximityValue < uiState.proximityMaxRange

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isNear) Color(0xFF2E7D32) else Color(0xFF616161),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isNear) "KONDISI: DEKAT (Dada Menyentuh Lantai)" else "KONDISI: JAUH (Badan Di Atas)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // --- 4. GPS / LOCATION ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "4. GPS (LocationManager)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        GpsStatusChip(status = gpsState.status)
                    }

                    if (gpsState.message != null) {
                        Text(
                            text = gpsState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (gpsState.rawData != null) {
                        val d = gpsState.rawData
                        RawMetricRow("Latitude", String.format(Locale.US, "%.6f", d.latitude))
                        RawMetricRow("Longitude", String.format(Locale.US, "%.6f", d.longitude))
                        RawMetricRow("Altitude", String.format(Locale.US, "%.1f m", d.altitude))
                        RawMetricRow("Akurasi", String.format(Locale.US, "%.1f m", d.accuracyMeters))
                        RawMetricRow("Kecepatan", String.format(Locale.US, "%.2f m/s", d.speedMs))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!gpsState.isTesting) {
                            Button(
                                onClick = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Mulai Uji GPS")
                            }
                        } else {
                            Button(
                                onClick = onStopGpsTest,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Hentikan Uji GPS")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SensorCard(
    title: String,
    isAvailable: Boolean,
    unavailableReason: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isAvailable) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isAvailable) "Tersedia" else "Tidak Ada",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!isAvailable) {
                Text(
                    text = unavailableReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                content()
            }
        }
    }
}

@Composable
private fun RawMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun GpsStatusChip(status: GpsStatus) {
    val (bgColor, textColor, label) = when (status) {
        GpsStatus.READY -> Triple(Color(0xFF2E7D32), Color.White, "READY")
        GpsStatus.SEARCHING -> Triple(Color(0xFFED6C02), Color.White, "SEARCHING")
        GpsStatus.WEAK -> Triple(Color(0xFFFFA726), Color.Black, "WEAK")
        GpsStatus.DISABLED -> Triple(Color(0xFFD32F2F), Color.White, "DISABLED")
        GpsStatus.PERMISSION_DENIED -> Triple(Color(0xFFD32F2F), Color.White, "NO PERM")
        GpsStatus.OFF -> Triple(Color(0xFF757575), Color.White, "OFF")
    }

    Box(
        modifier = Modifier
            .background(color = bgColor, shape = RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
