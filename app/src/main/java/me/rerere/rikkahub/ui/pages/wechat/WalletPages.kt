package me.rerere.rikkahub.ui.pages.wechat

import android.annotation.SuppressLint
import android.webkit.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalSettings
import org.koin.compose.koinInject

private data class BalanceState(val value:DeepSeekBalance?=null,val loading:Boolean=false,val error:String="")
private object WalletBalanceMemory {
    var fingerprint=""
    var value:DeepSeekBalance?=null
}
@Composable
private fun rememberBalance(refresh:Int):BalanceState {
    val key=deepSeekProvider(LocalSettings.current)?.apiKey.orEmpty()
    val fingerprint=remember(key){java.security.MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString(""){"%02x".format(it)}}
    var state by remember(key){mutableStateOf(BalanceState(value=WalletBalanceMemory.value.takeIf{WalletBalanceMemory.fingerprint==fingerprint}))}
    LaunchedEffect(key,refresh){
        if(key.isBlank()){state=BalanceState(error="请先在设置中填写 DeepSeek Key");return@LaunchedEffect}
        state=state.copy(loading=true,error="")
        state=try{
            val result=withContext(Dispatchers.IO){DeepSeekBalanceApi.get(key)}
            WalletBalanceMemory.fingerprint=fingerprint;WalletBalanceMemory.value=result
            BalanceState(result)
        }catch(e:kotlinx.coroutines.CancellationException){throw e}
        catch(e:Exception){state.copy(loading=false,error=e.message?:"无法连接 DeepSeek")}
    }
    return state
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeChatServices() {
    val nav=LocalNavController.current;var refresh by remember{mutableIntStateOf(0)};val balance=rememberBalance(refresh)
    var sheet by remember{mutableStateOf(false)}
    WxPage("服务",action={WxIcon("dots",{sheet=true})}) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(9.dp)) {
            Row(Modifier.fillMaxWidth().height(152.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xff25aa6c))) {
                Column(Modifier.weight(1f).fillMaxHeight().clickable{nav.navigate(Screen.WeChatKey)},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                    Glyph("settings",Modifier.size(38.dp),Color.White);Spacer(Modifier.height(12.dp));Text("模型服务",color=Color.White,fontSize=19.sp)
                }
                Column(Modifier.weight(1f).fillMaxHeight().clickable{nav.navigate(Screen.WeChatWallet)},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                    Glyph("wallet",Modifier.size(39.dp),Color.White);Spacer(Modifier.height(12.dp));Text("钱包",color=Color.White,fontSize=19.sp)
                    Text(balance.value?.primary?.let{it.symbol+it.total}?:if(balance.loading)"查询中…"else"未获取",color=Color.White.copy(alpha=.55f),fontSize=15.sp)
                }
            }
            if(balance.error.isNotBlank())Text(balance.error,Modifier.padding(13.dp),color=Secondary,fontSize=13.sp)
            Spacer(Modifier.height(9.dp));WxSection("模型服务")
            WxRow("DeepSeek 用量与余额",icon="pay"){nav.navigate(Screen.WeChatWallet)}
            WxRow("模型与密钥",icon="settings"){nav.navigate(Screen.WeChatKey)}
            WxRow("调用统计",icon="works"){nav.navigate(Screen.Stats)}
            WxSection("聊天与酒馆")
            WxRow("角色卡",icon="contacts"){nav.navigate(Screen.Assistant)}
            WxRow("世界书与预设",icon="book"){nav.navigate(Screen.SettingTavern)}
            WxRow("语音与音视频",icon="voice"){nav.navigate(Screen.SettingSpeech)}
        }
    }
    if(sheet)ModalBottomSheet(onDismissRequest={sheet=false},containerColor=Color(0xff292929)) {
        TextButton(onClick={sheet=false;nav.navigate(Screen.WeChatKey)},modifier=Modifier.fillMaxWidth()){Text("服务管理",color=Foreground,fontSize=18.sp)}
        HorizontalDivider(color=Backdrop,thickness=7.dp)
        TextButton(onClick={sheet=false},modifier=Modifier.fillMaxWidth()){Text("取消",color=Foreground,fontSize=18.sp)}
    }
}

@Composable
fun WeChatWallet() {
    var refresh by remember{mutableIntStateOf(0)};val state=rememberBalance(refresh)
    WxPage("钱包",action={TextButton(onClick={refresh++}){Text("刷新",color=Foreground)}}) {
        if(state.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        val primary=state.value?.primary
        WalletValue("零钱",primary?.let{it.symbol+it.total}?:"—",true)
        if(state.error.isNotBlank())Text(state.error,Modifier.padding(13.dp),color=Red,fontSize=13.sp)
        state.value?.let{balance->
            balance.currencies.forEach{row->
                if(row!=primary)WalletValue("余额（${row.currency}）",row.symbol+row.total)
                WalletValue("币种",row.currency)
                WalletValue("赠送余额",row.symbol+row.granted)
                WalletValue("充值余额",row.symbol+row.toppedUp)
            }
            WalletValue("可调用状态",if(balance.available)"余额充足"else"余额不足")
        }
        Text("DeepSeek 官网用量",Modifier.fillMaxWidth().padding(horizontal=17.dp,vertical=8.dp),color=Secondary,fontSize=12.sp)
        FixedUsageWeb(Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
private fun WalletValue(label:String,value:String,main:Boolean=false) {
    Row(Modifier.fillMaxWidth().background(RowColor).padding(horizontal=18.dp,vertical=if(main)20.dp else 9.dp),verticalAlignment=Alignment.CenterVertically){
        if(main){Text("¥",color=Color(0xffc1a31f),fontSize=20.sp);Spacer(Modifier.width(16.dp))}
        Text(label,Modifier.weight(1f),color=Foreground,fontSize=if(main)18.sp else 14.sp)
        Text(value,color=Foreground,fontSize=if(main)20.sp else 14.sp,fontWeight=if(main)FontWeight.Medium else FontWeight.Normal)
    }
    HorizontalDivider(Modifier.padding(start=17.dp),color=DividerColor,thickness=.5.dp)
}

/** One application-context WebView retains cookies and avoids rebuilding a white first frame. */
private object UsageBrowser {
    var view: WebView? = null
    var loading by mutableStateOf(true)
    var error by mutableStateOf("")
    fun open(context: android.content.Context): WebView {
        view?.let { return it }
        return WebView(context.applicationContext).apply {
            setBackgroundColor(android.graphics.Color.rgb(17,17,17))
            alpha = 0f
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.safeBrowsingEnabled = true
            settings.isAlgorithmicDarkeningAllowed = true
            settings.userAgentString = settings.userAgentString.replace("; wv", "").replace("Version/4.0 ", "")
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webChromeClient = object : WebChromeClient() {}
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean {
                    val blocked = request.isForMainFrame && !DeepSeekBalanceApi.allowedPage(request.url.toString())
                    if (blocked) { loading=false; error="官网跳转暂不支持：${request.url.host}${request.url.path}" }
                    return blocked
                }
                override fun onPageStarted(view:WebView,url:String?,favicon:android.graphics.Bitmap?) {
                    loading=true; error=""
                }
                override fun onPageCommitVisible(view:WebView,url:String?) { view.alpha=1f; loading=false }
                override fun onPageFinished(view:WebView,url:String?) { view.alpha=1f; loading=false; CookieManager.getInstance().flush() }
                override fun onReceivedError(view:WebView,request:WebResourceRequest,failure:WebResourceError) {
                    if(request.isForMainFrame){loading=false;error="网页加载失败：${failure.description}"}
                }
                override fun onReceivedHttpError(view:WebView,request:WebResourceRequest,response:WebResourceResponse) {
                    if(request.isForMainFrame && response.statusCode>=400){loading=false;error="官网返回 HTTP ${response.statusCode}"}
                }
            }
            UsageBrowser.view=this
            post { loadUrl(DeepSeekBalanceApi.USAGE) }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun FixedUsageWeb(modifier:Modifier) {
    val context=LocalContext.current
    val web=remember { UsageBrowser.open(context) }
    Column(modifier.background(Backdrop)) {
        if(UsageBrowser.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(UsageBrowser.error.isNotBlank()) Text(UsageBrowser.error,Modifier.padding(10.dp),color=Red,fontSize=12.sp)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("官网需独立登录",Modifier.weight(1f).padding(horizontal=12.dp),color=Secondary,fontSize=11.sp)
            TextButton(onClick={web.alpha=0f;UsageBrowser.loading=true;UsageBrowser.error="";web.loadUrl(DeepSeekBalanceApi.USAGE)}) { Text("重新加载",fontSize=12.sp) }
        }
        AndroidView(factory={ (web.parent as? android.view.ViewGroup)?.removeView(web); web },
            modifier=Modifier.weight(1f).fillMaxWidth(),onReset=null,onRelease={ (it.parent as? android.view.ViewGroup)?.removeView(it) },
            update={it.alpha=if(UsageBrowser.loading||UsageBrowser.error.isNotBlank())0f else 1f})
    }
    DisposableEffect(web){ web.onResume(); onDispose { web.onPause() } }
}

@Composable
fun WeChatKeySettings() {
    val settings=LocalSettings.current;val store:SettingsStore=koinInject();val scope=rememberCoroutineScope();val nav=LocalNavController.current
    val provider=deepSeekProvider(settings)
    var key by remember(provider?.id){mutableStateOf(provider?.apiKey.orEmpty())}
    var saved by remember{mutableStateOf(false)}
    WxPage("模型与密钥") {
        Text("DeepSeek",Modifier.padding(17.dp),color=Foreground,fontSize=21.sp,fontWeight=FontWeight.Medium)
        OutlinedTextField(key,{key=it;saved=false},Modifier.fillMaxWidth().padding(horizontal=17.dp),label={Text("API Key")},visualTransformation=PasswordVisualTransformation(),singleLine=true)
        Button(onClick={scope.launch{
            store.update{latest->
                val existing=deepSeekProvider(latest)
                val models=existing?.models?.takeIf{it.isNotEmpty()}?:listOf(Model(modelId="deepseek-flash",displayName="DeepSeek Flash"),Model(modelId="deepseek-v4-pro",displayName="DeepSeek V4 Pro"))
                val updated=existing?.copy(apiKey=key.trim(),models=models)?:ProviderSetting.OpenAI(name="DeepSeek",baseUrl="https://api.deepseek.com",apiKey=key.trim(),models=models)
                latest.copy(providers=if(existing==null)latest.providers+updated else latest.providers.map{if(it.id==existing.id)updated else it},chatModelId=if(latest.providers.none{p->p.models.any{it.id==latest.chatModelId}})models.first().id else latest.chatModelId)
            };saved=true
        }},Modifier.fillMaxWidth().padding(17.dp),enabled=key.isNotBlank()){Text(if(saved)"已保存"else"保存")}
        WxRow("服务商与模型管理"){nav.navigate(Screen.SettingProvider)}
        WxRow("默认模型"){nav.navigate(Screen.SettingModels)}
        Text("此 Key 同时用于聊天和官方余额查询。官网用量网页使用独立登录状态。",Modifier.padding(17.dp),color=Secondary,fontSize=13.sp)
    }
}
