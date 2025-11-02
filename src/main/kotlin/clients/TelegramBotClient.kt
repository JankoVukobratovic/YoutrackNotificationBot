package org.jankos.clients

import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.HandleCommand
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import clients.YouTrackClient
import com.github.kotlintelegrambot.Bot
import manager.SubscriptionManager
import models.AppConfig
import models.YouTrackActivity
import models.YouTrackIssue
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.collections.mapNotNull

class TelegramBotClient(
    private val youTrackClient: YouTrackClient,
    config: AppConfig
) {
    private val projectUrl = config.youtrack.baseUrl.removeSuffix("/api");
    private val projectShortName = config.project.shortName

    fun setupDispatchers(dispatcher: Dispatcher) {
        dispatcher.command("start", handleStartCommand())
        dispatcher.command("help", handleHelpCommand())
        dispatcher.command("issues", handleIssuesCommand())
        dispatcher.command("activities", handleActivitiesCommand())
        dispatcher.command("subscribe", handleSubscribeCommand())
        dispatcher.command("unsubscribe", handleUnsubscribeCommand())
        dispatcher.command("pingsubs", handlePingCommand())
    }

    private fun handleHelpCommand(): HandleCommand = {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = """/start - A welcome message
                    /issues - Last 24 hours of issues activity
                    /activities - Notifications from the last 24 hours
                    /subscribe - Subscribe this chat to receive regular notifications
                    /unsubscribe - Unsubscribe this chat from regular notifications"""
        )
    }

    // JUST A TEST :)
    private fun handlePingCommand(): HandleCommand = {
        val pingMessage = "🏓Ping pong!🏓 From: ${message.chat.title} \nWith id: ${message.chat.id}"

        sendToAll(bot, pingMessage, SubscriptionManager.subscriptions)
    }

    private fun sendToAll(bot: Bot, messageText: String, chatIds: Iterable<Long>){
        for (id in chatIds){
            bot.sendMessage(
                chatId = ChatId.fromId(id),
                text = messageText
            )
        }
    }

    private fun handleSubscribeCommand(): HandleCommand = {
        val messageText: String = if (SubscriptionManager.add(message.chat.id)) {
            "✅Chat subscribed!"
        } else {
            "⁉️Already subscribed!"
        }
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = messageText
        )
    }

    private fun handleUnsubscribeCommand(): HandleCommand = {
        val messageText: String = if(SubscriptionManager.remove(message.chat.id)){
            "✅Chat removed from subscription list!"
        } else {
            "⁉️Chat wasn't even subscribed :/"
        }
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = messageText
        )
    }

    private fun handleStartCommand(): HandleCommand = {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "Hello! I track YouTrack issues for $projectShortName. " +
                    "Try /help for a list of commands."
        )
    }


    private fun handleActivitiesCommand(period: String = "1d"): HandleCommand = {
        CoroutineScope(Dispatchers.IO).launch {
            val chatId = ChatId.fromId(message.chat.id)

            bot.sendMessage(
                chatId = chatId,
                text = "⏳ Fetching activities for $projectShortName from the last $period...",
                parseMode = ParseMode.MARKDOWN
            )

            try {
                val activities = youTrackClient.getActivities(period)
                val responseMessage = formatActivityList(activities)

                bot.sendMessage(
                    chatId = chatId,
                    text = responseMessage,
                    parseMode = ParseMode.MARKDOWN
                )
            } catch (e: Exception) {
                println("Critical Error in bot command: ${e.message}")
                bot.sendMessage(
                    chatId = chatId,
                    text = "🚨 **Internal Error Occurred** 🚨\n", //these emojis are so funny.
                    parseMode = ParseMode.MARKDOWN
                )
            }
        }
    }

    private fun formatActivityList(activities: List<YouTrackActivity>): String {
        if (activities.isEmpty()) {
            return "✅ No new activities found for the project `${projectShortName}` in the requested period."
        }

        val notifications = activities.mapNotNull { activity: YouTrackActivity ->
            val issueLink =
                "[${activity.target.idReadable}]($projectUrl)}/issue/${activity.target.idReadable})"
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
                    "*${author}* added an attachment to ${issueLink}"
                }

                "IssueFieldChangeCategory" -> {
                    val fieldName = activity.field?.name ?: "a field"
                    val addedValue = activity.added?.firstOrNull()?.name ?: activity.added?.firstOrNull()?.presentation
                    val removedValue =
                        activity.removed?.firstOrNull()?.name ?: activity.removed?.firstOrNull()?.presentation

                    when {
                        addedValue != null && removedValue != null -> {
                            "*${author}* changed *${fieldName}* on ${issueLink} \nfrom *${removedValue}* to *${addedValue}*."
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

    private fun formatTimestamp(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        // timezone-aware
        val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm:ss")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())

        return formatter.format(instant)
    }

    private fun handleIssuesCommand(): HandleCommand = {
        CoroutineScope(Dispatchers.IO).launch { //we love co-routines don't we? (internalized traumas from the parallel programming course are waking up)
            val chatId = ChatId.fromId(message.chat.id)
            bot.sendMessage(chatId = chatId, text = "Getting issues updated in the last 24 hours...")

            val issues = youTrackClient.getUpdatedIssues("1d")

            val responseMessage = formatIssueList(issues)
            bot.sendMessage(chatId = chatId, text = responseMessage, parseMode = ParseMode.MARKDOWN)
        }
    }

    private fun formatIssueList(issues: List<YouTrackIssue>): String {
        if (issues.isEmpty()) {
            return "✅ No issues updated in the period for project $projectShortName."
        }

        val sb = StringBuilder("📢 **Latest Updates for $projectShortName**:\n\n")  //funny emojies

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

}