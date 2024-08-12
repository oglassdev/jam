package team.ktusers.jam.event

import net.bladehunt.minigamelib.Game
import net.bladehunt.minigamelib.event.game.GameEvent
import net.minestom.server.entity.Player
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.event.trait.PlayerInstanceEvent
import team.ktusers.jam.generated.PaletteColor

data class PlayerCollectColorEvent(
    override val game: Game,
    private val player: Player,
    val color: PaletteColor
) : GameEvent, PlayerInstanceEvent, CancellableEvent {
    private var cancelled: Boolean = false

    override fun getPlayer(): Player = player

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }
}