package org.jankos

import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.HandleCommand
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class TelegramBot(private val youTrackClient: YouTrackClient,
                  config: AppConfig) {

    private val projectShortName = config.project.shortName

    fun setupDispatchers(dispatcher: Dispatcher) {
        dispatcher.command("start", handleStartCommand())
        dispatcher.command("updates", handleUpdatesCommand())
        dispatcher.command("updates_day", handleCustomUpdatesCommand("1d"))
        dispatcher.command("updates_week", handleCustomUpdatesCommand("7d"))
    }

    private fun handleStartCommand(): HandleCommand = {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "Hello! I track YouTrack issues for $projectShortName. " +
                    "Try /updates for the last 24 hours of activity."
        )
    }

    private fun handleUpdatesCommand(): HandleCommand = {
        CoroutineScope(Dispatchers.IO).launch { //we love co-routines don't we? (internalized traumas from the parallel programming course are waking up)
            val chatId = ChatId.fromId(message.chat.id)
            bot.sendMessage(chatId = chatId, text = "Getting issues updated in the last 24 hours...")

            val issues = youTrackClient.getUpdatedIssues("-1d")

            val responseMessage = formatIssueList(issues)
            bot.sendMessage(chatId = chatId, text = responseMessage, parseMode = ParseMode.MARKDOWN)
        }
    }

    private fun handleCustomUpdatesCommand(period: String): HandleCommand = {
        CoroutineScope(Dispatchers.IO).launch {
            val chatId = ChatId.fromId(message.chat.id)
            bot.sendMessage(chatId = chatId, text = "Fetching issues updated in the last $period...")

            // The format for YouTrack query is "-Nd" for N days ago
            val queryPeriod = if (period.endsWith("d")) "-$period" else period


            val issues = youTrackClient.getUpdatedIssues(queryPeriod)

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
            val status = issue.customFields
                .firstOrNull { it.name == "State" }
                ?.value?.toString()
                ?: "Unassigned" // Kotlin syntax is weird.


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