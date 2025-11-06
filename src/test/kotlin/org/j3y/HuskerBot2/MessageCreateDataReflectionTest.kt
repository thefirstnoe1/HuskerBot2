package org.j3y.HuskerBot2

import org.junit.jupiter.api.Test

class MessageCreateDataReflectionTest {
    @Test
    fun printConstructors() {
        val cls = Class.forName("net.dv8tion.jda.api.utils.messages.MessageCreateData")
        println("[DEBUG_LOG] Constructors:")
        for (c in cls.declaredConstructors) {
            println("[DEBUG_LOG] ctor: ${c.parameterTypes.joinToString(prefix = "(", postfix = ")") { it.name }}")
        }
    }
}
