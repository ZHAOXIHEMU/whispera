package me.rerere.rikkahub.data.sync

import kotlinx.serialization.json.*
import java.net.URI
import kotlin.uuid.Uuid

/** New IDs and private file names on every import: existing records are never update targets. */
internal class AppendRemapper(private val files: Map<String, String> = emptyMap()) {
    private val ids = mutableMapOf<String, String>()

    fun register(id: String): String = ids.getOrPut(id) { Uuid.random().toString() }

    fun collect(value: JsonElement) {
        when (value) {
            is JsonObject -> {
                (value["id"] as? JsonPrimitive)?.contentOrNull?.let { id ->
                    if (runCatching { Uuid.parse(id) }.isSuccess) register(id)
                }
                value.values.forEach(::collect)
            }
            is JsonArray -> value.forEach(::collect)
            else -> Unit
        }
    }

    fun rewrite(value: JsonElement): JsonElement = when (value) {
        is JsonObject -> JsonObject(value.entries.associate { (key, item) -> (ids[key] ?: key) to rewrite(item) })
        is JsonArray -> JsonArray(value.map(::rewrite))
        is JsonPrimitive -> if (value.isString) JsonPrimitive(ids[value.content] ?: fileReference(value.content)) else value
        else -> value
    }

    private fun fileReference(value: String): String {
        if (!value.startsWith("file:") && !value.startsWith("/data/")) return value
        val path = if (value.startsWith("file:")) runCatching { URI(value).path }.getOrNull() ?: return value else value
        val relative = path.substringAfter("/files/", "")
        return files[relative] ?: value
    }
}
