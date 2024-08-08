package team.ktusers.jam

import kotlinx.coroutines.coroutineScope
import net.bladehunt.kotstom.GlobalEventHandler
import net.bladehunt.kotstom.coroutines.startSuspending
import net.bladehunt.kotstom.dsl.listen
import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent

suspend fun main() = coroutineScope {
    val server = MinecraftServer.init()

    GlobalEventHandler.listen<AsyncPlayerConfigurationEvent> { event ->
        event.spawningInstance = Lobby
    }

    server.startSuspending("0.0.0.0", 25565)
}
