package org.j3y.HuskerBot2.scheduler

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import net.dv8tion.jda.api.JDA
import org.j3y.HuskerBot2.model.MessageData
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Configuration
class ChannelMessageSchedulerConfig {
    data class ChannelMessagePayload(val channelId: Long, val content: MessageData) : Serializable

    companion object {
        const val TASK_NAME: String = "channel-message-send"
    }

    @Bean
    fun channelMessageTask(@Lazy jda: JDA): Task<ChannelMessagePayload> {
        val log = LoggerFactory.getLogger("ChannelMessageTask")
        return Tasks.oneTime(TASK_NAME, ChannelMessagePayload::class.java)
            .execute { taskInstance, _ ->
                val payload = taskInstance.data
                val channel = jda.getTextChannelById(payload.channelId)
                    ?: jda.getThreadChannelById(payload.channelId)
                    ?: jda.getNewsChannelById(payload.channelId)
                if (channel == null) {
                    log.warn("Channel message task could not find channel with id {}", payload.channelId)
                    return@execute
                }

                val messageData = payload.content.toMessageCreateData()
                channel.sendMessage(messageData).queue(
                    { log.info("Sent scheduled message to channel {}", payload.channelId) },
                    { ex -> log.error("Failed to send scheduled message to channel {}: {}", payload.channelId, ex.message) }
                )
            }
    }
}

@Service
class ChannelMessageSchedulerService(
    private val schedulerClient: SchedulerClient,
    private val channelMessageTask: Task<ChannelMessageSchedulerConfig.ChannelMessagePayload>
) {
    private val log = LoggerFactory.getLogger(ChannelMessageSchedulerService::class.java)

    fun scheduleMessage(channelId: Long, content: MessageData, executionTime: Instant, instanceId: String? = null) {
        val id = instanceId ?: UUID.randomUUID().toString()
        val payload = ChannelMessageSchedulerConfig.ChannelMessagePayload(channelId, content)
        val taskInstance = channelMessageTask.instance(id, payload)
        log.info("Scheduling channel message for channel {} at {} with instance {}", channelId, executionTime, id)
        schedulerClient.scheduleIfNotExists(taskInstance, executionTime)
    }
}