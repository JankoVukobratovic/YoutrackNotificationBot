package services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object SchedulingService {

    private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())

    fun startScheduling(
        handler: () -> Unit,
        checkInterval: Duration
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            println("Scheduler started for method ${handler.toString()}!")

            delay(10.seconds) // to ensure bot started properly ig? doesn't crash with this.

            while (true) {
                try {
                    val currentTime = Instant.now().atZone(ZoneId.systemDefault()).format(TIME_FORMATTER)
                    println("\n[$currentTime]Scheduler invoking method ${handler.toString()}\n")
                    handler.invoke()
                } catch (e: Exception) {
                    System.err.println("Scheduler task failed: ${e.message}\n")
                }
                delay(checkInterval)
            }
        }
    }
}