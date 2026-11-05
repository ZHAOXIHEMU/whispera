package me.rerere.rikkahub.ui.pages.wechat

import android.icu.text.Transliterator
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.ui.context.LocalNavController

/** Shared by the contacts tab, role management and the new-chat chooser. */
@Composable
fun CharacterDirectory(
    assistants: List<Assistant>,
    modifier: Modifier = Modifier,
    query: String = "",
    onAdd: () -> Unit,
    chooseForChat: Boolean = false,
    onChat: ((Assistant) -> Unit)? = null,
    onManage: ((Assistant) -> Unit)? = null,
) {
    val nav = LocalNavController.current
    val scope = rememberCoroutineScope()
    val state = rememberLazyListState()
    val sorted by produceState<List<Pair<Assistant,String>>>(emptyList(),assistants) {
        value=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val transliterator=Transliterator.getInstance("Han-Latin; Latin-ASCII; Upper")
            assistants.map { it to transliterator.transliterate(it.name.trim()) }.sortedWith(compareBy({if(it.second.firstOrNull() in 'A'..'Z')0 else 1},{it.second}))
        }
    }
    val groups = remember(sorted, query) {
        sorted.filter { (role, key) -> query.isBlank() || role.name.contains(query, true) || key.replace(" ", "").contains(query.replace(" ", ""), true) }
            .groupBy { (_, key) -> key.firstOrNull()?.takeIf { it in 'A'..'Z' }?.toString() ?: "#" }
    }
    val starts = remember(groups) {
        var index = 2 // New friends and group chats.
        groups.mapValues { (_, roles) -> index.also { index += roles.size + 1 } }
    }
    val letters = remember { listOf("↑") + ('A'..'Z').map(Char::toString) + "#" }
    var jump by remember { mutableStateOf<Job?>(null) }
    var touching by remember { mutableStateOf<String?>(null) }
    fun go(letter: String) {
        if (letter == touching) return
        touching = letter
        val position = if (letter == "↑") 0 else starts[letter] ?: starts.entries.firstOrNull { it.key >= letter || it.key == "#" }?.value ?: (state.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
        jump?.cancel()
        jump = scope.launch { state.scrollToItem(position) }
    }
    Box(modifier.background(RowColor)) {
        LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(end = 25.dp, bottom = 20.dp)) {
            item("new-friend") {
                Row(Modifier.fillMaxWidth().height(62.dp).clickable(onClick = onAdd).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(Color(0xffff9d35), RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
                        Glyph("friend", Modifier.size(30.dp), Color.White)
                    }
                    Text("新的朋友", Modifier.padding(start = 16.dp), color = Foreground, fontSize = 18.sp)
                }
                HorizontalDivider(Modifier.padding(start = 74.dp), color = DividerColor, thickness = .5.dp)
            }
            item("groups") { WxRow("群聊", icon = "contacts") { nav.navigate(Screen.GroupChatList) } }
            groups.forEach { (letter, roles) ->
                item("letter-$letter") { Text(letter, Modifier.fillMaxWidth().background(Backdrop).padding(horizontal = 16.dp, vertical = 6.dp), color = Secondary, fontSize = 13.sp) }
                items(roles, key = { it.first.id.toString() }) { (role, _) ->
                    Row(Modifier.fillMaxWidth().height(64.dp).combinedClickable(onClick = {
                        if (chooseForChat && onChat != null) onChat(role) else nav.navigate(Screen.AssistantDetail(role.id.toString()))
                    }, onLongClick = { onManage?.invoke(role) }).padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        WxAvatar(role.name, role.avatar, Modifier.size(42.dp))
                        Text(role.name.ifBlank { "未命名角色" }, Modifier.weight(1f).padding(horizontal = 16.dp), color = Foreground, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        onChat?.let { WxIcon("chat") { it(role) } }
                        onManage?.let { WxIcon("more") { it(role) } }
                    }
                    HorizontalDivider(Modifier.padding(start = 74.dp), color = DividerColor, thickness = .5.dp)
                }
            }
            item("friend-count") { Box(Modifier.fillMaxWidth().padding(vertical=24.dp),contentAlignment=Alignment.Center){Text("${assistants.size}个朋友",color=Secondary,fontSize=16.sp)} }
            if (groups.isEmpty()) item("empty") { Text(if (query.isBlank()) "还没有角色，点击新的朋友添加" else "没有找到角色", Modifier.padding(24.dp), color = Secondary) }
        }
        Column(Modifier.align(Alignment.CenterEnd).width(25.dp).fillMaxHeight(.94f).pointerInput(starts) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                fun select(y: Float) = go(letters[(y / size.height.coerceAtLeast(1) * letters.size).toInt().coerceIn(letters.indices)])
                select(down.position.y)
                down.consume()
                do {
                    val event = awaitPointerEvent()
                    val point = event.changes.first()
                    if (point.pressed) select(point.position.y)
                    point.consume()
                } while (event.changes.any { it.pressed })
                touching = null
            }
        }, horizontalAlignment = Alignment.CenterHorizontally) {
            letters.forEach { letter -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(letter, color = if (letter == touching) Green else Secondary, fontSize = 10.sp)
            } }
        }
        touching?.let { Text(it, Modifier.align(Alignment.Center).background(Color(0xdd303030), RoundedCornerShape(6.dp)).padding(24.dp), color = Color.White, fontSize = 32.sp) }
    }
}
