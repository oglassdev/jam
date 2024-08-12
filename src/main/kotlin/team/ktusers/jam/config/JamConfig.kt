package team.ktusers.jam.config

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Pos

@Serializable
data class JamConfig(
    val lobby: Lobby,
    val game: Game
) {
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

    @Serializable
    sealed class Puzzle

    @Serializable
    @SerialName("entrance")
    data class Entrance(val pos: @Contextual Pos) : Puzzle()

    @Serializable
    @SerialName("select")
    data class Select(val pos: @Contextual BlockVec) : Puzzle()
}