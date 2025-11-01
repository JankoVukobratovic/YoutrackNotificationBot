package models
import kotlinx.serialization.Serializable


@Serializable
data class YouTrackActivity(
    val id: String,
    val timestamp: Long,
    val category: ActivityCategory,
    val target: ActivityTarget,
    val author: ActivityAuthor,

    val field: FieldName? = null,
    val added: List<ActivityValue>? = null,
    val removed: List<ActivityValue>? = null,

    val text: String? = null
)

@Serializable
data class ActivityCategory(
    val id: String
)

@Serializable
data class ActivityTarget(
    val idReadable: String,
    val summary: String? = null
)

@Serializable
data class ActivityAuthor(
    val login: String
)

@Serializable
data class FieldName(
    val name: String
)

@Serializable
data class ActivityValue(
    val name: String? = null,
    val presentation: String? = null
)