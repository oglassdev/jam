package team.ktusers.jam.gui

import net.bladehunt.kotstom.dsl.item.customName
import net.bladehunt.kotstom.dsl.item.item
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.minigamelib.ext.game
import net.kyori.adventure.text.format.NamedTextColor.GOLD
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.Material
import team.ktusers.jam.dsl.simpleGui

val QueueGui =
    simpleGui(InventoryType.CHEST_3_ROW, text("Queue", GOLD)) {
        set(at(4, 1), item(Material.BRAIN_CORAL) { customName = text("Queue") }) { event ->
            event.player.game
        }
    }
