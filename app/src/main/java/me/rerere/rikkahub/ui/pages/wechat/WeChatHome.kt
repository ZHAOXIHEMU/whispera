package me.rerere.rikkahub.ui.pages.wechat

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Avatar
import me.rerere.rikkahub.data.repository.ConversationRepository
import me.rerere.rikkahub.ui.components.ui.UIAvatar
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalSettings
import org.koin.compose.koinInject
import kotlin.uuid.Uuid
import me.rerere.rikkahub.data.model.bindConfig
import me.rerere.rikkahub.service.ChatService
import me.rerere.rikkahub.ui.pages.assistant.CharacterCreationSheet
import me.rerere.rikkahub.ui.pages.assistant.CharacterActionsHost

val Backdrop=Color(0xff111111)
val RowColor=Color(0xff191919)
val BottomColor=Color(0xff1e1e1e)
val Foreground=Color(0xffc9c9c9)
val Secondary=Color(0xff777777)
val DividerColor=Color(0xff242424)
val Green=Color(0xff07c160)
val Red=Color(0xfffa5151)
object HomeDestination { var chatRequested by mutableStateOf(false) }

@Composable
fun WxHeader(title:String,back:Boolean=true,action:(@Composable ()->Unit)?=null, backEnabled:Boolean=true) {
    val nav=LocalNavController.current
    Box(Modifier.fillMaxWidth().height(54.dp).background(Backdrop)) {
        if(back&&backEnabled)Box(Modifier.align(Alignment.CenterStart)){WxIcon("back",{nav.popBackStack()})}
        Text(title,Modifier.align(Alignment.Center).padding(horizontal=58.dp),color=Foreground,fontSize=19.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        action?.let{Box(Modifier.align(Alignment.CenterEnd)){it()}}
    }
    HorizontalDivider(color=DividerColor,thickness=.5.dp)
}

@Composable
fun WxIcon(name:String,onClick:()->Unit) {
    Box(Modifier.size(46.dp).clickable(onClick=onClick),contentAlignment=Alignment.Center){Glyph(name,Modifier.size(24.dp))}
}

@Composable
fun WxPage(title:String,action:(@Composable ()->Unit)?=null,backEnabled:Boolean=true,content:@Composable ColumnScope.()->Unit) {
    MaterialTheme(colorScheme=darkColorScheme(primary=Green,background=Backdrop,surface=RowColor,onSurface=Foreground)) {
        Column(Modifier.fillMaxSize().background(Backdrop).windowInsetsPadding(WindowInsets.systemBars)) {WxHeader(title,action=action,backEnabled=backEnabled);content()}
    }
}

@Composable
fun WxRow(title:String,detail:String="",icon:String?=null,onClick:()->Unit) {
    Column(Modifier.fillMaxWidth().background(RowColor).clickable(onClick=onClick)) {
        Row(Modifier.fillMaxWidth().heightIn(min=59.dp).padding(horizontal=17.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically) {
            icon?.let{Glyph(it,Modifier.size(23.dp),if(it=="pay")Green else Color(0xff1997e6));Spacer(Modifier.width(18.dp))}
            Text(title,Modifier.weight(1f),color=Foreground,fontSize=17.sp)
            if(detail.isNotEmpty())Text(detail,color=Secondary,fontSize=14.sp,maxLines=1)
            Glyph("chevron",Modifier.padding(start=7.dp).size(18.dp),Color(0xff555555))
        }
        HorizontalDivider(Modifier.padding(start=if(icon==null)17.dp else 58.dp),color=DividerColor,thickness=.5.dp)
    }
}

@Composable
fun WxSection(title:String) {Text(title,Modifier.fillMaxWidth().padding(start=17.dp,top=20.dp,bottom=8.dp),color=Secondary,fontSize=13.sp)}

@Composable
fun WeChatHome() {
    val context=LocalContext.current
    val nav=LocalNavController.current
    val settings=LocalSettings.current
    val store:SettingsStore=koinInject()
    val repo:ConversationRepository=koinInject()
    val koin=org.koin.compose.getKoin()
    val service=remember { lazy { koin.get<ChatService>() } }
    val history by remember(repo){repo.searchConversations("")}.collectAsStateWithLifecycle(emptyList())
    val pager=rememberPagerState(pageCount={4})
    val scope=rememberCoroutineScope()
    var retainedPages by remember { mutableIntStateOf(0) }
    LaunchedEffect(pager) {
        snapshotFlow { pager.isScrollInProgress }.collectLatest { scrolling ->
            if (!scrolling && retainedPages == 0) {
                kotlinx.coroutines.delay(500)
                retainedPages = 3
            }
        }
    }
    LaunchedEffect(HomeDestination.chatRequested) {
        if(HomeDestination.chatRequested){pager.scrollToPage(0);HomeDestination.chatRequested=false}
    }
    LaunchedEffect(Unit){MomentsStore.schedule(context); me.rerere.rikkahub.ui.pages.wechat.picker.ImagePickerActivity.warm(context)}
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME){scope.launch{runCatching{MomentsStore.get(context).refresh()}}}
    var menuTab by remember { mutableStateOf<Int?>(null) }
    var creating by remember { mutableStateOf(false) }
    var roleActions by remember { mutableStateOf<Assistant?>(null) }
    var searchContacts by remember { mutableStateOf(false) }
    var contactQuery by remember { mutableStateOf("") }
    var conversationMenu by remember { mutableStateOf<String?>(null) }
    var operationError by remember { mutableStateOf("") }
    val listNow by produceState(java.time.Instant.now()) { while(true) { value=java.time.Instant.now();kotlinx.coroutines.delay(60_000) } }
    val visibleHistory = remember(history) { history.filter { it.config?.hiddenFromChatList != true }.sortedWith(compareByDescending<me.rerere.rikkahub.data.model.Conversation> { it.isPinned }.thenByDescending { it.updateAt }) }
    LaunchedEffect(pager.currentPage) { menuTab = null; conversationMenu = null }
    val labels=listOf("聊天","通讯录","发现","我")
    val icons=listOf("chat","contacts","discover","person")
    var choice by remember { mutableStateOf<List<me.rerere.rikkahub.data.model.Conversation>?>(null) }
    fun chat(assistant:Assistant){scope.launch{
        store.selectAssistant(assistant.id)
        val chats=repo.searchConversationsOfAssistant(assistant.id, "").first()
        when(chats.size){0->nav.navigate(Screen.Chat(Uuid.random().toString()));1->nav.navigate(Screen.Chat(chats.single().id.toString()));else->choice=chats}

    }}
    Column(Modifier.fillMaxSize().background(if(pager.currentPage==3)RowColor else Backdrop).windowInsetsPadding(WindowInsets.systemBars)) {
        HorizontalPager(pager,Modifier.weight(1f),beyondViewportPageCount=retainedPages) { tab ->
            Column(Modifier.fillMaxSize()) {
                if(tab!=3)WxHeader(labels[tab],back=false,action={
                    Row {
                        WxIcon("search",{ if(tab==1) searchContacts=!searchContacts else nav.navigate(Screen.MessageSearch) })
                        Box {
                            WxIcon("plus",{menuTab=tab})
                            DropdownMenu(menuTab==tab && pager.currentPage==tab,{menuTab=null},containerColor=Color(0xff303030)) {
                                DropdownMenuItem(text={Text("发起聊天")},onClick={menuTab=null;nav.navigate(Screen.WeChatStartChat)})
                                DropdownMenuItem(text={Text("新的朋友")},onClick={menuTab=null;creating=true})
                                DropdownMenuItem(text={Text("群聊")},onClick={menuTab=null;nav.navigate(Screen.GroupChatList)})
                            }
                        }
                    }
                })
                when(tab) {
                    0->LazyColumn(Modifier.fillMaxSize().background(RowColor)) {
                        if(visibleHistory.isEmpty())item {
                            Column(Modifier.fillMaxWidth().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                Text("开始一段新的聊天",color=Foreground,fontSize=18.sp)
                                Spacer(Modifier.height(10.dp));Text("在通讯录选择角色，或点击右上角 ＋",color=Secondary,fontSize=14.sp)
                            }
                        }
                        items(visibleHistory,key={it.id.toString()}){conversation->
                            val assistant=settings.assistants.find{it.id==conversation.assistantId}
                            Box {
                            val summary by produceState<Pair<String,java.time.Instant>?>(null,conversation.id,conversation.updateAt) { value=repo.lastMessageSummary(conversation.id) }
                            Row(Modifier.fillMaxWidth().height(76.dp).background(if(conversation.isPinned)Color(0xff252525)else RowColor).combinedClickable(onClick={nav.navigate(Screen.Chat(conversation.id.toString()))},onLongClick={conversationMenu=conversation.id.toString()}).padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
                                WxAvatar(conversation.config?.characterName?:assistant?.name.orEmpty(),conversation.config?.characterAvatar?:assistant?.avatar?:Avatar.Dummy,Modifier.size(50.dp))
                                Spacer(Modifier.width(13.dp))
                                Column(Modifier.weight(1f).fillMaxHeight()) {
                                    Row(Modifier.fillMaxWidth().padding(top=15.dp),verticalAlignment=Alignment.CenterVertically) {
                                        Text(conversation.title.ifBlank{"新的聊天"},Modifier.weight(1f),color=Foreground,fontSize=17.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                                        Text(chatListTime(summary?.second?:conversation.updateAt,listNow),color=Secondary,fontSize=12.sp)
                                    }
                                    Text(summary?.first.orEmpty(),Modifier.weight(1f).padding(top=4.dp),color=Secondary,fontSize=14.sp,maxLines=1)
                                    HorizontalDivider(color=DividerColor,thickness=.5.dp)
                                }
                            }
                            DropdownMenu(expanded=conversationMenu==conversation.id.toString(),onDismissRequest={conversationMenu=null},containerColor=Color(0xff303030)) {
                                DropdownMenuItem(text={Text(if(conversation.isPinned)"取消置顶"else"置顶该聊天")},onClick={conversationMenu=null;scope.launch{try{service.value.toggleConversationPinned(conversation.id)}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){operationError="置顶失败：${e.message}"}}})
                                DropdownMenuItem(text={Text("不显示该聊天")},onClick={conversationMenu=null;scope.launch{try{service.value.updateIsolatedConfig(conversation.id){c->val bound=c.bindConfig(settings);bound.copy(config=bound.config!!.copy(hiddenFromChatList=true))}}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){operationError="隐藏失败：${e.message}"}}})
                            }
                            }
                        }
                    }
                    1->Column(Modifier.fillMaxSize()) {
                        if(searchContacts)TextField(contactQuery,{contactQuery=it},Modifier.fillMaxWidth().padding(12.dp),placeholder={Text("搜索角色或拼音")},singleLine=true,shape=RoundedCornerShape(6.dp))
                        CharacterDirectory(settings.assistants,Modifier.weight(1f),query=if(searchContacts)contactQuery else "",onAdd={creating=true},onChat={chat(it)},onManage={roleActions=it})
                    }
                    2->Column(Modifier.fillMaxSize()) {
                        WxRow("朋友圈",icon="moments"){nav.navigate(Screen.WeChatMoments)}
                        Spacer(Modifier.height(8.dp))
                        WxRow("媒体创作",icon="works"){nav.navigate(Screen.MediaCreationSessions)}
                        WxRow("AI 翻译",icon="book"){nav.navigate(Screen.Translator)}
                        Spacer(Modifier.height(8.dp))
                        WxRow("情侣空间",icon="collect"){nav.navigate(Screen.CoupleSpace)}
                        WxRow("生活空间",icon="discover"){nav.navigate(Screen.LifeHub)}
                    }
                    3->Column(Modifier.fillMaxSize().background(Backdrop).verticalScroll(rememberScrollState())) {
                        Row(Modifier.fillMaxWidth().background(RowColor).clickable{nav.navigate(Screen.WeChatProfile)}.padding(start=25.dp,end=17.dp,top=58.dp,bottom=24.dp),verticalAlignment=Alignment.Top) {
                            WxAvatar(settings.displaySetting.userNickname,settings.displaySetting.userAvatar,Modifier.size(67.dp))
                            Spacer(Modifier.width(18.dp))
                            Column(Modifier.weight(1f)) {
                                Text(settings.displaySetting.userNickname.ifBlank{"设置昵称"},color=Foreground,fontSize=23.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                                Spacer(Modifier.height(16.dp));Text("个人资料",color=Secondary,fontSize=16.sp)
                                Spacer(Modifier.height(18.dp));Text("＋ 状态",Modifier.border(.6.dp,Color(0xff555555),RoundedCornerShape(20.dp)).padding(horizontal=10.dp,vertical=3.dp),color=Secondary,fontSize=13.sp)
                            }
                            Glyph("chevron",Modifier.size(19.dp),Secondary)
                        }
                        Spacer(Modifier.fillMaxWidth().height(8.dp).background(Backdrop));WxRow("服务",icon="pay"){nav.navigate(Screen.WeChatServices)}
                        Spacer(Modifier.fillMaxWidth().height(8.dp).background(Backdrop));WxRow("收藏",icon="collect"){nav.navigate(Screen.Favorite)}
                        WxRow("作品",icon="works"){nav.navigate(Screen.MediaCreationSessions)}
                        WxRow("角色卡与预设",icon="shop"){nav.navigate(Screen.Assistant)}
                        WxRow("快捷消息",icon="smile"){nav.navigate(Screen.QuickMessages)}
                        Spacer(Modifier.height(8.dp));WxRow("导演备注",detail="全局默认",icon="book"){nav.navigate(Screen.AuthorsNote)}
                        WxRow("调用统计",icon="works"){nav.navigate(Screen.Stats)}
                        Spacer(Modifier.fillMaxWidth().height(8.dp).background(Backdrop))
                        WxRow("设置",icon="settings"){nav.navigate(Screen.WeChatSettings)}
                        Spacer(Modifier.fillMaxWidth().height(8.dp).background(Backdrop))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(58.dp).background(BottomColor)) {
            labels.forEachIndexed{index,label->
                Column(Modifier.weight(1f).fillMaxHeight().clickable{scope.launch{pager.animateScrollToPage(index)}}.padding(top=6.dp),horizontalAlignment=Alignment.CenterHorizontally){
                    BottomTabIcon(index,Modifier.width(34.dp).height(30.dp),index==pager.currentPage)
                    Spacer(Modifier.height(3.dp))
                    Text(label,color=if(index==pager.currentPage)Green else Foreground,fontSize=13.sp)
                }
            }
        }
    }
    choice?.let { ConversationChoice(it,onDismiss={choice=null},onSelect={id->choice=null;nav.navigate(Screen.Chat(id))}) }
    if(creating)CharacterCreationSheet(onDismiss={creating=false})
    CharacterActionsHost(roleActions,onDismiss={roleActions=null})
    if(operationError.isNotEmpty())AlertDialog(onDismissRequest={operationError=""},title={Text("操作未完成")},text={Text(operationError)},confirmButton={TextButton({operationError=""}){Text("确定")}})

}

@Composable
fun WxAvatar(name:String,avatar:Avatar,modifier:Modifier=Modifier) {
    Box(modifier.clip(CircleShape).background(Color(0xff303030))) {
        if(avatar!=Avatar.Dummy||name.isNotBlank())UIAvatar(name,avatar,Modifier.fillMaxSize(),shape=CircleShape)
    }
}

@Composable
fun WeChatSettings() {
    val nav=LocalNavController.current
    var query by remember{mutableStateOf("")}
    val groups=listOf(
        "账号" to listOf("个人资料" to Screen.WeChatProfile,"个人信息与权限" to Screen.SettingSecurity,"照片读取权限" to Screen.WeChatPhotoPermission),
        "通用设置" to listOf("主题与颜色" to Screen.SettingPreferencesTheme,"聊天与显示" to Screen.SettingPreferencesUI,"外观与素材" to Screen.WeChatMedia,"常规" to Screen.SettingPreferencesGeneral,"通知" to Screen.SettingPreferencesNotification,"网络" to Screen.SettingPreferencesNetwork,"华灯设置" to Screen.SettingHuaDeng,"酒馆功能" to Screen.SettingTavern,"导演备注" to Screen.AuthorsNote),
        "模型与服务" to listOf("模型与服务" to Screen.SettingModels,"提供商" to Screen.SettingProvider,"Web 服务器" to Screen.SettingWeb),
        "数据设置" to listOf("备份与同步" to Screen.Backup,"存储管理" to Screen.SettingFiles,"隐藏的聊天" to Screen.WeChatHiddenChats),
        "关于" to listOf("关于" to Screen.SettingAbout,"请求日志" to Screen.Log)
    )
    WxPage("设置") {
        androidx.compose.foundation.text.BasicTextField(
            value=query,onValueChange={query=it},singleLine=true,
            textStyle=androidx.compose.ui.text.TextStyle(color=Foreground,fontSize=15.sp),
            cursorBrush=androidx.compose.ui.graphics.SolidColor(Green),
            modifier=Modifier.fillMaxWidth().padding(horizontal=17.dp,vertical=8.dp)
                .height(40.dp).background(RowColor,RoundedCornerShape(6.dp)).padding(horizontal=12.dp),
            decorationBox={input->
                Row(Modifier.fillMaxSize(),verticalAlignment=Alignment.CenterVertically){
                    Glyph("search",Modifier.size(18.dp),Secondary)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)){
                        if(query.isEmpty())Text("搜索设置",color=Secondary,fontSize=15.sp)
                        input()
                    }
                }
            }
        )
        LazyColumn(Modifier.weight(1f)) {
            groups.forEach{(label,rows)->val filtered=rows.filter{query.isBlank()||it.first.contains(query,true)};if(filtered.isNotEmpty()){
                item{WxSection(label)}
                filtered.forEach{(title,route)->item{WxRow(title){nav.navigate(route)}}}
            }}
        }
    }
}

@Composable
fun WeChatProfile() {
    val settings=LocalSettings.current
    val store:SettingsStore=koinInject()
    val scope=rememberCoroutineScope()
    var name by remember(settings.displaySetting.userNickname){mutableStateOf(settings.displaySetting.userNickname)}
    var saved by remember{mutableStateOf(false)}
    WxPage("个人资料") {
        Row(Modifier.fillMaxWidth().background(RowColor).padding(20.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("头像",Modifier.weight(1f),color=Foreground,fontSize=18.sp)
            UIAvatar(name,settings.displaySetting.userAvatar,Modifier.size(68.dp),shape=RoundedCornerShape(6.dp),onUpdate={avatar->scope.launch{store.update{latest->latest.copy(displaySetting=latest.displaySetting.copy(userAvatar=avatar),personas=latest.personas.map{if(it.id==latest.activePersonaId)it.copy(avatar=avatar)else it})}}})
        }
        OutlinedTextField(name,{name=it;saved=false},Modifier.fillMaxWidth().padding(17.dp),label={Text("昵称")},singleLine=true)
        Button(onClick={scope.launch{store.update{latest->latest.copy(displaySetting=latest.displaySetting.copy(userNickname=name.trim()),personas=latest.personas.map{if(it.id==latest.activePersonaId)it.copy(name=name.trim())else it})};saved=true}},modifier=Modifier.padding(horizontal=17.dp).fillMaxWidth(),enabled=name.isNotBlank()) {Text(if(saved)"已保存" else "保存")}
    }
}
