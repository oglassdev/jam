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
import net.kyori.adventure.text.format.NamedTextColor.GRAY
import net.minestom.server.event.player.PlayerChatEvent
import team.ktusers.jam.command.CutsceneCommand
import team.ktusers.jam.command.JoinCommand
import team.ktusers.jam.command.LobbyCommand
import team.ktusers.jam.config.JamConfig
import team.ktusers.jam.cutscene.CUTSCENE_REFERENCE
import team.ktusers.jam.game.JamGame

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
        "minestom.chunk-view-distance" to 18
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

        CompletableFuture.allOf(
            this.instance.loadChunks(-9, -8, 10),
            this.instance.loadChunks(12, -19, 16)
        ).thenAccept {
            polar {
                fromPath("./world.polar")
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

    onConfigure {
        spawningInstance = Lobby
    }
}
