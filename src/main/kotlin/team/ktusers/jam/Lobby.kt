package team.ktusers.jam

import net.bladehunt.blade.dsl.instance.buildInstance
import net.bladehunt.kotstom.dsl.item.item
import net.bladehunt.kotstom.dsl.item.itemName
import net.bladehunt.kotstom.dsl.listen
import net.bladehunt.kotstom.extension.adventure.plus
import net.bladehunt.kotstom.extension.adventure.text
import net.kyori.adventure.text.Component.newline
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Player
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.item.Material
import team.ktusers.jam.gui.queueGui
import team.ktusers.jam.util.PlayerNpc

private val actionbarMessage = text("Welcome to ", color = GOLD) + text("cured", TextDecoration.BOLD, color = DARK_GRAY)

private val queueItem = item(Material.NETHER_STAR) {
    itemName = text("Play ", GRAY) + text("cured", TextDecoration.BOLD, color = DARK_GRAY)
}

val Lobby = buildInstance {
    enableLighting()

    polar { fromResource("/lobby.polar") }

    val npc = PlayerNpc("oglass_queue_npc", Skin.KING)
    npc.setInstance(instance, Config.lobby.npcPos).join()
    npc.addPassenger(PlayerNpc.Name(text("Queue", TextDecoration.BOLD, color = GOLD)))

    eventNode.listen<PlayerSpawnEvent> { event ->
        event.player.teleport(Config.lobby.spawnPos)

        val inventory = event.player.inventory
        inventory.setItemStack(4, queueItem)

        event.player.sendPlayerListHeaderAndFooter(
            text("┌                                     ┐ ") +
                    newline() + text("ᴍɪɴᴇꜱᴛᴏᴍ ɢᴀᴍᴇ ᴊᴀᴍ", LIGHT_PURPLE) + newline(),
            newline() + text("cured", TextDecoration.BOLD, color = DARK_GRAY) + newline() +
                    text("└                                     ┘ ")
        )

        instance.sendActionBar(actionbarMessage)
    }

    eventNode.listen<PlayerUseItemEvent> {
        when (it.itemStack) {
            queueItem -> {
                it.player.openInventory(queueGui())
            }

            else -> {}
        }

        it.isCancelled = true
    }

    eventNode.listen<RemoveEntityFromInstanceEvent> { event ->
        val player = event.entity as? Player ?: return@listen

        player.inventory.clear()
    }

    eventNode.listen<EntityDamageEvent> { event ->
        if (event.entity !is Player) return@listen

        event.isCancelled = true
    }

    eventNode.listen<EntityDamageEvent> { event ->
        if (event.entity !is Player) return@listen

        event.isCancelled = true
    }

    eventNode.listen<PlayerTickEvent> { event ->
        if (event.player.position.y > -64) return@listen

        event.player.teleport(Config.lobby.spawnPos)
    }

    fun <T : CancellableEvent> cancelEvent(event: T) {
        event.isCancelled = true
    }

    eventNode.listen<ItemDropEvent>(::cancelEvent)
    eventNode.listen<PlayerSwapItemEvent>(::cancelEvent)
    eventNode.listen<PlayerBlockBreakEvent>(::cancelEvent)
    eventNode.listen<PlayerBlockPlaceEvent>(::cancelEvent)
    eventNode.listen<PlayerBlockInteractEvent>(::cancelEvent)
}
