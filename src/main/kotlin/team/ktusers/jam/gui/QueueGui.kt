package team.ktusers.jam.gui

import net.bladehunt.kotstom.dsl.item.item
import net.bladehunt.kotstom.dsl.item.itemName
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.minigamelib.GameManager
import net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.Material
import team.ktusers.jam.dsl.simpleGui
import team.ktusers.jam.game.JamGame

fun queueGui() =
    simpleGui(InventoryType.CHEST_3_ROW, text("Queue", DARK_GRAY)) {
        set(at(4, 1), item(Material.BRAIN_CORAL) { itemName = text("Queue") }) { event ->
            val player = event.player
            player.closeInventory()
            val game = GameManager.getOrCreateFirstJoinableGame(gameProvider = ::JamGame)

            if (game.players.contains(player)) {
                player.sendMessage(text("You are already in this game!", RED))
                return@set
            }

            game.addPlayer(player)
        }
    }
