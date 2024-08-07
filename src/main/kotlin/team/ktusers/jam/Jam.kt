package team.ktusers.jam

import kotlinx.coroutines.*
import net.minestom.server.MinecraftServer

suspend fun main() = coroutineScope {
    val server = MinecraftServer.init()

    withContext(Dispatchers.IO) { server.start("0.0.0.0", 25565) }
}