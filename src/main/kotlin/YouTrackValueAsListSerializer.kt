package org.jankos

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import models.CustomFieldValue

class YouTrackValueAsListSerializer : KSerializer<List<CustomFieldValue>?> {
    private val listSerializer = ListSerializer(CustomFieldValue.serializer())

    override val descriptor = listSerializer.descriptor

    override fun serialize(encoder: Encoder, value: List<CustomFieldValue>?) {
        listSerializer.serialize(encoder, value ?: emptyList())
    }

    override fun deserialize(decoder: Decoder): List<CustomFieldValue>? {
        val jsonDecoder = decoder as? JsonDecoder ?: throw IllegalStateException("Only JsonDecoder is supported")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonNull -> null
            is JsonArray -> jsonDecoder.json.decodeFromJsonElement(listSerializer, element)
            is JsonObject -> {
                val singleValue = jsonDecoder.json.decodeFromJsonElement(CustomFieldValue.serializer(), element)
                listOf(singleValue)
            }
            else -> throw IllegalArgumentException("Unexpected JSON element type for CustomField value: ${element::class.simpleName}")
        }
    }

}