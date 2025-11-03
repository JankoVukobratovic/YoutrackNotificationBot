import clients.TelegramBotClient
import clients.YouTrackClient
import config.ConfigurationLoader
import repository.Repository
import serializers.MessageFormatter
import services.SchedulingService
import kotlin.time.Duration.Companion.minutes

fun main() {
    println("Starting YouTrack Telegram Bot")

    val config = try {
        ConfigurationLoader.load()
    } catch (e: Exception) {
        System.err.println("FATAL: Failed to load configuration! Probably config.json :(.")
        e.printStackTrace()
        return
    }
    Repository.initialize(config)

    MessageFormatter.initialize(config)

    val youTrackClient = YouTrackClient(config)

    val telegramBotClient = TelegramBotClient(youTrackClient, config)
    telegramBotClient.startPolling()

    SchedulingService.startScheduling(
        handler = { telegramBotClient.handleActivitiesNews() },
        checkInterval = config.selfConfig.updateIntervalMinutes.minutes
    )

    println("Bot started. Listening for updates...")
}