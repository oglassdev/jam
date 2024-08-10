package team.ktusers.jam.event

import net.bladehunt.minigamelib.Game
import net.bladehunt.minigamelib.event.game.GameEvent
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.entity.Player
import net.minestom.server.event.trait.BlockEvent
import net.minestom.server.event.trait.PlayerInstanceEvent
import net.minestom.server.instance.block.Block

data class PlayerCleanseBlockEvent(
    override val game: Game,
    private val player: Player,
    private val block: Block,
    private val position: BlockVec
) : GameEvent, PlayerInstanceEvent, BlockEvent {
    override fun getPlayer(): Player = player

    override fun getBlock(): Block = block

    override fun getBlockPosition(): BlockVec = position
}