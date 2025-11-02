package repository

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.AppConfig
import models.SaveData
import java.io.File
import java.io.FileNotFoundException

/**
 * Acts as a global repository interface
 */
object Repository {
    var saveData: SaveData = SaveData(emptySet(), 0)
    var savePath: String = ""

    /**
     * Saves all with the new subscription list
     */
    fun initialize(config: AppConfig) {
        savePath = config.selfConfig.saveFile
        loadAll()
    }

    fun addSubscription(subscriptionId: Long): Boolean {
        synchronized(this) {
            val newSet = saveData.subscribedIds.toMutableSet()
            val added = newSet.add(subscriptionId)

            if (added) {
                saveData = saveData.copy(subscribedIds = newSet)
                saveAll()
            }
            return added;
        }
    }

    fun removeSubscription(id: Long): Boolean {
        synchronized(this) {
            val newSet = saveData.subscribedIds.toMutableSet()
            val removed = newSet.remove(id)

            if (removed) {
                saveData = saveData.copy(subscribedIds = newSet)
                saveAll()
                println("Subscription with id $id removed.")
            } else {
                println("Subscription with id $id does not exist.")
            }
            return removed
        }

    }

    fun setLastCheckTime(newTime: Long) {
        synchronized(this) {
            saveData = saveData.copy(lastCheckTime = newTime)
            saveAll()
        }
    }

    /**
     * Saves all data
     */
    fun saveAll() {
        synchronized(this) {
            try {
                val jsonString = Json.Default.encodeToString(saveData)
                File(savePath).writeText(jsonString)
                println("Saved ${saveData.subscribedIds.size} subscriptions to file.")
            } catch (e: Exception) {
                System.err.println("Error saving subscriptions: ${e.message}")
            }
        }
    }

    /**
     * Loads all data into SaveManager
     */
    fun loadAll() {
        synchronized(this) {
            try {
                val file = File(savePath)
                if (file.exists()) {
                    val jsonString = file.readText()
                    saveData = Json.Default.decodeFromString<SaveData>(jsonString)
                    println("Loaded ${saveData.subscribedIds.size} subscriptions from file.")
                }
            } catch (e: FileNotFoundException) {
                println("Save file not found. Starting with an empty list.")
                //start with empty set
            } catch (e: Exception) {
                System.err.println("Error loading subscriptions: ${e.message}")
            }
        }
    }
}