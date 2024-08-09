package team.ktusers.jam.game

import kotlinx.serialization.Serializable
import net.minestom.server.instance.block.Block

@Serializable
sealed class JamColor {
    abstract val block: Block
    abstract val slab: Block
    abstract val stairs: Block

    @Serializable
    data object Black : JamColor() {
        override val block: Block = Block.BLACKSTONE
        override val slab: Block = Block.BLACKSTONE_SLAB
        override val stairs: Block = Block.BLACKSTONE_STAIRS
    }

    @Serializable
    data object Red : JamColor() {
        override val block: Block = Block.RED_NETHER_BRICKS
        override val slab: Block = Block.RED_NETHER_BRICK_SLAB
        override val stairs: Block = Block.RED_NETHER_BRICK_STAIRS
    }
}