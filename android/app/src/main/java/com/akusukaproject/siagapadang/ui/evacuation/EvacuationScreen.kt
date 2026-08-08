package com.akusukaproject.siagapadang.ui.evacuation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akusukaproject.siagapadang.R
import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import com.akusukaproject.siagapadang.domain.ManeuverGuidance
import com.akusukaproject.siagapadang.domain.ManeuverType
import com.akusukaproject.siagapadang.domain.RouteGuidanceSnapshot
import com.akusukaproject.siagapadang.ui.map.OfflineMap
import com.akusukaproject.siagapadang.ui.theme.SiagaCream
import com.akusukaproject.siagapadang.ui.theme.SiagaNavy
import com.akusukaproject.siagapadang.ui.theme.SiagaNextGreen
import com.akusukaproject.siagapadang.ui.theme.SiagaRust
import com.akusukaproject.siagapadang.ui.theme.SiagaWarning
import kotlinx.coroutines.launch
import kotlin.math.ceil

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
    var showBlockedRouteDialog by rememberSaveable { mutableStateOf(false) }
    val mapPanelState = remember { AnchoredDraggableState(MapPanelValue.COLLAPSED) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SiagaNavy)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val scale = (maxWidth.value / FIGMA_WIDTH_DP).coerceIn(0.82f, 1.25f)
        fun scaled(value: Float): Dp = (value * scale).dp

        val collapsedMapHeight = scaled(FIGMA_MAP_HEIGHT_DP).coerceAtMost(maxHeight * 0.40f)
        val expandedMapHeight = maxHeight
        val dragRangePx = with(density) {
            (expandedMapHeight - collapsedMapHeight).toPx().coerceAtLeast(1f)
        }
        LaunchedEffect(dragRangePx) {
            mapPanelState.updateAnchors(
                DraggableAnchors {
                    MapPanelValue.COLLAPSED at 0f
                    MapPanelValue.EXPANDED at -dragRangePx
                },
            )
        }
        val panelOffset = mapPanelState.offset.takeUnless(Float::isNaN) ?: 0f
        val expansionProgress = (-panelOffset / dragRangePx).coerceIn(0f, 1f)
        val mapHeight = lerp(collapsedMapHeight, expandedMapHeight, expansionProgress)
        val collapsedContentAlpha = (1f - expansionProgress * 1.7f).coerceIn(0f, 1f)

        BackHandler(enabled = expansionProgress > 0.01f) {
            coroutineScope.launch {
                mapPanelState.animateTo(
                    targetValue = MapPanelValue.COLLAPSED,
                    animationSpec = MAP_PANEL_SPRING,
                )
            }
        }

        EvacuationMapPanel(
            state = state,
            mapHeight = mapHeight,
            scale = scale,
            expansionProgress = expansionProgress,
            onBlockedRouteClick = { showBlockedRouteDialog = true },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        val handleTop = maxHeight - mapHeight +
            lerp((7f * scale).dp, (103f * scale).dp, expansionProgress)
        MapPanelHandle(
            scale = scale,
            expansionProgress = expansionProgress,
            dragState = mapPanelState,
            onClick = {
                coroutineScope.launch {
                    val target = if (expansionProgress >= 0.5f) {
                        MapPanelValue.COLLAPSED
                    } else {
                        MapPanelValue.EXPANDED
                    }
                    mapPanelState.animateTo(target, MAP_PANEL_SPRING)
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = handleTop)
                .zIndex(20f),
        )

        OfflineStatusBadge(
            scale = scale,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = scaled(8f))
                .graphicsLayer(alpha = collapsedContentAlpha),
        )

        val route = state.route
        if (route != null) {
            NavigationInstructionCard(
                route = route,
                guidance = state.guidance,
                scale = scale,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = scaled(53f))
                    .graphicsLayer(
                        alpha = collapsedContentAlpha,
                        translationY = -expansionProgress * with(density) { scaled(45f).toPx() },
                    ),
            )

            EvacuationTiming(
                route = route,
                remainingSeconds = state.remainingEvacuationSeconds,
                compassMessage = state.compassMessage,
                scale = scale,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = scaled(304f))
                    .graphicsLayer(alpha = collapsedContentAlpha),
            )

            NextInstructionStrip(
                guidance = state.guidance,
                scale = scale,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -(mapHeight + scaled(22f)))
                    .graphicsLayer(alpha = collapsedContentAlpha),
            )
        } else {
            RoutePreparationState(
                state = state,
                onRequestLocationPermission = onRequestLocationPermission,
                onRetryRoute = onRetryRoute,
                scale = scale,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = scaled(75f)),
            )
        }
    }

    if (showBlockedRouteDialog) {
        BlockedRouteDialog(
            onDismiss = { showBlockedRouteDialog = false },
            onConfirm = {
                showBlockedRouteDialog = false
                onSelectAlternative()
            },
        )
    }
}

@Composable
private fun OfflineStatusBadge(scale: Float, modifier: Modifier = Modifier) {
    Surface(
        color = Color.Transparent,
        contentColor = SiagaCream,
        shape = CircleShape,
        border = BorderStroke(1.dp, SiagaRust),
        modifier = modifier.height((29f * scale).dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy((8f * scale).dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = (11f * scale).dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_figma_offline),
                contentDescription = null,
                modifier = Modifier.size((15f * scale).dp),
            )
            Text(
                text = "Data rute luring",
                color = SiagaCream,
                fontSize = (11f * scale).sp,
            )
        }
    }
}

@Composable
private fun NavigationInstructionCard(
    route: EvacuationRoute,
    guidance: RouteGuidanceSnapshot?,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val instruction = guidance?.currentInstruction ?: ManeuverGuidance(
        type = ManeuverType.STRAIGHT,
        distanceMeters = estimatedDistanceMeters(route),
    )
    val presentation = maneuverPresentation(instruction.type)
    val shape = RoundedCornerShape((18f * scale).dp)
    Surface(
        color = SiagaCream,
        contentColor = SiagaNavy,
        shape = shape,
        border = BorderStroke(1.dp, Color(0xFFB9B9B9)),
        modifier = modifier
            .size(width = (213f * scale).dp, height = (239f * scale).dp)
            .shadow(4.dp, shape),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = (10f * scale).dp),
        ) {
            Image(
                painter = painterResource(presentation.drawableRes),
                contentDescription = presentation.label,
                colorFilter = if (presentation.tint) ColorFilter.tint(SiagaNavy) else null,
                modifier = Modifier
                    .size((126f * scale).dp)
                    .graphicsLayer(
                        rotationZ = presentation.assetRotationDegrees,
                        scaleX = if (presentation.mirrorHorizontally) -1f else 1f,
                    ),
            )
            Text(
                text = presentation.label,
                color = SiagaNavy,
                fontSize = (24f * scale).sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height((12f * scale).dp))
            Text(
                text = if (instruction.type == ManeuverType.ARRIVE) {
                    "Tujuan di depan"
                } else {
                    "${formatDistance(instruction.distanceMeters)} lagi"
                },
                color = SiagaNavy,
                fontSize = (16f * scale).sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = route.destinationName,
                color = SiagaNavy,
                fontSize = (20f * scale).sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (21f * scale).sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EvacuationTiming(
    route: EvacuationRoute,
    remainingSeconds: Int,
    compassMessage: String?,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = "Berjalan cepat ±${estimatedMinutes(route)} menit",
            color = SiagaCream,
            fontSize = (15f * scale).sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height((20f * scale).dp))
        CountdownCard(
            remainingSeconds = remainingSeconds,
            scale = scale,
        )
        compassMessage?.let { message ->
            Spacer(modifier = Modifier.height((4f * scale).dp))
            Text(
                text = message,
                color = Color(0xFFFFD8A8),
                fontSize = (10f * scale).sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CountdownCard(remainingSeconds: Int, scale: Float) {
    Box(
        contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.size(width = (150f * scale).dp, height = (73f * scale).dp),
    ) {
        Surface(
            color = Color.Transparent,
            border = BorderStroke(1.dp, Color(0xFFB9B9B9)),
            shape = RoundedCornerShape((9f * scale).dp),
            modifier = Modifier
                .width((144f * scale).dp)
                .height((61f * scale).dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = formatDuration(remainingSeconds, spaced = true),
                    color = Color.White,
                    fontSize = (32f * scale).sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Surface(
            color = SiagaNavy,
            border = BorderStroke(1.dp, SiagaCream),
            shape = RoundedCornerShape((8f * scale).dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .height((23f * scale).dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = (10f * scale).dp),
            ) {
                Text(
                    text = "Perkiraan sisa waktu",
                    color = SiagaCream,
                    fontSize = (9f * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun NextInstructionStrip(
    guidance: RouteGuidanceSnapshot?,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val instructions = guidance?.instructions.orEmpty().ifEmpty {
        listOf(ManeuverGuidance(ManeuverType.STRAIGHT, 0))
    }.take(4)
    Box(
        modifier = modifier.size(width = (314f * scale).dp, height = (72f * scale).dp),
    ) {
        Surface(
            color = Color.Transparent,
            border = BorderStroke(1.dp, SiagaCream),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height((50f * scale).dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize(),
            ) {
                instructions.forEachIndexed { index, instruction ->
                    val presentation = maneuverPresentation(instruction.type)
                    MiniInstruction(
                        drawableRes = presentation.drawableRes,
                        label = if (instruction.type == ManeuverType.ARRIVE) {
                            "TES"
                        } else {
                            formatDistance(instruction.distanceMeters)
                        },
                        rotationDegrees = presentation.assetRotationDegrees,
                        mirrorHorizontally = presentation.mirrorHorizontally,
                        tint = presentation.tint || instruction.type != ManeuverType.ARRIVE,
                        contentColor = if (index == 0) Color.White else SiagaNextGreen,
                        scale = scale,
                    )
                    if (index < instructions.lastIndex) StepDot(scale, SiagaNextGreen)
                }
            }
        }
        Surface(
            color = SiagaNextGreen,
            border = BorderStroke(1.dp, Color(0xFFB9B9B9)),
            shape = RoundedCornerShape(topStart = (12f * scale).dp, topEnd = (12f * scale).dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width((127f * scale).dp)
                .height((23f * scale).dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Berikutnya",
                    color = SiagaNavy,
                    fontSize = (12f * scale).sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun MiniInstruction(
    drawableRes: Int,
    label: String,
    scale: Float,
    rotationDegrees: Float = 0f,
    mirrorHorizontally: Boolean = false,
    tint: Boolean = false,
    contentColor: Color = Color.White,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width((45f * scale).dp),
    ) {
        Image(
            painter = painterResource(drawableRes),
            contentDescription = null,
            colorFilter = if (tint) ColorFilter.tint(contentColor) else null,
            modifier = Modifier
                .size((22f * scale).dp)
                .graphicsLayer(
                    rotationZ = rotationDegrees,
                    scaleX = if (mirrorHorizontally) -1f else 1f,
                ),
        )
        Text(
            text = label,
            color = contentColor,
            fontSize = (8f * scale).sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun StepDot(scale: Float, color: Color = Color.White) {
    Text(
        text = "•",
        color = color,
        fontSize = (12f * scale).sp,
    )
}

@Composable
private fun MapPanelHandle(
    scale: Float,
    expansionProgress: Float,
    dragState: AnchoredDraggableState<MapPanelValue>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .anchoredDraggable(
                state = dragState,
                orientation = Orientation.Vertical,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = if (expansionProgress > 0.5f) {
                    "Tarik ke bawah untuk mengecilkan peta"
                } else {
                    "Tarik ke atas untuk memperbesar peta"
                }
            },
    ) {
        Surface(
            color = Color(0xFF8B8B8B),
            shape = CircleShape,
            modifier = Modifier
                .width((169f * scale).dp)
                .height((5f * scale).dp),
        ) {}
    }
}

@Composable
private fun EvacuationMapPanel(
    state: EvacuationUiState,
    mapHeight: Dp,
    scale: Float,
    expansionProgress: Float,
    onBlockedRouteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var followUserLocation by rememberSaveable { mutableStateOf(true) }
    var recenterRequest by rememberSaveable { mutableIntStateOf(0) }
    val shape = RoundedCornerShape(
        topStart = (25f * scale * (1f - expansionProgress)).dp,
        topEnd = (25f * scale * (1f - expansionProgress)).dp,
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(mapHeight)
            .clip(shape),
    ) {
        OfflineMap(
            routeCoordinates = state.route?.coordinates.orEmpty(),
            currentLocation = state.currentLocation,
            destinationLocation = state.route?.destinationCoordinate,
            destinationName = state.route?.destinationName,
            destinationDistanceLabel = state.guidance?.remainingDistanceMeters?.let(::formatDistance),
            deviceHeadingDegrees = state.deviceHeadingDegrees,
            followUserLocation = followUserLocation,
            recenterRequest = recenterRequest,
            onUserMapGesture = { followUserLocation = false },
            modifier = Modifier.fillMaxSize(),
        )

        state.route?.let { route ->
            ExpandedMapHeader(
                route = route,
                guidance = state.guidance,
                remainingSeconds = state.remainingEvacuationSeconds,
                scale = scale,
                modifier = Modifier.graphicsLayer(alpha = expansionProgress),
            )

            VerticalInstructionStrip(
                guidance = state.guidance,
                scale = scale,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8f * scale).dp, y = (211f * scale).dp)
                    .graphicsLayer(alpha = expansionProgress),
            )
        }

        NavigationCompass(
            headingDegrees = state.deviceHeadingDegrees ?: 0f,
            scale = scale,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(
                    x = -lerp((12f * scale).dp, (14f * scale).dp, expansionProgress),
                    y = lerp((18f * scale).dp, (151f * scale).dp, expansionProgress),
                )
                .size(lerp((54f * scale).dp, (60f * scale).dp, expansionProgress)),
        )

        if (state.currentLocation != null) {
            RecenterMapButton(
                onClick = {
                    followUserLocation = true
                    recenterRequest++
                },
                scale = scale,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = (16f * scale).dp,
                        bottom = (82f * scale).dp,
                    )
                    .graphicsLayer(alpha = if (followUserLocation) 0.78f else 1f),
            )
        }

        BlockedRouteButton(
            enabled = state.canSelectAlternative,
            isLoading = state.isLoadingRoute,
            onClick = onBlockedRouteClick,
            scale = scale,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = (15f * scale).dp, vertical = (12f * scale).dp),
        )
    }
}

@Composable
private fun NavigationCompass(
    headingDegrees: Float,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SiagaCream.copy(alpha = 0.96f),
        contentColor = SiagaNavy,
        shape = CircleShape,
        border = BorderStroke(1.dp, SiagaNavy),
        shadowElevation = 4.dp,
        modifier = modifier.semantics {
            contentDescription = "Kompas, arah utara ${headingDegrees.toInt()} derajat"
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding((7f * scale).dp)
                .graphicsLayer(rotationZ = -headingDegrees),
        ) {
            CompassLabel("U", Alignment.TopCenter, 9f, scale)
            CompassLabel("T", Alignment.CenterEnd, 9f, scale)
            CompassLabel("S", Alignment.BottomCenter, 9f, scale)
            CompassLabel("B", Alignment.CenterStart, 9f, scale)
            Image(
                painter = painterResource(R.drawable.ic_figma_compass),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(0.48f),
            )
        }
    }
}

@Composable
private fun BoxScope.CompassLabel(
    label: String,
    alignment: Alignment,
    fontSize: Float,
    scale: Float,
) {
    Text(
        text = label,
        color = if (label == "U") SiagaRust else SiagaNavy,
        fontSize = (fontSize * scale).sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.align(alignment),
    )
}

@Composable
private fun ExpandedMapHeader(
    route: EvacuationRoute,
    guidance: RouteGuidanceSnapshot?,
    remainingSeconds: Int,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val instruction = guidance?.currentInstruction ?: ManeuverGuidance(
        ManeuverType.STRAIGHT,
        estimatedDistanceMeters(route),
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height((190f * scale).dp),
    ) {
        Surface(
            color = SiagaNavy,
            shape = RoundedCornerShape(
                bottomStart = (15f * scale).dp,
                bottomEnd = (15f * scale).dp,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height((135f * scale).dp),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = (20f * scale).dp, top = (15f * scale).dp),
                ) {
                    Text(
                        text = route.destinationName,
                        color = SiagaCream,
                        fontSize = (20f * scale).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width((245f * scale).dp),
                    )
                    Text(
                        text = formatDistance(
                            guidance?.remainingDistanceMeters ?: estimatedDistanceMeters(route),
                        ),
                        color = SiagaCream,
                        fontSize = (32f * scale).sp,
                        fontWeight = FontWeight.Light,
                    )
                    Spacer(modifier = Modifier.height((6f * scale).dp))
                    Text(
                        text = "Berjalan cepat ±${estimatedMinutes(route)} menit",
                        color = SiagaCream,
                        fontSize = (15f * scale).sp,
                    )
                }

                CompactManeuverCard(
                    instruction = instruction,
                    scale = scale,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = (15f * scale).dp, end = (15f * scale).dp),
                )
            }
        }

        Surface(
            color = SiagaNavy,
            shape = RoundedCornerShape((6f * scale).dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (145f * scale).dp)
                .height((34f * scale).dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = (10f * scale).dp),
            ) {
                Text(
                    text = formatDuration(remainingSeconds, spaced = true),
                    color = SiagaCream,
                    fontSize = (20f * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun CompactManeuverCard(
    instruction: ManeuverGuidance,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val presentation = maneuverPresentation(instruction.type)
    Surface(
        color = SiagaCream,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape((18f * scale).dp),
        border = BorderStroke(1.dp, Color(0xFFB9B9B9)),
        modifier = modifier.size((92f * scale).dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(presentation.drawableRes),
                contentDescription = null,
                colorFilter = if (presentation.tint) ColorFilter.tint(SiagaNavy) else null,
                modifier = Modifier
                    .size((53f * scale).dp)
                    .graphicsLayer(
                        rotationZ = presentation.assetRotationDegrees,
                        scaleX = if (presentation.mirrorHorizontally) -1f else 1f,
                    ),
            )
            Text(
                text = presentation.label,
                color = SiagaNavy,
                fontSize = (11f * scale).sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun VerticalInstructionStrip(
    guidance: RouteGuidanceSnapshot?,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val instructions = guidance?.instructions.orEmpty().ifEmpty {
        listOf(ManeuverGuidance(ManeuverType.STRAIGHT, 0))
    }.take(4)
    Surface(
        color = SiagaNavy,
        contentColor = Color.White,
        shape = RoundedCornerShape((32f * scale).dp),
        border = BorderStroke(1.dp, Color(0xFFB9B9B9)),
        modifier = modifier
            .width((53f * scale).dp)
            .height((291f * scale).dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.padding(vertical = (10f * scale).dp),
        ) {
            instructions.forEachIndexed { index, instruction ->
                val presentation = maneuverPresentation(instruction.type)
                MiniInstruction(
                    drawableRes = presentation.drawableRes,
                    label = if (instruction.type == ManeuverType.ARRIVE) {
                        "TES"
                    } else {
                        formatDistance(instruction.distanceMeters)
                    },
                    rotationDegrees = presentation.assetRotationDegrees,
                    mirrorHorizontally = presentation.mirrorHorizontally,
                    tint = instruction.type != ManeuverType.ARRIVE,
                    contentColor = if (index == 0) Color.White else SiagaNextGreen,
                    scale = scale,
                )
                if (index < instructions.lastIndex) StepDot(scale, SiagaNextGreen)
            }
        }
    }
}

@Composable
private fun RecenterMapButton(
    onClick: () -> Unit,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SiagaCream,
        contentColor = SiagaNavy,
        shape = CircleShape,
        border = BorderStroke(1.dp, SiagaNavy),
        shadowElevation = 5.dp,
        modifier = modifier
            .size((54f * scale).dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Kembali ke lokasi saya" },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.ic_figma_map_arrow),
                contentDescription = null,
                modifier = Modifier.size((31f * scale).dp),
            )
        }
    }
}

@Composable
private fun BlockedRouteButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val label = when {
        isLoading -> "Mencari alternatif tujuan…"
        enabled -> "Jalur terhalang?"
        else -> "Alternatif tujuan tidak tersedia"
    }
    Surface(
        color = if (enabled && !isLoading) SiagaWarning else Color(0xFFD5D7A5),
        contentColor = SiagaNavy,
        shape = RoundedCornerShape((11f * scale).dp),
        modifier = modifier
            .fillMaxWidth()
            .height((56f * scale).dp)
            .clickable(
                enabled = enabled && !isLoading,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = SiagaNavy,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size((22f * scale).dp),
                )
                Spacer(modifier = Modifier.width((10f * scale).dp))
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_figma_warning),
                    contentDescription = null,
                    modifier = Modifier.size((22f * scale).dp),
                )
                Spacer(modifier = Modifier.width((10f * scale).dp))
            }
            Text(
                text = label,
                color = SiagaNavy,
                fontSize = (20f * scale).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RoutePreparationState(
    state: EvacuationUiState,
    onRequestLocationPermission: () -> Unit,
    onRetryRoute: () -> Unit,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val title: String
    val detail: String
    when {
        !state.hasLocationPermission -> {
            title = "Izinkan akses lokasi"
            detail = "Lokasi diperlukan untuk menentukan arah evakuasi dan diproses di perangkat."
        }
        state.errorMessage != null -> {
            title = "Arahan belum tersedia"
            detail = state.errorMessage
        }
        state.currentLocation == null -> {
            title = "Mencari lokasi…"
            detail = "Pastikan GPS perangkat aktif. Arahan tetap disiapkan tanpa jaringan."
        }
        else -> {
            title = "Menyiapkan arahan…"
            detail = "Rute sedang dibaca dari data luring."
        }
    }

    Surface(
        color = SiagaCream,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape((18f * scale).dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = (32f * scale).dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((14f * scale).dp),
            modifier = Modifier.padding((24f * scale).dp),
        ) {
            if (state.isLoadingRoute || state.currentLocation == null && state.hasLocationPermission) {
                CircularProgressIndicator(color = SiagaNavy)
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_figma_destination),
                    contentDescription = null,
                    modifier = Modifier.size((48f * scale).dp),
                )
            }
            Text(
                text = title,
                fontSize = (24f * scale).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = detail,
                fontSize = (14f * scale).sp,
                textAlign = TextAlign.Center,
            )
            when {
                !state.hasLocationPermission -> ActionButton(
                    text = "Izinkan",
                    onClick = onRequestLocationPermission,
                )
                state.errorMessage != null -> ActionButton(
                    text = "Coba lagi",
                    onClick = onRetryRoute,
                )
            }
        }
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = SiagaNavy,
            contentColor = Color.White,
        ),
        shape = RoundedCornerShape(11.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BlockedRouteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = Color.White,
            contentColor = Color.Black,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .height(250.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_figma_close),
                        contentDescription = "Tutup",
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 20.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_figma_dialog_warning),
                        contentDescription = null,
                        modifier = Modifier.size(68.dp),
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Laporkan jalur terhalang?",
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sistem akan mengarahkan Anda ke alternatif tujuan.",
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, Color(0xFF7F7F7F)),
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(
                            text = "Batal",
                            color = Color(0xFF7F7F7F),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A4D7A),
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(
                            text = "Ya, cari alternatif",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

private data class DirectionPresentation(
    val label: String,
    val drawableRes: Int,
    val assetRotationDegrees: Float = 0f,
    val mirrorHorizontally: Boolean = false,
    val tint: Boolean = false,
)

private fun maneuverPresentation(type: ManeuverType): DirectionPresentation = when (type) {
        ManeuverType.STRAIGHT -> DirectionPresentation(
            label = "Lurus",
            drawableRes = R.drawable.ic_figma_straight_arrow,
            tint = true,
        )
        ManeuverType.U_TURN -> DirectionPresentation(
            label = "Putar balik",
            drawableRes = R.drawable.ic_maneuver_uturn,
        )
        ManeuverType.SLIGHT_RIGHT -> DirectionPresentation(
            label = "Sedikit ke kanan",
            drawableRes = R.drawable.ic_figma_turn_arrow,
            assetRotationDegrees = -90f,
        )
        ManeuverType.RIGHT -> DirectionPresentation(
            label = "Belok kanan",
            drawableRes = R.drawable.ic_figma_turn_arrow,
            assetRotationDegrees = -90f,
        )
        ManeuverType.SHARP_RIGHT -> DirectionPresentation(
            label = "Belok tajam kanan",
            drawableRes = R.drawable.ic_figma_turn_arrow,
            assetRotationDegrees = -90f,
        )
        ManeuverType.SLIGHT_LEFT -> DirectionPresentation(
            label = "Sedikit ke kiri",
            drawableRes = R.drawable.ic_figma_turn_arrow,
            assetRotationDegrees = -90f,
            mirrorHorizontally = true,
        )
        ManeuverType.LEFT -> DirectionPresentation(
            label = "Belok kiri",
            drawableRes = R.drawable.ic_figma_turn_arrow,
            assetRotationDegrees = -90f,
            mirrorHorizontally = true,
        )
        ManeuverType.SHARP_LEFT -> DirectionPresentation(
            label = "Belok tajam kiri",
            drawableRes = R.drawable.ic_figma_turn_arrow,
            assetRotationDegrees = -90f,
            mirrorHorizontally = true,
        )
        ManeuverType.ARRIVE -> DirectionPresentation(
            label = "Tiba di TES",
            drawableRes = R.drawable.ic_figma_destination,
        )
    }

private fun estimatedMinutes(route: EvacuationRoute): Int =
    ceil(route.estimatedSeconds / 60.0).toInt().coerceAtLeast(1)

private fun estimatedDistanceMeters(route: EvacuationRoute): Int =
    (route.estimatedSeconds * WALKING_SPEED_METERS_PER_SECOND).toInt().coerceAtLeast(0)

private fun formatDistance(distanceMeters: Int): String = when {
    distanceMeters < 1_000 -> "${(distanceMeters / 10) * 10} m"
    else -> "%.1f km".format(distanceMeters / 1_000.0)
}

private fun formatDuration(totalSeconds: Int, spaced: Boolean = false): String {
    val minutes = totalSeconds.coerceAtLeast(0) / 60
    val seconds = totalSeconds.coerceAtLeast(0) % 60
    val separator = if (spaced) " : " else ":"
    return "%02d%s%02d".format(minutes, separator, seconds)
}

private enum class MapPanelValue {
    COLLAPSED,
    EXPANDED,
}

private val MAP_PANEL_SPRING = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

private const val FIGMA_WIDTH_DP = 390f
private const val FIGMA_MAP_HEIGHT_DP = 269f
private const val WALKING_SPEED_METERS_PER_SECOND = 1.2
