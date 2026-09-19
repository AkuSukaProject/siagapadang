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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SiagaNavy)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                border = BorderStroke(1.dp, MutedOnNavy),
                shape = RoundedCornerShape(11.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(text = "← Kembali", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Rencana Keluarga",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.semantics { heading() },
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            item { PrincipleNotice() }
            item {
                SectionTitle("Titik temu keluarga")
                Spacer(modifier = Modifier.height(6.dp))
                MeetingPointCard(name = plan.meetingPointName, onPick = onPickMeetingPoint)
            }
            item {
                SectionTitle("Anggota keluarga")
                if (plan.members.isEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Belum ada anggota. Tambahkan siapa saja yang biasanya berada di tempat berbeda saat siang hari.",
                        color = MutedOnNavy,
                        fontSize = 14.sp,
                    )
                }
            }
            items(plan.members, key = FamilyMember::id) { member ->
                MemberCard(
                    member = member,
                    onEdit = { onEditMember(member) },
                    onRemove = { onRemoveMember(member) },
                )
            }
            item {
                Button(
                    onClick = onAddMember,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SiagaWarning,
                        contentColor = SiagaNavy,
                    ),
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(text = "+ Tambah anggota", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
            if (!plan.isEmpty) {
                item {
                    OutlinedButton(
                        onClick = onShare,
                        border = BorderStroke(1.5.dp, Color.White),
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Text(text = "Bagikan rencana ke keluarga", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            plan.updatedAtMillis?.let { updatedAt ->
                item {
                    Text(
                        text = "Tersimpan di HP ini · diperbarui ${formatUpdatedAt(updatedAt)}",
                        color = MutedOnNavy,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrincipleNotice() {
    Surface(
        color = SiagaCream,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, SiagaRust),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Susun saat tenang. Saat gempa, jangan kembali untuk menjemput.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Setiap anggota berjalan cepat ke TES masing-masing, lalu bertemu di titik temu setelah petugas menyatakan aman. " +
                    "Aplikasi tidak mengetahui posisi anggota keluarga saat bencana. Rencana ini tersimpan di HP dan dapat dibuka tanpa internet.",
                fontSize = 13.sp,
                color = MutedOnCream,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(Locale("id", "ID")),
        color = SiagaWarning,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun MeetingPointCard(name: String?, onPick: () -> Unit) {
    Surface(
        color = SiagaCream,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClickLabel = "Pilih titik temu", onClick = onPick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name ?: "Belum dipilih",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = if (name == null) MutedOnCream else SiagaNavy,
                )
                Text(
                    text = "Tempat berkumpul setelah semua anggota selesai evakuasi.",
                    fontSize = 12.sp,
                    color = MutedOnCream,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (name == null) "Pilih" else "Ubah", fontWeight = FontWeight.Black, fontSize = 15.sp)
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
        color = SiagaCream,
        contentColor = SiagaNavy,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 6.dp)) {
            Text(text = member.name, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(4.dp))
            LabeledValue(label = "Biasanya di", value = member.routineLocation.ifBlank { "Belum diisi" })
            LabeledValue(label = "Menuju", value = member.destinationName ?: "TES belum dipilih")
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                CardTextButton(text = "Hapus", color = SiagaRust, onClick = onRemove)
                CardTextButton(text = "Ubah", color = SiagaNavy, onClick = onEdit)
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Text(text = "$label: ", fontSize = 14.sp, color = MutedOnCream)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CardTextButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Text(text = text, color = color, fontWeight = FontWeight.Black, fontSize = 15.sp)
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
