package me.rerere.rikkahub.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.Uuid

/** Serialize deletion with late streaming/title/config saves so a deleted chat cannot return. */
internal class ConversationPersistenceGate {
    private class Entry {
        val mutex = Mutex()
        var deleted = false
    }
    private val entries = java.util.concurrent.ConcurrentHashMap<Uuid, Entry>()

    suspend fun save(id: Uuid, block: suspend () -> Unit) {
        val entry = entries.getOrPut(id) { Entry() }
        entry.mutex.withLock { if (!entry.deleted) block() }
    }

    suspend fun delete(id: Uuid, block: suspend () -> Unit) {
        val entry = entries.getOrPut(id) { Entry() }
        entry.mutex.withLock {
            if (!entry.deleted) {
                block()
                entry.deleted = true
            }
        }
    }
}
