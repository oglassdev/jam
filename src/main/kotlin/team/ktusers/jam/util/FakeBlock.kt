package team.ktusers.jam.util

import net.bladehunt.kotstom.dsl.listen
import net.bladehunt.kotstom.extension.asVec
import net.bladehunt.kotstom.extension.editMeta
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.BlockDisplayMeta
import net.minestom.server.entity.metadata.other.InteractionMeta
import net.minestom.server.instance.block.Block
import team.ktusers.jam.event.PlayerChangeColorEvent
import team.ktusers.jam.game.JamGame
import team.ktusers.jam.generated.PaletteColor
import java.util.function.Predicate

class FakeBlock(block: Block, val visibleWhen: PaletteColor) : Entity(EntityType.BLOCK_DISPLAY) {
    val interaction: Entity = Interaction()

    private class Interaction : Entity(EntityType.INTERACTION) {
        init {
            editMeta<InteractionMeta> {
                height = 1.2f
                width = 1.2f
                response = true
                isHasNoGravity = true
                this
            }
        }
    }

    init {
        editMeta<BlockDisplayMeta> {
            setBlockState(block)
            isHasGlowingEffect = true
            isHasNoGravity = true
            this
        }
    }

    override fun remove() {
        interaction.remove()
        super.remove()
    }

    fun setGame(game: JamGame, point: Point) {
        val vec = point.asVec()
        setInstance(game.instance, vec)
        interaction.setInstance(game.instance, vec.add(0.5, -0.1, 0.5))
        val predicate: Predicate<Player> = Predicate { player ->
            with(game) {
                player.currentColor == visibleWhen
            }
        }
        updateViewableRule(predicate)
        interaction.updateViewableRule(predicate)
        game.eventNode().listen<PlayerChangeColorEvent> { event ->
            if (isRemoved) return@listen
            if (event.toColor == visibleWhen) {
                addViewer(event.player)
                interaction.addViewer(event.player)
                return@listen
            }
            removeViewer(event.player)
            interaction.removeViewer(event.player)
        }
    }
}