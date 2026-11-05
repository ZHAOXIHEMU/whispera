package me.rerere.rikkahub.ui.pages.wechat

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.model.*
import me.rerere.rikkahub.service.ChatService
import me.rerere.rikkahub.ui.components.ui.UIAvatar
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.pages.wechat.picker.ImagePickerActivity
import org.koin.compose.koinInject
import java.io.File
import java.util.UUID
import kotlin.uuid.Uuid

@Composable
fun ConversationSettings(id:String) {
    val service:ChatService=koinInject(); val settings=LocalSettings.current
    val nav=LocalNavController.current; val scope=rememberCoroutineScope(); val context=LocalContext.current
    val conversation by service.getConversationFlow(Uuid.parse(id)).collectAsStateWithLifecycle()
    LaunchedEffect(id){service.initializeConversation(Uuid.parse(id))}
    val original=settings.getStoredAssistantOf(conversation)
    val character=settings.getAssistantOf(conversation)
    var edit by remember{mutableStateOf<String?>(null)}
    var text by remember{mutableStateOf("")}
    var status by remember{mutableStateOf("")}
    var confirmDelete by remember{mutableStateOf(false)}
    var titling by remember{mutableStateOf(false)}
    var deleting by remember{mutableStateOf(false)}
    WxPage("聊天信息",action={WxIcon("search",{nav.navigate(Screen.MessageSearch)})}) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(horizontal=17.dp,vertical=10.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                UIAvatar(original.name,original.avatar,Modifier.size(48.dp),onClick={nav.navigate(Screen.AssistantDetail(original.id.toString()))})
                Text(original.name,Modifier.weight(1f).padding(start=12.dp),color=Foreground)
                Text("原始角色卡 · 点击头像编辑",color=Secondary,style=MaterialTheme.typography.labelSmall)
            }
            WxRow("会话标题",detail=conversation.title.take(20)){text=conversation.title;edit="title"}
            WxRow(if(titling)"正在生成标题…"else"重新生成标题"){if(!titling)scope.launch{titling=true;try{service.generateTitle(Uuid.parse(id),conversation,force=true)}finally{titling=false}}}
            WxRow("会话内角色名称",detail=character.name.take(20)){text=character.name;edit="name"}
            Row(Modifier.fillMaxWidth().background(RowColor).height(59.dp).padding(horizontal=17.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Text("会话内角色头像",Modifier.weight(1f),color=Foreground)
                UIAvatar(character.name,character.avatar,Modifier.size(36.dp),onUpdate={avatar->scope.launch{service.updateIsolatedConfig(Uuid.parse(id)){c->val b=c.bindConfig(settings);b.copy(config=b.config!!.copy(characterAvatar=avatar))}}})
            }
            Spacer(Modifier.fillMaxWidth().height(8.dp).background(Backdrop))
            WxRow("聊天历史"){nav.navigate(Screen.History)}
            WxRow("查找聊天记录"){nav.navigate(Screen.MessageSearch)}
            WxRow("查看本次请求"){ val html=me.rerere.rikkahub.data.ai.PromptDebugCache.get(Uuid.parse(id))
                if(html==null)status="发送消息后可查看实际请求提示词"
                else nav.navigate(Screen.WebView(contentId=me.rerere.ui.webview.WebViewContentCache.store(context.cacheDir,html)))
            }
            WxRow("AI 翻译"){nav.navigate(Screen.Translator)}
            WxRow("导演备注",detail="仅此会话"){nav.navigate(Screen.ConversationAuthorsNote(id))}
            WxRow("收藏"){nav.navigate(Screen.Favorite)}
            WxRow("更改聊天背景",detail=if(conversation.config?.backgroundPath.isNullOrEmpty())"空背景"else"已设置"){nav.navigate(Screen.ConversationBackground(id,false))}
            Spacer(Modifier.fillMaxWidth().height(8.dp).background(Backdrop))
            WxRow("语音通话"){nav.navigate(Screen.VoiceCall(id))}
            WxRow("视频通话"){nav.navigate(Screen.VideoCall(id))}
            if(status.isNotBlank())Text(status,Modifier.padding(17.dp),color=Secondary)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().background(RowColor).clickable(enabled=!deleting){confirmDelete=true}.padding(19.dp),contentAlignment=androidx.compose.ui.Alignment.Center) {
                Text(if(deleting)"正在删除…"else"删除聊天",color=Red)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    if(confirmDelete)AlertDialog(onDismissRequest={confirmDelete=false},title={Text("删除聊天？")},text={Text("将删除此会话及其聊天记录，角色卡和其他会话会保留。此操作无法撤销。")},
        confirmButton={TextButton(onClick={confirmDelete=false;deleting=true;scope.launch{
            try {
                service.deleteConversation(Uuid.parse(id))
                HomeDestination.chatRequested=true
                nav.navigate(Screen.WeChatHome){popUpTo(Screen.WeChatHome);launchSingleTop=true}
            }catch(e:kotlinx.coroutines.CancellationException){throw e}
            catch(e:Exception){status="删除失败：${e.message}";deleting=false}
        }}){Text("删除",color=Red)}},
        dismissButton={TextButton(onClick={confirmDelete=false}){Text("取消")}})
    if(edit!=null)AlertDialog(onDismissRequest={edit=null},title={Text(if(edit=="title")"会话标题"else"会话内角色名称")},text={OutlinedTextField(text,{text=it},singleLine=true)},
        confirmButton={TextButton(onClick={val field=edit;val value=text;edit=null;scope.launch{service.updateIsolatedConfig(Uuid.parse(id)){c->val b=c.bindConfig(settings);if(field=="title")b.copy(title=value)else b.copy(config=b.config!!.copy(characterName=value))}}}){Text("保存")}},
        dismissButton={TextButton(onClick={edit=null}){Text("取消")}})
}

@Composable
fun ConversationBackground(id:String,presets:Boolean) {
    val context=LocalContext.current;val service:ChatService=koinInject();val settings=LocalSettings.current;val scope=rememberCoroutineScope();val nav=LocalNavController.current
    var revision by remember{mutableIntStateOf(0)};var error by remember{mutableStateOf("")}
    val folder=remember{File(context.filesDir,"background-presets").apply{mkdirs()}}
    val files=remember(revision){folder.listFiles()?.filter{it.isFile}?.sortedByDescending{it.lastModified()}.orEmpty()}
    fun select(path:String){scope.launch{service.updateIsolatedConfig(Uuid.parse(id)){c->val b=c.bindConfig(settings);b.copy(config=b.config!!.copy(backgroundPath=path))};nav.popBackStack()}}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        val uri=result.data?.data
        if(result.resultCode==Activity.RESULT_OK&&uri!=null)scope.launch {
            try {
                val file=withContext(Dispatchers.IO){File(if(presets)folder else File(context.filesDir,"conversation-backgrounds").apply{mkdirs()},UUID.randomUUID().toString()).also{target->context.contentResolver.openInputStream(uri)?.use{input->target.outputStream().use{input.copyTo(it)}}?:error("无法读取图片")}}
                if(presets)revision++ else select(file.toURI().toString())
            }catch(e:Exception){error=e.message?:"无法导入图片"}
        }
    }
    WxPage(if(presets)"背景预设"else"选择背景图") {
        if(error.isNotEmpty())Text(error,Modifier.padding(17.dp),color=Red)
        if(!presets){
            WxRow("从手机相册选择"){picker.launch(Intent(context,ImagePickerActivity::class.java))}
            WxRow("从预设图库选择"){nav.navigate(Screen.ConversationBackground(id,true))}
            WxRow("使用空背景"){select("")}
        }else{
            Button(onClick={picker.launch(Intent(context,ImagePickerActivity::class.java))},modifier=Modifier.padding(17.dp)){Text("上传图片")}
            LazyVerticalGrid(GridCells.Fixed(2),Modifier.weight(1f),contentPadding=PaddingValues(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                items(files,key={it.name}){file->AsyncImage(file.absolutePath,null,Modifier.aspectRatio(.8f).clickable{select(file.toURI().toString())},contentScale=androidx.compose.ui.layout.ContentScale.Crop)}
            }
        }
    }
}
