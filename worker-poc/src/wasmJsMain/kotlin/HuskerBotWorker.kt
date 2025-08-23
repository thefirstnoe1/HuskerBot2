// Simple Kotlin/Wasm Discord bot POC
// Note: Kotlin/Wasm has significant limitations - this is a minimal demonstration

// External JavaScript function declarations (simplified)
external fun processRequest(method: String, url: String): String

// Simplified data classes for Discord interactions
data class SimpleDiscordResponse(
    val type: Int,
    val content: String
)

class HuskerBotWorker {
    
    fun handleDiscordInteraction(commandName: String): String {
        return when (commandName) {
            "ping" -> createResponse(4, "Pong! HuskerBot is running on Cloudflare Workers! 🚀")
            "gameday-weather" -> createResponse(4, getGamedayWeatherResponse())
            else -> createResponse(4, "Unknown command: $commandName")
        }
    }
    
    private fun createResponse(type: Int, content: String): String {
        // Manual JSON creation since JSON.stringify isn't available
        return """{"type":$type,"data":{"content":"$content"}}"""
    }
    
    private fun getGamedayWeatherResponse(): String {
        return buildString {
            append("🏈 **Game Day Weather POC**\\n\\n")
            append("🌡️ 72°F\\n")
            append("☁️ Partly Cloudy\\n") 
            append("💨 10 mph NW\\n\\n")
            append("🔥 **Hot Take**: Perfect football weather! ")
            append("Even if we're losing, at least you won't freeze your ")
            append("corn-fed butt off in Memorial Stadium! 🌽")
        }
    }
    
    fun getHealthCheck(): String {
        return "HuskerBot Worker is healthy! 🌽"
    }
}

// Entry point for the Worker (simplified)
@JsExport
fun handleWorkerRequest(method: String, command: String): String {
    val worker = HuskerBotWorker()
    
    return when (method) {
        "POST" -> worker.handleDiscordInteraction(command)
        "GET" -> worker.getHealthCheck()
        else -> "Method not allowed"
    }
}