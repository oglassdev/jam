package team.ktusers.jam

import net.bladehunt.blade.dsl.instance.buildInstance
import net.bladehunt.kotstom.dsl.listen
import net.bladehunt.kotstom.dsl.scheduleTask
import net.bladehunt.kotstom.extension.adventure.plus
import net.bladehunt.kotstom.extension.adventure.text
import net.kyori.adventure.text.Component.empty
import net.kyori.adventure.text.Component.newline
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Player
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent
import net.minestom.server.event.player.*
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.timer.TaskSchedule
import team.ktusers.jam.util.PlayerNpc

private val actionbarMessage = text("Click the NPC to queue", TextDecoration.BOLD, color = GOLD)

val Lobby = buildInstance {
    enableLighting()

    polar { fromResource("/lobby.polar") }

    scheduler.scheduleTask(repeat = TaskSchedule.seconds(1)) {
        instance.sendActionBar(actionbarMessage)
    }

    val npc = PlayerNpc("oglass_queue_npc", Skin.KING)
    npc.setInstance(instance, Config.lobby.npcPos).join()
    npc.addPassenger(PlayerNpc.Name(text("Queue", TextDecoration.BOLD, color = GOLD)))

    eventNode.listen<PlayerSpawnEvent> { event ->
        event.player.teleport(Config.lobby.spawnPos)

        event.player.sendPlayerListHeaderAndFooter(
            newline() + text("  Team:", BLUE) + text(" ktusers", GOLD) + newline(),
            newline() + text("  Minestom Game Jam  ", LIGHT_PURPLE) + newline(),
        )

        instance.sendActionBar(actionbarMessage)
    }

    eventNode.listen<RemoveEntityFromInstanceEvent> { event ->
        val player = event.entity as? Player ?: return@listen

        player.sendPlayerListHeaderAndFooter(empty(), empty())
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

    eventNode.listen<PlayerUseItemEvent>(::cancelEvent)
    eventNode.listen<PlayerBlockBreakEvent>(::cancelEvent)
    eventNode.listen<PlayerBlockPlaceEvent>(::cancelEvent)
    eventNode.listen<PlayerBlockInteractEvent>(::cancelEvent)
}
