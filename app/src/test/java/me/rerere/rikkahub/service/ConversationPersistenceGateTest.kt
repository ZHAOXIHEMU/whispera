package me.rerere.rikkahub.service

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.uuid.Uuid

class ConversationPersistenceGateTest {
    @Test fun deletionWaitsForExistingSaveAndRejectsLateWrites() = runBlocking {
        val gate = ConversationPersistenceGate()
        val id = Uuid.random()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val writing = launch(start = CoroutineStart.UNDISPATCHED) {
            gate.save(id) { entered.complete(Unit); release.await(); events += "saved" }
        }
        entered.await()
        val deleting = launch(start = CoroutineStart.UNDISPATCHED) { gate.delete(id) { events += "deleted" } }
        assertTrue(events.isEmpty())
        release.complete(Unit)
        writing.join(); deleting.join()
        gate.save(id) { fail("A late streaming or title update resurrected the deleted chat") }
        assertEquals(listOf("saved", "deleted"), events)
        gate.save(Uuid.random()) { events += "other chat" }
        assertEquals(3, events.size)
    }

    @Test fun failedDeletionCanBeRetriedWithoutSuppressingSaves() = runBlocking {
        val gate = ConversationPersistenceGate()
        val id = Uuid.random()
        try { gate.delete(id) { throw java.io.IOException("storage failure") }; fail("Expected storage error") }
        catch (_: java.io.IOException) { }
        var saved = false
        gate.save(id) { saved = true }
        assertTrue(saved)
        gate.delete(id) { }
        gate.save(id) { fail("Retry should have completed deletion") }
    }
}
