package com.akusukaproject.siagapadang.ui.onboarding

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akusukaproject.siagapadang.R
import com.akusukaproject.siagapadang.ui.common.CalmCardShape
import com.akusukaproject.siagapadang.ui.common.IconBadge
import com.akusukaproject.siagapadang.ui.common.LightStatusBarIcons
import com.akusukaproject.siagapadang.ui.theme.SiagaCalmBackground
import com.akusukaproject.siagapadang.ui.theme.SiagaLine
import com.akusukaproject.siagapadang.ui.theme.SiagaNavy
import com.akusukaproject.siagapadang.ui.theme.SiagaOnNavyMuted
import com.akusukaproject.siagapadang.ui.theme.SiagaRustDeep
import com.akusukaproject.siagapadang.ui.theme.SiagaSafeGreen
import com.akusukaproject.siagapadang.ui.theme.SiagaTailGray
import com.akusukaproject.siagapadang.ui.theme.SiagaTextSecondary
import com.akusukaproject.siagapadang.ui.theme.SiagaWarning
import com.akusukaproject.siagapadang.widget.EvacuationWidgetProvider

private const val PAGE_COUNT = 4

/**
 * Pengenalan singkat saat aplikasi pertama kali dibuka. Setiap halaman dapat dilewati agar orang
 * yang pertama kali membuka aplikasi saat gempa langsung sampai ke layar evakuasi.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onFinishToFamilyPlan: () -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { page = 3 }
    BackHandler(enabled = page > 0) { page -= 1 }

    when (page) {
        0 -> WelcomePage(onStart = { page = 1 }, onSkip = onFinish)
        1 -> LightPage(page = 1, onSkip = onFinish, primaryLabel = "Lanjut", onPrimary = { page = 2 }) {
            Headline("Saat gempa kuat, cukup buka aplikasi.")
            Body("Satu arahan pada satu waktu. Tidak perlu membaca peta.")
            Spacer(Modifier.height(16.dp))
            Step(R.drawable.ic_ms_bolt, "Buka Siaga Padang", "Dari ikon atau widget di layar utama.", highlight = false)
            Step(R.drawable.ic_ms_turn_left, "Ikuti panah besar", "Panah mengikuti arah hadap HP Anda.", highlight = true)
            Step(R.drawable.ic_ms_directions_walk, "Berjalan cepat ke TES/TEA", "Hitung mundur menunjukkan sisa waktu.", highlight = false)
            Surface(color = Color(0xFFE3F4EA), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                    Icon(painterResource(R.drawable.ic_ms_wifi_off), contentDescription = null, tint = SiagaSafeGreen, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Rute sudah tersimpan di HP. Tetap berjalan dalam mode pesawat.", color = SiagaNavy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        2 -> LightPage(
            page = 2,
            onSkip = null,
            primaryLabel = "Izinkan lokasi",
            primaryIcon = R.drawable.ic_ms_my_location,
            onPrimary = {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            },
            secondaryLabel = "Nanti saja",
            onSecondary = { page = 3 },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFFE7EEF8)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(Color(0x26007BFA)),
                ) {
                    Icon(painterResource(R.drawable.ic_ms_my_location), contentDescription = null, tint = Color(0xFF007BFA), modifier = Modifier.size(54.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Headline("Izinkan lokasi agar arah langsung muncul.")
            Surface(
                color = Color.White,
                shape = CalmCardShape,
                border = BorderStroke(1.dp, SiagaLine),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                    Reason(R.drawable.ic_ms_route, SiagaNavy, "Mencari simpang terdekat dan rute ke TES/TEA.")
                    Reason(R.drawable.ic_ms_map, SiagaNavy, "Menentukan apakah Anda di zona rawan tsunami.")
                    Reason(R.drawable.ic_ms_lock, SiagaSafeGreen, "Lokasi hanya dikirim ke posko saat Anda menekan lapor selamat atau melaporkan kendala.")
                }
            }
        }
        else -> LightPage(page = 3, onSkip = onFinish, primaryLabel = "Selesai", onPrimary = onFinish) {
            Headline("Siapkan sekarang, saat keadaan tenang.")
            Body("Dua langkah ini membuat aplikasi lebih cepat dipakai saat bencana.")
            Spacer(Modifier.height(12.dp))
            val canPinWidget = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                context.getSystemService(AppWidgetManager::class.java)?.isRequestPinAppWidgetSupported == true
            if (canPinWidget) {
                PrepCard(
                    iconRes = R.drawable.ic_ms_widgets,
                    iconBackground = Color(0xFFDDE7FB),
                    iconTint = Color(0xFF2F5FBF),
                    title = "Pasang widget",
                    detail = "Status zona dan tombol evakuasi di layar utama HP.",
                    action = "Tambahkan widget",
                    onClick = {
                        context.getSystemService(AppWidgetManager::class.java)?.requestPinAppWidget(
                            ComponentName(context, EvacuationWidgetProvider::class.java),
                            null,
                            null,
                        )
                    },
                )
            }
            PrepCard(
                iconRes = R.drawable.ic_ms_family_restroom,
                iconBackground = Color(0xFFFBE3D9),
                iconTint = SiagaRustDeep,
                title = "Rencana keluarga",
                detail = "Tentukan titik temu dan TES tiap anggota keluarga.",
                action = "Susun rencana",
                onClick = onFinishToFamilyPlan,
            )
        }
    }
}

@Composable
private fun WelcomePage(onStart: () -> Unit, onSkip: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(SiagaNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        SkipButton(onSkip, color = SiagaOnNavyMuted)
        Spacer(Modifier.weight(1f))
        Image(
            painter = painterResource(R.drawable.siaga_padang_splash_logo),
            contentDescription = "Logo Siaga Padang",
            modifier = Modifier.size(180.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Siaga Padang",
            color = Color.White,
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Arah evakuasi tsunami ke TES dan TEA terdekat, tetap berjalan tanpa internet.",
            color = SiagaOnNavyMuted,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        PageDots(active = 0, onDark = true)
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Mulai", onStart, onDark = true)
    }
}

@Composable
private fun LightPage(
    page: Int,
    onSkip: (() -> Unit)?,
    primaryLabel: String,
    onPrimary: () -> Unit,
    primaryIcon: Int? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    LightStatusBarIcons()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SiagaCalmBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        if (onSkip != null) SkipButton(onSkip, color = SiagaTextSecondary) else Spacer(Modifier.height(48.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            content = content,
        )
        Spacer(Modifier.height(12.dp))
        PageDots(active = page, onDark = false)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(primaryLabel, onPrimary, onDark = false, iconRes = primaryIcon)
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(10.dp))
            Surface(
                color = Color.White,
                contentColor = SiagaNavy,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SiagaLine),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button, onClick = onSecondary),
            ) {
                Box(contentAlignment = Alignment.Center) { Text(secondaryLabel, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun ColumnScope.SkipButton(onClick: () -> Unit, color: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .align(Alignment.End)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Text("Lewati", color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PageDots(active: Int, onDark: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(PAGE_COUNT) { index ->
            Box(
                modifier = Modifier
                    .size(width = if (index == active) 22.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            index == active && onDark -> SiagaWarning
                            index == active -> SiagaNavy
                            onDark -> Color.White.copy(alpha = 0.35f)
                            else -> SiagaNavy.copy(alpha = 0.2f)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit, onDark: Boolean, iconRes: Int? = null) {
    Surface(
        color = SiagaWarning,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape(20.dp),
        border = if (onDark) null else BorderStroke(2.dp, SiagaNavy),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            iconRes?.let {
                Icon(painterResource(it), contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(label, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            if (iconRes == null) {
                Spacer(Modifier.width(8.dp))
                Icon(painterResource(R.drawable.ic_ms_arrow_forward), contentDescription = null, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun Headline(text: String) {
    Text(text, color = SiagaNavy, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp, modifier = Modifier.semantics { heading() })
}

@Composable
private fun Body(text: String) {
    Text(text, color = SiagaTextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun Step(iconRes: Int, title: String, detail: String, highlight: Boolean) {
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = CalmCardShape,
        border = BorderStroke(1.dp, SiagaLine),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            IconBadge(iconRes, if (highlight) SiagaNavy else SiagaTailGray, if (highlight) Color.White else SiagaNavy, size = 52)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(detail, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SiagaTextSecondary)
            }
        }
    }
}

@Composable
private fun Reason(iconRes: Int, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, color = SiagaNavy, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PrepCard(
    iconRes: Int,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    detail: String,
    action: String,
    onClick: () -> Unit,
) {
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = CalmCardShape,
        border = BorderStroke(1.dp, SiagaLine),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(iconRes, iconBackground, iconTint, size = 48)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text(detail, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SiagaTextSecondary)
                }
            }
            Surface(
                color = Color(0xFFEEF2F7),
                contentColor = SiagaNavy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button, onClick = onClick),
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(action, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Icon(painterResource(R.drawable.ic_ms_chevron_right), contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
