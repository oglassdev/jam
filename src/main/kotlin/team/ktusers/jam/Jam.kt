package team.ktusers.jam

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.bladehunt.blade.blade
import net.bladehunt.blade.module.PropertiesModule
import net.bladehunt.kotstom.CommandManager
import net.bladehunt.kotstom.GlobalEventHandler
import net.bladehunt.kotstom.dsl.listen
import net.bladehunt.kotstom.extension.adventure.plus
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.kotstom.extension.register
import net.bladehunt.kotstom.serialization.MinestomConfigModule
import net.bladehunt.kotstom.serialization.MinestomModule
import net.bladehunt.kotstom.serialization.adventure.AdventureNbt
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor.GRAY
import net.minestom.server.event.player.PlayerChatEvent
import team.ktusers.jam.command.CutsceneCommand
import team.ktusers.jam.command.JoinCommand
import team.ktusers.jam.command.LobbyCommand
import team.ktusers.jam.config.JamConfig
import team.ktusers.jam.cutscene.CUTSCENE_REFERENCE
import team.ktusers.jam.game.JamGame
import java.net.URI
import java.util.*

val Json = Json {
    serializersModule = MinestomConfigModule
}

val AdventureNbt = AdventureNbt {
    serializersModule = MinestomModule
    shouldEncodeDefaults = true
}

@OptIn(ExperimentalSerializationApi::class)
val Config: JamConfig =
    team.ktusers.jam.Json.decodeFromStream(
        requireNotNull(JamConfig::class.java.getResourceAsStream("/config.json")) {
            "Failed to find config!"
        })

suspend fun main() = blade(
    PropertiesModule(
        "minestom.chunk-view-distance" to 8
    )
) {
    JamGame
    CUTSCENE_REFERENCE
    /*

        val inst = buildInstance {
            anvil {
                fromPath("./world")
            }
            instance.enableAutoChunkLoad(false)

            val chunks = ArrayList<CompletableFuture<Chunk>>()

            for (x in -11..10) {
                for (y in -11..10) {
                    chunks.add(this.instance.loadChunk(x, y))
                }
            }
            CompletableFuture.allOf(*chunks.toTypedArray()).thenAccept {
                polar {
                    fromPath("./world.polar")
                }

                val batch = AbsoluteBlockBatch()
                for (chunk in instance.chunks) {
                    for (x in 0..15) {
                        for (y in -64..<0) {
                            for (z in 0..15) {
                                if (!chunk.getBlock(x, y, z).isAir) batch.setBlock(x, y, z, Block.AIR)
                            }
                        }
                    }
                }
                batch.apply(instance) {
                    println("applied goated batch")
                }

                instance.saveChunksToStorage().thenAccept {
                    println("saved chunks")

                }

            }
        }*/


    CommandManager.register(LobbyCommand, JoinCommand, CutsceneCommand)

    GlobalEventHandler.listen<PlayerChatEvent> { event ->
        event.recipients.removeIf { it.instance != event.player.instance }
        event.setChatFormat {
            it.player.name + text(": " + it.message, GRAY)
        }
    }

    val rp = ResourcePackRequest.resourcePackRequest()
        .required(true)
        .prompt(
            text("You must use the resource pack for the best experience!") +
                    Component.newline() +
                    text("Additionally, fabulous quality is recommended.")
        )
        .packs(
            ResourcePackInfo.resourcePackInfo(
                UUID.randomUUID(),
                URI(Config.resourcePack.url),
                Config.resourcePack.hash
            )
        )
        .build()

    onConfigure {
        spawningInstance = Lobby
        player.sendResourcePacks(rp)
    }
}
