package clients

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import models.*

class YouTrackClient(config: AppConfig) {

    val youTrackUrl = config.youtrack.baseUrl
    private val projectId: String
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

    init {
        projectId = runBlocking {
            getProjectInternalId(projectShortName)
        }
        println("YouTrackClient initialized. Project ID for $projectShortName: $projectId")
    }

    private suspend fun getProjectInternalId(shortName: String): String {
        println("Fetching internal project ID for short name: $shortName...")
        val fields = "id,shortName"

        val httpResponse = httpClient.get("$youTrackUrl/admin/projects") {
            header("Authorization", "Bearer $youTrackToken")
            parameter("fields", fields)
        }

        if (!httpResponse.status.isSuccess()) {
            println("Fatal: Failed obtaining projectId, http code: ${httpResponse.status.value}")
            throw RuntimeException(
                "YouTrack API Error (Status ${httpResponse.status.value}): " + "Response: ${httpResponse.bodyAsText()}"
            )
        }

        val jsonString = httpResponse.body<String>()
        val projectsArray = Json.parseToJsonElement(jsonString).jsonArray

        val projectElement = projectsArray.find {
            it.jsonObject["shortName"]?.jsonPrimitive?.content == shortName
        }

        val projectId = projectElement?.jsonObject?.get("id")?.jsonPrimitive?.content

        return projectId ?: throw IllegalStateException(
            "FATAL: Could not find YouTrack project with short name '$shortName'."
        )
    }

    /**
     * Gets a list of activities in the last 24 hours for the defined project.
     * @return A list of YouTrackActivity objects, or an empty list if an error occurs.
     */
    suspend fun getActivities(sinceWhenMillis: Long): List<YouTrackActivity> {
        val youTrackQuery = "project: $projectShortName"

        val fields =
            "id,timestamp,category(id),target(idReadable,summary),author(login)," + "field(name),added(name,presentation),removed(name,presentation),text"


        println("Sending request to $youTrackUrl/activities")
        /*
                println("query:\n$youTrackQuery")
                println("fields:\n$fields")
                println("start: $sinceWhenMillis")
        */
        val httpResponse = httpClient.get("$youTrackUrl/activities") {
            header("Authorization", "Bearer $youTrackToken")
            parameter(
                "categories",
                "IssueCreatedCategory,IssueCommentCategory,IssueFieldChangeCategory,IssueAttachmentCategory"
            )
            parameter("start", sinceWhenMillis)
            parameter("query", youTrackQuery)
            parameter("fields", fields)
            parameter("reverse", true)
            parameter("\$top", 10)
        }

        if (httpResponse.status == HttpStatusCode.OK) {
            println("YouTrack request successful. Parsing activities...")
            // Ktor deserializes the response body into a List<YouTrackActivity>
            return httpResponse.body()
        } else {
            val errorBody = httpResponse.bodyAsText()
            throw RuntimeException(
                "YouTrack API Error (Status ${httpResponse.status.value}): " + "Response: $errorBody"
            )
        }

    }

    /**
     * Gets a list of issues updated in the last 24 hours for the defined project.
     * @return A list of YouTrackIssue objects, or an empty list if an error occurs.
     */
    suspend fun getUpdatedIssues(updatePeriod: String = "1d"): List<YouTrackIssue> {
//        val youTrackQuery = "project: $projectShortName updated: $updatePeriod .. now"
        val youTrackQuery = "updated: {minus $updatePeriod} .. *"

        val fields = "idReadable,summary,updated,customFields(name,value(name))"

        println("Sending request to $youTrackUrl/issues")
        /*
        println("query:\n$youTrackQuery")
        println("fields:\n$fields")
        */

        val httpResponse = httpClient.get("$youTrackUrl/issues") {
            header("Authorization", "Bearer $youTrackToken")
            parameter("query", youTrackQuery)
            parameter("fields", fields)
            parameter("\$orderBy", "updated desc")
            parameter("\$top", 10) // Limit to the top 10 updated issues
        }

        if (httpResponse.status == HttpStatusCode.OK) {
            println("YouTrack request successful. Parsing issues...")
            //   println(httpResponse.body<String>())
            return httpResponse.body()
        } else {
            val errorBody = httpResponse.bodyAsText()
            throw RuntimeException(
                "YouTrack API Error (Status ${httpResponse.status.value}): " + "Response: $errorBody"
            )
        }

    }

    /**
     * posts an issue to the YouTrack instance
     */
    suspend fun createIssue(summary: String, description: String): String {
        println("Posting new issue: $summary description: $description")

        val issuePayload = NewYouTrackIssue(
            summary = summary,
            description = description,
            project = YouTrackProject(id = projectId)
        )

        val httpResponse = httpClient.post("$youTrackUrl/issues") {
            header("Authorization", "Bearer $youTrackToken")
            contentType(ContentType.Application.Json)

            // Set the body using the serializable data class
            setBody(issuePayload)

            // Request minimal fields for confirmation response
            parameter("fields", "idReadable,summary")
        }
        if (httpResponse.status == HttpStatusCode.OK) {
            println("YouTrack request successful. Parsing response...")
            val jsonObject = Json.parseToJsonElement(httpResponse.body<String>()).jsonObject
            return jsonObject["idReadable"]?.jsonPrimitive?.content
                ?: throw IllegalStateException("Failed to parse post response")
        } else {
            val errorBody = httpResponse.bodyAsText()
            throw RuntimeException(
                "YouTrack API Error (Status ${httpResponse.status.value}): " + "Response: $errorBody"
            )
        }
    }
}