package com.akusukaproject.siagapadang.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akusukaproject.siagapadang.R
import com.akusukaproject.siagapadang.ui.theme.SiagaCalmBackground
import com.akusukaproject.siagapadang.ui.theme.SiagaLine
import com.akusukaproject.siagapadang.ui.theme.SiagaNavy
import com.akusukaproject.siagapadang.ui.theme.SiagaTextSecondary

val CalmCardShape = RoundedCornerShape(20.dp)

/** Kerangka halaman masa tenang: latar krem, tombol kembali, judul besar, isi dapat digulir. */
@Composable
fun CalmScaffold(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    LightStatusBarIcons()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SiagaCalmBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
    ) {
        BackPill(label = backLabel, onClick = onBack)
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            color = SiagaNavy,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.semantics { heading() },
        )
        subtitle?.let {
            Text(text = it, color = SiagaTextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

/** Halaman berlatar terang memakai ikon status bar gelap; dikembalikan saat halaman ditutup. */
@Composable
fun LightStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = true
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}

@Composable
fun BackPill(label: String, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, SiagaLine),
        shadowElevation = 2.dp,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Icon(painterResource(R.drawable.ic_ms_chevron_left), contentDescription = null, modifier = Modifier.size(24.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = SiagaTextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
    )
}

/** Kotak putih terpisah untuk satu baris daftar. */
@Composable
fun CalmTile(
    iconRes: Int,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    detail: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = CalmCardShape,
        border = BorderStroke(1.dp, SiagaLine),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(CalmCardShape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(PaddingValues(horizontal = 14.dp, vertical = 12.dp)),
        ) {
            IconBadge(iconRes, iconBackground, iconTint)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                detail?.let { Text(it, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SiagaTextSecondary) }
            }
            when {
                trailing != null -> trailing()
                onClick != null -> Icon(
                    painterResource(R.drawable.ic_ms_chevron_right),
                    contentDescription = null,
                    tint = SiagaTextSecondary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
fun IconBadge(iconRes: Int, background: Color, tint: Color, size: Int = 42) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(background),
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.55f).dp))
    }
}
