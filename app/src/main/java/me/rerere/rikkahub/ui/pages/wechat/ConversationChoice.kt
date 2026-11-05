package me.rerere.rikkahub.ui.pages.wechat
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.rerere.rikkahub.data.model.Conversation
@Composable
fun ConversationChoice(chats:List<Conversation>,onDismiss:()->Unit,onSelect:(String)->Unit){
    ModalBottomSheet(onDismissRequest=onDismiss,containerColor=RowColor,contentColor=Foreground,
        shape=RoundedCornerShape(topStart=12.dp,topEnd=12.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically){WxIcon("back",onDismiss);Text("选择聊天",Modifier.weight(1f));Text("${chats.size}段",Modifier.padding(12.dp),color=Secondary)}
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val max=androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp*.7f
            LazyColumn(Modifier.fillMaxWidth().heightIn(max=max).padding(bottom=20.dp)){
                items(chats,key={it.id}){ chat->WxRow(chat.title.ifBlank{"未命名聊天"},detail=chatListTime(chat.updateAt)){onSelect(chat.id.toString())} }
            }
        }
    }
}
