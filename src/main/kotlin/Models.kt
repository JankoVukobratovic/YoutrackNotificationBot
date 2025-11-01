package org.jankos

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YouTrackIssue(
    val idReadable: String,
    val summary: String,
    val updated: Long, //timestamp
    val customFields: List<CustomField>
)

@Serializable
data class CustomField(
    val name: String,
    val value: JsonElement? = null
)

@Serializable
data class CustomFieldValue(
    val name: String
)