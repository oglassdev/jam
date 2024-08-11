package team.ktusers.jam.gui

import net.bladehunt.kotstom.dsl.item.item
import net.bladehunt.kotstom.dsl.item.itemName
import net.bladehunt.kotstom.dsl.item.lore
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.minigamelib.ext.game
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import team.ktusers.jam.dsl.simpleGui
import team.ktusers.jam.game.JamGame
import team.ktusers.jam.item.ColorSelector
import team.ktusers.jam.item.getCustomItemData

private fun getItem(name: Component, material: Material, hasCollected: Boolean): ItemStack =
    if (hasCollected) item(material) {
        itemName = name
    } else item(Material.BLACK_WOOL) {
        itemName = name.color(DARK_GRAY)
        lore {
            +text("You haven't collected this", TextDecoration.ITALIC to false)
        }
    }

fun handler(color: String): (InventoryPreClickEvent) -> Unit = l@{ event: InventoryPreClickEvent ->
    val currentGame = event.player.game as? JamGame ?: return@l

    if (!currentGame.teamInventory.collectedColors.contains(color)) return@l

    val updates = hashMapOf<Int, ItemStack>()
    event.player.inventory.itemStacks.forEachIndexed { slot, itemStack ->
        val data = itemStack.getCustomItemData() as? ColorSelector? ?: return@forEachIndexed

        updates[slot] = data.copy(selectedColor = color).createItemStack()
    }

    updates.forEach { (slot, itemStack) ->
        event.player.inventory.setItemStack(slot, itemStack)
    }

    with(currentGame) {
        currentGame.updateColor(event.player, event.player.currentColor, color)
    }
    event.player.closeInventory()
}

fun colorSelector(game: JamGame) =
    simpleGui(InventoryType.CHEST_4_ROW, text("Color Selector", DARK_GRAY)) {
        val colors = game.teamInventory.collectedColors
        set(
            at(2, 1),
            getItem(text("Red", RED), Material.RED_WOOL, colors.contains("RED")),
            handler("RED")
        )
        set(
            at(3, 1),
            getItem(text("Orange", GOLD), Material.RED_WOOL, colors.contains("ORANGE")),
            handler("ORANGE")
        )
        set(
            at(4, 1),
            getItem(text("Yellow", YELLOW), Material.RED_WOOL, colors.contains("ORANGE")),
            handler("ORANGE")
        )
        set(
            at(5, 1),
            getItem(text("Green", GREEN), Material.RED_WOOL, colors.contains("GREEN")),
            handler("GREEN")
        )
        set(
            at(6, 1),
            getItem(text("Blue", BLUE), Material.RED_WOOL, colors.contains("BLUE")),
            handler("BLUE")
        )

        set(
            at(3, 2),
            getItem(text("Indigo", DARK_PURPLE), Material.PURPLE_WOOL, colors.contains("INDIGO")),
            handler("INDIGO")
        )
        set(
            at(4, 2),
            getItem(text("Violet", LIGHT_PURPLE), Material.MAGENTA_WOOL, colors.contains("VIOLET")),
            handler("VIOLET")
        )
        set(
            at(5, 2),
            getItem(text("Grey", GRAY), Material.GRAY_WOOL, colors.contains("GREY")),
            handler("GREY")
        )
    }
