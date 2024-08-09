package team.ktusers.jam.game

import net.minestom.server.instance.block.Block
import net.minestom.server.utils.NamespaceID

object JamBlock {
    val BLACK = Block.BLACKSTONE
    val BLACK_SLAB = Block.BLACKSTONE_SLAB
    val BLACK_STAIRS = Block.BLACKSTONE_STAIRS

    val RED = Block.RED_NETHER_BRICKS
    val RED_SLAB = Block.RED_NETHER_BRICK_SLAB
    val RED_STAIRS = Block.RED_NETHER_BRICK_STAIRS

    val ORANGE = Block.ACACIA_PLANKS
    val ORANGE_SLAB = Block.ACACIA_SLAB
    val ORANGE_STAIRS = Block.ACACIA_STAIRS

    val YELLOW = Block.BAMBOO_PLANKS
    val YELLOW_SLAB = Block.BAMBOO_SLAB
    val YELLOW_STAIRS = Block.BAMBOO_STAIRS

    val GREEN = Block.WEATHERED_CUT_COPPER
    val GREEN_SLAB = Block.WEATHERED_CUT_COPPER_SLAB
    val GREEN_STAIRS = Block.WEATHERED_CUT_COPPER_STAIRS

    val BLUE = Block.DARK_PRISMARINE
    val BLUE_SLAB = Block.DARK_PRISMARINE_SLAB
    val BLUE_STAIRS = Block.DARK_PRISMARINE_STAIRS

    val INDIGO = Block.CRIMSON_PLANKS
    val INDIGO_SLAB = Block.CRIMSON_SLAB
    val INDIGO_STAIRS = Block.CRIMSON_STAIRS

    val VIOLET = Block.PURPUR_BLOCK
    val VIOLET_SLAB = Block.PURPUR_SLAB
    val VIOLET_STAIRS = Block.PURPUR_STAIRS

    val values = setOf(
        BLACK, BLACK_SLAB, BLACK_STAIRS,
        RED, RED_SLAB, RED_STAIRS,
        ORANGE, ORANGE_SLAB, ORANGE_STAIRS,
        YELLOW, YELLOW_SLAB, YELLOW_STAIRS,
        GREEN, GREEN_SLAB, GREEN_STAIRS,
        BLUE, BLUE_SLAB, BLUE_STAIRS,
        INDIGO, INDIGO_SLAB, INDIGO_STAIRS,
        VIOLET, VIOLET_SLAB, VIOLET_STAIRS
    )

    val mapped = buildMap<NamespaceID, Block> {
        this@JamBlock.values.forEach {
            put(
                it.namespace(),
                if (it.name().contains("slab")) BLACK_SLAB
                else if (it.name().contains("stairs")) BLACK_STAIRS
                else BLACK
            )
        }
    }
}