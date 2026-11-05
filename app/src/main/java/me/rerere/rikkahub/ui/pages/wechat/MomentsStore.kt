package me.rerere.rikkahub.ui.pages.wechat

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.AtomicFile
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.rerere.rikkahub.data.datastore.SettingsStore
import org.json.JSONArray
import org.koin.core.context.GlobalContext
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class MomentsStore private constructor(private val context:Context) {
    private val prefs=context.getSharedPreferences("wechat-moments-settings",Context.MODE_PRIVATE)
    private val file=AtomicFile(File(context.filesDir,"wechat-moments.json"))
    private val json=Json{ignoreUnknownKeys=true}
    private val mutex=Mutex()
    private var loaded=false
    private val _feed=MutableStateFlow(MomentsSnapshot())
    val feed=_feed.asStateFlow()
    private val _folders=MutableStateFlow(readFolders())
    val folders=_folders.asStateFlow()
    private val _cover=MutableStateFlow(prefs.getString("cover",null))
    val cover=_cover.asStateFlow()
    private val _warning=MutableStateFlow("")
    val warning=_warning.asStateFlow()

    data class Folders(val images:List<String>,val videos:List<String>)
    private fun readList(key:String):List<String> =runCatching{val a=JSONArray(prefs.getString(key,"[]"));List(a.length()){a.getString(it)}}.getOrDefault(emptyList())
    private fun readFolders()=Folders(readList("images"),readList("videos"))

    suspend fun addFolder(uri:Uri,video:Boolean) {
        context.contentResolver.takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        withContext(Dispatchers.IO){
            val key=if(video)"videos" else "images"
            val updated=(readList(key)+uri.toString()).distinct()
            prefs.edit().putString(key,JSONArray(updated).toString()).commit()
            _folders.value=readFolders()
        }
        refresh(forceScan=true)
    }
    suspend fun removeFolder(uri:String,video:Boolean) {
        withContext(Dispatchers.IO){val key=if(video)"videos" else "images";prefs.edit().putString(key,JSONArray(readList(key)-uri).toString()).commit();_folders.value=readFolders()}
        refresh(forceScan=true)
    }
    suspend fun setCover(uri:Uri)=withContext(Dispatchers.IO) {
        val previous=_cover.value
        val target=AtomicFile(File(context.filesDir,"wechat-moments-cover-${UUID.randomUUID()}"))
        val output=target.startWrite()
        try{context.contentResolver.openInputStream(uri)?.use{it.copyTo(output)}?:error("无法读取这张图片");target.finishWrite(output)}catch(e:Exception){target.failWrite(output);throw e}
        check(prefs.edit().putString("cover",target.baseFile.absolutePath).commit()) { "无法保存背景设置" }
        _cover.value=target.baseFile.absolutePath
        previous?.let { old -> val file=File(old); if(file.parentFile==context.filesDir && file.name.startsWith("wechat-moments-cover")) file.delete() }
    }
    suspend fun toggleLike(id:String)=withContext(Dispatchers.IO){mutex.withLock{load();save(_feed.value.copy(posts=_feed.value.posts.map{if(it.id==id)it.copy(liked=!it.liked)else it}))}}
    private fun load(){if(loaded)return;_feed.value=runCatching{json.decodeFromString<MomentsSnapshot>(file.openRead().bufferedReader().use{it.readText()})}.getOrDefault(MomentsSnapshot());loaded=true}
    private fun save(snapshot:MomentsSnapshot){val output=file.startWrite();try{output.write(json.encodeToString(snapshot).toByteArray(Charsets.UTF_8));file.finishWrite(output);_feed.value=snapshot}catch(e:Exception){file.failWrite(output);throw e}}

    suspend fun refresh(forceScan:Boolean=false)=withContext(Dispatchers.IO) {mutex.withLock {
        load()
        val settings=GlobalContext.get().get<SettingsStore>().settingsFlowRaw.first()
        val authors=settings.assistants.filter{it.tavernData!=null}.map{it.id.toString()}
        var snapshot=_feed.value
        if(authors.isNotEmpty()&&snapshot.posts.any{it.authorId==null})snapshot=snapshot.copy(posts=snapshot.posts.map{if(it.authorId==null)it.copy(authorId=authors.random())else it})
        val due=MomentsPlanner.times(snapshot.lastSlot,System.currentTimeMillis())
        if(due.isEmpty()&&!forceScan){if(snapshot!=_feed.value)save(snapshot);return@withLock}
        val errors=mutableListOf<String>()
        val folders=readFolders()
        val images=folders.images.flatMap{tree->runCatching{scanTree(Uri.parse(tree),false)}.getOrElse{errors+="部分图片文件夹无法读取，请重新选择";emptyList()}}.distinctBy{it.uri}
        val videos=folders.videos.flatMap{tree->runCatching{scanTree(Uri.parse(tree),true)}.getOrElse{errors+="部分视频文件夹无法读取，请重新选择";emptyList()}}.distinctBy{it.uri}
        _warning.value=errors.distinct().joinToString("\n")
        val allowed=(images+videos).map{it.uri}.toSet()
        // A removed folder must stop contributing both new and existing feed media.
        val kept=snapshot.posts.mapNotNull{p->p.copy(media=p.media.filter{it.uri in allowed}).takeIf{it.media.isNotEmpty()}}
        if(images.isEmpty()&&videos.isEmpty()){save(MomentsSnapshot());return@withLock}
        val times=MomentsPlanner.times(if(kept.isEmpty())null else snapshot.lastSlot,System.currentTimeMillis())
        val generated=times.map{time->LocalMoment(UUID.randomUUID().toString(),time,authors.randomOrNull(),MomentsPlanner.choose(images,videos))}
        save(MomentsSnapshot((kept+generated).sortedByDescending{it.time},MomentsPlanner.slot(System.currentTimeMillis())))
    }}

    private fun scanTree(tree:Uri,video:Boolean):List<LocalMedia> {
        val result=mutableListOf<LocalMedia>();val visited=mutableSetOf<String>();val pending=ArrayDeque<Pair<String,Int>>()
        pending.add(DocumentsContract.getTreeDocumentId(tree) to 0)
        while(pending.isNotEmpty()){
            val (parent,depth)=pending.removeFirst();if(!visited.add(parent)||depth>32)continue
            val children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,parent)
            context.contentResolver.query(children,arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_MIME_TYPE),null,null,null)?.use{cursor->
                while(cursor.moveToNext()){
                    val id=cursor.getString(0);val mime=cursor.getString(1).orEmpty()
                    if(mime==DocumentsContract.Document.MIME_TYPE_DIR)pending.add(id to depth+1)
                    else if(mime.startsWith(if(video)"video/" else "image/"))result+=LocalMedia(DocumentsContract.buildDocumentUriUsingTree(tree,id).toString(),video)
                }
            }?:error("文件夹不可用")
        }
        return result
    }
    companion object {
        @Volatile private var instance:MomentsStore?=null
        fun get(context:Context)=instance?:synchronized(this){instance?:MomentsStore(context.applicationContext).also{instance=it}}
        fun schedule(context:Context){
            val now=System.currentTimeMillis();val delay=MomentsPlanner.slot(now)+MomentsPlanner.INTERVAL-now
            val request=PeriodicWorkRequestBuilder<MomentsWorker>(12,TimeUnit.HOURS).setInitialDelay(delay,TimeUnit.MILLISECONDS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("wechat-local-moments",ExistingPeriodicWorkPolicy.KEEP,request)
        }
    }
}

class MomentsWorker(context:Context,parameters:WorkerParameters):CoroutineWorker(context,parameters) {
    override suspend fun doWork():Result=try{MomentsStore.get(applicationContext).refresh();Result.success()}catch(_:Exception){Result.retry()}
}
