package team.ktusers.jam.game.puzzle

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.bladehunt.kotstom.GlobalEventHandler
import net.bladehunt.kotstom.dsl.item.amount
import net.bladehunt.kotstom.dsl.item.item
import net.bladehunt.kotstom.dsl.item.itemName
import net.bladehunt.kotstom.dsl.item.lore
import net.bladehunt.kotstom.dsl.listen
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.kotstom.extension.editMeta
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.entity.metadata.display.BlockDisplayMeta
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
import team.ktusers.jam.util.FakeBlock
import kotlin.random.Random

@Serializable
@SerialName("safe")
data class Safe(
    val pos: @Contextual BlockVec,
    val color: PaletteColor
) : Puzzle {
    override fun onElementStart(game: JamGame, eventNode: EventNode<InstanceEvent>) {
        val block = FakeBlock(Block.COMMAND_BLOCK, color)
        block.setGame(game, pos)

        var isComplete = false

        eventNode.listen<PlayerEntityInteractEvent> { event ->
            if (event.target != block.interaction) return@listen

            if (isComplete) {
                event.player.sendMessage(text("This puzzle is complete.", NamedTextColor.RED))

                return@listen
            }
            val random = arrayListOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
            val code = Array(3) { -1 }
            repeat(3) { i ->
                code[i] = Random.nextInt(random.size).let { num ->
                    val final = random[num]
                    random.remove(final)
                    final
                }
            }
            val clicked = Array(3) { -1 }
            val gui = simpleGui(InventoryType.CRAFTING, text("Combination Safe", NamedTextColor.DARK_GRAY)) {
                repeat(9) { i ->
                    set(
                        i + 1,
                        item(if (!random.contains(i)) Material.LIGHT_GRAY_STAINED_GLASS_PANE else Material.BLACK_STAINED_GLASS_PANE) {
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
                        if (index >= 2 || index == -1) {
                            if (clicked.contentEquals(code)) {

                                clickEvent.player.playSound(
                                    Sound.sound()
                                        .type(SoundEvent.ENTITY_ARROW_HIT_PLAYER)
                                        .volume(0.8f)
                                        .pitch(1.5f)
                                        .build()
                                )
                                clickEvent.player.closeInventory()

                                block.editMeta<BlockDisplayMeta> {
                                    this.isHasGlowingEffect = false
                                    this
                                }

                                isComplete = true
                                GlobalEventHandler.call(PlayerCollectColorEvent(game, event.player, color))
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
                            return@set
                        }

                    }
                }
                set(
                    0, item(
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
                    ) {
                        itemName = text(color.name.lowercase().capitalize(), color.textColor)

                        lore {
                            text(
                                "Input the 4 correct numbers",
                                TextDecoration.ITALIC to false,
                                color = NamedTextColor.GRAY
                            )
                            text(
                                "to receive the color!", TextDecoration.ITALIC to false,
                                color = NamedTextColor.GRAY
                            )
                        }
                    }
                )
            }
            event.player.openInventory(gui)
        }

    }
}