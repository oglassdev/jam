package team.ktusers.jam.game.puzzle

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.bladehunt.kotstom.GlobalEventHandler
import net.bladehunt.kotstom.dsl.listen
import net.bladehunt.kotstom.extension.adventure.plus
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.kotstom.extension.asVec
import net.bladehunt.kotstom.extension.editMeta
import net.bladehunt.kotstom.extension.minus
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component.newline
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.BlockDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.entity.metadata.other.InteractionMeta
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import team.ktusers.jam.event.PlayerCollectColorEvent
import team.ktusers.jam.event.PlayerPlaceAllRelicsEvent
import team.ktusers.jam.game.JamGame
import team.ktusers.jam.generated.PaletteColor

@Serializable
@SerialName("dark_matter")
data class DarkMatter(
    val center: @Contextual Vec,
    val positions: List<@Contextual Vec>
) : Puzzle {
    override fun onElementStart(game: JamGame, eventNode: EventNode<InstanceEvent>) {
        val altars = positions.mapIndexed { index, vec ->
            val interaction = AltarInteraction(PaletteColor.entries[index])
            interaction.setInstance(game.instance, vec)
            interaction.text.setInstance(game.instance, vec.add(0.0, 1.5, 0.0))
            interaction
        }.associateBy { it.color }

        eventNode.listen<PlayerCollectColorEvent> { event ->
            val altar = altars[event.color] ?: return@listen
            altar.text.editMeta<TextDisplayMeta> {
                text = text(altar.color.name.lowercase().capitalize(), altar.color.textColor) + newline() +
                        text("Not Placed", NamedTextColor.GRAY)
                this
            }
        }

        eventNode.listen<PlayerEntityInteractEvent> { event ->
            val altar = event.target as? AltarInteraction ?: return@listen
            if (game.teamInventory.colors.contains(altar.color)) {
                if (altar.isPlaced) {
                    return@listen
                }
                game.sendMessage(
                    text(
                        "The ${altar.color.name.lowercase()} relic has been placed by ",
                        NamedTextColor.GRAY
                    ) + event.player.name
                )
                altar.text.editMeta<TextDisplayMeta> {
                    text = text(altar.color.name.lowercase().capitalize(), altar.color.textColor) + newline() +
                            text("Placed", NamedTextColor.GREEN)
                    this
                }
                val beam = ColorBeam(altar.color, altar.position.distance(center))
                beam.setInstance(
                    game.instance,
                    altar.position.withDirection(center.asVec() - altar.position.asVec())
                )
                game.playSound(
                    Sound.sound()
                        .type(SoundEvent.ENTITY_BREEZE_DEATH)
                        .build(),
                )
                altar.isPlaced = true
                if (altars.values.all { it.isPlaced }) {
                    GlobalEventHandler.call(PlayerPlaceAllRelicsEvent(game, event.player))
                }
                return@listen
            }
            event.player.sendMessage(text("This relic hasn't been found yet!", NamedTextColor.RED))
        }
    }
}

class AltarInteraction(
    val color: PaletteColor
) : Entity(EntityType.INTERACTION) {
    var isPlaced: Boolean = false

    val text = Entity(EntityType.TEXT_DISPLAY).apply {
        editMeta<TextDisplayMeta> {
            text = text(color.name.lowercase().capitalize(), color.textColor) + newline() +
                    text("Not Found", NamedTextColor.RED)
            billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
            isHasNoGravity = true
            this
        }
    }

    init {
        editMeta<InteractionMeta> {
            height = 1.25f
            width = 1.25f
            response = true
            isHasNoGravity = true
            this
        }
    }
}

class ColorBeam(
    color: PaletteColor,
    length: Double
) : Entity(EntityType.BLOCK_DISPLAY) {
    init {
        editMeta<BlockDisplayMeta> {
            setBlockState(
                when (color) {
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
            this.scale = Vec(0.25, 0.25, length)
            isHasNoGravity = true
            this
        }
    }
}