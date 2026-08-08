package com.akusukaproject.siagapadang.ui.evacuation

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akusukaproject.siagapadang.ui.map.OfflineMap

@Composable
fun EvacuationScreen(
    viewModel: EvacuationViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        viewModel.onLocationPermissionChanged(
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true,
        )
    }

    fun requestLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    LaunchedEffect(Unit) {
        val granted = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onLocationPermissionChanged(true)
        } else {
            requestLocationPermission()
        }
    }

    EvacuationContent(
        state = state,
        onRequestLocationPermission = ::requestLocationPermission,
        onRetryRoute = viewModel::retryRoute,
        onSelectAlternative = viewModel::selectAlternativeDestination,
    )
}

@Composable
private fun EvacuationContent(
    state: EvacuationUiState,
    onRequestLocationPermission: () -> Unit,
    onRetryRoute: () -> Unit,
    onSelectAlternative: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        OfflineMap(
            routeCoordinates = state.route?.coordinates.orEmpty(),
            currentLocation = state.currentLocation,
            modifier = Modifier.fillMaxSize(),
        )

        Surface(
            color = Color(0xE6102931),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp),
        ) {
            Text(
                text = "Peta dasar daring · data rute luring",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }

        when {
            state.route != null -> Text(
                text = "↑",
                color = Color(0xFFFFC857),
                fontSize = 112.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer(rotationZ = state.arrowRotationDegrees)
                    .semantics { contentDescription = "Arah evakuasi" },
            )
            state.isLoadingRoute -> CircularProgressIndicator(
                color = Color(0xFFFFC857),
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Surface(
            color = Color(0xF20B252D),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            ) {
                StatusInstruction(state)

                state.route?.let { route ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TimeCard(
                            label = "Sisa waktu",
                            value = formatDuration(state.remainingEvacuationSeconds),
                            modifier = Modifier.weight(1f),
                        )
                        TimeCard(
                            label = "Estimasi tempuh",
                            value = formatDuration(route.estimatedSeconds),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                when {
                    !state.hasLocationPermission -> ActionButton(
                        text = "Izinkan lokasi",
                        onClick = onRequestLocationPermission,
                    )
                    state.errorMessage != null -> ActionButton(
                        text = "Coba lagi",
                        onClick = onRetryRoute,
                    )
                    state.canSelectAlternative -> ActionButton(
                        text = "Gunakan alternatif tujuan",
                        onClick = onSelectAlternative,
                    )
                }

                state.compassMessage?.let { message ->
                    Text(
                        text = message,
                        color = Color(0xFFFFD8A8),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusInstruction(state: EvacuationUiState) {
    val title: String
    val detail: String
    when {
        !state.hasLocationPermission -> {
            title = "Lokasi belum diizinkan"
            detail = "Izinkan lokasi agar aplikasi dapat mencari rute terdekat."
        }
        state.errorMessage != null -> {
            title = "Arahan belum tersedia"
            detail = state.errorMessage
        }
        state.route != null -> {
            title = state.route.destinationName
            detail = "Berjalan cepat mengikuti panah menuju TES."
        }
        state.currentLocation == null -> {
            title = "Mencari lokasi…"
            detail = "Pastikan GPS perangkat aktif."
        }
        else -> {
            title = "Menyiapkan arahan…"
            detail = "Rute dibaca dari data luring."
        }
    }

    Text(
        text = title,
        color = Color.White,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = detail,
        color = Color(0xFFD7E4E8),
        style = MaterialTheme.typography.bodyLarge,
    )
    state.locationAccuracyMeters?.let { accuracy ->
        Text(
            text = "Akurasi GPS ±${accuracy.toInt()} m",
            color = Color(0xFFAFC4CA),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TimeCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF163842),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                color = Color(0xFFAFC4CA),
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFC857),
            contentColor = Color(0xFF152025),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds.coerceAtLeast(0) / 60
    val seconds = totalSeconds.coerceAtLeast(0) % 60
    return "%02d:%02d".format(minutes, seconds)
}
