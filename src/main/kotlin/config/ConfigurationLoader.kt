package config

import kotlinx.serialization.json.Json
import models.AppConfig
import java.io.InputStreamReader

object ConfigurationLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Loads the application configuration from 'config.json' located in the resources directory.
     *
     * @return The parsed AppConfig object.
     * @throws IllegalStateException if the file is not found or parsing fails.
     */
    fun load(): AppConfig {
        val resourceStream = this::class.java.classLoader.getResourceAsStream("config.json")
            ?: throw IllegalStateException("Configuration file 'config.json' not found in resources. Check src/main/resources directory.")

        val jsonString = InputStreamReader(resourceStream).use { it.readText() }

        return json.decodeFromString(jsonString)
    }
}