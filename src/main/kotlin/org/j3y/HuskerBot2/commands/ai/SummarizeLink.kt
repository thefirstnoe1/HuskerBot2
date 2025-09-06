package org.j3y.HuskerBot2.commands.ai

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.service.GoogleGeminiService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.LoadState
import java.awt.Color
import java.net.URI
import java.time.OffsetDateTime
import org.jsoup.Jsoup

@Component
class SummarizeLink(
    private val geminiService: GoogleGeminiService
) : SlashCommand() {

    private val log = LoggerFactory.getLogger(SummarizeLink::class.java)
    // Using Selenium WebDriver (headless Firefox) instead of RestTemplate

    override fun getCommandKey(): String = "summarize-link"
    override fun getDescription(): String = "Fetch a web page, extract text, and summarize it with Gemini"

    override fun getOptions(): List<OptionData> = listOf(
        OptionData(OptionType.STRING, "link", "The URL to summarize", true)
    )

    override fun execute(commandEvent: SlashCommandInteractionEvent) {
        commandEvent.deferReply(false).queue()
        try {
            var link = commandEvent.getOption("link")?.asString?.trim()
            if (link.isNullOrBlank()) {
                commandEvent.hook.sendMessage("You must provide a valid link.").queue()
                return
            }

            if (!link.startsWith("https://") && !link.startsWith("http://")) {
                link = "https://$link"
            }

            val uri = try { URI(link) } catch (e: Exception) {
                commandEvent.hook.sendMessage("That doesn't look like a valid URL.").queue(); return
            }

            val html = try {
                fetchWithHeadlessBrowser(uri.toString())
            } catch (e: Exception) {
                log.warn("Failed to fetch URL: {}", link, e)
                null
            }

            if (html.isNullOrBlank()) {
                commandEvent.hook.sendMessage("I couldn't retrieve that page or it was empty.").queue()
                return
            }

            val text = stripHtmlToText(html)
            val maxLen = 50_000
            val truncated = if (text.length > maxLen) text.substring(0, maxLen) else text

            log.info("Content: ${truncated}")

            if (truncated.isBlank()) {
                commandEvent.hook.sendMessage("The page didn't contain readable text to summarize.").queue()
                return
            }

            val prompt = buildPrompt(truncated, link)
            val summary = geminiService.generateText(prompt)

            val embed = EmbedBuilder()
                .setTitle("Link Summary")
                .setColor(Color(0x3B, 0x88, 0xC3))
                .setDescription(truncateForDiscord(summary))
                .addField("Source", link, false)
                .setFooter("Requested by ${commandEvent.member?.effectiveName ?: commandEvent.user.effectiveName}", commandEvent.user.avatarUrl)
                .setTimestamp(OffsetDateTime.now())
                .build()

            commandEvent.hook.sendMessageEmbeds(embed).queue()
        } catch (e: Exception) {
            log.error("Error executing /summarize-link", e)
            commandEvent.hook.sendMessage("Error while summarizing link: ${e.message}").setEphemeral(true).queue()
        }
    }

    private fun fetchWithHeadlessBrowser(url: String): String? {
        // Playwright-based headless fetch with realistic User-Agent
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
        var playwright: Playwright? = null
        var browser: Browser? = null
        var page: Page? = null
        return try {
            playwright = Playwright.create()
            val launchOptions = com.microsoft.playwright.BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(listOf("--disable-blink-features=AutomationControlled"))
            browser = playwright.chromium().launch(launchOptions)

            val context = browser.newContext(
                Browser.NewContextOptions()
                    .setUserAgent(userAgent)
                    .setViewportSize(1920, 1080)
            )
            page = context.newPage()
            page.setDefaultTimeout(30_000.0)
            page.navigate(url)
            page.waitForLoadState(LoadState.DOMCONTENTLOADED)
            //page.waitForTimeout(1000.0)
            val content = page.content()
            if (content.isNullOrBlank()) null else content
        } catch (e: Exception) {
            log.warn("Playwright failed to fetch {}", url, e)
            null
        } finally {
            try { page?.context()?.close() } catch (_: Exception) {}
            try { browser?.close() } catch (_: Exception) {}
            try { playwright?.close() } catch (_: Exception) {}
        }
    }

    private fun stripHtmlToText(html: String): String {
        // Use Jsoup to parse HTML and extract human-readable text
        return try {
            val doc = Jsoup.parse(html)
            // Remove script and style elements explicitly (Jsoup's text() ignores them, but to be safe)
            doc.select("script, style, noscript").remove()
            val text = doc.text()
            text.replace("\u00A0", " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        } catch (e: Exception) {
            // Fallback to previous naive stripping in case of unexpected parsing issues
            val noScripts = html.replace(Regex("(?is)<script.*?>.*?</script>"), " ")
                .replace(Regex("(?is)<style.*?>.*?</style>"), " ")
            val noTags = noScripts.replace(Regex("(?s)<[^>]+>"), " ")
            val decoded = org.springframework.web.util.HtmlUtils.htmlUnescape(noTags)
            decoded.replace("\u00A0", " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }

    private fun buildPrompt(content: String, link: String): String {
        return """
            Summarize the following web page content from $link.
            - Provide 5-15 concise bullet points capturing the main ideas, facts, and any conclusions.
            - If the content is news, include who/what/when/where and implications.
            - Keep the summary neutral and avoid speculation.
            Content:
            ---
            $content
            ---
        """.trimIndent()
    }

    private fun truncateForDiscord(text: String): String {
        val clean = text
            .replace("@everyone", "@\u200Beveryone")
            .replace("@here", "@\u200Bhere")
        return if (clean.length <= 3900) clean else clean.substring(0, 3897) + "..."
    }
}
