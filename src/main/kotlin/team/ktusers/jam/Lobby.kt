package team.ktusers.jam

import net.bladehunt.kotstom.InstanceManager
import net.hollowcube.polar.PolarLoader
import net.minestom.server.instance.LightingChunk
import net.minestom.server.utils.chunk.ChunkSupplier

val Lobby =
    InstanceManager.createInstanceContainer().apply {
        chunkSupplier = ChunkSupplier(::LightingChunk)

        this::class.java.getResourceAsStream("lobby.polar")?.let { stream ->
            chunkLoader = PolarLoader(stream)
        }
    }
