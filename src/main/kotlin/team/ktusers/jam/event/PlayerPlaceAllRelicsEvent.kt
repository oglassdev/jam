package team.ktusers.jam.event

import net.bladehunt.minigamelib.Game
import net.bladehunt.minigamelib.event.game.GameEvent
import net.minestom.server.entity.Player
import net.minestom.server.event.trait.PlayerInstanceEvent

data class PlayerPlaceAllRelicsEvent(
    override val game: Game,
    private val player: Player
) : GameEvent, PlayerInstanceEvent {
    override fun getPlayer(): Player = player
}