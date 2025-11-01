import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.logging.LogLevel
import org.jankos.clients.TelegramBotClient
import clients.YouTrackClient
import config.ConfigurationLoader

fun main() {
    println("Starting YouTrack Telegram Bot")

    val config = try {
        ConfigurationLoader.load()
    } catch (e: Exception) {
        System.err.println("FATAL: Failed to load configuration! Probably config.json :(.")
        e.printStackTrace()
        return
    }

    val youTrackClient = YouTrackClient(config)

    val youTrackBotLogic = TelegramBotClient(youTrackClient, config)

    val bot = bot {
        token = config.telegram.botToken
        logLevel = LogLevel.Error
        dispatch {
            youTrackBotLogic.setupDispatchers(this)
        }
    }
    bot.startPolling()
    println("Bot started. Listening for updates...")
}