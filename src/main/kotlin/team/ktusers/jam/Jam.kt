package team.ktusers.jam

import kotlinx.coroutines.coroutineScope
import net.bladehunt.kotstom.coroutines.startSuspending
import net.minestom.server.MinecraftServer

suspend fun main() = coroutineScope {
    val server = MinecraftServer.init()

    server.startSuspending("0.0.0.0", 25565)
}
