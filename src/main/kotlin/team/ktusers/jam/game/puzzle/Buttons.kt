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
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.coordinate.Pos
import net.minestom.server.event.EventNode
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryOpenEvent
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
import team.ktusers.jam.util.SimpleGui

@Serializable
@SerialName("buttons")
data class Buttons(val blocks: List<Button>, val color: PaletteColor) : Puzzle {
    private val waitingItem = item(Material.RED_WOOL) {
        amount = 1
        itemName = text("Waiting for players...", NamedTextColor.RED)

        lore {
            +text("Other players must be present on", TextDecoration.ITALIC to false, color = NamedTextColor.GRAY)
            +text("other colors to click the button!", TextDecoration.ITALIC to false, color = NamedTextColor.GRAY)
        }
    }

    private val clickItem = item(Material.GREEN_WOOL) {
        amount = 1
        itemName = text("Click!", NamedTextColor.GREEN)
    }

    override fun onElementStart(game: JamGame, eventNode: EventNode<InstanceEvent>) {
        val blocks = blocks.map {
            FakeBlock(Block.CHAIN_COMMAND_BLOCK, it.color).also { block ->
                block.setGame(game, it.pos)
            }
        }
        val interactions = blocks.map { it.interaction }

        var isComplete = false

        val viewers = mutableSetOf<PaletteColor>()
        val clickers = mutableSetOf<PaletteColor>()
        val inventory = simpleGui(InventoryType.CHEST_3_ROW, text("Linked")) {
            set(
                at(4, 1),
                waitingItem
            )
            eventNode().listen<InventoryOpenEvent> { event ->
                val inv = event.inventory as? SimpleGui? ?: return@listen

                if (isComplete) {
                    event.player.sendMessage(text("This has already been completed.", NamedTextColor.RED))
                    event.isCancelled = true
                }

                with(game) {
                    viewers.add(event.player.currentColor)
                }
                if (viewers.size == this@Buttons.blocks.size) inv.set(at(4, 1), clickItem) { clickEvent ->
                    with(game) {
                        if (!clickers.contains(clickEvent.player.currentColor)) clickEvent.player.playSound(
                            Sound.sound()
                                .type(SoundEvent.ENTITY_ARROW_HIT_PLAYER)
                                .volume(0.8f)
                                .pitch(1.5f)
                                .build()
                        )
                        
                        clickers.add(clickEvent.player.currentColor)
                    }
                    if (clickers.size >= this@Buttons.blocks.size) {
                        clickEvent.inventory?.viewers?.forEach {
                            it.closeInventory()
                        }
                        isComplete = true
                        GlobalEventHandler.call(PlayerCollectColorEvent(game, clickEvent.player, color))
                    }
                }
            }
            eventNode().listen<InventoryCloseEvent> { event ->
                val inv = event.inventory as? SimpleGui? ?: return@listen

                with(game) {
                    viewers.remove(event.player.currentColor)
                }
                if (viewers.size < this@Buttons.blocks.size) inv.set(
                    at(4, 1), waitingItem
                )
            }
        }

        eventNode.listen<PlayerEntityInteractEvent> { event ->
            if (!interactions.contains(event.target)) return@listen

            event.player.openInventory(inventory)
        }
    }

    @Serializable
    data class Button(val pos: @Contextual Pos, val color: PaletteColor)
}