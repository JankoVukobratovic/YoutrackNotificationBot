package manager

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.AppConfig
import java.io.File
import java.io.FileNotFoundException

object SubscriptionManager {
    val subscriptions = HashSet<Long>()
    var savePath: String = ""

    public fun initialize(config: AppConfig) {
        savePath = config.selfConfig.subscriptionFile
        loadAll()
    }

    public fun loadAll() {
        synchronized(this) {
            try {
                val file = File(savePath)
                if (file.exists()) {
                    val jsonString = file.readText()
                    val loadedList = Json.Default.decodeFromString<List<Long>>(jsonString)
                    subscriptions.clear()
                    subscriptions.addAll(loadedList)
                    println("Loaded ${subscriptions.size} subscriptions from file.")
                }
            } catch (e: FileNotFoundException) {
                println("Subscription file not found. Starting with an empty list.")
                //start with empty set
            } catch (e: Exception) {
                System.err.println("Error loading subscriptions: ${e.message}")
            }
        }
    }

    public fun saveAll() {
        synchronized(this) {
            try {
                val listToSave = subscriptions.toList().sorted()
                val jsonString = Json.Default.encodeToString(listToSave)
                File(savePath).writeText(jsonString)
                println("Saved ${subscriptions.size} subscriptions to file.")
            } catch (e: Exception) {
                System.err.println("Error saving subscriptions: ${e.message}")
            }
        }
    }

    /**
     * Adds an element if and only if it was not already subscribed,
     * in which case it saves the list
     * @return Returns true if a new element was added
     */
    public fun add(newId: Long): Boolean {
        return if (subscriptions.add(newId)) {
            saveAll()
            true
        } else false
    }

    /**
     * Removes an element if and only if it was in the subscription list,
     * in which case it saves the list
     * @return Returns true if an element was actually removed
     */
    fun remove(id: Long): Boolean {
        return if (!subscriptions.contains(id)) {
            println("Subscription with id $id does not exist.")
            false
        } else {
            subscriptions.remove(id)
            println("Subscription with id $id removed.")
            saveAll();
            true
        }
    }
}