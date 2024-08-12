package team.ktusers.jam.gui

import net.bladehunt.kotstom.dsl.item.item
import net.bladehunt.kotstom.dsl.item.itemName
import net.bladehunt.kotstom.dsl.item.lore
import net.bladehunt.kotstom.extension.adventure.plus
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.minigamelib.ext.game
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import team.ktusers.jam.dsl.simpleGui
import team.ktusers.jam.event.PlayerChangeColorEvent
import team.ktusers.jam.game.JamGame
import team.ktusers.jam.generated.PaletteColor

private fun getItem(name: Component, material: Material, hasCollected: Boolean): ItemStack =
    if (hasCollected) item(material) {
        itemName = name + text(" (Collected)")
    } else item(material) {
        itemName = name
        lore {
            +text("You haven't collected this!", TextDecoration.ITALIC to false, color = RED)
            +text("ᴄᴏᴍᴘʟᴇᴛᴇ ᴀ ᴘᴜᴢᴢʟᴇ ꜰᴏʀ ᴛʜɪꜱ ᴄᴏʟᴏʀ", TextDecoration.ITALIC to false, color = GRAY)
        }
    }

fun handler(color: PaletteColor): (InventoryPreClickEvent) -> Unit = l@{ event: InventoryPreClickEvent ->
    val currentGame = event.player.game as? JamGame ?: return@l

    with(currentGame) {
        MinecraftServer.getGlobalEventHandler()
            .call(PlayerChangeColorEvent(currentGame, event.player, event.player.currentColor, color))
    }
    event.player.closeInventory()
}

fun colorSelector(game: JamGame) =
    simpleGui(InventoryType.CHEST_4_ROW, text("Color Selector", DARK_GRAY)) {
        val inv = game.teamInventory
        set(
            at(2, 1),
            getItem(text("Red", PaletteColor.RED.textColor), Material.RED_WOOL, inv.isCollected(PaletteColor.RED)),
            handler(PaletteColor.RED)
        )
        set(
            at(3, 1),
            getItem(
                text("Orange", PaletteColor.ORANGE.textColor),
                Material.ORANGE_WOOL,
                inv.isCollected(PaletteColor.ORANGE)
            ),
            handler(PaletteColor.ORANGE)
        )
        set(
            at(4, 1),
            getItem(
                text("Yellow", PaletteColor.YELLOW.textColor),
                Material.YELLOW_WOOL,
                inv.isCollected(PaletteColor.YELLOW)
            ),
            handler(PaletteColor.YELLOW)
        )
        set(
            at(5, 1),
            getItem(
                text("Green", PaletteColor.GREEN.textColor),
                Material.GREEN_WOOL,
                inv.isCollected(PaletteColor.GREEN)
            ),
            handler(PaletteColor.GREEN)
        )
        set(
            at(6, 1),
            getItem(text("Blue", PaletteColor.BLUE.textColor), Material.BLUE_WOOL, inv.isCollected(PaletteColor.BLUE)),
            handler(PaletteColor.BLUE)
        )

        set(
            at(3, 2),
            getItem(
                text("Indigo", PaletteColor.INDIGO.textColor),
                Material.PURPLE_WOOL,
                inv.isCollected(PaletteColor.INDIGO)
            ),
            handler(PaletteColor.INDIGO)
        )
        set(
            at(4, 2),
            getItem(
                text("Violet", PaletteColor.VIOLET.textColor),
                Material.MAGENTA_WOOL,
                inv.isCollected(PaletteColor.VIOLET)
            ),
            handler(PaletteColor.VIOLET)
        )
        set(
            at(5, 2),
            getItem(text("Grey", PaletteColor.GREY.textColor), Material.GRAY_WOOL, inv.isCollected(PaletteColor.GREY)),
            handler(PaletteColor.GREY)
        )
    }
