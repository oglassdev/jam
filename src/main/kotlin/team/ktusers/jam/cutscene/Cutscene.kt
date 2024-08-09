package team.ktusers.jam.cutscene

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.bladehunt.kotstom.extension.await
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.minestom.server.Viewable
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.instance.Instance
import net.minestom.server.network.packet.server.play.ChangeGameStatePacket
import net.minestom.server.timer.TaskSchedule
import team.ktusers.jam.util.PlayerNpc
import java.util.*

class Cutscene(instance: Instance, val createPlayerReplacements: Boolean, positions: List<CutscenePosition>, texts: List<CutsceneText>) :
    Viewable {
    val camera: Camera = Camera()

    val positions = positions.iterator()
    val texts = texts.iterator()

    init {
        if (positions.isNotEmpty()) {
            camera.setInstance(instance, this.positions.next().pos).join()
        }
    }

    suspend fun start() = coroutineScope {
        launch {
            positions.forEach { pos ->
                (camera.entityMeta as TextDisplayMeta).posRotInterpolationDuration = pos.duration
                camera.teleport(pos.pos).await()
                camera.scheduler().await(TaskSchedule.tick(pos.duration * 2))
            }
        }

        launch {
            viewers.forEach { it.playSound(Sound.sound(
                Key.key("jam:start_cutscene"),
                Sound.Source.VOICE,
                1f, 1f
            ), camera) }
            texts.forEach { text ->
                viewers.forEach { player ->
                    text.show(player)
                }
            }
        }
    }

    private val viewers = arrayListOf<Player>()

    private val originalGameModes = hashMapOf<UUID, GameMode>()

    private val npcs = hashMapOf<UUID, PlayerNpc>()

    override fun addViewer(player: Player): Boolean = viewers.add(player).also {
        if (!it) return@also
        val uuid = player.uuid
        originalGameModes[uuid] = player.gameMode
        player.sendPacket(
            ChangeGameStatePacket(
                ChangeGameStatePacket.Reason.CHANGE_GAMEMODE,
                GameMode.SPECTATOR.id().toFloat()
            )
        )
        if (createPlayerReplacements) {
            val npc = PlayerNpc(player.username + "_", player.skin)
            npc.updateViewableRule { entity -> entity.uuid == uuid }
            npc.setInstance(player.instance, player.position)
            npcs[uuid] = npc
        }
        player.spectate(camera)
    }

    override fun removeViewer(player: Player): Boolean = viewers.remove(player).also {
        if (!it) return@also
        val uuid = player.uuid
        player.stopSpectating()
        player.sendPacket(
            ChangeGameStatePacket(
                ChangeGameStatePacket.Reason.CHANGE_GAMEMODE,
                originalGameModes[uuid]?.id()?.toFloat() ?: GameMode.SURVIVAL.id().toFloat()
            )
        )

        npcs.remove(uuid)?.remove()
        originalGameModes.remove(uuid)
    }

    override fun getViewers(): Set<Player> = viewers.toSet()
}