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
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.text.Component.newline
import net.kyori.adventure.text.format.NamedTextColor
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
private val handbookItem = item(Material.BOOK) {
    itemName = text("Handbook", GRAY)
}

private val handbook = Book.builder()
    .title(text("Handbook"))
    .author(text("oglass, omega, ashyy, and sadness"))
    .pages(
        text(
            """
                cured.
                
                Cured is a team based game that requires a minimum of 3 players.
                Players must work together to get all 7 relics within 9 minutes.
                
                Next section: Puzzle - Safe
            """.trimIndent()
        ),
        text(
            """
                Puzzle - Safe (red & green)
                
                Reference: Glowing Command Blocks
                Players must guess a 3 number combination to receive a relic. The possible (unique) numbers are colored in gray.

                Next section: Puzzle - Fragments
            """.trimIndent()
        ),
        text(
            """
                Puzzle - Fragments (orange & blue)
                
                Reference: Glowing Items (Right click)
                Players must search around the map for fragments of relics. Upon retrieval of all three, the relic will be acquired.
                
                Next section: Puzzle - Buttons
            """.trimIndent()
        ),
        text(
            """
                Puzzle - Safe (yellow & indigo)
                
                Reference: Glowing Chain Command Blocks
                Three players must work together to press a button in an inventory while having different colors selected.
                
                Next section: Puzzle - Lasers
            """.trimIndent()
        ),
        text(
            """
                Puzzle - Lasers (violet)
                
                Reference: Glowing item objective
                Players must traverse a laser filled house to retrieve the relic at the end.
                
                Next section: Gameplay - Relics
            """.trimIndent()
        ),
        text(
            """
                Gameplay - Relics
                
                After finding all the relics, they must be placed in the center to shoot lasers at the dark matter.
            """.trimIndent()
        )
    )
    .build()

val Lobby = InstanceBuilder(InstanceManager.createInstanceContainer(JamGame.DIMENSION)).apply {
    polar { fromResource("/lobby.polar") }

    eventNode.listen<PlayerSpawnEvent> { event ->
        event.player.teleport(Config.lobby.spawnPos)
        event.player.inventory.clear()
        event.player.heal()

        val inventory = event.player.inventory
        inventory.setItemStack(8, queueItem)
        inventory.setItemStack(0, handbookItem)
        event.player.sendMessage(
            text(
                "Welcome to cured! Please read the handbook to get an idea on how to play.",
                GRAY
            )
        )

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

            handbookItem -> {
                it.player.openBook(
                    handbook
                )
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
