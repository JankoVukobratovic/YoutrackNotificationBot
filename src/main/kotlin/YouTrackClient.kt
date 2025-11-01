package org.jankos

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class YouTrackClient(config: AppConfig) {

    private val youTrackUrl = config.youtrack.baseUrl
    private val youTrackToken = config.youtrack.apiToken
    private val projectShortName = config.project.shortName

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /**
     * Gets a list of issues updated in the last 24 hours for the defined project.
     * @return A list of YouTrackIssue objects, or an empty list if an error occurs.
     */
    suspend fun getUpdatedIssues(updatePeriod: String = "-1d"): List<YouTrackIssue> {

//        val youTrackQuery = "project: $projectShortName updated: $updatePeriod .. now"
        val youTrackQuery = "updated: {minus 1d} .. *"


        val fields = "idReadable,summary,updated,customFields(name,value(name))"

        try {
            // TODO Implement a method that only returns *NEW* notifications

            println("Sending request to $youTrackUrl/issues")
            println("query:\n$youTrackQuery")
            println("fields:\n$fields")



            val httpResponse = httpClient.get("$youTrackUrl/issues") {
                header("Authorization", "Bearer $youTrackToken")
                parameter("query", youTrackQuery)
                parameter("fields", fields)
                parameter("\$top", 10) // Limit to the top 10 updated issues
            }

            if (httpResponse.status == HttpStatusCode.OK) {
                println("YouTrack request successful. Parsing issues...")
                return httpResponse.body()
            } else {
                val errorBody = httpResponse.bodyAsText()
                throw RuntimeException(
                    "YouTrack API Error (Status ${httpResponse.status.value}): " +
                            "Response: $errorBody"
                )
            }


            // TODO IF ALL 10 are new, fetch more, repeat until no new notifications.
        } catch (e: Exception) {
            println("Error fetching issues from YouTrack: ${e.message}")
            return emptyList()
        }
    }

}