package team.ktusers.jam.game.puzzle

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.bladehunt.kotstom.dsl.item.item
import net.bladehunt.kotstom.dsl.listen
import net.bladehunt.kotstom.dsl.scheduleTask
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.kotstom.extension.editMeta
import net.minestom.server.collision.BoundingBox
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.BlockDisplayMeta
import net.minestom.server.entity.metadata.display.ItemDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.entity.metadata.other.InteractionMeta
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerTickEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.timer.TaskSchedule
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

        LaserObjective(objective).setGame(game, objectivePosition)

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

class LaserObjective(
    val color: PaletteColor
) : Entity(EntityType.ITEM_DISPLAY) {
    private val hitbox = Hitbox(this)
    private val title = Title(this)

    init {
        editMeta<ItemDisplayMeta> {
            this.itemStack = item(
                when (color) {
                    PaletteColor.RED -> Material.RED_STAINED_GLASS
                    PaletteColor.ORANGE -> Material.ORANGE_STAINED_GLASS
                    PaletteColor.YELLOW -> Material.YELLOW_STAINED_GLASS
                    PaletteColor.GREEN -> Material.GREEN_STAINED_GLASS
                    PaletteColor.BLUE -> Material.BLUE_STAINED_GLASS
                    PaletteColor.INDIGO -> Material.PURPLE_STAINED_GLASS
                    PaletteColor.VIOLET -> Material.MAGENTA_STAINED_GLASS
                    PaletteColor.GREY -> Material.GRAY_STAINED_GLASS
                    else -> throw IllegalArgumentException("Cannot use black or none")
                }
            )
            isHasNoGravity = true
            posRotInterpolationDuration = 1
            isHasGlowingEffect = true
            this
        }

        scheduleTask(repeat = TaskSchedule.nextTick(), delay = TaskSchedule.nextTick()) {
            if (it.instance == null) return@scheduleTask
            it.teleport(it.position.withYaw { yaw -> (yaw + 5).takeIf { yaw < 180 } ?: -180.0 })
        }
    }

    fun setGame(game: JamGame, position: Pos) {
        setInstance(game.instance, position)

        updateViewableRule {
            with(game) {
                it.currentColor == color
            }
        }

        hitbox.setInstance(instance, position.add(0.0, -0.5, 0.0))
        hitbox.updateViewableRule {
            with(game) {
                it.currentColor == color
            }
        }

        title.setInstance(instance, position.add(0.0, 0.5, 0.0))
        title.updateViewableRule {
            with(game) {
                it.currentColor == color
            }
        }
    }

    fun view(player: Player) {
        if (isRemoved) return
        addViewer(player)
        hitbox.addViewer(player)
        title.addViewer(player)
    }

    fun unview(player: Player) {
        removeViewer(player)
        hitbox.removeViewer(player)
        title.removeViewer(player)
    }

    override fun remove() {
        hitbox.remove()
        title.remove()
        super.remove()
    }

    class Hitbox(val objective: LaserObjective) : Entity(EntityType.INTERACTION) {
        init {
            editMeta<InteractionMeta> {
                height = 1f
                width = 1f
                response = true
                isHasNoGravity = true
                this
            }
        }
    }

    class Title(objective: LaserObjective) : Entity(EntityType.TEXT_DISPLAY) {
        init {
            editMeta<TextDisplayMeta> {
                text =
                    text(objective.color.name.lowercase().capitalize() + " Fragment", objective.color.textColor)
                billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
                isHasNoGravity = true
                this
            }
        }
    }
}