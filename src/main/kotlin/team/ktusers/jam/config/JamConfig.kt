package team.ktusers.jam.config

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minestom.server.coordinate.Pos
import team.ktusers.jam.game.puzzle.Puzzle

@Serializable
data class JamConfig(
    @SerialName("resource_pack")
    val resourcePack: ResourcePack,
    val lobby: Lobby,
    val game: Game
) {
    @Serializable
    data class ResourcePack(
        val require: Boolean,
        val url: String,
        val hash: String
    )

    @Serializable
    data class Lobby(
        @SerialName("spawn_pos") val spawnPos: @Contextual Pos,
        @SerialName("npc_pos") val npcPos: @Contextual Pos
    )

    @Serializable
    data class Game(
        @SerialName("spawn_pos") val spawnPos: @Contextual Pos,
        @SerialName("spawn_radius") val spawnRadius: Double,
        val puzzles: List<Puzzle>
    )
}