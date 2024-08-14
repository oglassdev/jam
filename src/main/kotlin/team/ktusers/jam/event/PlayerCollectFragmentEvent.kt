package team.ktusers.jam.event

import net.bladehunt.minigamelib.Game
import net.bladehunt.minigamelib.event.game.GameEvent
import net.minestom.server.entity.Player
import net.minestom.server.event.trait.PlayerInstanceEvent
import team.ktusers.jam.game.puzzle.Fragment

data class PlayerCollectFragmentEvent(
    override val game: Game,
    val collector: Player,
    val fragment: Fragment
) : GameEvent, PlayerInstanceEvent {
    override fun getPlayer(): Player = collector
}