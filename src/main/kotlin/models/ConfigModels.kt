package models
import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val telegram: TelegramConfig,
    val youtrack: YouTrackConfig,
    val project: ProjectConfig
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