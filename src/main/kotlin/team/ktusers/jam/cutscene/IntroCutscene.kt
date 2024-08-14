package team.ktusers.jam.cutscene

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import net.bladehunt.blade.dsl.instance.InstanceBuilder
import net.bladehunt.blade.ext.loadChunks
import net.bladehunt.kotstom.InstanceManager
import net.bladehunt.kotstom.dsl.builder
import net.bladehunt.kotstom.extension.await
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.instance.Instance
import net.minestom.server.timer.TaskSchedule
import team.ktusers.jam.Lobby
import java.time.Duration

val CUTSCENE_REFERENCE = InstanceBuilder(InstanceManager.createInstanceContainer()).apply {
    polar {
        fromResource("/cutscene.polar")
    }

    instance.loadChunks(-9, -8, 10).join()
    instance.loadChunks(12, -19, 16).join()
}.instance

fun cutsceneInstance(): Instance = CUTSCENE_REFERENCE.apply {
    InstanceManager.registerInstance(this)

    eventNode().builder<PlayerSpawnEvent> {
        asyncHandler(Dispatchers.Default) { event ->
            event.player.await(TaskSchedule.nextTick())
            intro(event.player, event.instance)
            event.player.setInstance(Lobby).await()

            InstanceManager.unregisterInstance(this@apply)
        }
    }
}

suspend fun intro(player: Player, instance: Instance) {
    val lab = Cutscene(
        instance, false, listOf(
            CutscenePosition(Pos(-126.5, -42.5, -194.5, 100f, 10f), 0),
            CutscenePosition(Pos(-96.5, -43.5, -185.5, 100f, 4f), 20),
            CutscenePosition(Pos(-58.5, -43.5, -170.5, 180f, 4f), 30),
            CutscenePosition(Pos(-18.5, -39.5, -151.5, -120f, 24f), 20),
            CutscenePosition(Pos(-29.5, -39.5, -166.5, -130f, 30f), 20),
        ),
        listOf(
            CutsceneText("Scientists were experimenting with a form of dark matter!", Duration.ofMillis(3634)),
            CutsceneText("when suddenly the experiment went wrong!", Duration.ofMillis(5991 - 3634)),
            CutsceneText("There was a LEAK!", Duration.ofMillis(7233 - 5991)),
            CutsceneText("The dark matter started to spread!", Duration.ofMillis(8970 - 7233)),
            CutsceneText("And only you, can stop it!", Duration.ofMillis(10671 - 8970))
        )
    )

    lab.addViewer(player)
    player.playSound(
        Sound.sound(
            Key.key("jam:start_cutscene"),
            Sound.Source.VOICE,
            1f, 1f
        ), lab.camera
    )
    lab.start()
}