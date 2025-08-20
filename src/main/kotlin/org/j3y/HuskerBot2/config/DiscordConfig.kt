package org.j3y.HuskerBot2.config

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import org.j3y.HuskerBot2.commands.CommandListener
import org.j3y.HuskerBot2.commands.SlashCommand
import org.j3y.HuskerBot2.service.DefaultEspnService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.EnumSet

@Configuration
class DiscordConfig {

    private final val log = LoggerFactory.getLogger(DefaultEspnService::class.java)

    @Bean
    fun getDiscordClient(
        @Value("\${discord.token}") token: String,
        slashCommands: Array<SlashCommand>,
        commandListener: CommandListener
    ): JDA {
        val jda = JDABuilder.createLight(token, EnumSet.of(
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_PRESENCES,
                GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
                GatewayIntent.GUILD_MESSAGE_REACTIONS
            ))
            .addEventListeners(commandListener)
            .build()

        val commands = jda.updateCommands()
        commands.addCommands(
            slashCommands.map {
                Commands.slash(it.getCommandKey(), it.getDescription())
                    .addOptions(it.getOptions())
            }
        )

        commands.queue()

        return jda
    }

}