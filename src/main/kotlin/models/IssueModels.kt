package models

import kotlinx.serialization.Serializable
import serializers.YouTrackValueAsListSerializer


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
    @Serializable(with = YouTrackValueAsListSerializer::class)
    val value: List<CustomFieldValue>? = null
)

@Serializable
data class CustomFieldValue(
    val name: String? = null
)