package me.rerere.rikkahub.ui.pages.wechat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import me.rerere.rikkahub.data.sync.BackupManager
import me.rerere.rikkahub.data.repository.ConversationRepository
import org.koin.compose.koinInject
import java.io.File

@Composable
fun TransferPage() {
    val context = LocalContext.current
    val manager: BackupManager = koinInject()
    val repository: ConversationRepository = koinInject()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    BackHandler(enabled = busy) { status = "正在处理，请等待完成后返回。" }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            busy = true; status = "正在导出全部角色、对话及附件…"
            try {
                val archive = manager.createBackup(includeDatabase = true, includeFiles = true)
                try { withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        archive.inputStream().use { it.copyTo(output) }
                    } ?: error("无法写入所选位置")
                } } finally { archive.delete() }
                status = "全部导出完成。ZIP 包含角色、所有对话分支、附件和应用设置；可能包含 API Key，请妥善保管。"
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { status = "导出失败：${e.message}" }
            finally { busy = false }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true; status = "正在追加导入，请保持当前页面…"
            var lastProgress=0L
            var archive: File? = null
            try {
                archive = withContext(Dispatchers.IO) {
                    File.createTempFile("huadeng-", ".zip", context.cacheDir).also { file ->
                        try {
                            context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
                                ?: error("无法读取备份")
                        } catch (e: Exception) { file.delete(); throw e }
                    }
                }
                val (characters, chats) = manager.appendCharactersAndChats(archive, repository) { message -> val now=android.os.SystemClock.uptimeMillis();if(now-lastProgress>150){lastProgress=now;scope.launch { status=message }} }
                status = "已追加 $characters 个角色、$chats 段对话。原有数据保留；导入的角色需选择 whispera 中的模型。重复导入会生成另一份副本。"
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { status = "导入失败：${e.message}。已有角色和对话不会被覆盖。" }
            finally { archive?.delete(); busy = false }
        }
    }
    WxPage("角色与对话迁移",backEnabled=!busy) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("从华灯追加导入", color = Foreground, style = MaterialTheme.typography.titleMedium)
            Text("Android 无法直接读取另一应用的私有数据。请先在华灯的「备份与同步」导出完整本地备份，并勾选数据库和附件，再在这里选择 ZIP。自动解析并追加，已有角色、对话和密钥不会被替换。", color = Secondary)
            Button(onClick = { import.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")) }, enabled = !busy) { Text("选择华灯备份 · 追加导入") }
            HorizontalDivider()
            Text("全部导出", color = Foreground, style = MaterialTheme.typography.titleMedium)
            Text("一次导出全部角色卡、聊天记录（含回复分支）、头像附件及设置为完整 ZIP，便于迁移或恢复。备份可能包含 API Key。", color = Secondary)
            Button(onClick = { export.launch("whispera-full-${System.currentTimeMillis()}.zip") }, enabled = !busy) { Text("导出全部角色与对话") }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(status, color = Foreground)
        }
    }
}
