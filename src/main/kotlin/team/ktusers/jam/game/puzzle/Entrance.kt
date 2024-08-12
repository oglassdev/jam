package team.ktusers.jam.game.puzzle

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.bladehunt.kotstom.dsl.listen
import net.bladehunt.kotstom.dsl.scheduleTask
import net.bladehunt.kotstom.extension.adventure.plus
import net.bladehunt.kotstom.extension.adventure.text
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.timer.TaskSchedule
import team.ktusers.jam.game.JamGame
import team.ktusers.jam.util.PlayerNpc

@Serializable
@SerialName("entrance")
data class Entrance(val pos: @Contextual Pos) : Puzzle {
    private val prefix = text("Wanderer: ", NamedTextColor.GRAY)

    override fun onElementStart(game: JamGame, eventNode: EventNode<InstanceEvent>) {
        val npc = PlayerNpc("_npc_entr")

        npc.setInstance(game.instance, pos).thenAccept {
            npc.addPassenger(PlayerNpc.Name(text("Wanderer")))
        }
        var hasInteracted = false
        var isComplete = false

        fun onInteract(player: Player) {
            if (hasInteracted) {
                if (isComplete) player.sendMessage(text("Keep going! I believe in you.", NamedTextColor.RED))
                return
            }
            hasInteracted = true
            npc.scheduler().scheduleTask(delay = TaskSchedule.seconds(0), repeat = TaskSchedule.stop()) {
                game.sendMessage(prefix + "I've been stuck here for ages!")
            }
            npc.scheduler().scheduleTask(delay = TaskSchedule.seconds(3), repeat = TaskSchedule.stop()) {
                game.sendMessage(prefix + "I might know some information that could help you...")
            }
            npc.scheduler().scheduleTask(delay = TaskSchedule.seconds(6), repeat = TaskSchedule.stop()) {
                game.sendMessage(prefix + "Your glasses can view different colors. Head over to the color selector and try it out.")
            }
            npc.scheduler().scheduleTask(delay = TaskSchedule.seconds(9), repeat = TaskSchedule.stop()) {
                game.sendMessage(prefix + "You can only see the colors with your glasses until you collect them by doing their puzzles.")
                isComplete = true
            }
        }

        eventNode.listen<EntityAttackEvent> { event ->
            val player = event.entity as? Player ?: return@listen
            if (event.entity != npc) return@listen

            onInteract(player)
        }
        eventNode.listen<PlayerEntityInteractEvent> { event ->
            if (event.target != npc) return@listen
            if (event.hand != Player.Hand.MAIN) return@listen

            onInteract(event.player)
        }
    }
}