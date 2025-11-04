package models

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val telegram: TelegramConfig,
    val youtrack: YouTrackConfig,
    val project: ProjectConfig,
    val selfConfig: SelfConfig
)

@Serializable
data class SelfConfig(
    val saveFile: String,
    val updateIntervalMinutes: Double
)

@Serializable
data class TelegramConfig(
    val botToken: String,
    val targetChatId: Long
)

@Serializable
data class YouTrackConfig(
    val baseUrl: String,
    val apiToken: String
)

@Serializable
data class ProjectConfig(
    val shortName: String
)