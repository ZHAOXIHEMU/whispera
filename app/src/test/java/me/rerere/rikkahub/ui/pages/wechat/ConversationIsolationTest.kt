package me.rerere.rikkahub.ui.pages.wechat

import org.junit.Assert.*
import org.junit.Test
import me.rerere.rikkahub.data.model.*
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.utils.JsonInstant
import kotlin.uuid.Uuid

class ConversationIsolationTest {
    @Test fun identityAndNotesAreCopiedAndSurviveSerialization(){
        val card=Assistant(name="原始角色",avatar=Avatar.Emoji("A"))
        val defaults=Settings(assistants=listOf(card),assistantId=card.id,authorNoteEnabled=true,authorNoteEntries=listOf(AuthorNoteEntry(id="note",content="旧备注",depth=7,interval=3)))
        val first=Conversation.ofId(Uuid.random(),card.id).bindConfig(defaults)
        val modified=defaults.copy(assistants=listOf(card.copy(name="新版角色",avatar=Avatar.Emoji("B"))),authorNoteEnabled=false,authorNoteEntries=listOf(AuthorNoteEntry(id="note",content="新备注",enabled=false)))
        val second=Conversation.ofId(Uuid.random(),card.id).bindConfig(modified)
        assertEquals("原始角色",modified.getAssistantOf(first).name)
        assertEquals("新版角色",modified.getAssistantOf(second).name)
        assertTrue(first.config!!.authorNotes!!.enabled)
        assertFalse(second.config!!.authorNotes!!.enabled)
        assertEquals("旧备注",first.config!!.authorNotes!!.entries.single().content)
        assertEquals(7,first.config!!.authorNotes!!.entries.single().depth)
        assertEquals(3,first.config!!.authorNotes!!.entries.single().interval)
        val edited=first.copy(title="会话一",config=first.config!!.copy(characterName="会话角色",hiddenFromChatList=true,authorNotes=first.config!!.authorNotes!!.copy(enabled=false)))
        assertEquals("新版角色",modified.assistants.single().name)
        assertEquals("会话角色",modified.getAssistantOf(edited).name)
        assertTrue(first.config!!.authorNotes!!.enabled)
        val restored=JsonInstant.decodeFromString<Conversation>(JsonInstant.encodeToString(edited))
        assertEquals(edited.id,restored.id)
        assertEquals("会话一",restored.title)
        assertEquals(edited.config,restored.config)
        val modelUpdate=edited.withAssistantUpdate(modified.getAssistantOf(edited).copy(enableWebSearch=true),modified)
        assertEquals(edited.config!!.authorNotes,modelUpdate.config!!.authorNotes)
        assertEquals("会话角色",modelUpdate.config!!.characterName)
        assertTrue(modelUpdate.config!!.hiddenFromChatList)
        assertTrue(restored.config!!.hiddenFromChatList)
        assertFalse(second.config!!.hiddenFromChatList)
    }
}
