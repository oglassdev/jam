package team.ktusers.jam.game.puzzle

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.bladehunt.kotstom.dsl.listen
import net.bladehunt.kotstom.extension.editMeta
import net.minestom.server.collision.BoundingBox
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.metadata.display.BlockDisplayMeta
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerTickEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.block.Block
import team.ktusers.jam.event.PlayerChangeColorEvent
import team.ktusers.jam.game.JamGame
import team.ktusers.jam.generated.PaletteColor

@Serializable
@SerialName("lasers")
data class Lasers(
    val lasers: List<Laser.Config>,
    val objective: PaletteColor,
    @SerialName("objective_position")
    val objectivePosition: @Contextual Pos
) : Puzzle {
    override fun onElementStart(game: JamGame, eventNode: EventNode<InstanceEvent>) {
        val lasers = lasers.map {
            val laser = Laser(it.visible, it.sizeX, it.rotation)
            laser.setInstance(game.instance, it.position.asPosition().withYaw((it.rotation * 90).toFloat()))
            laser.updateViewableRule { player ->
                with(game) {
                    player.currentColor == it.visible
                }
            }
            laser
        }

        eventNode.listen<PlayerChangeColorEvent> { event ->
            lasers.forEach {
                if (event.toColor != it.visible) {
                    it.removeViewer(event.player)
                } else it.addViewer(event.player)
            }
        }

        eventNode.listen<PlayerTickEvent> { event ->
            val color = with(game) { event.player.currentColor }
            if (event.player.position == event.player.previousPosition) return@listen
            lasers.filter { it.visible == color }.forEach { laser ->
                if (laser.boundingBox.intersectEntity(laser.position, event.player)) {
                    event.player.damage(Damage.fromEntity(laser, 0.25f))
                }
            }
        }
    }
}

data class Laser(
    val visible: PaletteColor,
    val sizeX: Double,
    val rot: Int
) : Entity(EntityType.BLOCK_DISPLAY) {
    init {
        editMeta<BlockDisplayMeta> {
            setBlockState(
                when (visible) {
                    PaletteColor.RED -> Block.RED_STAINED_GLASS
                    PaletteColor.ORANGE -> Block.ORANGE_STAINED_GLASS
                    PaletteColor.YELLOW -> Block.YELLOW_STAINED_GLASS
                    PaletteColor.GREEN -> Block.GREEN_STAINED_GLASS
                    PaletteColor.BLUE -> Block.BLUE_STAINED_GLASS
                    PaletteColor.INDIGO -> Block.PURPLE_STAINED_GLASS
                    PaletteColor.VIOLET -> Block.MAGENTA_STAINED_GLASS
                    PaletteColor.GREY -> Block.GRAY_STAINED_GLASS
                    else -> throw IllegalArgumentException("Cannot use black or none")
                }
            )
            this.scale = Vec(sizeX, 0.1, 0.1)
            isHasNoGravity = true
            this
        }

        boundingBox = BoundingBox(
            if (rot % 2 == 0) sizeX else 0.1,
            0.1,
            if (rot % 2 == 0) 0.1 else sizeX,
            Vec(
                if (rot == 2) -sizeX else 0.0,
                0.0,
                if (rot == 3) -sizeX else 0.0,
            )
        )
    }

    @Serializable
    data class Config(val visible: PaletteColor, val sizeX: Double, val position: @Contextual Vec, val rotation: Int)
}