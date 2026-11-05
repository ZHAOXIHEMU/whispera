package me.rerere.rikkahub.data.sync

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class AppendRemapperTest {
    private val role = "11111111-1111-4111-8111-111111111111"
    private val chat = "22222222-2222-4222-8222-222222222222"

    @Test fun referencesFollowNewIdsAndEveryImportIsIndependent() {
        val source = Json.parseToJsonElement("""{"id":"$role","name":"角色甲","chat":{"id":"$chat","assistantId":"$role"},"variables":{"$chat":"value"},"text":"原提示词"}""")
        val remap = AppendRemapper().apply { collect(source) }
        val result = remap.rewrite(source).jsonObject
        assertNotEquals(role, result["id"]!!.jsonPrimitive.content)
        assertEquals(result["id"], result["chat"]!!.jsonObject["assistantId"])
        assertTrue(result["variables"]!!.jsonObject.containsKey(result["chat"]!!.jsonObject["id"]!!.jsonPrimitive.content))
        assertEquals("原提示词", result["text"]!!.jsonPrimitive.content)
        assertEquals(role, source.jsonObject["id"]!!.jsonPrimitive.content)
        val other = AppendRemapper().apply { collect(source) }.rewrite(source).jsonObject
        assertNotEquals(result["id"], other["id"])
        assertEquals(result, remap.rewrite(source))
    }

    @Test fun rewritesOnlyKnownLocalAttachmentsAndPreservesRemoteUrls() {
        val remap = AppendRemapper(mapOf("upload/图片.png" to "file:/private/upload/new.png"))
        val input = JsonArray(listOf(
            JsonPrimitive("file:///data/user/0/huadeng/files/upload/%E5%9B%BE%E7%89%87.png"),
            JsonPrimitive("/data/data/huadeng/files/upload/图片.png"),
            JsonPrimitive("https://example.org/files/upload/图片.png"),
            JsonPrimitive("file:///data/user/0/huadeng/files/upload/missing.png")
        ))
        val output = remap.rewrite(input).jsonArray
        assertEquals("file:/private/upload/new.png", output[0].jsonPrimitive.content)
        assertEquals(output[0], output[1])
        assertEquals(input[2], output[2])
        assertEquals(input[3], output[3])
    }
}
