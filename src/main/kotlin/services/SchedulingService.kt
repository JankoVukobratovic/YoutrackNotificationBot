package services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object SchedulingService {

    fun startScheduling(
        handler: () -> Unit,
        checkInterval: Duration
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            println("Scheduler started for method ${handler.toString()}!")

            delay(10.seconds) // to ensure bot started properly ig? doesn't crash with this.

            while (true) {
                try {
                    println("Scheduler invoking method ${handler.toString()}")
                    handler.invoke()
                } catch (e: Exception) {
                    System.err.println("Scheduler task failed: ${e.message}")
                }
                delay(checkInterval)
            }
        }
    }
}