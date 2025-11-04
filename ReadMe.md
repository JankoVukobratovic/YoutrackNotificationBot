# 🤖 YouTrack Telegram Issue Bot 🤖

This project is a Telegram bot that integrates with YouTrack to provide scheduled
news updates and allows users to quickly create new issues directly from a Telegram chat.

It is built using **Kotlin**, **Ktor** for networking, 
and the **Kotlin Telegram Bot Library**.

## Features

* **Scheduled Updates:** Automatically checks YouTrack for recent activities (e.g., new comments, state changes) 
* and publishes them to subscribed chats on a configurable interval.

* **Persistent Subscriber List:** The subscriber list persists through re-runs of the code.

* **Quick Issue Creation (`/newissue`):** Create new issues in your target 
* YouTrack project directly from the chat message text.

* **Configuration:** Reads all API keys, tokens, project names, and scheduling 
* intervals from `config.json`.

## Commands
* `/newissue <summary>`: Creates a new issue with provided summary. 
The description contains the Telegram username of the caller
* `/subscribe` and `/unsubscribe`: Subscribes/unsubscribes the caller chat to the newsletter
* `/issues`: Retrieves issues in the past 24 hours
* `/activities`: Retrieves notifications in the past 24 hours
* `/pingsubs`: Debugging command to send a "ping" message to all current subscribers
* `/help`: List of commands
* `/start`: Introduction message

## 🛠️ Setup and Installation

### Prerequisites

1. **Java Development Kit (JDK) 21:** Build and run the application.

2. **YouTrack Instance:** Access URL and an API token with permissions to read activities and create issues.

3. **Telegram Bot Token:** Obtained from BotFather.

### Configuration (`config.json`)

Before running, you must create a `src/main/resources/config.json` file with the structure example given in the `src/main/resources/config-example.json`.

