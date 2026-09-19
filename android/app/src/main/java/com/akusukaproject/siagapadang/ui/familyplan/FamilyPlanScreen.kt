package com.akusukaproject.siagapadang.ui.familyplan

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akusukaproject.siagapadang.data.model.FamilyMember
import com.akusukaproject.siagapadang.data.model.FamilyPlan
import com.akusukaproject.siagapadang.ui.theme.SiagaCream
import com.akusukaproject.siagapadang.ui.theme.SiagaNavy
import com.akusukaproject.siagapadang.ui.theme.SiagaRust
import com.akusukaproject.siagapadang.ui.theme.SiagaWarning
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import com.akusukaproject.siagapadang.R
import com.akusukaproject.siagapadang.ui.common.CalmCardShape
import com.akusukaproject.siagapadang.ui.common.CalmScaffold
import com.akusukaproject.siagapadang.ui.common.CalmTile
import com.akusukaproject.siagapadang.ui.common.SectionLabel
import com.akusukaproject.siagapadang.ui.theme.SiagaLine
import com.akusukaproject.siagapadang.ui.theme.SiagaRustDeep
import com.akusukaproject.siagapadang.ui.theme.SiagaTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MutedOnCream = Color(0xFF3D4A5C)
private val MutedOnNavy = Color(0xFFC9D6E8)

@Composable
fun FamilyPlanScreen(
    onBack: () -> Unit,
    viewModel: FamilyPlanViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editingMember by remember { mutableStateOf<FamilyMember?>(null) }
    var isPickingMeetingPoint by rememberSaveable { mutableStateOf(false) }
    var memberPendingRemoval by remember { mutableStateOf<FamilyMember?>(null) }

    BackHandler(onBack = onBack)

    FamilyPlanContent(
        plan = state.plan,
        onBack = onBack,
        onPickMeetingPoint = { isPickingMeetingPoint = true },
        onAddMember = {
            viewModel.clearSuggestionMessage()
            editingMember = FamilyMember(
                id = viewModel.newMemberId(),
                name = "",
                routineLocation = "",
                destinationName = null,
            )
        },
        onEditMember = { member ->
            viewModel.clearSuggestionMessage()
            editingMember = member
        },
        onRemoveMember = { member -> memberPendingRemoval = member },
        onShare = {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Rencana evakuasi tsunami keluarga")
                putExtra(Intent.EXTRA_TEXT, FamilyPlanShareText.build(state.plan))
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan rencana keluarga"))
        },
    )

    editingMember?.let { member ->
        MemberEditorDialog(
            initial = member,
            isNew = state.plan.members.none { it.id == member.id },
            state = state,
            onSuggestFromLocation = viewModel::suggestDestinationFromCurrentLocation,
            onConsumeSuggestion = viewModel::consumeSuggestion,
            onDismiss = { editingMember = null },
            onSave = { updated ->
                viewModel.saveMember(updated)
                editingMember = null
            },
        )
    }

    if (isPickingMeetingPoint) {
        EvacuationPointPickerDialog(
            title = "Pilih titik temu keluarga",
            state = state,
            selectedName = state.plan.meetingPointName,
            onDismiss = { isPickingMeetingPoint = false },
            onSelect = { name ->
                viewModel.setMeetingPoint(name)
                isPickingMeetingPoint = false
            },
        )
    }

    memberPendingRemoval?.let { member ->
        ConfirmRemovalDialog(
            memberName = member.name,
            onDismiss = { memberPendingRemoval = null },
            onConfirm = {
                viewModel.removeMember(member.id)
                memberPendingRemoval = null
            },
        )
    }

    state.saveErrorMessage?.let { message ->
        MessageDialog(message = message, onDismiss = viewModel::dismissSaveError)
    }
}

@Composable
private fun FamilyPlanContent(
    plan: FamilyPlan,
    onBack: () -> Unit,
    onPickMeetingPoint: () -> Unit,
    onAddMember: () -> Unit,
    onEditMember: (FamilyMember) -> Unit,
    onRemoveMember: (FamilyMember) -> Unit,
    onShare: () -> Unit,
) {
    CalmScaffold(title = "Rencana keluarga", backLabel = "Menu", onBack = onBack) {
        PrincipleNotice()
        SectionLabel("Titik temu setelah aman")
        CalmTile(
            iconRes = R.drawable.ic_ms_flag,
            iconBackground = SiagaWarning,
            iconTint = SiagaNavy,
            title = plan.meetingPointName ?: "Belum dipilih",
            detail = "Tempat berkumpul setelah semua anggota selesai evakuasi",
            trailing = {
                Text(
                    text = if (plan.meetingPointName == null) "Pilih" else "Ubah",
                    color = SiagaNavy,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            onClick = onPickMeetingPoint,
        )
        SectionLabel("Anggota · ${plan.members.size}")
        if (plan.members.isEmpty()) {
            Text(
                text = "Belum ada anggota. Tambahkan siapa saja yang biasanya berada di tempat berbeda saat siang hari.",
                color = SiagaTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            plan.members.forEach { member ->
                MemberCard(member = member, onEdit = { onEditMember(member) }, onRemove = { onRemoveMember(member) })
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onAddMember,
                colors = ButtonDefaults.buttonColors(containerColor = SiagaWarning, contentColor = SiagaNavy),
                border = BorderStroke(2.dp, SiagaNavy),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Icon(painterResource(R.drawable.ic_ms_person_add), contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tambah", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (!plan.isEmpty) {
                OutlinedButton(
                    onClick = onShare,
                    border = BorderStroke(1.dp, SiagaLine),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = SiagaNavy),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_ms_share), contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Bagikan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        plan.updatedAtMillis?.let { updatedAt ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Tersimpan di HP ini · diperbarui ${formatUpdatedAt(updatedAt)}",
                color = SiagaTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PrincipleNotice() {
    Surface(
        color = Color(0xFFFBE3D9),
        contentColor = SiagaNavy,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SiagaRustDeep.copy(alpha = 0.4f)),
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Icon(
                painterResource(R.drawable.ic_ms_do_not_disturb_on),
                contentDescription = null,
                tint = SiagaRustDeep,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Saat gempa, jangan kembali untuk menjemput.", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Setiap anggota berjalan cepat ke TES masing-masing. Aplikasi tidak mengetahui posisi anggota keluarga. Rencana ini tersimpan di HP dan dapat dibuka tanpa internet.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = SiagaTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun MemberCard(
    member: FamilyMember,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        color = Color.White,
        contentColor = SiagaNavy,
        shape = CalmCardShape,
        border = BorderStroke(1.dp, SiagaLine),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFFDDE7FB), CircleShape),
            ) {
                Text(member.name.take(1).uppercase(), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2F5FBF))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${member.routineLocation.ifBlank { "Lokasi belum diisi" }} → ${member.destinationName ?: "TES belum dipilih"}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = SiagaTextSecondary,
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                Icon(painterResource(R.drawable.ic_ms_edit), contentDescription = "Ubah ${member.name}", tint = SiagaNavy)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                Icon(painterResource(R.drawable.ic_ms_delete), contentDescription = "Hapus ${member.name}", tint = SiagaRustDeep)
            }
        }
    }
}

@Composable
private fun MemberEditorDialog(
    initial: FamilyMember,
    isNew: Boolean,
    state: FamilyPlanUiState,
    onSuggestFromLocation: () -> Unit,
    onConsumeSuggestion: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (FamilyMember) -> Unit,
) {
    var name by rememberSaveable(initial.id) { mutableStateOf(initial.name) }
    var routineLocation by rememberSaveable(initial.id) { mutableStateOf(initial.routineLocation) }
    var destinationName by rememberSaveable(initial.id) { mutableStateOf(initial.destinationName) }
    var isPickingDestination by rememberSaveable(initial.id) { mutableStateOf(false) }

    LaunchedEffect(state.suggestion) {
        state.suggestion?.let { suggestion ->
            destinationName = suggestion.destinationName
            onConsumeSuggestion()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = Color.White,
            contentColor = SiagaNavy,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isNew) "Tambah anggota" else "Ubah anggota",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(modifier = Modifier.height(12.dp))
                FamilyTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nama",
                    placeholder = "Contoh: Ibu, Adi",
                )
                Spacer(modifier = Modifier.height(8.dp))
                FamilyTextField(
                    value = routineLocation,
                    onValueChange = { routineLocation = it },
                    label = "Biasanya berada di",
                    placeholder = "Contoh: SDN 12 Air Tawar, Pasar Raya",
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "TES tujuan", fontSize = 13.sp, color = MutedOnCream, fontWeight = FontWeight.Bold)
                Text(
                    text = destinationName ?: "Belum dipilih",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = if (destinationName == null) MutedOnCream else SiagaNavy,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { isPickingDestination = true },
                        border = BorderStroke(1.dp, SiagaNavy),
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                    ) {
                        Text(text = "Pilih dari daftar", color = SiagaNavy, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onSuggestFromLocation,
                        enabled = !state.isFindingSuggestion,
                        border = BorderStroke(1.dp, SiagaNavy),
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                    ) {
                        if (state.isFindingSuggestion) {
                            CircularProgressIndicator(
                                color = SiagaNavy,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Text(text = "Dari posisi saya", color = SiagaNavy, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    text = state.suggestionMessage
                        ?: "\"Dari posisi saya\" memakai rute evakuasi di tempat HP berada sekarang. Pakai saat sedang di lokasi rutin anggota.",
                    fontSize = 12.sp,
                    color = MutedOnCream,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, Color(0xFF7F7F7F)),
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(text = "Batal", color = Color(0xFF595959), fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            onSave(
                                initial.copy(
                                    name = name,
                                    routineLocation = routineLocation,
                                    destinationName = destinationName,
                                ),
                            )
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SiagaNavy,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFD9D9D9),
                            disabledContentColor = Color(0xFF4D4D4D),
                        ),
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(text = "Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (isPickingDestination) {
        EvacuationPointPickerDialog(
            title = "Pilih TES tujuan",
            state = state,
            selectedName = destinationName,
            onDismiss = { isPickingDestination = false },
            onSelect = { selected ->
                destinationName = selected
                isPickingDestination = false
            },
        )
    }
}

@Composable
private fun FamilyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
        ),
        colors = familyTextFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun familyTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SiagaNavy,
    unfocusedTextColor = SiagaNavy,
    focusedBorderColor = SiagaNavy,
    unfocusedBorderColor = Color(0xFF7F7F7F),
    focusedLabelColor = SiagaNavy,
    unfocusedLabelColor = MutedOnCream,
    cursorColor = SiagaNavy,
    focusedPlaceholderColor = Color(0xFF6B7280),
    unfocusedPlaceholderColor = Color(0xFF6B7280),
)

@Composable
private fun EvacuationPointPickerDialog(
    title: String,
    state: FamilyPlanUiState,
    selectedName: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, state.evacuationPoints) {
        val keyword = query.trim()
        if (keyword.isEmpty()) {
            state.evacuationPoints
        } else {
            state.evacuationPoints.filter { it.point.name.contains(keyword, ignoreCase = true) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = Color.White,
            contentColor = SiagaNavy,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 32.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(
                    text = if (state.isSortedByDistance) {
                        "Diurutkan dari yang terdekat dengan posisi HP."
                    } else {
                        "Diurutkan menurut nama. Posisi HP belum diketahui."
                    },
                    fontSize = 12.sp,
                    color = MutedOnCream,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Cari nama TES") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = familyTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 120.dp, max = 420.dp),
                ) {
                    when {
                        state.isLoadingPoints -> CircularProgressIndicator(
                            color = SiagaNavy,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        state.pointsErrorMessage != null -> Text(
                            text = state.pointsErrorMessage,
                            color = SiagaRust,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        filtered.isEmpty() -> Text(
                            text = "Tidak ada TES dengan nama itu.",
                            color = MutedOnCream,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        else -> LazyColumn {
                            items(filtered, key = { it.point.externalId }) { option ->
                                EvacuationPointRow(
                                    option = option,
                                    isSelected = option.point.name == selectedName,
                                    onClick = { onSelect(option.point.name) },
                                )
                                HorizontalDivider(color = Color(0xFFE2E2E2))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, Color(0xFF7F7F7F)),
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(text = "Batal", color = Color(0xFF595959), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun EvacuationPointRow(
    option: EvacuationPointOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(if (isSelected) SiagaCream else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = option.point.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Kapasitas ${formatPeople(option.point.capacityPeople)} jiwa",
                fontSize = 12.sp,
                color = MutedOnCream,
            )
        }
        option.distanceMeters?.let { distance ->
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = formatDistance(distance), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "✓", fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ConfirmRemovalDialog(
    memberName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color.White,
            contentColor = SiagaNavy,
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Hapus $memberName?", fontSize = 19.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Anggota ini akan dihapus dari rencana di HP ini.",
                    fontSize = 14.sp,
                    color = MutedOnCream,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, Color(0xFF7F7F7F)),
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(text = "Batal", color = Color(0xFF595959), fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SiagaRust,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(text = "Hapus", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageDialog(message: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = Color.White, contentColor = SiagaNavy, shape = RoundedCornerShape(14.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = message, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SiagaNavy, contentColor = Color.White),
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(text = "Mengerti", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatUpdatedAt(timestamp: Long): String =
    SimpleDateFormat("d MMM yyyy, HH.mm", Locale("id", "ID")).format(Date(timestamp))

private fun formatDistance(distanceMeters: Int): String = when {
    distanceMeters < 1_000 -> "$distanceMeters m"
    else -> String.format(Locale("id", "ID"), "%.1f km", distanceMeters / 1_000.0)
}

private fun formatPeople(value: Int): String = String.format("%,d", value).replace(',', '.')
