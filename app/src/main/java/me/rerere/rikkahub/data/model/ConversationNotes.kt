package me.rerere.rikkahub.data.model

import kotlinx.serialization.Serializable
import me.rerere.ai.core.MessageRole
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.authorNoteEntriesOrLegacy

@Serializable
data class ConversationNotes(
    val enabled:Boolean,
    val entries:List<AuthorNoteEntry>,
    val position:AuthorNotePosition,
    val depth:Int,
    val role:MessageRole,
    val interval:Int,
) {
    fun applyTo(settings:Settings)=settings.copy(authorNoteEnabled=enabled,authorNoteEntries=entries,authorNote="",authorNotePosition=position,authorNoteDepth=depth,authorNoteRole=role,authorNoteInterval=interval)
    companion object {
        fun capture(settings:Settings)=ConversationNotes(settings.authorNoteEnabled,settings.authorNoteEntriesOrLegacy().map{it.copy()},settings.authorNotePosition,settings.authorNoteDepth,settings.authorNoteRole,settings.authorNoteInterval)
    }
}
