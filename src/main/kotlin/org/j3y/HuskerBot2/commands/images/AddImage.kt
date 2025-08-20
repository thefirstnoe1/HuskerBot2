package org.j3y.HuskerBot2.commands.images

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.model.ImageEntity
import org.j3y.HuskerBot2.repository.ImageRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

@Component
class AddImage : SlashCommand() {
    @Autowired
    lateinit var imageRepo: ImageRepo

    override fun getCommandKey(): String = "add"
    override fun isSubcommand(): Boolean = true
    override fun getDescription(): String = "Add an image"
    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.STRING, "name", "The name of the image you want to add", true),
        OptionData(OptionType.STRING, "url", "The URL of the image you want to add", true),
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply(true).queue()

        val name = commandEvent.getOption("name")?.asString
        val imageUrl = commandEvent.getOption("url")?.asString

        if (name.isNullOrBlank() || imageUrl.isNullOrBlank()) {
            commandEvent.hook.sendMessage("Both name and url are required.").queue()
            return
        }

        if (!isValidHttpUrl(imageUrl)) {
            commandEvent.hook.sendMessage("The provided URL is not valid. Please use http or https.").queue()
            return
        }

        if (!validateImageUrl(imageUrl)) {
            commandEvent.hook.sendMessage("The provided URL does not appear to be a valid image.").queue()
            return
        }

        // At this point, the URL looks valid and points to an image.
        if (!imageRepo.findById(name).isEmpty) {
            commandEvent.hook.sendMessage("An image with name '$name' already exists.").queue()
            return
        }
        val image = ImageEntity(name, imageUrl, commandEvent.user.id)

        imageRepo.save(image)

        commandEvent.hook.sendMessage("Added image '$name' from URL: $imageUrl").queue()
    }

    // Validates that the string is a well-formed http/https URL.
    fun isValidHttpUrl(url: String): Boolean {
        return try {
            val uri = URI(url.trim())
            if (uri.scheme == null || (uri.scheme != "http" && uri.scheme != "https")) return false
            // Ensure the URI can be turned into a URL (full syntax check)
            uri.toURL()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Tries to determine whether the URL definitely points to an image by using a HEAD request for Content-Type,
    // with a safe GET fallback if HEAD is not allowed.
    fun isImageUrlByContentType(url: String, connectTimeoutMs: Int = 5000, readTimeoutMs: Int = 5000): Boolean {
        val contentType = fetchContentType(url, connectTimeoutMs, readTimeoutMs)
        return contentType?.lowercase()?.startsWith("image/") == true
    }

    // Returns the Content-Type header if obtainable, else null.
    fun fetchContentType(url: String, connectTimeoutMs: Int = 5000, readTimeoutMs: Int = 5000): String? {
        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                requestMethod = "HEAD"
            }

            val responseCode = connection.responseCode
            val typeFromHead = connection.contentType
            connection.disconnect()

            if (responseCode in 200..399 && !typeFromHead.isNullOrBlank()) {
                return typeFromHead
            }

            // Some servers don't support HEAD; try a minimal GET request.
            val getConn = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                requestMethod = "GET"
                setRequestProperty("Range", "bytes=0-0")
            }
            val typeFromGet = getConn.contentType
            getConn.inputStream.use { /* read a byte to initiate */ it.read() }
            getConn.disconnect()

            if (getConn.responseCode in 200..399) typeFromGet else null
        } catch (e: Exception) {
            null
        }
    }

    // High-level validator combining syntactic URL check, reachability, and content-type/image hints.
    fun validateImageUrl(url: String): Boolean {
        if (!isValidHttpUrl(url)) return false

        // Prefer authoritative Content-Type check
        if (isImageUrlByContentType(url)) return true

        // Fall back to extension heuristic if content-type not obtainable
        return false
    }
}
