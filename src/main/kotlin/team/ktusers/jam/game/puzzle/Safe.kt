package team.ktusers.jam.game.puzzle

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.bladehunt.kotstom.GlobalEventHandler
import net.bladehunt.kotstom.dsl.item.amount
import net.bladehunt.kotstom.dsl.item.item
import net.bladehunt.kotstom.dsl.item.itemName
import net.bladehunt.kotstom.dsl.listen
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.kotstom.extension.editMeta
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.display.BlockDisplayMeta
import net.minestom.server.entity.metadata.other.InteractionMeta
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import team.ktusers.jam.dsl.simpleGui
import team.ktusers.jam.event.PlayerCollectColorEvent
import team.ktusers.jam.game.JamGame
import team.ktusers.jam.generated.PaletteColor
import kotlin.random.Random

@Serializable
@SerialName("select")
data class Safe(val pos: @Contextual BlockVec) : Puzzle {
    override fun onElementStart(game: JamGame, eventNode: EventNode<InstanceEvent>) {
        val interaction = Entity(EntityType.INTERACTION)
        interaction.editMeta<InteractionMeta> {
            height = 1.2f
            width = 1.2f
            response = true
            isHasNoGravity = true
            this
        }
        interaction.setInstance(game.instance, pos.add(0.5, -0.1, 0.5))

        var isComplete = false

        eventNode.listen<PlayerEntityInteractEvent> { event ->
            if (event.target != interaction) return@listen

            if (isComplete) {
                event.player.sendMessage(text("This puzzle is complete.", NamedTextColor.RED))

                return@listen
            }
            val random = arrayListOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
            val code = Array(4) { -1 }
            repeat(4) { i ->
                code[i] = Random.nextInt(random.size).let { num ->
                    val final = random[num]
                    random.remove(final)
                    final
                }
            }
            val clicked = Array(4) { -1 }
            val gui = simpleGui(InventoryType.CRAFTER_3X3, text("Number Safe", NamedTextColor.GRAY)) {
                repeat(9) { i ->
                    set(
                        i,
                        item(if (!random.contains(i)) Material.GRAY_STAINED_GLASS_PANE else Material.BLACK_STAINED_GLASS_PANE) {
                            itemName = text("${i + 1}")
                            amount = i + 1
                        }) { clickEvent ->
                        val index = clicked.indexOf(-1)
                        if (index >= 0) {
                            clicked[index] = i

                            clickEvent.player.playSound(
                                Sound.sound()
                                    .type(SoundEvent.ENTITY_ARROW_HIT_PLAYER)
                                    .volume(1.2f)
                                    .pitch(1.5f)
                                    .build()
                            )
                        }
                        if (index >= 3 || index == -1) {
                            if (clicked.contentEquals(code)) {

                                clickEvent.player.playSound(
                                    Sound.sound()
                                        .type(SoundEvent.ENTITY_ARROW_HIT_PLAYER)
                                        .volume(0.8f)
                                        .pitch(1.5f)
                                        .build()
                                )
                                clickEvent.player.closeInventory()

                                isComplete = true
                                GlobalEventHandler.call(PlayerCollectColorEvent(game, event.player, PaletteColor.GREY))
                                return@set
                            }
                            clickEvent.player.playSound(
                                Sound.sound()
                                    .type(SoundEvent.ENTITY_VILLAGER_NO)
                                    .volume(0.7f)
                                    .build()
                            )
                            clicked[0] = -1
                            clicked[1] = -1
                            clicked[2] = -1
                            clicked[3] = -1
                            return@set
                        }

                    }
                }
            }
            event.player.openInventory(gui)
        }

        val display = Entity(EntityType.BLOCK_DISPLAY)
        display.editMeta<BlockDisplayMeta> {
            setBlockState(Block.COMMAND_BLOCK)
            isHasGlowingEffect = true
            isHasNoGravity = true
            this
        }
        display.setInstance(game.instance, pos)

    }
}