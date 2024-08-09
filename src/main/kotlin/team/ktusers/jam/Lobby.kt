package team.ktusers.jam

import net.bladehunt.kotstom.InstanceManager
import net.bladehunt.kotstom.dsl.listen
import net.bladehunt.kotstom.dsl.scheduleTask
import net.bladehunt.kotstom.extension.adventure.plus
import net.bladehunt.kotstom.extension.adventure.text
import net.hollowcube.polar.PolarLoader
import net.kyori.adventure.text.Component.empty
import net.kyori.adventure.text.Component.newline
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Player
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent
import net.minestom.server.event.player.*
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.instance.LightingChunk
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.chunk.ChunkSupplier
import team.ktusers.jam.util.PlayerNpc

private val actionbarMessage = text("Click the NPC to queue", TextDecoration.BOLD, color = GOLD)

val Lobby =
    InstanceManager.createInstanceContainer().apply {
        chunkSupplier = ChunkSupplier(::LightingChunk)

        this::class.java.getResourceAsStream("/lobby.polar")?.let { stream ->
            chunkLoader = PolarLoader(stream)
        }

        scheduler().scheduleTask(repeat = TaskSchedule.seconds(1)) {
            sendActionBar(actionbarMessage)
        }
        val npc = PlayerNpc("oglass_queue_npc", Skin.KING)
        npc.setInstance(this, Config.lobby.npcPos).join()
        npc.addPassenger(PlayerNpc.Name(text("Queue", TextDecoration.BOLD, color = GOLD)))

        eventNode().apply {
            listen<PlayerSpawnEvent> { event ->
                if (!event.isFirstSpawn) return@listen

                event.player.teleport(Config.lobby.spawnPos)

                event.player.sendPlayerListHeaderAndFooter(
                    newline() + text("  Team:", BLUE) + text(" ktusers", GOLD) + newline(),
                    newline() + text("  Minestom Game Jam  ", LIGHT_PURPLE) + newline(),
                )

                sendActionBar(actionbarMessage)
            }
            listen<RemoveEntityFromInstanceEvent> { event ->
                val player = event.entity as? Player ?: return@listen

                player.sendPlayerListHeaderAndFooter(empty(), empty())
            }

            listen<EntityDamageEvent> { event ->
                if (event.entity !is Player) return@listen

                event.isCancelled = true
            }

            listen<EntityDamageEvent> { event ->
                if (event.entity !is Player) return@listen

                event.isCancelled = true
            }

            listen<PlayerTickEvent> { event ->
                if (event.player.position.y > -64) return@listen

                event.player.teleport(Config.lobby.spawnPos)
            }

            fun <T : CancellableEvent> cancelEvent(event: T) {
                event.isCancelled = true
            }

            listen<PlayerUseItemEvent>(::cancelEvent)
            listen<PlayerBlockBreakEvent>(::cancelEvent)
            listen<PlayerBlockPlaceEvent>(::cancelEvent)
            listen<PlayerBlockInteractEvent>(::cancelEvent)
        }
    }
