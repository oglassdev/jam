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
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Pos
import net.minestom.server.event.EventNode
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryOpenEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.Material
import team.ktusers.jam.dsl.simpleGui
import team.ktusers.jam.event.PlayerCollectColorEvent
import team.ktusers.jam.game.JamGame
import team.ktusers.jam.generated.PaletteColor
import team.ktusers.jam.util.FakeBlock
import team.ktusers.jam.util.SimpleGui

@Serializable
@SerialName("buttons")
data class Buttons(val blocks: List<Button>, val color: PaletteColor) : Puzzle {
    override fun onElementStart(game: JamGame, eventNode: EventNode<InstanceEvent>) {
        val blocks = blocks.map {
            FakeBlock(it.color).also { block ->
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
                item(Material.RED_WOOL) {
                    amount = 1
                    itemName = text("Waiting for players...", NamedTextColor.RED)

                    lore {
                        +text("Other players must be present to click the button!")
                    }
                }
            )
            eventNode().listen<InventoryOpenEvent> { event ->
                val inv = event.inventory as? SimpleGui? ?: return@listen

                with(game) {
                    viewers.add(event.player.currentColor)
                }
                if (viewers.size == this@Buttons.blocks.size) inv.set(
                    at(4, 1),
                    item(Material.GREEN_WOOL) {
                        amount = 1
                        itemName = text("Click!", NamedTextColor.GREEN)
                    }
                ) { clickEvent ->
                    with(game) {
                        clickers.add(clickEvent.player.currentColor)
                    }
                    if (clickers.size >= this@Buttons.blocks.size) {
                        clickEvent.inventory?.viewers?.forEach {
                            it.closeInventory()
                        }
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
                    at(4, 1),
                    item(Material.RED_WOOL) {
                        amount = 1
                        itemName = text("Waiting for players...", NamedTextColor.RED)

                        lore {
                            +text("Other players must be present to click the button!")
                        }
                    }
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