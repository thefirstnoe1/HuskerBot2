package org.j3y.HuskerBot2.scheduler

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.Task
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.lang.reflect.Method
import java.time.Instant

class ReminderSchedulerConfigTest {

    private val config = ReminderSchedulerConfig()

    @Nested
    @DisplayName("reminderTask bean")
    inner class ReminderTaskBeanTests {
        @Test
        fun `returns one-time task with correct name`() {
            val jda = Mockito.mock(JDA::class.java)
            val task: Task<ReminderSchedulerConfig.ReminderPayload> = config.reminderTask(jda)
            assertNotNull(task)

            val name = extractTaskName(task)
            assertEquals(ReminderSchedulerConfig.TASK_NAME, name)
        }

        @Test
        fun `execute sends embed when TextChannel found`() {
            val jda = Mockito.mock(JDA::class.java)
            val channel = Mockito.mock(TextChannel::class.java)
            val action = Mockito.mock(MessageCreateAction::class.java)

            Mockito.`when`(jda.getTextChannelById(123L)).thenReturn(channel)
            Mockito.`when`(channel.sendMessageEmbeds(Mockito.any(MessageEmbed::class.java))).thenReturn(action)

            val task = config.reminderTask(jda)
            val payload = ReminderSchedulerConfig.ReminderPayload(123L, 456L, "Hello")

            val invoked = invokeExecute(task, payload)
            assertTrue(invoked, "Could not invoke execute handler")

            Mockito.verify(channel, Mockito.times(1))
                .sendMessageEmbeds(Mockito.any(MessageEmbed::class.java))
        }

        @Test
        fun `execute sends embed when ThreadChannel found`() {
            val jda = Mockito.mock(JDA::class.java)
            val channel = Mockito.mock(ThreadChannel::class.java)
            val action = Mockito.mock(MessageCreateAction::class.java)

            Mockito.`when`(jda.getTextChannelById(9999L)).thenReturn(null)
            Mockito.`when`(jda.getThreadChannelById(9999L)).thenReturn(channel)
            Mockito.`when`(channel.sendMessageEmbeds(Mockito.any(MessageEmbed::class.java))).thenReturn(action)

            val task = config.reminderTask(jda)
            val payload = ReminderSchedulerConfig.ReminderPayload(9999L, 1L, "Thread msg")

            val invoked = invokeExecute(task, payload)
            assertTrue(invoked, "Could not invoke execute handler")

            Mockito.verify(channel, Mockito.times(1))
                .sendMessageEmbeds(Mockito.any(MessageEmbed::class.java))
        }

        @Test
        fun `execute sends embed when NewsChannel found`() {
            val jda = Mockito.mock(JDA::class.java)
            val channel = Mockito.mock(NewsChannel::class.java)
            val action = Mockito.mock(MessageCreateAction::class.java)

            Mockito.`when`(jda.getTextChannelById(42L)).thenReturn(null)
            Mockito.`when`(jda.getThreadChannelById(42L)).thenReturn(null)
            Mockito.`when`(jda.getNewsChannelById(42L)).thenReturn(channel)
            Mockito.`when`(channel.sendMessageEmbeds(Mockito.any(MessageEmbed::class.java))).thenReturn(action)

            val task = config.reminderTask(jda)
            val payload = ReminderSchedulerConfig.ReminderPayload(42L, 5L, "News msg")

            val invoked = invokeExecute(task, payload)
            assertTrue(invoked, "Could not invoke execute handler")

            Mockito.verify(channel, Mockito.times(1))
                .sendMessageEmbeds(Mockito.any(MessageEmbed::class.java))
        }

        @Test
        fun `execute does nothing when channel not found`() {
            val jda = Mockito.mock(JDA::class.java)
            Mockito.`when`(jda.getTextChannelById(7L)).thenReturn(null)
            Mockito.`when`(jda.getThreadChannelById(7L)).thenReturn(null)
            Mockito.`when`(jda.getNewsChannelById(7L)).thenReturn(null)

            val task = config.reminderTask(jda)
            val payload = ReminderSchedulerConfig.ReminderPayload(7L, 8L, "nope")

            val invoked = invokeExecute(task, payload)
            assertTrue(invoked, "Could not invoke execute handler")

            // Ensure no send was attempted: since we returned null from all getters, just verify the getters were called
            Mockito.verify(jda, Mockito.times(1)).getTextChannelById(7L)
            Mockito.verify(jda, Mockito.times(1)).getThreadChannelById(7L)
            Mockito.verify(jda, Mockito.times(1)).getNewsChannelById(7L)
        }
    }

    @Nested
    inner class ReminderServiceTests {
        @Test
        fun `scheduleReminder schedules task instance with payload and time`() {
            var capturedInstance: Any? = null
            var capturedTime: Instant? = null
            val schedulerClient = Mockito.mock(SchedulerClient::class.java, org.mockito.stubbing.Answer { inv ->
                if (inv.method.name == "scheduleIfNotExists" && inv.arguments.size == 2 && inv.arguments[1] is Instant) {
                    capturedInstance = inv.arguments[0]
                    capturedTime = inv.arguments[1] as Instant
                    return@Answer null
                }
                return@Answer Mockito.RETURNS_DEFAULTS.answer(inv)
            })
            val jda = Mockito.mock(JDA::class.java)
            val task: Task<ReminderSchedulerConfig.ReminderPayload> = config.reminderTask(jda)

            val service = ReminderService(schedulerClient, task)

            val whenTo = Instant.parse("2025-08-23T20:00:00Z")
            val channelId = 111L
            val userId = 222L
            val message = "Pay rent"

            service.scheduleReminder(channelId, userId, message, whenTo)

            // Verify time
            assertEquals(whenTo, capturedTime)

            // Attempt to verify payload and task name from the captured TaskInstance
            val instanceObj = capturedInstance!!
            assertNotNull(instanceObj)

            // Extract payload via method or field
            val payloadObj: Any? = run {
                val method = instanceObj.javaClass.methods.firstOrNull { it.name == "getData" && it.parameterCount == 0 }
                method?.invoke(instanceObj)
            } ?: run {
                val field = instanceObj.javaClass.declaredFields.firstOrNull { it.name == "data" }
                if (field != null) {
                    field.isAccessible = true
                    field.get(instanceObj)
                } else null
            }
            assertNotNull(payloadObj, "TaskInstance should expose payload via getData() or data field")
            val payload = payloadObj as ReminderSchedulerConfig.ReminderPayload
            assertEquals(channelId, payload.channelId)
            assertEquals(userId, payload.userId)
            assertEquals(message, payload.message)

            // Task name already validated in another test; focus here on payload and scheduling time.
        }
    }

    // --- Helpers ---

    private fun <T> extractTaskName(task: Task<T>): String {
        // Try instance(...) path which tends to be stable
        return runCatching {
            val payload: Any? = null
            val instanceMethod: Method = task.javaClass.methods.first { it.name == "instance" }
            val params = instanceMethod.parameterTypes
            val args: Array<Any?> = when (params.size) {
                1 -> arrayOf("test-id")
                2 -> arrayOf("test-id", payload)
                else -> arrayOf("test-id")
            }
            val instanceObj = instanceMethod.invoke(task, *args)
            val tnField = instanceObj.javaClass.declaredFields.firstOrNull { it.name == "taskName" || it.name == "name" }
            if (tnField != null) {
                tnField.isAccessible = true
                val taskNameObj = tnField.get(instanceObj)
                val valueField = taskNameObj.javaClass.declaredFields.firstOrNull { it.name == "value" }
                if (valueField != null) {
                    valueField.isAccessible = true
                    val value = valueField.get(taskNameObj) as? String
                    if (value != null) return value
                }
                return taskNameObj.toString()
            }
            instanceObj.toString()
        }.getOrElse {
            // Fallback to toString parsing
            val text = task.toString()
            val marker = "name="
            val idx = text.indexOf(marker)
            if (idx >= 0) {
                val after = text.substring(idx + marker.length)
                val end = after.indexOf(',')
                if (end >= 0) after.substring(0, end).trim() else after.trim()
            } else text
        }
    }

    private fun invokeExecute(task: Task<ReminderSchedulerConfig.ReminderPayload>, payload: ReminderSchedulerConfig.ReminderPayload): Boolean {
        // Build a TaskInstance via public API
        val instanceMethod = task.javaClass.methods.firstOrNull { it.name == "instance" }
            ?: return false
        val instanceObj = when (instanceMethod.parameterTypes.size) {
            1 -> instanceMethod.invoke(task, "test-id")
            2 -> instanceMethod.invoke(task, "test-id", payload)
            else -> instanceMethod.invoke(task, "test-id", payload)
        }
        // Find an object that has execute(TaskInstance, ExecutionContext)
        val visited = mutableSetOf<Any>()
        fun tryInvoke(obj: Any?): Boolean {
            if (obj == null) return false
            if (!visited.add(obj)) return false
            // Look for any execute(TaskInstance, ExecutionContext) method (public or non-public)
            val exec = (obj.javaClass.methods + obj.javaClass.declaredMethods)
                .firstOrNull { it.name == "execute" && it.parameterCount == 2 }
            if (exec != null) {
                return runCatching {
                    exec.isAccessible = true
                    exec.invoke(obj, instanceObj, null)
                    true
                }.getOrDefault(false)
            }
            // Recurse into fields (including superclass declared fields)
            val allFields = mutableListOf<java.lang.reflect.Field>()
            var c: Class<*>? = obj.javaClass
            while (c != null) {
                allFields += c.declaredFields
                c = c.superclass
            }
            allFields.forEach { f ->
                runCatching {
                    f.isAccessible = true
                    val v = f.get(obj)
                    if (tryInvoke(v)) return true
                }
            }
            return false
        }
        if (tryInvoke(task)) return true
        task.javaClass.declaredFields.forEach { f ->
            runCatching {
                f.isAccessible = true
                val v = f.get(task)
                if (tryInvoke(v)) return true
            }
        }
        return false
    }
}
