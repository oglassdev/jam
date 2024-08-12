package team.ktusers.jam.game.puzzle

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.bladehunt.kotstom.dsl.item.item
import net.bladehunt.kotstom.dsl.listen
import net.bladehunt.kotstom.dsl.scheduleTask
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.kotstom.extension.editMeta
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.ItemDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.entity.metadata.other.InteractionMeta
import net.minestom.server.event.EventNode
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.item.Material
import net.minestom.server.timer.TaskSchedule
import team.ktusers.jam.event.PlayerChangeColorEvent
import team.ktusers.jam.game.JamGame
import team.ktusers.jam.generated.PaletteColor

@Serializable
@SerialName("fragments")
data class Fragments(val fragments: List<Fragment.Config>) : Puzzle {
    override fun onElementStart(game: JamGame, eventNode: EventNode<InstanceEvent>) {
        val frags = fragments.map {
            val fragment = Fragment(it.color)
            fragment.setGame(game, it.position)
            fragment
        }

        eventNode.listen<PlayerChangeColorEvent> { event ->
            frags.forEach {
                if (event.toColor != it.color) {
                    it.unview(event.player)
                } else it.view(event.player)
            }
        }
    }
}

class Fragment(
    val color: PaletteColor
) : Entity(EntityType.ITEM_DISPLAY) {
    private val hitbox = Hitbox()
    private val title = Title()

    init {
        editMeta<ItemDisplayMeta> {
            this.itemStack = item(Material.FEATHER)
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
        addViewer(player)
        hitbox.addViewer(player)
        title.addViewer(player)
    }

    fun unview(player: Player) {
        removeViewer(player)
        hitbox.removeViewer(player)
        title.removeViewer(player)
    }

    @Serializable
    data class Config(val color: PaletteColor, val position: @Contextual Pos)

    inner class Hitbox : Entity(EntityType.INTERACTION) {
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

    inner class Title : Entity(EntityType.TEXT_DISPLAY) {
        init {
            editMeta<TextDisplayMeta> {
                text = text(color.name.lowercase().capitalize(), color.textColor)
                billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
                isHasNoGravity = true
                this
            }
        }
    }
}