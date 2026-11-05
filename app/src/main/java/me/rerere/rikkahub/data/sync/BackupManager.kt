package me.rerere.rikkahub.data.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import me.rerere.rikkahub.data.repository.ConversationRepository
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.data.model.MessageNode
import me.rerere.rikkahub.data.model.bindConfig
import me.rerere.rikkahub.utils.JsonInstant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.migration.SettingsJsonMigrator
import me.rerere.rikkahub.data.db.AppDatabaseFactory
import me.rerere.rikkahub.data.db.AppDatabase
import me.rerere.rikkahub.data.db.SQLiteConfiguration
import me.rerere.rikkahub.data.files.FileFolders
import me.rerere.rikkahub.data.files.MediaCreationFiles
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Shared archive format and restore lifecycle for local, WebDAV and S3 backups. */
class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsStore: SettingsStore,
    private val json: Json,
) {
    private val restoreMutex = Mutex()

    /** Append characters and conversations; never stage a destructive restore or import account keys. */
    suspend fun appendCharactersAndChats(archive: File, repository: ConversationRepository, progress: (String) -> Unit = {}): Pair<Int, Int> =
        withContext(Dispatchers.IO) { restoreMutex.withLock {
            val staging = Files.createTempDirectory(context.cacheDir.toPath(), "append-").toFile()
            val copied = mutableListOf<File>()
            var committed = false
            try {
                var total = 0L
                val seen = mutableSetOf<String>()
                ZipFile(archive).use { zip ->
                    require(zip.size() <= 100_000) { "备份文件条目过多" }
                    for (entry in zip.entries()) {
                        currentCoroutineContext().ensureActive()
                        progress("正在解压：${entry.name.takeLast(60)}")
                        if (entry.isDirectory) continue
                        require(seen.add(entry.name)) { "备份存在重复条目" }
                        val allowed = entry.name == "settings.json" || entry.name == DatabaseBackup.ARCHIVE_DATABASE ||
                            entry.name == DatabaseBackup.WAL || isAttachment(entry.name)
                        if (!allowed) continue
                        val relative = if (entry.name == DatabaseBackup.WAL) DatabaseBackup.ARCHIVE_DATABASE + "-wal" else entry.name
                        val target = PendingRestore.resolveInside(staging, relative)
                        target.parentFile!!.mkdirs()
                        var size = 0L
                        zip.getInputStream(entry).use { input -> target.outputStream().use { output ->
                            val buffer = ByteArray(65536)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val n = input.read(buffer)
                                if (n < 0) break
                                size += n; total += n
                                require(size <= 2L * 1024 * 1024 * 1024 && total <= 8L * 1024 * 1024 * 1024) { "备份解压内容过大" }
                                if (entry.name == "settings.json") require(size <= 64L * 1024 * 1024) { "备份设置过大" }
                                output.write(buffer, 0, n)
                            }
                        } }
                    }
                }
                val settingsFile = File(staging, "settings.json")
                require(settingsFile.isFile) { "请选择华灯的完整备份 ZIP（缺少 settings.json）" }
                val source = json.decodeFromString<Settings>(SettingsJsonMigrator.migrate(settingsFile.readText()))
                val fileMap = mutableMapOf<String, String>()
                for (folder in ATTACHMENT_FOLDERS) {
                    val directory = File(staging, folder)
                    if (!directory.exists()) continue
                    for (file in directory.walkTopDown().filter { it.isFile }) {
                        val relative = file.relativeTo(directory).invariantSeparatorsPath
                        val importedName = "import_${Uuid.random()}_${relative.replace('/', '_')}"
                        val destination = PendingRestore.resolveInside(context.filesDir, "$folder/$importedName")
                        destination.parentFile!!.mkdirs()
                        copied += destination
                        if (!file.renameTo(destination)) file.copyTo(destination, overwrite = false)
                        fileMap["$folder/$relative"] = destination.toURI().toString()
                    }
                }
                val remap = AppendRemapper(fileMap)
                val sourceJson = JsonInstant.encodeToJsonElement(source)
                remap.collect(sourceJson)
                val dbFile = File(staging, DatabaseBackup.ARCHIVE_DATABASE)
                var count = 0
                var importedCharacters = source.assistants.size
                val staged = if (dbFile.exists()) {
                    DatabaseBackup.normalize(context, dbFile)
                    DatabaseBackup.upgradeToCurrent(context, dbFile)
                    openStagedDatabase(context, dbFile)
                } else null
                try {
                    val conversationIds = staged?.conversationDao()?.getAllIds().orEmpty()
                    conversationIds.forEach { remap.register(it) }
                    val knownRoles = source.assistants.map { it.id.toString() }.toSet()
                    val missingRoles = conversationIds.mapNotNull { staged!!.conversationDao().getConversationById(it)?.assistantId }
                        .distinct().filterNot { it in knownRoles }.map { id ->
                            me.rerere.rikkahub.data.model.Assistant(id = Uuid.parse(id), name = "导入的角色（原角色已缺失）")
                        }
                    val importSource = source.copy(assistants = source.assistants + missingRoles)
                    val completeJson = JsonInstant.encodeToJsonElement(importSource)
                    remap.collect(completeJson)
                    val imported = JsonInstant.decodeFromJsonElement<Settings>(remap.rewrite(completeJson))
                    importedCharacters = imported.assistants.size
                    val indexed = mutableListOf<Conversation>()
                    withContext(kotlinx.coroutines.NonCancellable) {
                    database.withTransaction {
                        for (id in conversationIds) {
                            val entity = staged!!.conversationDao().getConversationById(id) ?: continue
                            val nodes = staged.messageNodeDao().getNodesOfConversation(id).map { node ->
                                MessageNode(Uuid.parse(node.id), JsonInstant.decodeFromString(node.messages), node.selectIndex)
                            }
                            val original = repository.conversationEntityToConversation(entity, nodes).bindConfig(importSource)
                            val tree = JsonInstant.encodeToJsonElement(original)
                            remap.collect(tree)
                            val conversation = JsonInstant.decodeFromJsonElement<Conversation>(remap.rewrite(tree)).copy(
                                folderId = null, workspaceCwd = null
                            )
                            require(conversation.assistantId in imported.assistants.map { it.id }) { "备份对话缺少关联角色" }
                            val ready = conversation.copy(config = conversation.config?.copy(
                                chatModelId = null, mcpServers = emptySet(), workspaceId = null, enabledSkills = emptySet()
                            ))
                            repository.insertConversation(ready, index = false)
                            indexed += ready
                            count++
                            progress("正在提交对话 $count / ${conversationIds.size}")
                        }
                        settingsStore.update { current -> current.copy(
                            assistants = current.assistants + imported.assistants.map { it.copy(
                                chatModelId = null, mcpServers = emptySet(), workspaceId = null, enabledSkills = emptySet()
                            ) },
                            assistantTags = current.assistantTags + imported.assistantTags,
                            lorebooks = current.lorebooks + imported.lorebooks,
                            modeInjections = current.modeInjections + imported.modeInjections,
                            personas = current.personas + imported.personas,
                            groupChats = current.groupChats + imported.groupChats.map { it.copy(chatModelId = null) },
                            macroChatVariables = current.macroChatVariables + imported.macroChatVariables,
                        ) }
                        committed = true // Settings are durable; retain their new files even if a later DB commit fails.
                    }
                    committed = true
                    }
                    indexed.forEachIndexed { index, chat ->
                        progress("正在建立搜索索引 ${index+1} / ${indexed.size}")
                        repository.indexImportedConversation(chat)
                    }
                } finally { staged?.close() }
                importedCharacters to count
            } finally {
                if (!committed) copied.forEach { it.delete() }
                staging.deleteRecursively()
            }
        } }

    /** [upstreamCompatible] 为 true 时导出上游 rikkahub 能直接导入的备份，本地独有数据会被丢弃。 */
    suspend fun createBackup(
        includeDatabase: Boolean,
        includeFiles: Boolean,
        upstreamCompatible: Boolean = false,
    ): File = withContext(Dispatchers.IO) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val archive = File.createTempFile("backup_${timestamp}_", ".zip", context.cacheDir)
        val staging = Files.createTempDirectory(context.cacheDir.toPath(), "backup-").toFile()
        try {
            val settings = settingsStore.awaitLoaded()
            ZipOutputStream(FileOutputStream(archive)).use { zip ->
                zip.putNextEntry(ZipEntry("settings.json"))
                val settingsJson = json.encodeSettings(settings, settingsStore.launchCountFlow.first())
                val payload =
                    if (upstreamCompatible) UpstreamCompat.settingsJson(settingsJson, json) else settingsJson
                zip.write(payload.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                if (includeDatabase) {
                    val snapshot = File(staging, SQLiteConfiguration.DATABASE_NAME)
                    DatabaseBackup.createSnapshot(database.openHelper.writableDatabase, snapshot)
                    if (upstreamCompatible) UpstreamCompat.downgradeDatabase(context, snapshot, json)
                    addFile(zip, snapshot, DatabaseBackup.ARCHIVE_DATABASE)
                }
                if (includeFiles) {
                    for (folder in ATTACHMENT_FOLDERS) {
                        val directory = File(context.filesDir, folder)
                        val files = if (folder in NESTED_ATTACHMENT_FOLDERS) directory.walkTopDown().asSequence()
                        else directory.listFiles().orEmpty().asSequence()
                        for (file in files.filter { it.isFile }) {
                            currentCoroutineContext().ensureActive()
                            // A media result still being downloaded is incomplete and gets renamed when it finishes.
                            if (folder == FileFolders.MEDIA_CREATION &&
                                file.name.endsWith(MediaCreationFiles.PARTIAL_SUFFIX)
                            ) continue
                            val relative = file.relativeTo(directory).invariantSeparatorsPath
                            PendingRestore.resolveInside(directory, relative)
                            addFile(zip, file, "$folder/$relative")
                        }
                    }
                }
            }
            archive
        } catch (e: Throwable) {
            archive.delete()
            throw e
        } finally {
            staging.deleteRecursively()
        }
    }

    suspend fun stageRestore(archive: File, includeDatabase: Boolean, includeFiles: Boolean) =
        withContext(Dispatchers.IO) {
            restoreMutex.withLock {
                val restore = pendingRestore(context)
                val staging = restore.createStagingDirectory()
                try {
                    val payload = File(staging, "payload")
                    val stagedDatabase = File(payload, "database/${SQLiteConfiguration.DATABASE_NAME}")
                    val stagedWal = File(stagedDatabase.path + "-wal")
                    val seen = mutableSetOf<String>()
                    var restoredEntries = 0
                    ZipFile(archive).use { zip ->
                        // 恢复资源预算（移植自 Rikkahub-Revised）：防御异常/恶意归档的 zip 炸弹
                        val budget = BackupRestoreBudget()
                        for (entry in zip.entries()) {
                            currentCoroutineContext().ensureActive()
                            if (entry.isDirectory) continue
                            budget.consume(entry)
                            val target = when (entry.name) {
                                "settings.json" -> File(staging, "settings.json")
                                DatabaseBackup.ARCHIVE_DATABASE -> if (includeDatabase) stagedDatabase else null
                                DatabaseBackup.WAL -> if (includeDatabase) stagedWal else null
                                DatabaseBackup.SHM -> null // Rebuilt by SQLite; never restore shared-memory state.
                                else -> if (includeFiles && isAttachment(entry.name)) {
                                    PendingRestore.resolveInside(File(payload, "files"), entry.name)
                                } else null
                            } ?: continue
                            require(seen.add(entry.name)) { "Duplicate backup entry: ${entry.name}" }
                            check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs()) {
                                "Cannot create backup staging directory"
                            }
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(target).use { output ->
                                    input.copyTo(output)
                                    output.fd.sync()
                                }
                            }
                            restoredEntries++
                        }
                    }
                    require(restoredEntries > 0) { "No selected data found in the backup" }
                    require(!stagedWal.exists() || stagedDatabase.exists()) { "Backup WAL has no matching database" }
                    if (stagedDatabase.exists()) {
                        DatabaseBackup.normalize(context, stagedDatabase)
                        // 显式升级 + 结构对齐：恢复不再依赖 Room 的迁移容器，
                        // 任意旧版本备份（v1 起）都能升到当前结构
                        DatabaseBackup.upgradeToCurrent(context, stagedDatabase)
                        val room = openStagedDatabase(context, stagedDatabase)
                        try {
                            DatabaseBackup.checkpoint(room.openHelper.writableDatabase)
                        } finally {
                            room.close()
                        }
                        DatabaseBackup.removeSidecars(stagedDatabase)
                    }

                    val settingsFile = File(staging, "settings.json")
                    if (settingsFile.exists()) {
                        val migrated = SettingsJsonMigrator.migrate(settingsFile.readText())
                        val settings = json.decodeFromString<Settings>(migrated)
                        require(!settings.init) { "Backup contains uninitialized settings" }
                        // Persist the migrated value once, including generated IDs, for restart/retry consistency.
                        PendingRestore.writeDurably(settingsFile, json.encodeSettings(settings, json.launchCountOf(migrated)))
                    }
                    currentCoroutineContext().ensureActive()
                    restore.publish(staging)
                } finally {
                    staging.deleteRecursively()
                }
            }
        }

    /**
     * 打开暂存库并触发 Room 的 schema 校验。
     *
     * 正常情况下 Room 会拿当前 schema 校验对齐后的结构；上游合并遗留的结构漂移
     * （上游某个版本有、本地中间版本又没有的列）会让校验失败，这时退回「写回当前
     * identity hash」的宽松路径，让旧备份仍然能导入，问题只记在日志里。
     */
    private fun openStagedDatabase(context: Context, stagedDatabase: File): AppDatabase {
        return try {
            openRoomDatabase(context, stagedDatabase)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Staged database failed Room's schema validation, accepting the aligned schema", e)
            DatabaseBackup.acceptAlignedSchema(context, stagedDatabase)
            openRoomDatabase(context, stagedDatabase)
        }
    }

    /** Room 的迁移与 schema 校验都发生在第一次拿到数据库连接时。 */
    private fun openRoomDatabase(context: Context, stagedDatabase: File): AppDatabase {
        val room = AppDatabaseFactory.create(context, stagedDatabase.absolutePath)
        try {
            room.openHelper.writableDatabase
        } catch (e: Throwable) {
            runCatching { room.close() }
            throw e
        }
        return room
    }

    private fun isAttachment(name: String): Boolean {
        val folder = name.substringBefore('/')
        if (folder !in ATTACHMENT_FOLDERS || '/' !in name) return false
        val relative = name.substringAfter('/')
        require(relative.isNotBlank()) { "Invalid backup attachment: $name" }
        require(folder in NESTED_ATTACHMENT_FOLDERS || '/' !in relative) { "Invalid backup attachment: $name" }
        return true
    }

    /**
     * 备份恢复资源预算（移植自 Rikkahub-Revised BackupRestoreBudget）：
     * 限制条目数、单条目大小与总解压大小，防止恶意或损坏的归档耗尽磁盘/内存。
     */
    private class BackupRestoreBudget(
        private val maxEntries: Int = 100_000,
        private val maxEntryBytes: Long = 2L * 1024 * 1024 * 1024,
        private val maxTotalBytes: Long = 8L * 1024 * 1024 * 1024,
    ) {
        private var entries = 0
        private var totalBytes = 0L

        fun consume(entry: ZipEntry) {
            entries += 1
            require(entries <= maxEntries) { "Backup archive has too many entries" }
            val size = entry.size
            if (size >= 0) {
                require(size <= maxEntryBytes) { "Backup entry too large: ${entry.name}" }
                totalBytes += size
                require(totalBytes <= maxTotalBytes) { "Backup archive too large to restore" }
            }
        }
    }

    private fun addFile(zip: ZipOutputStream, file: File, name: String) {
        zip.putNextEntry(ZipEntry(name))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    companion object {
        private const val TAG = "BackupManager"

        private val ATTACHMENT_FOLDERS =
            listOf(FileFolders.UPLOAD, FileFolders.SKILLS, FileFolders.FONTS, FileFolders.MEDIA_CREATION,
                "background-presets", "conversation-backgrounds")

        /** Backed up with their subdirectories; the other folders only contain top-level files. */
        private val NESTED_ATTACHMENT_FOLDERS = setOf(FileFolders.SKILLS, FileFolders.MEDIA_CREATION)

        // 启动次数不在 Settings 里，备份文件里仍放在 settings.json 的这个字段，和旧备份保持一致
        private const val LAUNCH_COUNT_KEY = "launchCount"

        private fun Json.encodeSettings(settings: Settings, launchCount: Int): String {
            val fields = encodeToJsonElement(settings).jsonObject + (LAUNCH_COUNT_KEY to JsonPrimitive(launchCount))
            return encodeToString(JsonObject(fields))
        }

        // 更早的备份没有这个字段，按 0 恢复，和它还在 Settings 里时的默认值一致
        private fun Json.launchCountOf(settingsJson: String): Int =
            parseToJsonElement(settingsJson).jsonObject[LAUNCH_COUNT_KEY]?.jsonPrimitive?.intOrNull ?: 0

        private fun pendingRestore(context: Context) = PendingRestore(
            root = File(context.noBackupFilesDir, "backup-restore"),
            databaseFile = context.getDatabasePath(SQLiteConfiguration.DATABASE_NAME),
            filesDir = context.filesDir,
        )

        fun cleanupCompletedRestores(context: Context) = pendingRestore(context).cleanupCompleted()

        /** Must finish before Koin, Room, SettingsStore or any background consumers are initialized. */
        suspend fun applyPendingRestore(context: Context, json: Json): Boolean = withContext(Dispatchers.IO) {
            pendingRestore(context).apply { settingsJson ->
                SettingsStore.restoreBeforeInitialization(
                    context = context,
                    settings = json.decodeFromString<Settings>(settingsJson),
                    launchCount = json.launchCountOf(settingsJson),
                )
            }
        }
    }
}
