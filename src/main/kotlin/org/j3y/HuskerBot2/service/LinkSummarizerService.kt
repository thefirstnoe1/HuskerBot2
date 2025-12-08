package org.j3y.HuskerBot2.service

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.LoadState
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.awt.Color
import java.net.URI
import java.time.OffsetDateTime

@Service
class LinkSummarizerService(
    private val geminiService: GoogleGeminiService
) {
    private val log = LoggerFactory.getLogger(LinkSummarizerService::class.java)

    fun summarizeToEmbed(
        rawLink: String,
        requesterName: String?,
        requesterAvatarUrl: String?
    ): MessageEmbed {
        var link = rawLink.trim()
        if (!link.startsWith("https://") && !link.startsWith("http://")) {
            link = "https://$link"
        }

        val uri = try {
            URI(link)
        } catch (e: Exception) {
            throw IllegalArgumentException("That doesn't look like a valid URL.")
        }

        val (finalUri, useRestTemplate) = rewriteIfReddit(uri)

        val html = try {
            if (useRestTemplate) fetchWithRestTemplate(finalUri.toString()) else fetchWithHeadlessBrowser(finalUri.toString())
        } catch (e: Exception) {
            log.warn("Failed to fetch URL: {}", finalUri, e)
            null
        }

        if (html.isNullOrBlank()) {
            throw IllegalStateException("I couldn't retrieve that page or it was empty.")
        }

        val text = stripHtmlToText(html)
        val maxLen = 50_000
        val truncated = if (text.length > maxLen) text.substring(0, maxLen) else text

        if (truncated.isBlank()) {
            throw IllegalStateException("The page didn't contain readable text to summarize.")
        }

        val prompt = buildPrompt(truncated, link)
        val summary = geminiService.generateText(prompt)

        return EmbedBuilder()
            .setTitle("Link Summary")
            .setColor(Color(0x3B, 0x88, 0xC3))
            .setDescription(truncateForDiscord(summary))
            .addField("Source", link, false)
            .setFooter("Requested by ${requesterName ?: "Unknown"}", requesterAvatarUrl)
            .setTimestamp(OffsetDateTime.now())
            .build()
    }

    fun rewriteIfReddit(input: URI): Pair<URI, Boolean> {
        return try {
            val host = input.host?.lowercase() ?: return Pair(input, false)
            return if (host == "reddit.com" || host == "www.reddit.com") {
                val newUri = URI(
                    input.scheme,
                    input.userInfo,
                    "old.reddit.com",
                    input.port,
                    input.path,
                    input.query,
                    input.fragment
                )
                Pair(newUri, true)
            } else {
                Pair(input, false)
            }
        } catch (e: Exception) {
            Pair(input, false)
        }
    }

    private fun fetchWithRestTemplate(url: String): String? {
        return try {
            val factory = SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(10_000)
                setReadTimeout(20_000)
            }
            val restTemplate = RestTemplate(factory)
            val headers = HttpHeaders().apply {
                add("User-Agent", USER_AGENT)
                add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                add("Accept-Language", "en-US,en;q=0.9")
            }
            val entity = HttpEntity<Void>(headers)
            val response = restTemplate.exchange(url, HttpMethod.GET, entity, String::class.java)
            val body = response.body
            if (body.isNullOrBlank()) null else body
        } catch (e: Exception) {
            log.warn("RestTemplate failed to fetch {}", url, e)
            null
        }
    }

    private fun fetchWithHeadlessBrowser(url: String): String? {
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
                    .setUserAgent(USER_AGENT)
                    .setViewportSize(1920, 1080)
            )
            page = context.newPage()
            page.setDefaultTimeout(30_000.0)
            page.navigate(url)
            page.waitForLoadState(LoadState.DOMCONTENTLOADED)
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
        return try {
            val doc = Jsoup.parse(html)
            doc.select("script, style, noscript").remove()
            val text = doc.text()
            text.replace("\u00A0", " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        } catch (e: Exception) {
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
            - Provide 5-8 concise bullet points capturing the main ideas, facts, and any conclusions.
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

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }
}
