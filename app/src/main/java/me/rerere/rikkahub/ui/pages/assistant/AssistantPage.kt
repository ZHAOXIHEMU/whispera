package me.rerere.rikkahub.ui.pages.assistant

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.TavernCharacterData
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantImporter
import me.rerere.rikkahub.ui.pages.assistant.detail.ExportCardDialog
import me.rerere.rikkahub.ui.pages.wechat.*
import org.koin.androidx.compose.koinViewModel
import kotlin.uuid.Uuid

@Composable
fun AssistantPage(vm: AssistantVM = koinViewModel(), openCreation: Boolean = false) {
    val settings = me.rerere.rikkahub.ui.context.LocalSettings.current
    var query by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(openCreation) }
    var selectedTags by remember { mutableStateOf(emptySet<Uuid>()) }
    var target by remember { mutableStateOf<Assistant?>(null) }
    WxPage("角色管理", action = { WxIcon("plus") { creating = true } }) {
        TextField(query, { query = it }, Modifier.fillMaxWidth().padding(12.dp), placeholder = { Text("搜索角色或拼音") }, singleLine = true, shape = RoundedCornerShape(6.dp),
            colors = TextFieldDefaults.colors(focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent, unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent))
        if (settings.assistantTags.isNotEmpty()) LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(settings.assistantTags, key = { it.id }) { tag -> FilterChip(selected = tag.id in selectedTags, onClick = { selectedTags = if (tag.id in selectedTags) selectedTags - tag.id else selectedTags + tag.id }, label = { Text(tag.name) }, shape = RoundedCornerShape(5.dp)) }
        }
        CharacterDirectory(settings.assistants.filter { selectedTags.isEmpty() || it.tags.any { id -> id in selectedTags } }, Modifier.weight(1f), query, onAdd = { creating = true }, onManage = { target = it })
    }
    if (creating) CharacterCreationSheet(onDismiss = { creating = false }, vm = vm)
    CharacterActionsHost(target, onDismiss = { target = null }, vm = vm)
}

@Composable
fun CharacterActionsHost(target: Assistant?, onDismiss: () -> Unit, vm: AssistantVM = koinViewModel()) {
    val nav = LocalNavController.current
    var clone by remember { mutableStateOf<Assistant?>(null) }
    var copyMemories by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Assistant?>(null) }
    var exporting by remember { mutableStateOf<Assistant?>(null) }
    target?.let { role ->
        ModalBottomSheet(onDismissRequest = { onDismiss() }, containerColor = RowColor, contentColor = Foreground, shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)) {
            Text(role.name, Modifier.padding(20.dp), fontSize = 18.sp)
            WxRow("编辑角色") { onDismiss(); nav.navigate(Screen.AssistantDetail(role.id.toString())) }
            WxRow("复制角色") { onDismiss(); copyMemories = false; clone = role }
            WxRow("导出角色 · PNG / JSON") { onDismiss(); exporting = role }
            WxRow("删除角色") { onDismiss(); deleting = role }
            TextButton({ onDismiss() }, Modifier.fillMaxWidth().padding(8.dp)) { Text("取消") }
        }
    }
    clone?.let { role ->
        val memories by vm.getMemories(role).collectAsStateWithLifecycle(emptyList())
        AlertDialog(onDismissRequest = { clone = null }, title = { Text("复制角色") }, text = {
            Column { Text("创建独立的角色副本，保留原角色。")
                if (!role.useGlobalMemory && memories.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(copyMemories, { copyMemories = it }); Text("同时复制角色记忆") }
            }
        }, confirmButton = { TextButton({ vm.copyAssistant(role, copyMemories); clone = null }) { Text("复制") } }, dismissButton = { TextButton({ clone = null }) { Text("取消") } })
    }
    deleting?.let { role -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("删除角色？") }, text = { Text("将删除此角色及其关联的聊天记录。此操作无法撤销。") }, confirmButton = { TextButton({ vm.removeAssistant(role); deleting = null }) { Text("删除", color = Red) } }, dismissButton = { TextButton({ deleting = null }) { Text("取消") } }) }
    exporting?.let { ExportCardDialog(it, onDismiss = { exporting = null }) }
}

/** The original import pipeline is reused; only the creation sheet is restyled. */
@Composable
fun CharacterCreationSheet(onDismiss: () -> Unit, vm: AssistantVM = koinViewModel()) {
    var role by remember { mutableStateOf(Assistant(tavernData = TavernCharacterData(spec = "chara_card_v3", specVersion = "3.0"))) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Green, surface = RowColor, onSurface = Foreground, background = Backdrop)) {
        ModalBottomSheet(onDismissRequest = onDismiss,
            sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)),
            containerColor = RowColor, contentColor = Foreground,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)) {
            Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("新的朋友", Modifier.weight(1f), fontSize = 19.sp)
                    TextButton(onDismiss) { Text("取消") }
                }
                OutlinedTextField(role.name, { role = role.copy(name = it, tavernData = role.tavernData?.copy(name = it)) }, Modifier.fillMaxWidth(), label = { Text("角色名称") }, singleLine = true, shape = RoundedCornerShape(5.dp))
                AssistantImporter(Modifier.fillMaxWidth(), onImport = { result -> vm.addAssistantWithLorebooks(result.assistant, result.newLorebooks); onDismiss() })
                Button(onClick = { vm.addAssistant(role); onDismiss() }, enabled = role.name.isNotBlank(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(5.dp)) { Text("新建角色") }
            }
        }
    }
}
