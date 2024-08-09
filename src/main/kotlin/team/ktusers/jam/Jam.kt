package team.ktusers.jam

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.bladehunt.blade.blade
import net.bladehunt.kotstom.serialization.MinestomConfigModule
import net.bladehunt.kotstom.serialization.MinestomModule
import net.bladehunt.kotstom.serialization.adventure.AdventureNbt
import team.ktusers.jam.config.JamConfig
import team.ktusers.jam.module.PropertiesModule

val Json = Json {
    serializersModule = MinestomConfigModule
}

val AdventureNbt = AdventureNbt {
    serializersModule = MinestomModule
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
    onConfigure {
        spawningInstance = Lobby
    }
}
