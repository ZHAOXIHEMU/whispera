package me.rerere.rikkahub.ui.pages.wechat

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalSettings
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WeChatMediaSettings() {
    val context=LocalContext.current;val store=remember{MomentsStore.get(context)};val scope=rememberCoroutineScope()
    val folders by store.folders.collectAsStateWithLifecycle()
    var error by remember{mutableStateOf("")};var loading by remember{mutableStateOf(false)}
    val photoPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){uri->uri?.let{scope.launch{loading=true;try{store.addFolder(it,false);error=""}catch(e:Exception){error=e.message?:"读取文件夹失败"}finally{loading=false}}}}
    val videoPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){uri->uri?.let{scope.launch{loading=true;try{store.addFolder(it,true);error=""}catch(e:Exception){error=e.message?:"读取文件夹失败"}finally{loading=false}}}}
    val nav=LocalNavController.current
    WxPage("外观与素材") {
        LazyColumn(Modifier.weight(1f)) {
            item{WxSection("朋友圈图片文件夹")}
            items(folders.images,key={"image-$it"}){uri->FolderItem(uri){scope.launch{loading=true;try{store.removeFolder(uri,false)}finally{loading=false}}}}
            item{WxRow("＋ 添加图片文件夹"){if(!loading)photoPicker.launch(null)}}
            item{WxSection("朋友圈视频文件夹")}
            items(folders.videos,key={"video-$it"}){uri->FolderItem(uri){scope.launch{loading=true;try{store.removeFolder(uri,true)}finally{loading=false}}}}
            item{WxRow("＋ 添加视频文件夹"){if(!loading)videoPicker.launch(null)}}
            item{Text("可分别添加多个文件夹，包含子文件夹。素材只在手机本地读取，不会上传给模型。首次生成20条，此后按北京时间00:00、12:00各新增一条。",Modifier.padding(17.dp),color=Secondary,fontSize=13.sp)}
            if(loading)item{LinearProgressIndicator(Modifier.fillMaxWidth().padding(17.dp))}
            if(error.isNotBlank())item{Text(error,Modifier.padding(17.dp),color=Red)}
        }
    }
}

@Composable
private fun FolderItem(uri:String,remove:()->Unit) {
    Row(Modifier.fillMaxWidth().background(RowColor).padding(start=17.dp),verticalAlignment=Alignment.CenterVertically){
        Text(Uri.decode(uri.substringAfterLast('/')),Modifier.weight(1f).padding(vertical=15.dp),color=Foreground,fontSize=14.sp)
        TextButton(onClick=remove){Text("移除",color=Red)}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeChatMoments() {
    val context=LocalContext.current;val settings=LocalSettings.current;val store=remember{MomentsStore.get(context)};val scope=rememberCoroutineScope()
    val feed by store.feed.collectAsStateWithLifecycle();val cover by store.cover.collectAsStateWithLifecycle();val warning by store.warning.collectAsStateWithLifecycle()
    var refreshing by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
    var opened by remember{mutableStateOf<Pair<List<LocalMedia>,Int>?>(null)}
    fun refresh(scan:Boolean=false){scope.launch{refreshing=true;try{store.refresh(scan);error=""}catch(e:Exception){error=e.message?:"刷新失败"}finally{refreshing=false}}}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->if(uri!=null)scope.launch{try{store.setCover(uri)}catch(e:Exception){error=e.message?:"无法设置背景图"}}}
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME){refresh()}
    PullToRefreshBox(isRefreshing=refreshing,onRefresh={refresh(true)},modifier=Modifier.fillMaxSize().background(RowColor).windowInsetsPadding(WindowInsets.navigationBars)) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Box(Modifier.fillMaxWidth().height(368.dp)) {
                    Box(Modifier.fillMaxWidth().height(344.dp).background(Color(0xff30383b))) {
                        if(cover!=null)AsyncImage(cover,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                        Row(Modifier.fillMaxWidth().statusBarsPadding().height(54.dp).padding(horizontal=4.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                            val nav=LocalNavController.current
                            IconButton(onClick={nav.popBackStack()}){Glyph("back",Modifier.size(24.dp),Color.White)}
                            IconButton(onClick={picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}){Glyph("photo",Modifier.size(24.dp),Color.White)}
                        }
                    }
                    Row(Modifier.align(Alignment.BottomEnd).padding(end=18.dp,bottom=6.dp),verticalAlignment=Alignment.Top){
                        Text(settings.displaySetting.userNickname,Modifier.padding(top=15.dp,end=14.dp),color=Color.White,fontSize=22.sp)
                        WxAvatar(settings.displaySetting.userNickname,settings.displaySetting.userAvatar,Modifier.size(68.dp))
                    }
                }
            }
            if(warning.isNotBlank()||error.isNotBlank())item{Text(listOf(warning,error).filter{it.isNotBlank()}.joinToString("\n"),Modifier.padding(17.dp),color=Red,fontSize=13.sp)}
            if(feed.posts.isEmpty())item {
                val nav=LocalNavController.current
                Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){
                    Text("还没有朋友圈",color=Secondary)
                    TextButton(onClick={nav.navigate(Screen.WeChatMedia)}){Text("选择图片与视频文件夹",color=Green)}
                }
            }
            items(feed.posts,key={it.id}) { post ->
                val author=settings.assistants.find{it.id.toString()==post.authorId&&it.tavernData!=null}
                Row(Modifier.fillMaxWidth().padding(start=20.dp,end=15.dp,top=24.dp,bottom=14.dp)) {
                    WxAvatar(author?.name.orEmpty(),author?.avatar?:me.rerere.rikkahub.data.model.Avatar.Dummy,Modifier.size(42.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(author?.name.orEmpty(),color=Color(0xff8da6cc),fontSize=17.sp)
                        Spacer(Modifier.height(9.dp))
                        MediaGrid(post.media){index->opened=post.media to index}
                        Row(Modifier.fillMaxWidth().padding(top=10.dp),verticalAlignment=Alignment.CenterVertically){
                            Text(DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai")).format(Instant.ofEpochMilli(post.time)),Modifier.weight(1f),color=Secondary,fontSize=12.sp)
                            TextButton(onClick={scope.launch{store.toggleLike(post.id)}}){Text(if(post.liked)"♥" else "♡",color=if(post.liked)Red else Color(0xff8296b1),fontSize=20.sp)}
                        }
                        if(post.liked)Text("♡ "+settings.displaySetting.userNickname,Modifier.fillMaxWidth().background(Color(0xff222222)).padding(8.dp),color=Color(0xff8da6cc),fontSize=13.sp)
                    }
                }
                HorizontalDivider(color=DividerColor,thickness=.5.dp)
            }
        }
    }
    opened?.let{(media,index)->LocalMediaViewer(media,index){opened=null}}
}

@Composable
private fun MediaGrid(media:List<LocalMedia>,open:(Int)->Unit) {
    val columns=if(media.size==1)1 else if(media.size==2||media.size==4)2 else 3
    Column(Modifier.widthIn(max=300.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
        media.chunked(columns).forEachIndexed{row,entries->
            Row(horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                entries.forEachIndexed{column,item->
                    Box(Modifier.weight(1f).aspectRatio(if(media.size==1)1.05f else 1f).background(Color(0xff292929)).clickable{open(row*columns+column)}){MediaThumbnail(item,Modifier.fillMaxSize())}
                }
                repeat(columns-entries.size){Spacer(Modifier.weight(1f))}
            }
        }
    }
}

private val videoThumbs=LruCache<String,Bitmap>(36)
@Composable
private fun MediaThumbnail(media:LocalMedia,modifier:Modifier=Modifier) {
    val context=LocalContext.current
    if(!media.video){AsyncImage(media.uri,null,modifier,contentScale=ContentScale.Crop);return}
    val thumb by produceState<Bitmap?>(videoThumbs.get(media.uri),media.uri) {
        if(value==null)value=withContext(Dispatchers.IO){runCatching{
            MediaMetadataRetriever().use{retriever->retriever.setDataSource(context,Uri.parse(media.uri));retriever.getScaledFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,480,480)?.also{videoThumbs.put(media.uri,it)}}
        }.getOrNull()}
    }
    Box(modifier,contentAlignment=Alignment.Center){
        thumb?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)}
        Text("▶",Modifier.background(Color.Black.copy(alpha=.35f),RoundedCornerShape(30.dp)).padding(horizontal=11.dp,vertical=5.dp),color=Color.White,fontSize=24.sp)
    }
}

@Composable
private fun LocalMediaViewer(media:List<LocalMedia>,initial:Int,close:()->Unit) {
    val pager=rememberPagerState(initialPage=initial,pageCount={media.size})
    val context=LocalContext.current
    var controls by remember{mutableStateOf(false)}
    val players=remember(media){media.filter{it.video}.associate{it.uri to ExoPlayer.Builder(context).build().apply{setMediaItem(MediaItem.fromUri(it.uri));prepare()}}}
    DisposableEffect(players){onDispose{players.values.forEach{it.release()}}}
    LifecycleEventEffect(Lifecycle.Event.ON_STOP){players.values.forEach{it.pause()}}
    LaunchedEffect(pager.settledPage){controls=false}
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        val view=androidx.compose.ui.platform.LocalView.current
        DisposableEffect(view){
            val window=(view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            window?.let{it.setBackgroundDrawableResource(android.R.color.black);androidx.core.view.WindowCompat.getInsetsController(it,view).hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())}
            onDispose{}
        }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(pager,Modifier.fillMaxSize(),beyondViewportPageCount=1){index->
                val item=media[index]
                if(item.video)LocalVideo(players.getValue(item.uri),index==pager.settledPage&&!pager.isScrollInProgress,controls,{controls=!controls})
                else AsyncImage(item.uri,null,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
            }
            if(controls||!media[pager.currentPage].video)Row(Modifier.align(Alignment.TopStart).fillMaxWidth().statusBarsPadding(),verticalAlignment=Alignment.CenterVertically){TextButton(onClick=close){Text("‹ 返回",color=Color.White)};Text("${pager.currentPage+1} / ${media.size}",color=Color.White)}
        }
    }
}
@Composable
private fun LocalVideo(player:ExoPlayer,active:Boolean,controls:Boolean,toggle:()->Unit) {
    LaunchedEffect(player,active){player.playWhenReady=active}
    var error by remember(player){mutableStateOf("")}
    var position by remember(player){mutableLongStateOf(0L)}
    var duration by remember(player){mutableLongStateOf(0L)}
    var speedMenu by remember{mutableStateOf(false)}
    LaunchedEffect(player,controls){if(controls)while(true){position=player.currentPosition;duration=player.duration.coerceAtLeast(0);kotlinx.coroutines.delay(250)}}
    DisposableEffect(player){val listener=object:Player.Listener{override fun onPlayerError(e:PlaybackException){error="视频无法播放："+e.errorCodeName}};player.addListener(listener);onDispose{player.removeListener(listener)}}
    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
        AndroidView(factory={android.view.LayoutInflater.from(it).inflate(me.rerere.rikkahub.R.layout.moments_video_player,null) as PlayerView},update={it.useController=false;it.player=player},onRelease={it.player=null},modifier=Modifier.fillMaxSize())
        Box(Modifier.matchParentSize().pointerInput(player){detectTapGestures(onTap={toggle()},onDoubleTap={if(player.isPlaying)player.pause()else player.play()})})
        if(error.isNotBlank())Text(error,color=Color.White,modifier=Modifier.padding(24.dp))
        if(controls)Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha=.65f)).navigationBarsPadding().padding(horizontal=16.dp,vertical=10.dp)) {
            Slider(value=position.coerceIn(0,duration).toFloat(),onValueChange={position=it.toLong();player.seekTo(position)},valueRange=0f..duration.coerceAtLeast(1).toFloat())
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                fun time(ms:Long)="%02d:%02d".format(ms/60000,ms/1000%60)
                Text("${time(position)} / ${time(duration)}",Modifier.weight(1f),color=Color.White)
                Box { TextButton(onClick={speedMenu=true}){Text("设置",color=Color.White)}
                    DropdownMenu(speedMenu,{speedMenu=false}){listOf(.5f,1f,1.5f,2f).forEach{speed->DropdownMenuItem(text={Text("${speed}×")},onClick={player.setPlaybackSpeed(speed);speedMenu=false})}}
                }
            }
        }
    }
}
