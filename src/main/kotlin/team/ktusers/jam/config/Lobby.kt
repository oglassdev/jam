package team.ktusers.jam.config

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minestom.server.coordinate.Pos

@Serializable data class Lobby(@SerialName("spawn_pos") val spawnPos: @Contextual Pos)
