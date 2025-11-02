package models

import kotlinx.serialization.Serializable

@Serializable
data class SaveData(
    val subscribedIds: Set<Long>,
    val lastCheckTime: Long
)
