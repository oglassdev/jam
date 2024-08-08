package team.ktusers.jam

import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.bladehunt.kotstom.GlobalEventHandler
import net.bladehunt.kotstom.coroutines.startSuspending
import net.bladehunt.kotstom.dsl.listen
import net.bladehunt.kotstom.serialization.MinestomConfigModule
import net.bladehunt.kotstom.serialization.MinestomModule
import net.bladehunt.kotstom.serialization.adventure.AdventureNbt
import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import team.ktusers.jam.config.JamConfig

val Json = Json { serializersModule = MinestomConfigModule }

val AdventureNbt = AdventureNbt { serializersModule = MinestomModule }

val Config: JamConfig =
    team.ktusers.jam.Json.decodeFromStream(
        requireNotNull(JamConfig::class.java.getResourceAsStream("/config.json")) {
            "Failed to find config!"
        })

suspend fun main() = coroutineScope {
    val server = MinecraftServer.init()

    GlobalEventHandler.listen<AsyncPlayerConfigurationEvent> { event ->
        event.spawningInstance = Lobby
    }

    server.startSuspending("0.0.0.0", 25565)
}
