package team.ktusers.jam

import net.bladehunt.blade.dsl.instance.InstanceBuilder
import net.bladehunt.kotstom.InstanceManager
import net.bladehunt.kotstom.dsl.item.item
import net.bladehunt.kotstom.dsl.item.itemName
import net.bladehunt.kotstom.dsl.listen
import net.bladehunt.kotstom.extension.adventure.plus
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.kotstom.extension.editMeta
import net.bladehunt.minigamelib.GameManager
import net.kyori.adventure.text.Component.newline
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.item.Material
import team.ktusers.jam.game.JamGame
import team.ktusers.jam.gui.queueGui

private val actionbarMessage = text("Welcome to ", color = GOLD) + text("cured", TextDecoration.BOLD, color = DARK_GRAY)

private val queueItem = item(Material.NETHER_STAR) {
    itemName = text("Play ", GRAY) + text("cured", TextDecoration.BOLD, color = DARK_GRAY)
}

val Lobby = InstanceBuilder(InstanceManager.createInstanceContainer(JamGame.DIMENSION)).apply {
    polar { fromResource("/lobby.polar") }

    eventNode.listen<PlayerSpawnEvent> { event ->
        event.player.teleport(Config.lobby.spawnPos)
        event.player.inventory.clear()
        event.player.heal()

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

    val portalInfo = Entity(EntityType.TEXT_DISPLAY)
    portalInfo.editMeta<TextDisplayMeta> {
        text = text("Jump in the portal to queue!", LIGHT_PURPLE)
        billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
        isHasNoGravity = true
        this
    }
    portalInfo.setInstance(instance, Vec(0.5, -20.0, -35.5))

    val info = Entity(EntityType.TEXT_DISPLAY)
    info.editMeta<TextDisplayMeta> {
        text = text("Fabulous quality is recommended", LIGHT_PURPLE) + newline() +
                text("for the best experience.")
        billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
        isHasNoGravity = true
        this
    }
    info.setInstance(instance, Vec(0.5, -18.0, -10.5))


    eventNode.listen<PlayerTickEvent> { event ->
        val player = event.player
        val position = player.position
        when {
            position.y < -64 -> player.teleport(Config.lobby.spawnPos)
            position.x in -3.0..4.0 && position.z in -39.0..-31.0 && position.y in -26.0..-23.0 -> {
                val game = GameManager.getOrCreateFirstJoinableGame(gameProvider = ::JamGame)

                if (!game.players.contains(player)) game.addPlayer(player)
                else player.sendMessage(text("You are already in this game!", RED))
            }
        }
    }

    fun <T : CancellableEvent> cancelEvent(event: T) {
        event.isCancelled = true
    }

    eventNode.listen<ItemDropEvent>(::cancelEvent)
    eventNode.listen<PlayerSwapItemEvent>(::cancelEvent)
    eventNode.listen<PlayerBlockBreakEvent>(::cancelEvent)
    eventNode.listen<PlayerBlockPlaceEvent>(::cancelEvent)
    eventNode.listen<PlayerBlockInteractEvent>(::cancelEvent)
}.instance
