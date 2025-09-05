package org.j3y.HuskerBot2.commands.other

import net.dv8tion.jda.api.interactions.commands.OptionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PoliceTest {

    @Test
    fun testPoliceCommand() {
        val police = Police()

        assertEquals("police", police.getCommandKey())
        assertEquals("Call the police on someone", police.getDescription())
        assertFalse(police.isSubcommand())
        assertTrue(police.isLogged())

        val options = police.getOptions()
        assertEquals(1, options.size)
        
        val arresteeOption = options[0]
        assertEquals("arrestee", arresteeOption.name)
        assertEquals("The user to arrest", arresteeOption.description)
        assertEquals(OptionType.USER, arresteeOption.type)
        assertTrue(arresteeOption.isRequired)
    }
}