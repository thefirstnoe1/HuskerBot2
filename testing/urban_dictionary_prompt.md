# Urban Dictionary Command Implementation

## Overview
Create a `/urban-dictionary` Discord slash command for HuskerBot2 that looks up word definitions using the Unofficial Urban Dictionary API and displays paginated results with navigation arrows.

## Technical Requirements

### Framework & Language
- **Language**: Kotlin 1.9.25 with Java 21 toolchain
- **Framework**: Spring Boot 3.5.4 with JPA, JDA 5.6+
- **Package**: `org.j3y.HuskerBot2.commands.UrbanDictionaryCommand`

### Command Structure
```kotlin
@Component
class UrbanDictionaryCommand : SlashCommand() {
    override val name = "urban-dictionary"
    override val description = "Look up a word or phrase on Urban Dictionary"
    
    override fun buildCommand(): SlashCommandData {
        return Commands.slash(name, description)
            .addOption(OptionType.STRING, "word", "The word or phrase to look up", true)
    }
}
```

## Implementation Components

### 1. Data Models
Create models in `org.j3y.HuskerBot2.model` package:

#### Urban Dictionary Response Models
```kotlin
data class UrbanDictionaryResponse(
    val list: List<UrbanDefinition>
)

data class UrbanDefinition(
    val definition: String,
    val permalink: String,
    val thumbsUp: Int,
    val author: String,
    val word: String,
    val defid: Long,
    val currentVote: String?,
    val writtenOn: String,
    val example: String,
    val thumbsDown: Int
)

data class PaginatedUrbanResult(
    val definitions: List<UrbanDefinition>,
    val currentPage: Int,
    val totalPages: Int,
    val searchTerm: String
)
```

### 2. Service Layer
Create service in `org.j3y.HuskerBot2.service` package:

#### UrbanDictionaryService
```kotlin
@Service
class UrbanDictionaryService {
    
    private val logger = LoggerFactory.getLogger(UrbanDictionaryService::class.java)
    private val restTemplate = RestTemplate()
    
    companion object {
        private const val URBAN_API_BASE_URL = "https://unofficialurbandictionaryapi.com/api"
        private const val MAX_DEFINITION_LENGTH = 1024
        private const val MAX_EXAMPLE_LENGTH = 512
    }
    
    @Cacheable("urban-definitions", unless = "#result == null")
    fun searchDefinitions(term: String): List<UrbanDefinition>?
    
    private fun truncateText(text: String, maxLength: Int): String
    private fun cleanUrbanText(text: String): String
}
```

### 3. Pagination Manager
Create utility class in `org.j3y.HuskerBot2.service` package:

#### PaginationService
```kotlin
@Service
class PaginationService {
    
    private val activePages = ConcurrentHashMap<String, PaginatedUrbanResult>()
    
    companion object {
        private const val PAGE_TIMEOUT_MINUTES = 15L
        private val PREV_EMOJI = "⬅️"
        private val NEXT_EMOJI = "➡️"
        private val CLOSE_EMOJI = "❌"
    }
    
    fun createPagination(
        userId: String, 
        messageId: String, 
        definitions: List<UrbanDefinition>, 
        searchTerm: String
    ): PaginatedUrbanResult
    
    fun getPageData(userId: String, messageId: String): PaginatedUrbanResult?
    
    fun updatePage(userId: String, messageId: String, newPage: Int): PaginatedUrbanResult?
    
    fun removePagination(userId: String, messageId: String)
    
    fun createPaginationButtons(hasMultiplePages: Boolean): List<Button>
    
    private fun getPaginationKey(userId: String, messageId: String): String = "$userId:$messageId"
    
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    fun cleanupExpiredPages()
}
```

### 4. Button Interaction Handler
Create component in `org.j3y.HuskerBot2.commands` package:

#### UrbanDictionaryButtonHandler
```kotlin
@Component
class UrbanDictionaryButtonHandler {
    
    private val logger = LoggerFactory.getLogger(UrbanDictionaryButtonHandler::class.java)
    private val paginationService: PaginationService
    private val urbanDictionaryService: UrbanDictionaryService
    
    @EventListener
    fun onButtonClick(event: ButtonInteractionEvent) {
        if (!event.componentId.startsWith("urban_")) return
        
        val userId = event.user.id
        val messageId = event.messageId
        
        when (event.componentId) {
            "urban_prev" -> handlePreviousPage(event, userId, messageId)
            "urban_next" -> handleNextPage(event, userId, messageId)
            "urban_close" -> handleClose(event, userId, messageId)
        }
    }
    
    private fun handlePreviousPage(event: ButtonInteractionEvent, userId: String, messageId: String)
    private fun handleNextPage(event: ButtonInteractionEvent, userId: String, messageId: String)
    private fun handleClose(event: ButtonInteractionEvent, userId: String, messageId: String)
    private fun updateMessage(event: ButtonInteractionEvent, pageData: PaginatedUrbanResult)
}
```

### 5. Main Command Implementation
```kotlin
override suspend fun execute(event: SlashCommandInteractionEvent) {
    val word = event.getOption("word")?.asString
    if (word.isNullOrBlank()) {
        event.reply("Please provide a word to look up!").setEphemeral(true).queue()
        return
    }
    
    event.deferReply().queue()
    
    try {
        val definitions = urbanDictionaryService.searchDefinitions(word.trim())
        
        if (definitions.isNullOrEmpty()) {
            event.hook.sendMessage("No definitions found for \"$word\". Try a different spelling or term.").queue()
            return
        }
        
        // Create pagination
        val userId = event.user.id
        event.hook.sendMessage("Loading...").queue { response ->
            val messageId = response.id
            val pageData = paginationService.createPagination(userId, messageId, definitions, word)
            
            val embed = createUrbanEmbed(pageData)
            val buttons = paginationService.createPaginationButtons(definitions.size > 1)
            
            response.editOriginal()
                .setEmbeds(embed)
                .setActionRow(buttons)
                .queue()
        }
        
    } catch (e: Exception) {
        logger.error("Error executing urban-dictionary command for word: $word", e)
        event.hook.sendMessage("Sorry, there was an error looking up that word. Please try again later.").queue()
    }
}
```

### 6. API Integration Implementation
Add to UrbanDictionaryService:

```kotlin
fun searchDefinitions(term: String): List<UrbanDefinition>? {
    return try {
        val encodedTerm = URLEncoder.encode(term, StandardCharsets.UTF_8)
        val url = "$URBAN_API_BASE_URL/search?term=$encodedTerm"
        
        val response = restTemplate.getForObject(url, UrbanDictionaryResponse::class.java)
        
        response?.list?.map { definition ->
            definition.copy(
                definition = cleanUrbanText(truncateText(definition.definition, MAX_DEFINITION_LENGTH)),
                example = cleanUrbanText(truncateText(definition.example, MAX_EXAMPLE_LENGTH))
            )
        }?.sortedByDescending { it.thumbsUp } // Sort by popularity
        
    } catch (e: Exception) {
        logger.error("Error fetching Urban Dictionary definitions for term: $term", e)
        null
    }
}

private fun cleanUrbanText(text: String): String {
    return text
        .replace("[", "**")
        .replace("]", "**")
        .replace("\\r\\n", "\n")
        .replace("\\n", "\n")
        .trim()
}

private fun truncateText(text: String, maxLength: Int): String {
    return if (text.length <= maxLength) {
        text
    } else {
        "${text.substring(0, maxLength - 3)}..."
    }
}
```

### 7. Embed Creation
```kotlin
private fun createUrbanEmbed(pageData: PaginatedUrbanResult): MessageEmbed {
    val definition = pageData.definitions[pageData.currentPage]
    
    val embed = EmbedBuilder()
    
    embed.setTitle("📚 ${definition.word}")
    embed.setColor(Color.ORANGE)
    embed.setUrl(definition.permalink)
    
    // Definition
    embed.addField("Definition", definition.definition, false)
    
    // Example (if available and not empty)
    if (definition.example.isNotBlank()) {
        embed.addField("Example", definition.example, false)
    }
    
    // Stats
    embed.addField("👍", definition.thumbsUp.toString(), true)
    embed.addField("👎", definition.thumbsDown.toString(), true)
    embed.addField("Author", definition.author, true)
    
    // Page info
    if (pageData.totalPages > 1) {
        embed.setFooter("Page ${pageData.currentPage + 1} of ${pageData.totalPages}")
    }
    
    embed.setTimestamp(Instant.now())
    
    return embed.build()
}
```

### 8. Button Handler Implementation
```kotlin
private fun handlePreviousPage(event: ButtonInteractionEvent, userId: String, messageId: String) {
    val pageData = paginationService.getPageData(userId, messageId) ?: return
    
    val newPage = if (pageData.currentPage > 0) pageData.currentPage - 1 else pageData.totalPages - 1
    val updatedPageData = paginationService.updatePage(userId, messageId, newPage) ?: return
    
    updateMessage(event, updatedPageData)
}

private fun handleNextPage(event: ButtonInteractionEvent, userId: String, messageId: String) {
    val pageData = paginationService.getPageData(userId, messageId) ?: return
    
    val newPage = if (pageData.currentPage < pageData.totalPages - 1) pageData.currentPage + 1 else 0
    val updatedPageData = paginationService.updatePage(userId, messageId, newPage) ?: return
    
    updateMessage(event, updatedPageData)
}

private fun handleClose(event: ButtonInteractionEvent, userId: String, messageId: String) {
    paginationService.removePagination(userId, messageId)
    
    event.editMessage()
        .setEmbeds(EmbedBuilder()
            .setTitle("📚 Urban Dictionary")
            .setDescription("Search closed.")
            .setColor(Color.GRAY)
            .build())
        .setActionRow(emptyList())
        .queue()
}

private fun updateMessage(event: ButtonInteractionEvent, pageData: PaginatedUrbanResult) {
    val embed = createUrbanEmbed(pageData)
    val buttons = paginationService.createPaginationButtons(pageData.totalPages > 1)
    
    event.editMessage()
        .setEmbeds(embed)
        .setActionRow(buttons)
        .queue()
}
```

### 9. Pagination Service Implementation
```kotlin
fun createPagination(
    userId: String, 
    messageId: String, 
    definitions: List<UrbanDefinition>, 
    searchTerm: String
): PaginatedUrbanResult {
    val pageData = PaginatedUrbanResult(
        definitions = definitions,
        currentPage = 0,
        totalPages = definitions.size,
        searchTerm = searchTerm
    )
    
    activePages[getPaginationKey(userId, messageId)] = pageData
    return pageData
}

fun createPaginationButtons(hasMultiplePages: Boolean): List<Button> {
    return if (hasMultiplePages) {
        listOf(
            Button.secondary("urban_prev", Emoji.fromUnicode(PREV_EMOJI)),
            Button.secondary("urban_next", Emoji.fromUnicode(NEXT_EMOJI)),
            Button.danger("urban_close", Emoji.fromUnicode(CLOSE_EMOJI))
        )
    } else {
        listOf(Button.danger("urban_close", Emoji.fromUnicode(CLOSE_EMOJI)))
    }
}
```

### 10. Configuration
Add to `application.yml`:

```yaml
urban-dictionary:
  api:
    base-url: "https://unofficialurbandictionaryapi.com/api"
    timeout: 10000 # 10 seconds
  pagination:
    timeout-minutes: 15
    cleanup-interval: 300000 # 5 minutes
  content:
    max-definition-length: 1024
    max-example-length: 512
  cache:
    definitions-ttl: 3600 # 1 hour
```

### 11. Error Handling & Edge Cases

- **Empty search results**: Clear message when no definitions found
- **API unavailable**: Graceful error handling with retry suggestion
- **Content filtering**: Truncate overly long definitions/examples
- **Button expiration**: Auto-cleanup of expired pagination sessions
- **Invalid interactions**: Handle clicks from users who didn't initiate search
- **Network timeouts**: Set appropriate timeout values for API calls

### 12. Testing
```kotlin
@SpringBootTest
class UrbanDictionaryCommandTest {
    
    @Test
    fun `should return definitions for valid word`()
    
    @Test
    fun `should handle no results gracefully`()
    
    @Test
    fun `should paginate through multiple definitions`()
    
    @Test
    fun `should clean and truncate text properly`()
    
    @Test
    fun `should handle API failures gracefully`()
}
```

### 13. Content Considerations

- **Text Cleaning**: Convert Urban Dictionary's bracket notation to Discord markdown
- **Length Limits**: Respect Discord's embed field limits
- **Content Warning**: Consider adding a warning about potentially offensive content
- **Link Safety**: The API provides permalink URLs to original definitions

### 14. Logging
```kotlin
private val logger = LoggerFactory.getLogger(UrbanDictionaryCommand::class.java)

// Log searches and errors
logger.info("Urban Dictionary search for term: $word by user: ${event.user.asTag}")
logger.warn("No definitions found for term: $word")
logger.error("Failed to fetch Urban Dictionary definitions", exception)
```

## Implementation Notes

- Use Spring's `@Cacheable` to cache definitions for popular terms
- Implement proper pagination with user-specific state management
- Handle Urban Dictionary's special bracket notation for word links
- Respect Discord's embed field length limitations
- Clean up expired pagination sessions to prevent memory leaks
- Consider content warnings due to Urban Dictionary's nature
- Use concurrent data structures for thread-safe pagination management
- Sort definitions by popularity (thumbs up) for best results first