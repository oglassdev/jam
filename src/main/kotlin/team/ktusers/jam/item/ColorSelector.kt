package team.ktusers.jam.item

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.bladehunt.kotstom.dsl.item.item
import net.bladehunt.kotstom.dsl.item.itemName
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.minigamelib.ext.game
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import team.ktusers.jam.AdventureNbt
import team.ktusers.jam.game.JamGame
import team.ktusers.jam.game.getPaletteColorFromString
import team.ktusers.jam.gui.colorSelector
import java.util.*

@Serializable
@SerialName("color_selector")
data class ColorSelector(
    val selectedColor: String = "RED",
    val uuid: @Contextual UUID = UUID.randomUUID()
) : JamItem {
    override fun createItemStack(): ItemStack = item(Material.BREEZE_ROD) {
        val thisColor = getPaletteColorFromString("RED")
        itemName = text("Color: $selectedColor", TextColor.color(thisColor.red, thisColor.green, thisColor.blue))

        this[ItemTag] = AdventureNbt.encodeToCompound<JamItem>(this@ColorSelector)
    }

    override fun onUse(event: PlayerUseItemEvent) {
        val game = event.player.game as? JamGame ?: return

        event.player.openInventory(colorSelector(game))
    }

    override fun onBlockInteract(event: PlayerBlockInteractEvent) {
        event.isCancelled = true
    }
}