package me.rerere.rikkahub.ui.pages.wechat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.data.model.bindConfig
import me.rerere.rikkahub.data.repository.ConversationRepository
import me.rerere.rikkahub.service.ChatService
import me.rerere.rikkahub.ui.pages.assistant.CharacterCreationSheet
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalSettings
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

@Composable
fun StartCharacterChat() {
    val settings=LocalSettings.current
    val nav=LocalNavController.current
    val store:SettingsStore=koinInject()
    val scope=rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    WxPage("选择聊天角色") {
        TextField(query,{query=it},Modifier.fillMaxWidth().padding(12.dp),placeholder={Text("搜索角色或拼音")},singleLine=true)
        CharacterDirectory(settings.assistants,Modifier.weight(1f),query,onAdd={creating=true},chooseForChat=true,onChat={ role -> scope.launch {
            store.selectAssistant(role.id)
            nav.navigate(Screen.Chat(Uuid.random().toString())) { popUpTo(Screen.WeChatHome) }
        } })
    }
    if(creating)CharacterCreationSheet(onDismiss={creating=false})
}

@Composable
fun HiddenChats() {
    val settings=LocalSettings.current
    val service:ChatService=koinInject()
    val repo:ConversationRepository=koinInject()
    val scope=rememberCoroutineScope()
    val chats by remember(repo){repo.searchConversations("")}.collectAsStateWithLifecycle(emptyList())
    var error by remember { mutableStateOf("") }
    WxPage("隐藏的聊天") {
        Text("点击会话恢复到聊天列表，聊天记录不会被删除。",Modifier.padding(17.dp),color=Secondary)
        if(error.isNotEmpty())Text(error,Modifier.padding(17.dp),color=Red)
        LazyColumn(Modifier.weight(1f)) {
            val hidden=chats.filter{it.config?.hiddenFromChatList==true}
            if(hidden.isEmpty())item { Text("没有隐藏的聊天",Modifier.padding(24.dp),color=Secondary) }
            items(hidden,key={it.id}) { chat -> WxRow(chat.title.ifBlank{"新的聊天"},detail="恢复") { scope.launch {
                try { service.updateIsolatedConfig(chat.id){c->val b=c.bindConfig(settings);b.copy(config=b.config!!.copy(hiddenFromChatList=false))} }
                catch(e:kotlinx.coroutines.CancellationException){throw e}
                catch(e:Exception){error="恢复失败：${e.message}"}
            } } }
        }
    }
}

@Composable
fun WhisperaAbout() {
    WxPage("关于 whispera") {
        Text("whispera ${me.rerere.rikkahub.BuildConfig.VERSION_NAME}",Modifier.padding(20.dp),color=Foreground)
        Text("个人聊天客户端。本地界面与数据由本应用管理，AI 请求发送到你配置的服务商。未接入上游更新、公告或赞助服务。",Modifier.padding(20.dp),color=Secondary)
        Text("基于 RikkaHub / 华灯开源代码修改，保留原项目版权与 AGPL-3.0 许可。第三方依赖按各自许可证使用。",Modifier.padding(20.dp),color=Secondary)
    }
}
