// External JavaScript function declarations for Worker environment
external interface Request {
    val method: String
    val url: String
    fun json(): dynamic
}

external interface Response {
    companion object {
        fun Response(body: String, options: dynamic = definedExternally): Response
    }
}

external interface ExecutionContext

// Simplified Discord interaction types
data class DiscordInteraction(
    val type: Int,
    val data: DiscordInteractionData?
)

data class DiscordInteractionData(
    val name: String,
    val options: List<DiscordOption>?
)

data class DiscordOption(
    val name: String,
    val value: String
)

// Simple response builder
data class DiscordResponse(
    val type: Int,
    val data: DiscordResponseData?
)

data class DiscordResponseData(
    val content: String
)

class HuskerBotWorker {
    fun handleRequest(request: Request, context: ExecutionContext): Response {
        return when (request.method) {
            "POST" -> handleDiscordInteraction(request)
            "GET" -> handleHealthCheck()
            else -> Response("Method not allowed", js("{ status: 405 }"))
        }
    }
    
    private fun handleDiscordInteraction(request: Request): Response {
        try {
            // Parse Discord interaction
            val interaction = parseDiscordInteraction(request)
            
            val response = when (interaction.type) {
                1 -> handlePing()
                2 -> handleSlashCommand(interaction)
                else -> DiscordResponse(4, DiscordResponseData("Unknown interaction type"))
            }
            
            return Response(
                JSON.stringify(response),
                js("{ headers: { 'Content-Type': 'application/json' } }")
            )
        } catch (e: Exception) {
            return Response(
                "Internal Server Error",
                js("{ status: 500 }")
            )
        }
    }
    
    private fun parseDiscordInteraction(request: Request): DiscordInteraction {
        val body = request.json()
        return DiscordInteraction(
            type = body.type as Int,
            data = body.data?.let { data ->
                DiscordInteractionData(
                    name = data.name as String,
                    options = (data.options as? Array<dynamic>)?.map { option ->
                        DiscordOption(
                            name = option.name as String,
                            value = option.value as String
                        )
                    }
                )
            }
        )
    }
    
    private fun handlePing(): DiscordResponse {
        return DiscordResponse(1, null) // PONG
    }
    
    private fun handleSlashCommand(interaction: DiscordInteraction): DiscordResponse {
        val commandName = interaction.data?.name ?: "unknown"
        
        return when (commandName) {
            "gameday-weather" -> handleGamedayWeather()
            "ping" -> DiscordResponse(4, DiscordResponseData("Pong! HuskerBot is running on Cloudflare Workers! 🚀"))
            else -> DiscordResponse(4, DiscordResponseData("Unknown command: $commandName"))
        }
    }
    
    private fun handleGamedayWeather(): DiscordResponse {
        // Simplified weather response for POC
        return DiscordResponse(
            4,
            DiscordResponseData("🏈 **Game Day Weather POC**\n\n🌡️ 72°F\n☁️ Partly Cloudy\n💨 10 mph NW\n\n🔥 **Hot Take**: Perfect football weather! Even if we're losing, at least you won't freeze your corn-fed butt off in Memorial Stadium! 🌽")
        )
    }
    
    private fun handleHealthCheck(): Response {
        return Response(
            "HuskerBot Worker is healthy! 🌽",
            js("{ headers: { 'Content-Type': 'text/plain' } }")
        )
    }
}

// Entry point for the Worker
@JsExport
fun handleRequest(request: Request, env: dynamic, context: ExecutionContext): Response {
    val worker = HuskerBotWorker()
    return worker.handleRequest(request, context)
}