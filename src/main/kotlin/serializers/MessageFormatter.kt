package serializers

import models.AppConfig
import models.YouTrackActivity
import models.YouTrackIssue
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object MessageFormatter {
    var projectShortName: String = ""
    var projectUrl: String = ""

    fun initialize(config: AppConfig) {
        projectShortName = config.project.shortName
        projectUrl = config.youtrack.baseUrl.removeSuffix("/api")
    }

    fun formatActivityList(activities: List<YouTrackActivity>): String {
        if (activities.isEmpty()) {
            return "✅ No new activities found for the project `${projectShortName}` in the requested period."
        }

        val notifications = activities.mapNotNull { activity: YouTrackActivity ->
            val issueLink =
                "[${activity.target.idReadable}]($projectUrl)/issue/${activity.target.idReadable})"
            val author = activity.author.login
            val timestamp = formatTimestamp(activity.timestamp)
            val summary = activity.target.summary?.take(60) ?: "No summary"

            val notificationText = when (activity.category.id) {
                "IssueCreatedCategory" -> {
                    "*${author}* created issue ${issueLink}: \n_${summary}_"
                }

                "IssueCommentCategory" -> {
                    val commentText = activity.text?.take(80)?.replace("\n", " ") ?: ""
                    "*${author}* commented on ${issueLink}: \n_\"${commentText}\"_..."
                }

                "IssueAttachmentCategory" -> {
                    "*${author}* added an attachment to $issueLink"
                }

                "IssueFieldChangeCategory" -> {
                    val fieldName = activity.field?.name ?: "a field"
                    val addedValue = activity.added?.firstOrNull()?.name ?: activity.added?.firstOrNull()?.presentation
                    val removedValue =
                        activity.removed?.firstOrNull()?.name ?: activity.removed?.firstOrNull()?.presentation

                    when {
                        addedValue != null && removedValue != null -> {
                            "*${author}* changed *${fieldName}* on $issueLink \nfrom *${removedValue}* to *${addedValue}*."
                        }

                        addedValue != null && removedValue == null -> {
                            "*${author}* added *${addedValue}* to *${fieldName}* on ${issueLink}."
                        }

                        addedValue == null && removedValue != null -> {
                            "*${author}* removed *${removedValue}* from *${fieldName}* on ${issueLink}."
                        }
                        else -> "*${author}* updated ${issueLink}."
                    }
                }
                else -> null // Ignore others
            }

            if (notificationText != null) {
                "[$timestamp] $notificationText"
            } else {
                null
            }
        }.joinToString(separator = "\n\n\n")

        return if (notifications.isNotEmpty()) {
            "📢 *Recent Activities for ${projectShortName}*:\n\n$notifications"
        } else {
            "✅ No significant activities found for the project `${projectShortName}` in the requested period."
        }
    }


    fun formatIssueList(issues: List<YouTrackIssue>): String {
        if (issues.isEmpty()) {
            return "✅ No issues updated in the period for project $projectShortName."
        }

        val sb = StringBuilder("📢 **Latest Updates for $projectShortName**:\n\n")  //funny emojis

        val timeFormatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())

        for (issue in issues) {
            val stateField = issue.customFields.firstOrNull { it.name == "State" }

            val status = stateField?.value?.firstOrNull()?.name ?: "Unassigned"

            val updatedTime = issue.updated.let {
                timeFormatter.format(Instant.ofEpochMilli(it))
            } ?: "N/A"

            sb.append("**[${issue.idReadable}]** - ${issue.summary}\n")
                .append("Status: `$status` | Last Update: `$updatedTime`\n")
                .append("---\n")
        }

        return sb.toString()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        // timezone-aware
        val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm:ss")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())

        return formatter.format(instant)
    }

}