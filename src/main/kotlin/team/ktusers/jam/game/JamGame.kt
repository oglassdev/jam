package team.ktusers.jam.game

import net.bladehunt.blade.dsl.instance.buildInstance
import net.bladehunt.minigamelib.InstancedGame
import net.bladehunt.minigamelib.descriptor.GameDescriptor
import net.bladehunt.minigamelib.dsl.element
import net.bladehunt.minigamelib.dsl.gameDescriptor
import net.bladehunt.minigamelib.element.countdown
import net.minestom.server.entity.Player
import net.minestom.server.utils.NamespaceID
import java.util.*

class JamGame : InstancedGame(UUID.randomUUID(), buildInstance { }) {
    override val id: NamespaceID = NamespaceID.from("ktusers", "game")

    override val descriptor: GameDescriptor = gameDescriptor {
        +element {
            countdown(3, 30, 5)
        }
    }

    override fun Player.sendToFallback() {
        TODO("Not yet implemented")
    }
}