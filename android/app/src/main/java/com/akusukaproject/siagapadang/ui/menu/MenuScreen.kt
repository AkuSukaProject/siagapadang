package com.akusukaproject.siagapadang.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akusukaproject.siagapadang.R
import com.akusukaproject.siagapadang.ui.common.CalmCardShape
import com.akusukaproject.siagapadang.ui.common.CalmScaffold
import com.akusukaproject.siagapadang.ui.common.IconBadge
import com.akusukaproject.siagapadang.ui.evacuation.DataUpdateDialog
import com.akusukaproject.siagapadang.ui.evacuation.EvacuationViewModel
import com.akusukaproject.siagapadang.ui.theme.SiagaLine
import com.akusukaproject.siagapadang.ui.theme.SiagaNavy
import com.akusukaproject.siagapadang.ui.theme.SiagaOnNavyMuted
import com.akusukaproject.siagapadang.ui.theme.SiagaRustDeep
import com.akusukaproject.siagapadang.ui.theme.SiagaSafeGreen
import com.akusukaproject.siagapadang.ui.theme.SiagaTextSecondary
import com.akusukaproject.siagapadang.ui.theme.SiagaWarning
import kotlin.math.ceil

private const val WALKING_SPEED_METERS_PER_SECOND = 1.2

/** Dashboard persiapan masa tenang. Layar evakuasi tetap menjadi layar pertama saat aplikasi dibuka. */
@Composable
fun MenuScreen(
    onBack: () -> Unit,
    onOpenFamilyPlan: () -> Unit,
    evacuationViewModel: EvacuationViewModel = viewModel(),
) {
    val state by evacuationViewModel.uiState.collectAsStateWithLifecycle()
    var showDataDialog by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    CalmScaffold(
        title = "Siaga Padang",
        subtitle = "Siapkan sekarang, saat keadaan tenang.",
        backLabel = "Evakuasi",
        onBack = onBack,
    ) {
        val route = state.route
        Surface(
            color = SiagaNavy,
            contentColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_ms_location_on), contentDescription = null, tint = SiagaWarning, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("TES TUJUAN SAAT INI", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                Text(
                    text = route?.destinationName ?: "Menunggu posisi GPS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = if (route != null) {
                        // Sama dengan kartu waktu di layar evakuasi: dari sisa jarak, bukan ETA rute penuh.
                        val remainingMeters = state.guidance?.remainingDistanceMeters
                            ?: (route.estimatedSeconds * WALKING_SPEED_METERS_PER_SECOND).toInt()
                        val minutes = ceil(remainingMeters / WALKING_SPEED_METERS_PER_SECOND / 60.0)
                            .toInt()
                            .coerceAtLeast(1)
                        "Dari posisi Anda · ±$minutes menit berjalan cepat"
                    } else {
                        "Tujuan muncul setelah lokasi diketahui."
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = SiagaOnNavyMuted,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            DashboardTile(
                iconRes = R.drawable.ic_ms_family_restroom,
                iconBackground = Color(0xFFFBE3D9),
                iconTint = SiagaRustDeep,
                title = "Rencana keluarga",
                value = state.familyMeetingPointName?.let { "Titik temu siap" } ?: "Belum disusun",
                detail = state.familyMeetingPointName ?: "Tentukan titik temu",
                onClick = onOpenFamilyPlan,
                modifier = Modifier.weight(1f),
            )
            DashboardTile(
                iconRes = R.drawable.ic_ms_offline_pin,
                iconBackground = Color(0xFFDDF1E5),
                iconTint = SiagaSafeGreen,
                title = "Data wilayah",
                value = "Kota Padang",
                detail = state.localDatasetManifest?.let { "Versi ${it.version} · luring" } ?: "Tersimpan di HP",
                onClick = {
                    showDataDialog = true
                    evacuationViewModel.checkDataUpdates()
                },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showDataDialog) {
        DataUpdateDialog(
            state = state,
            onDismiss = { showDataDialog = false },
            onCheckUpdates = evacuationViewModel::checkDataUpdates,
            onInstallUpdate = evacuationViewModel::installDataUpdate,
        )
    }
}

@Composable
private fun DashboardTile(
    iconRes: Int,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    value: String,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = CalmCardShape,
        border = BorderStroke(1.dp, SiagaLine),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxHeight()
            .clip(CalmCardShape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            IconBadge(iconRes, iconBackground, iconTint, size = 40)
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SiagaTextSecondary)
        }
    }
}
