package clients

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.HandleCommand
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.logging.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import models.AppConfig
import serializers.MessageFormatter
import services.IntervalService
import services.SubscriptionService
import java.time.Duration
import java.time.Instant

class TelegramBotClient(
    private val youTrackClient: YouTrackClient, config: AppConfig
) {

    val botInstance: Bot = bot {
        token = config.telegram.botToken
        logLevel = LogLevel.Error

        dispatch {
            setupDispatchers(this)
        }
    }

    private val projectShortName = config.project.shortName

    fun setupDispatchers(dispatcher: Dispatcher) {
        dispatcher.command("start", handleStartCommand())
        dispatcher.command("help", handleHelpCommand())
        dispatcher.command("issues", handleIssuesCommand())
        dispatcher.command("activities", handleActivitiesCommand())
        dispatcher.command("subscribe", handleSubscribeCommand())
        dispatcher.command("unsubscribe", handleUnsubscribeCommand())
        dispatcher.command("pingsubs", handlePingCommand())
        dispatcher.command("newissue", handleNewIssueCommand())
    }

    fun startPolling() {
        botInstance.startPolling()
    }


    private fun handleNewIssueCommand(): HandleCommand = {
        CoroutineScope(Dispatchers.IO).launch {
            val chatId = ChatId.fromId(message.chat.id)

            val summary = message.text?.removePrefix("/newissue")?.trim() ?: ""

            if (summary.isBlank()) {
                bot.sendMessage(
                    chatId = chatId,
                    text = "⚠️ Please provide a **summary** for the new issue.\n\nUsage: `/newissue <issue summary>",
                    parseMode = ParseMode.MARKDOWN
                )
                return@launch
            }
            bot.sendMessage(
                chatId = chatId,
                text = "⏳ Creating new issue in project *$projectShortName* with summary: `$summary`...",
                parseMode = ParseMode.MARKDOWN
            )
            try {
                val newIssueIdReadable =
                    youTrackClient.createIssue(summary, "Reported from Telegram by ${message.from?.firstName}")
                val issueUrl = "${youTrackClient.youTrackUrl.removeSuffix("/api")}/issue/$newIssueIdReadable"

                val responseMessage = """
                    ✅ **Issue Created!**
                    
                    *Summary:* $summary
                    *ID:* [$newIssueIdReadable]($issueUrl)
                    
                    YouTrack URL: $issueUrl
                """.trimIndent()


                bot.sendMessage(
                    chatId = chatId,
                    text = responseMessage,
                    parseMode = ParseMode.MARKDOWN
                )


            } catch (e: Exception) {
                System.err.println("Error creating issue: ${e.message}")
                bot.sendMessage(
                    chatId = chatId,
                    text = "🚨 **Failed to Create Issue** 🚨\nAn internal error occurred. Check logs for details.",
                    parseMode = ParseMode.MARKDOWN
                )
            }
        }
    }

    private fun handleHelpCommand(): HandleCommand = {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id), text = """/start - A welcome message
                    /issues - Last 24 hours of issues activity
                    /activities - Notifications from the last 24 hours
                    /subscribe - Subscribe this chat to receive regular notifications
                    /unsubscribe - Unsubscribe this chat from regular notifications
                    /newissue <summary> - Posts a new issue to the YouTrack project instance"""
        )
    }

    // JUST A TEST :)
    fun handlePingCommand(): HandleCommand = {
        val pingMessage = "🏓Ping pong!🏓 From: ${message.chat.title} \nWith id: ${message.chat.id}"

        sendToAll(botInstance, pingMessage, SubscriptionService.getAllSubscriptions())
    }

    @Suppress("unused")
    fun pingAll() {
        val pingMessage = "🏓Ping pong!🏓 \nFrom scheduler!"
        sendToAll(botInstance, pingMessage, SubscriptionService.getAllSubscriptions())

    }

    private fun sendToAll(bot: Bot, messageText: String, chatIds: Iterable<Long>) {
        for (id in chatIds) {
            bot.sendMessage(
                chatId = ChatId.fromId(id), text = messageText
            )
        }
    }

    private fun handleSubscribeCommand(): HandleCommand = {
        val messageText: String = if (SubscriptionService.add(message.chat.id)) {
            "✅Chat subscribed!"
        } else {
            "⁉️Already subscribed!"
        }
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id), text = messageText
        )
    }

    private fun handleUnsubscribeCommand(): HandleCommand = {
        val messageText: String = if (SubscriptionService.remove(message.chat.id)) {
            "✅Chat removed from subscription list!"
        } else {
            "⁉️Chat wasn't even subscribed :/"
        }
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id), text = messageText
        )
    }

    private fun handleStartCommand(): HandleCommand = {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "Hello! I track YouTrack issues for $projectShortName. " + "Try /help for a list of commands."
        )
    }

    private fun handleActivitiesCommand(period: Duration = Duration.ofDays(1)): HandleCommand = {
        CoroutineScope(Dispatchers.IO).launch {
            val chatId = ChatId.fromId(message.chat.id)

            bot.sendMessage(
                chatId = chatId,
                text = "⏳ Fetching activities for $projectShortName from the last $period...",
                parseMode = ParseMode.MARKDOWN
            )

            try {
                val activities = youTrackClient.getActivities(Instant.now().toEpochMilli() - period.toMillis())
                val responseMessage = MessageFormatter.formatActivityList(activities)

                bot.sendMessage(
                    chatId = chatId, text = responseMessage, parseMode = ParseMode.MARKDOWN
                )
            } catch (e: Exception) {
                println("Critical Error in bot command: ${e.message}")
                bot.sendMessage(
                    chatId = chatId, text = "🚨 **Internal Error Occurred** 🚨\n", //these emojis are so funny.
                    parseMode = ParseMode.MARKDOWN
                )
            }
        }
    }

    private fun handleIssuesCommand(): HandleCommand = {
        CoroutineScope(Dispatchers.IO).launch { //we love co-routines don't we? (internalized traumas from the parallel programming course are waking up)
            val chatId = ChatId.fromId(message.chat.id)
            bot.sendMessage(chatId = chatId, text = "Getting issues updated in the last 24 hours...")

            val issues = youTrackClient.getUpdatedIssues("1d")

            val responseMessage = MessageFormatter.formatIssueList(issues)
            bot.sendMessage(chatId = chatId, text = responseMessage, parseMode = ParseMode.MARKDOWN)
        }
    }

    fun handleActivitiesNews() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val activities = youTrackClient.getActivities(IntervalService.getLastCheckTime().toEpochMilli())
                if (activities.isEmpty()) {
                    println("no new activities")
                    return@launch
                }
                val messageToSend = MessageFormatter.formatActivityList(activities)


                sendToAll(botInstance, messageToSend, SubscriptionService.getAllSubscriptions())
                IntervalService.setLastCheckTime(Instant.now())
            } catch (e: Exception) {
                println("Critical Error in handleActivitiesNews: ${e.message}")
            }
        }
    }
}