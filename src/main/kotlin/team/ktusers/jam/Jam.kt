package team.ktusers.jam

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.bladehunt.blade.blade
import net.bladehunt.blade.module.PropertiesModule
import net.bladehunt.kotstom.CommandManager
import net.bladehunt.kotstom.extension.register
import net.bladehunt.kotstom.serialization.MinestomConfigModule
import net.bladehunt.kotstom.serialization.MinestomModule
import net.bladehunt.kotstom.serialization.adventure.AdventureNbt
import team.ktusers.jam.command.CutsceneCommand
import team.ktusers.jam.command.JoinCommand
import team.ktusers.jam.command.LobbyCommand
import team.ktusers.jam.config.JamConfig
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

    /*
    FileOutputStream(File("world.polar")).use { stream ->
        stream.write(PolarWriter.write(AnvilPolar.anvilToPolar(Path("./world"))))
    }

     */

    CommandManager.register(LobbyCommand, JoinCommand, CutsceneCommand)
    onConfigure {
        spawningInstance = Lobby
    }
}
