package org.j3y.HuskerBot2.scheduler

import org.j3y.HuskerBot2.model.MessageData
import org.j3y.HuskerBot2.model.SimpleEmbed
import org.j3y.HuskerBot2.service.GoogleGeminiService
import org.j3y.HuskerBot2.service.WeatherService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
class SunriseSunsetScheduler(
    private val weatherService: WeatherService,
    private val channelMessageSchedulerService: ChannelMessageSchedulerService,
    private val googleGeminiService: GoogleGeminiService,
    @Value("\${discord.channels.general}") private val generalChannelId: String
) {
    private val log = LoggerFactory.getLogger(SunriseSunsetScheduler::class.java)
    private val zone: ZoneId = ZoneId.of("America/Chicago")
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    // Lincoln, Nebraska approximate coordinates
    private val lincolnLat = 40.8136
    private val lincolnLon = -96.7026

    // Run shortly after midnight local time to schedule for the day
    @Scheduled(cron = "0 5 0 * * *", zone = "America/Chicago")
    fun scheduleDailySunMessages() {
        try {
            val today = LocalDate.now(zone)
            scheduleForDate(today)
        } catch (e: Exception) {
            log.error("Error scheduling daily sunrise/sunset messages", e)
        }
    }

    private fun scheduleForDate(date: LocalDate) {
        val times = weatherService.getSunriseSunsetTimes(lincolnLat, lincolnLon, date, zone)
        if (times == null) {
            log.warn("Could not determine sunrise/sunset for {}", date)
            return
        }

        val (sunrise, sunset) = times //Pair(ZonedDateTime.now().plusSeconds(5), ZonedDateTime.now().plusSeconds(20)) //times
        val now = ZonedDateTime.now(zone).toInstant()
        val channelIdLong = generalChannelId.toLongOrNull()
        if (channelIdLong == null) {
            log.warn("Invalid general channel id: {}", generalChannelId)
            return
        }

        if (sunrise.toInstant().isAfter(now)) {
            val prompt = """
                Write one short Discord embed description line (max 200 characters). Tone: funny and sassy. Theme: sunrise. Explicitly poke fun at how annoying morning people are. No hashtags. No emojis.
            """.trimIndent()
            val geminiTextRaw = try { googleGeminiService.generateText(prompt) } catch (e: Exception) {
                log.warn("Gemini text generation failed for sunrise message", e)
                ""
            }
            val description = when {
                geminiTextRaw.isBlank() -> "Rise and whine. Shoutout to the morning people who woke up humming like it’s a musical—please lower your perkiness to a reasonable volume."
                geminiTextRaw.contains("Gemini is not configured", ignoreCase = true) -> "Rise and whine. Shoutout to the morning people who woke up humming like it’s a musical—please lower your perkiness to a reasonable volume."
                geminiTextRaw.startsWith("Error", ignoreCase = true) -> "Rise and whine. Shoutout to the morning people who woke up humming like it’s a musical—please lower your perkiness to a reasonable volume."
                geminiTextRaw.contains("No response from Gemini", ignoreCase = true) -> "Rise and whine. Shoutout to the morning people who woke up humming like it’s a musical—please lower your perkiness to a reasonable volume."
                else -> geminiTextRaw.trim().take(200)
            }

            val message = MessageData(
                embeds = listOf(
                    SimpleEmbed(
                        title = "☀\uFE0F Morning Gang",
                        description = description,
                        footer = "Sunrise @ ${sunrise.format(timeFormatter)}",
                        thumbnailUrl = "https://cdn.discordapp.com/emojis/991309655185817690.webp"
                    )
                )
            )
            channelMessageSchedulerService.scheduleMessage(
                channelIdLong,
                message,
                sunrise.toInstant(),
                instanceId = buildInstanceId(date, "sunrise")
            )
            log.info("Scheduled Morning Gang at {}", sunrise)
        } else {
            log.info("Sunrise for {} already passed at {}", date, sunrise)
        }

        if (sunset.toInstant().isAfter(now)) {
            val prompt = """
                Write one short Discord embed description line (max 200 characters). Tone: funny and sassy. Theme: sunset/night. Explicitly poke fun at how annoying people who stay up way too late are. No hashtags. No emojis.
            """.trimIndent()
            val geminiTextRawSunset = try { googleGeminiService.generateText(prompt) } catch (e: Exception) {
                log.warn("Gemini text generation failed for sunset message", e)
                ""
            }
            val sunsetDescription = when {
                geminiTextRawSunset.isBlank() -> "Night owls, congrats on surviving another 3am ‘grind.’ Please keep your chaotic sleep schedule and bragging at arm’s length from the rest of our circadian rhythms."
                geminiTextRawSunset.contains("Gemini is not configured", ignoreCase = true) -> "Night owls, congrats on surviving another 3am ‘grind.’ Please keep your chaotic sleep schedule and bragging at arm’s length from the rest of our circadian rhythms."
                geminiTextRawSunset.startsWith("Error", ignoreCase = true) -> "Night owls, congrats on surviving another 3am ‘grind.’ Please keep your chaotic sleep schedule and bragging at arm’s length from the rest of our circadian rhythms."
                geminiTextRawSunset.contains("No response from Gemini", ignoreCase = true) -> "Night owls, congrats on surviving another 3am ‘grind.’ Please keep your chaotic sleep schedule and bragging at arm’s length from the rest of our circadian rhythms."
                else -> geminiTextRawSunset.trim().take(200)
            }

            val message = MessageData(
                embeds = listOf(
                    SimpleEmbed(
                        title = "\uD83C\uDF1B Night Gang",
                        description = sunsetDescription,
                        footer = "Sunset @ ${sunset.format(timeFormatter)}",
                        thumbnailUrl = "https://cdn.discordapp.com/emojis/1104950612371701864.webp"
                    )
                )
            )

            channelMessageSchedulerService.scheduleMessage(
                channelIdLong,
                message,
                sunset.toInstant(),
                instanceId = buildInstanceId(date, "sunset")
            )
            log.info("Scheduled Night Gang at {}", sunset)
        } else {
            log.info("Sunset for {} already passed at {}", date, sunset)
        }
    }

    private fun buildInstanceId(date: LocalDate, type: String): String = "sun-${date}-$type"
}