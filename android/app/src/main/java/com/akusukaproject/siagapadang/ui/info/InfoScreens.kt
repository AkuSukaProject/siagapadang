package com.akusukaproject.siagapadang.ui.info

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akusukaproject.siagapadang.BuildConfig
import com.akusukaproject.siagapadang.R
import com.akusukaproject.siagapadang.SiagaPadangApplication
import com.akusukaproject.siagapadang.ui.common.CalmCardShape
import com.akusukaproject.siagapadang.ui.common.CalmScaffold
import com.akusukaproject.siagapadang.ui.common.CalmTile
import com.akusukaproject.siagapadang.ui.common.SectionLabel
import com.akusukaproject.siagapadang.ui.theme.SiagaLine
import com.akusukaproject.siagapadang.ui.theme.SiagaNavy
import com.akusukaproject.siagapadang.ui.theme.SiagaSafeGreen
import com.akusukaproject.siagapadang.ui.theme.SiagaTailGray
import com.akusukaproject.siagapadang.ui.theme.SiagaTextSecondary
import com.akusukaproject.siagapadang.ui.theme.SiagaWarning

private val InfoBlue = Color(0xFFDDE7FB)
private val InfoBlueText = Color(0xFF2F5FBF)

@Composable
fun GuideScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    CalmScaffold(
        title = "Panduan evakuasi",
        subtitle = "Baca sekarang. Dapat dibuka tanpa internet.",
        backLabel = "Menu",
        onBack = onBack,
    ) {
        Surface(
            color = Color(0xFFFFF6C2),
            contentColor = SiagaNavy,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFC9A800)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Icon(painterResource(R.drawable.ic_ms_edit_note), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Draf · teks menunggu verifikasi BPBD Kota Padang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.padding(top = 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                Triple("1", "Saat guncangan", "Lindungi kepala dan jauhi kaca atau benda yang bisa jatuh."),
                Triple("2", "Setelah guncangan berhenti", "Buka Siaga Padang dan ikuti arah ke TES atau TEA."),
                Triple("3", "Berjalan cepat", "Jangan kembali untuk menjemput. Setiap anggota keluarga menuju TES masing-masing."),
                Triple("4", "Ikuti petugas dan rambu", "Jika jalan terhalang, tekan \"Ada kendala?\" di layar evakuasi."),
                Triple("5", "Tetap di TES atau TEA", "Tunggu sampai petugas menyatakan aman. Pantau informasi resmi BMKG."),
            ).forEach { (number, title, detail) -> GuideStep(number, title, detail, highlight = number == "3") }
        }
    }
}

@Composable
private fun GuideStep(number: String, title: String, detail: String, highlight: Boolean) {
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = CalmCardShape,
        border = BorderStroke(1.dp, SiagaLine),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (highlight) SiagaWarning else SiagaNavy),
            ) {
                Text(number, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (highlight) SiagaNavy else Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(detail, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SiagaTextSecondary)
            }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenAbout: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val settings = remember { (context.applicationContext as SiagaPadangApplication).settingsRepository }
    var vibrate by remember { mutableStateOf(settings.vibrateBeforeTurn) }
    val locationGranted = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    CalmScaffold(title = "Pengaturan", backLabel = "Menu", onBack = onBack) {
        SectionLabel("Navigasi")
        CalmTile(
            iconRes = R.drawable.ic_ms_vibration,
            iconBackground = InfoBlue,
            iconTint = InfoBlueText,
            title = "Getar sebelum belokan",
            detail = "Sekitar 30 m sebelum titik belok",
            trailing = {
                Switch(
                    checked = vibrate,
                    onCheckedChange = {
                        vibrate = it
                        settings.vibrateBeforeTurn = it
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = SiagaSafeGreen),
                )
            },
        )
        Spacer(Modifier.padding(top = 10.dp))
        CalmTile(
            iconRes = R.drawable.ic_ms_my_location,
            iconBackground = Color(0xFFDDF1E5),
            iconTint = SiagaSafeGreen,
            title = "Izin lokasi",
            detail = if (locationGranted) "Diizinkan · presisi tinggi" else "Belum diizinkan · arah tidak dapat ditentukan",
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            },
        )
        SectionLabel("Tentang")
        CalmTile(
            iconRes = R.drawable.ic_ms_info,
            iconBackground = SiagaTailGray,
            iconTint = SiagaTextSecondary,
            title = "Tentang & sumber data",
            detail = "Versi ${BuildConfig.VERSION_NAME}",
            onClick = onOpenAbout,
        )
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    CalmScaffold(title = "Tentang", backLabel = "Kembali", onBack = onBack) {
        Text(
            "Siaga Padang menunjukkan arah evakuasi tsunami ke TES dan TEA terdekat. Rute dihitung sebelumnya dan disimpan di HP sehingga tetap berjalan tanpa internet.",
            color = SiagaNavy,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        SectionLabel("Sumber data")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CalmTile(R.drawable.ic_ms_location_on, SiagaWarning, SiagaNavy, "TES dan kapasitas rancangan", "BPBD Kota Padang")
            CalmTile(R.drawable.ic_ms_map, SiagaTailGray, SiagaTextSecondary, "Jaringan jalan dan peta", "© Kontributor OpenStreetMap (ODbL)")
            CalmTile(R.drawable.ic_ms_warning, SiagaTailGray, SiagaTextSecondary, "Informasi gempa", "BMKG, melalui server posko (butuh internet)")
            CalmTile(R.drawable.ic_ms_route, SiagaTailGray, SiagaTextSecondary, "Rute evakuasi", "Prakomputasi tim Siaga Padang. TES berbatas kapasitas, TEA terdekat")
        }
        SectionLabel("Komponen & lisensi")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CalmTile(R.drawable.ic_ms_gavel, SiagaTailGray, SiagaTextSecondary, "MapLibre Native Android", "BSD 2-Clause")
            CalmTile(R.drawable.ic_ms_gavel, SiagaTailGray, SiagaTextSecondary, "Jetpack Compose, Room, Coroutines", "Apache License 2.0")
            CalmTile(R.drawable.ic_ms_gavel, SiagaTailGray, SiagaTextSecondary, "Huruf Inter", "SIL Open Font License 1.1")
            CalmTile(R.drawable.ic_ms_gavel, SiagaTailGray, SiagaTextSecondary, "Ikon Material Symbols", "Apache License 2.0")
        }
        Spacer(Modifier.padding(top = 12.dp))
        Text("Versi ${BuildConfig.VERSION_NAME}", color = SiagaTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
