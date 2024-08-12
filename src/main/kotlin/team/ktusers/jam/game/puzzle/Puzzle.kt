package team.ktusers.jam.game.puzzle

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.minestom.server.event.EventNode
import net.minestom.server.event.trait.InstanceEvent
import team.ktusers.jam.game.JamGame

@Serializable
@Polymorphic
sealed interface Puzzle {
    fun onElementStart(game: JamGame, eventNode: EventNode<InstanceEvent>)
}