package services

import repository.Repository
import java.time.Duration
import java.time.Instant

object IntervalService {
    fun getLastCheckTime(): Instant {
        return Instant.ofEpochMilli(Repository.saveData.lastCheckTime)
    }

    fun setLastCheckTime(newInstant: Instant) {
        Repository.setLastCheckTime(newInstant.toEpochMilli())
    }

    fun getSinceLast(): Duration {
        val lastCheckTime = getLastCheckTime()
        val now = Instant.now()

        var duration = Duration.between(lastCheckTime, now)
        if (duration.isNegative || duration.isZero) {
            duration = Duration.ofSeconds(1)
        }

        return duration
    }

    fun getSinceLastAsString(): String {
        val duration = getSinceLast()

        val hours = duration.toHoursPart()
        val minutes = duration.toMinutesPart()
        val seconds = duration.toSecondsPart()

        val result = StringBuilder()

        if (hours > 0) {
            result.append("${hours}h ")
        }
        if (minutes > 0) {
            result.append("${minutes}m ")
        }

        if (hours == 0 && minutes == 0 && seconds > 0) {
            result.append("${seconds}s")
        }

        if (result.isEmpty()) {
            return "1s"
        }

        return result.toString().trim()
    }
}